package com.airport.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 华为云IoT设备配置读取器
 * 自动从设备密钥文件中读取设备信息
 * 
 * @author Corkedmzx
 */
@Slf4j
@Component
public class HuaweiIotDeviceConfig {

    @Value("${huawei.iot.mqtt.device-key-path:e:/share/设备密钥}")
    private String deviceKeyPath;

    @Value("${huawei.iot.mqtt.device-name:vehicle_001}")
    private String deviceName;

    private DeviceInfo cachedDeviceInfo;

    /**
     * 设备信息
     */
    @Data
    public static class DeviceInfo {
        @JsonProperty("device_id")
        private String deviceId;

        @JsonProperty("secret")
        private String secret;
    }

    /**
     * 获取设备信息
     * 优先从环境变量读取，如果未设置则从文件读取
     * 
     * @return 设备信息
     */
    public DeviceInfo getDeviceInfo() {
        // 如果已缓存，直接返回
        if (cachedDeviceInfo != null) {
            return cachedDeviceInfo;
        }

        // 优先从环境变量读取
        String envDeviceId = System.getenv("HUAWEI_IOT_MQTT_DEVICE_ID");
        String envDeviceSecret = System.getenv("HUAWEI_IOT_MQTT_DEVICE_SECRET");

        if (envDeviceId != null && !envDeviceId.isEmpty() &&
            envDeviceSecret != null && !envDeviceSecret.isEmpty()) {
            log.info("从环境变量读取设备信息，Device ID: {}", envDeviceId);
            cachedDeviceInfo = new DeviceInfo();
            cachedDeviceInfo.setDeviceId(envDeviceId);
            cachedDeviceInfo.setSecret(envDeviceSecret);
            return cachedDeviceInfo;
        }

        // 从文件读取
        try {
            cachedDeviceInfo = loadDeviceInfoFromFile();
            if (cachedDeviceInfo != null) {
                log.info("从文件读取设备信息成功，Device ID: {}", cachedDeviceInfo.getDeviceId());
            }
            return cachedDeviceInfo;
        } catch (Exception e) {
            log.error("读取设备信息失败", e);
            return null;
        }
    }

    /**
     * 从文件加载设备信息
     * 
     * @return 设备信息
     */
    private DeviceInfo loadDeviceInfoFromFile() throws IOException {
        // 构建文件路径
        // 文件名格式: DEVICES-KEY-{device_id}.txt
        // 例如: DEVICES-KEY-{productId}_vehicle_001.txt
        
        // 先尝试根据deviceName查找文件
        String fileName = String.format("DEVICES-KEY-*_%s.txt", deviceName);
        Path deviceKeyDir = Paths.get(deviceKeyPath);
        
        if (!Files.exists(deviceKeyDir)) {
            log.warn("设备密钥目录不存在: {}", deviceKeyPath);
            return null;
        }

        // 查找匹配的文件
        File[] files = deviceKeyDir.toFile().listFiles((dir, name) -> 
            name.startsWith("DEVICES-KEY-") && name.endsWith(".txt") && name.contains(deviceName));
        
        if (files == null || files.length == 0) {
            log.warn("未找到设备密钥文件，目录: {}, 设备名: {}", deviceKeyPath, deviceName);
            return null;
        }

        // 读取第一个匹配的文件
        File deviceFile = files[0];
        log.info("读取设备密钥文件: {}", deviceFile.getAbsolutePath());

        // 读取JSON内容
        String jsonContent = Files.readString(deviceFile.toPath());
        
        // 解析JSON
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
        
        DeviceInfo deviceInfo = objectMapper.readValue(jsonContent, DeviceInfo.class);
        
        if (deviceInfo.getDeviceId() == null || deviceInfo.getSecret() == null) {
            log.error("设备密钥文件格式错误，缺少device_id或secret: {}", deviceFile.getAbsolutePath());
            return null;
        }

        return deviceInfo;
    }

    /**
     * 清除缓存（用于重新加载配置）
     */
    public void clearCache() {
        cachedDeviceInfo = null;
    }
}
