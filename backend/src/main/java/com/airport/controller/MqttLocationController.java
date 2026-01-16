package com.airport.controller;

import com.airport.dto.Result;
import com.airport.service.MultiDeviceMqttService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT位置上传控制器
 * 
 * 用于接收PC位置信息并上传到华为云IoT平台
 * 数据流：PC位置 -> vehicle_001 (模拟mobile_001) -> web_001 -> 系统
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/api/mqtt")
@RequiredArgsConstructor
@Tag(name = "MQTT位置上传", description = "PC位置信息上传到华为云IoT平台")
public class MqttLocationController {

    private final MultiDeviceMqttService multiDeviceMqttService;
    private final ObjectMapper objectMapper;

    @Value("${huawei.iot.mqtt.instance-id:a494d922-ff97-4873-bd0c-2d6b1a72086d}")
    private String instanceId;

    /**
     * 上传PC位置信息
     * 
     * 数据流：
     * 1. PC位置 -> vehicle_001设备发布到web_001的主题（直接转发）
     * 2. web_001接收后推送到系统，通过WebSocket发送到前端
     * 
     * 注意：为了简化流程，PC位置直接通过vehicle_001发布到web_001的主题，
     * 这样web_001可以直接接收并处理，无需vehicle_001的转发逻辑
     */
    @PostMapping("/upload-pc-location")
    @Operation(summary = "上传PC位置信息", description = "将PC位置信息上传到华为云IoT平台，通过vehicle_001设备发布到web_001主题")
    public Result<String> uploadPCLocation(@RequestBody Map<String, Object> locationData) {
        try {
            log.info("[PC位置] 收到PC位置上传统计请求: {}", locationData);
            
            // 检查vehicle_001设备是否已连接
            if (!multiDeviceMqttService.isDeviceConnected("vehicle_001")) {
                log.error("[PC位置] vehicle_001设备未连接，无法上传PC位置信息");
                return Result.error("vehicle_001设备未连接，请检查MQTT服务状态");
            }
            
            log.debug("[PC位置] vehicle_001设备连接状态正常");

            // 提取位置信息
            Double latitude = getDoubleValue(locationData, "latitude");
            Double longitude = getDoubleValue(locationData, "longitude");
            Double accuracy = getDoubleValue(locationData, "accuracy");
            Long timestamp = getLongValue(locationData, "timestamp");

            if (latitude == null || longitude == null) {
                return Result.error("位置信息不完整：缺少latitude或longitude");
            }

            // 构建位置数据
            Map<String, Object> locationMessage = new HashMap<>();
            locationMessage.put("deviceId", "6961b5c87f2e6c302f48db15_web_001"); // web_001的设备ID
            locationMessage.put("deviceName", "pc_location"); // 标识为PC位置
            locationMessage.put("vehicleId", null); // PC位置没有关联车辆
            locationMessage.put("vehicleNo", "PC位置"); // 显示名称
            locationMessage.put("latitude", latitude);
            locationMessage.put("longitude", longitude);
            if (accuracy != null) {
                locationMessage.put("accuracy", accuracy);
            }
            if (timestamp != null) {
                locationMessage.put("timestamp", timestamp);
            } else {
                locationMessage.put("timestamp", System.currentTimeMillis());
            }
            locationMessage.put("source", "pc_browser"); // 标识来源为PC浏览器

            // 构建主题：web_001订阅的主题
            // 格式：/{instanceId}/{web_001_deviceId}/user/location
            String topic = String.format("/%s/%s/user/location", 
                    instanceId, "6961b5c87f2e6c302f48db15_web_001");

            // 转换为JSON字符串
            String payload = objectMapper.writeValueAsString(locationMessage);

            // 通过vehicle_001设备发布消息到web_001的主题
            // web_001会接收并推送到系统
            multiDeviceMqttService.publishToDevice("vehicle_001", topic, payload, 1);

            log.info("[PC位置] PC位置信息已通过vehicle_001上传到web_001，主题: {}, 位置: ({}, {}), 精度: {}米", 
                    topic, latitude, longitude, accuracy != null ? accuracy : "未知");

            return Result.success("PC位置信息上传成功");
        } catch (Exception e) {
            log.error("上传PC位置信息失败", e);
            return Result.error("上传PC位置信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取double值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取long值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
