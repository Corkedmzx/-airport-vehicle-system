package com.airport.controller;

import com.airport.dto.Result;
import com.airport.dto.VehicleDTO;
import com.airport.dto.VehicleLocationDTO;
import com.airport.dto.VehicleStatistics;
import com.airport.entity.DispatchTask;
import com.airport.entity.SysUser;
import com.airport.entity.Vehicle;
import com.airport.repository.SysUserRepository;
import com.airport.service.DispatchTaskService;
import com.airport.service.MultiDeviceMqttService;
import com.airport.service.VehicleService;
import com.airport.websocket.VehicleLocationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.airport.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 车辆管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "车辆管理", description = "车辆信息管理相关接口")
public class VehicleController {

    private final VehicleService vehicleService;
    private final DispatchTaskService taskService;
    private final MultiDeviceMqttService multiDeviceMqttService;
    private final SysUserRepository userRepository;
    private final VehicleLocationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final JwtUtils jwtUtils;

    @Value("${huawei.iot.mqtt.instance-id:a494d922-ff97-4873-bd0c-2d6b1a72086d}")
    private String instanceId;

    /**
     * 从请求头中获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtils.validateToken(token, jwtUtils.getUsernameFromToken(token))) {
                    return jwtUtils.getUsernameFromToken(token);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
        }
        return null;
    }

    /**
     * 从请求头中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtUtils.getUserIdFromToken(token);
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
        }
        return null;
    }

    @GetMapping
    @Operation(summary = "获取车辆列表", description = "获取所有车辆信息")
    public Result<List<Vehicle>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return Result.success(vehicles);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取车辆详情", description = "根据ID获取车辆详细信息")
    public Result<Vehicle> getVehicleById(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id) {
        Optional<Vehicle> vehicle = vehicleService.getVehicleById(id);
        if (vehicle.isPresent()) {
            return Result.success(vehicle.get());
        } else {
            return Result.notFound("车辆不存在");
        }
    }

    @GetMapping("/by-number/{vehicleNo}")
    @Operation(summary = "根据车牌号获取车辆", description = "根据车牌号获取车辆信息")
    public Result<Vehicle> getVehicleByNo(
            @Parameter(description = "车牌号", required = true) 
            @PathVariable String vehicleNo) {
        Optional<Vehicle> vehicle = vehicleService.getVehicleByNo(vehicleNo);
        if (vehicle.isPresent()) {
            return Result.success(vehicle.get());
        } else {
            return Result.notFound("车辆不存在");
        }
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "根据状态获取车辆", description = "根据车辆状态获取车辆列表")
    public Result<List<Vehicle>> getVehiclesByStatus(
            @Parameter(description = "车辆状态:0-停用,1-正常,2-维修中,3-故障", required = true) 
            @PathVariable Integer status) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByStatus(status);
        return Result.success(vehicles);
    }

    @GetMapping("/active")
    @Operation(summary = "获取正常车辆", description = "获取所有正常状态的车辆")
    public Result<List<Vehicle>> getActiveVehicles() {
        List<Vehicle> vehicles = vehicleService.getActiveVehicles();
        return Result.success(vehicles);
    }

    @GetMapping("/by-type/{vehicleTypeId}")
    @Operation(summary = "根据类型获取车辆", description = "根据车辆类型获取车辆列表")
    public Result<List<Vehicle>> getVehiclesByType(
            @Parameter(description = "车辆类型ID", required = true) 
            @PathVariable Long vehicleTypeId) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByType(vehicleTypeId);
        return Result.success(vehicles);
    }

    @PostMapping
    @Operation(summary = "创建车辆", description = "创建新的车辆记录（需要vehicle:create权限）")
    public Result<Vehicle> createVehicle(@RequestBody Vehicle vehicle, HttpServletRequest request) {
        try {
            // 权限检查：需要vehicle:create权限
            String currentUsername = getCurrentUsername(request);
            if (currentUsername == null) {
                return Result.error("未认证或认证已过期");
            }
            // 这里可以添加更详细的权限检查，暂时允许所有认证用户创建车辆
            // 前端已经通过hasPermission进行了权限控制
            
            Vehicle createdVehicle = vehicleService.createVehicle(vehicle);
            return Result.success("车辆创建成功", createdVehicle);
        } catch (Exception e) {
            log.error("创建车辆失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新车辆", description = "更新车辆信息（需要vehicle:update权限）")
    public Result<Vehicle> updateVehicle(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id,
            @RequestBody Vehicle vehicle,
            HttpServletRequest request) {
        try {
            // 权限检查：需要vehicle:update权限
            String currentUsername = getCurrentUsername(request);
            if (currentUsername == null) {
                return Result.error("未认证或认证已过期");
            }
            // 前端已经通过hasPermission进行了权限控制
            
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle);
            return Result.success("车辆更新成功", updatedVehicle);
        } catch (Exception e) {
            log.error("更新车辆失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除车辆", description = "删除车辆记录（需要vehicle:delete权限）")
    public Result<String> deleteVehicle(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            // 权限检查：需要vehicle:delete权限
            String currentUsername = getCurrentUsername(request);
            if (currentUsername == null) {
                return Result.error("未认证或认证已过期");
            }
            // 前端已经通过hasPermission进行了权限控制
            
            vehicleService.deleteVehicle(id);
            return Result.success("车辆删除成功");
        } catch (Exception e) {
            log.error("删除车辆失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "更新车辆位置", description = "更新车辆实时位置信息")
    public Result<Vehicle> updateVehicleLocation(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id,
            @RequestBody VehicleLocationDTO locationDTO) {
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicleLocation(id, locationDTO);
            return Result.success("位置更新成功", updatedVehicle);
        } catch (Exception e) {
            log.error("更新车辆位置失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/upload-location")
    @Operation(summary = "上传用户位置（小程序使用）", description = "小程序使用：上传用户位置信息，用户在线即可上传，无需关联车辆")
    public Result<Map<String, Object>> uploadVehicleLocation(
            @Parameter(description = "经度", required = true)
            @RequestParam Double longitude,
            @Parameter(description = "纬度", required = true)
            @RequestParam Double latitude,
            @Parameter(description = "位置地址", required = false)
            @RequestParam(required = false) String address,
            @Parameter(description = "速度(km/h)", required = false)
            @RequestParam(required = false) Double speed,
            @Parameter(description = "方向角(度)", required = false)
            @RequestParam(required = false) Double direction,
            @Parameter(description = "精度(m)", required = false)
            @RequestParam(required = false) Double accuracy,
            HttpServletRequest request) {
        try {
            // 获取当前登录用户ID
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                log.warn("[小程序位置上传] 用户未认证或认证已过期");
                return Result.unauthorized("未认证或认证已过期");
            }
            log.info("[小程序位置上传] 收到位置上传请求: userId={}, longitude={}, latitude={}, accuracy={}", 
                    userId, longitude, latitude, accuracy);
            
            // 检查定位精度，如果精度过低（>1000米），可能是基站定位或模拟定位
            if (accuracy != null && accuracy > 1000) {
                log.warn("[小程序位置上传] ⚠️ 定位精度较低: {}米，可能是基站定位或模拟定位（PC上运行微信开发者工具时常见）", accuracy);
            } else if (accuracy != null && accuracy > 500) {
                log.warn("[小程序位置上传] ⚠️ 定位精度中等: {}米，建议在真实手机上运行以获取GPS定位", accuracy);
            }

            // 获取用户信息
            SysUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            String userName = user.getRealName() != null ? user.getRealName() : user.getUsername();
            
            // 构建位置数据（用于WebSocket推送和MQTT发布）
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("userId", userId);
            locationData.put("userName", userName);
            locationData.put("longitude", BigDecimal.valueOf(longitude));
            locationData.put("latitude", BigDecimal.valueOf(latitude));
            if (address != null) {
                locationData.put("address", address);
            }
            if (speed != null) {
                locationData.put("speed", BigDecimal.valueOf(speed));
            }
            if (direction != null) {
                locationData.put("direction", BigDecimal.valueOf(direction));
            }
            if (accuracy != null) {
                locationData.put("accuracy", BigDecimal.valueOf(accuracy));
            }
            locationData.put("timestamp", System.currentTimeMillis());
            locationData.put("source", "miniprogram");
            locationData.put("deviceName", "mobile_001");
            
            // 发布位置数据到华为云IoT平台（通过mobile_001设备）
            // 数据流：小程序位置 -> mobile_001 -> vehicle_001 -> web_001 -> 系统 -> WebSocket -> 前端地图
            try {
                publishUserLocationToHuaweiCloud(userId, userName, locationData);
            } catch (Exception e) {
                log.warn("[小程序位置上传] 发布位置数据到华为云IoT失败: {}", e.getMessage());
                // 即使MQTT发布失败，也通过WebSocket直接推送
            }
            
            // 直接通过WebSocket推送用户位置（不依赖MQTT，确保实时性）
            try {
                Map<String, Object> broadcastData = new HashMap<>();
                broadcastData.put("vehicleId", userId); // 使用userId作为标识（前端会识别为小程序位置）
                broadcastData.put("vehicleNo", userName); // 使用用户名作为显示名称
                // 直接使用Double类型，确保前端能正确解析（longitude和latitude已经是Double类型）
                broadcastData.put("longitude", longitude);
                broadcastData.put("latitude", latitude);
                broadcastData.put("address", address != null ? address : "");
                broadcastData.put("speed", speed);
                broadcastData.put("direction", direction);
                broadcastData.put("accuracy", accuracy);
                broadcastData.put("timestamp", System.currentTimeMillis());
                broadcastData.put("source", "miniprogram");
                broadcastData.put("deviceName", "mobile_001");
                broadcastData.put("userId", userId);
                broadcastData.put("userName", userName);
                
                // 直接通过WebSocket推送用户位置
                log.info("[小程序位置上传] 准备推送WebSocket消息，broadcastData: {}", broadcastData);
                webSocketHandler.broadcastVehicleLocationUpdate(userId, broadcastData);
                log.info("[小程序位置上传] 用户位置已通过WebSocket推送，用户: {}, 位置: ({}, {}), vehicleId: {}, userId: {}, source: miniprogram, deviceName: mobile_001", 
                        userName, latitude, longitude, userId, userId);
            } catch (Exception e) {
                log.error("[小程序位置上传] WebSocket推送用户位置失败", e);
            }
            
            log.info("[小程序位置上传] 用户 {} ({}) 上传位置成功: ({}, {})", 
                    userName, userId, longitude, latitude);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("userName", userName);
            result.put("longitude", longitude);
            result.put("latitude", latitude);
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.success("位置上传成功", result);
        } catch (Exception e) {
            log.error("[小程序位置上传] 上传用户位置失败", e);
            return Result.error("上传位置失败: " + e.getMessage());
        }
    }

    /**
     * 发布用户位置数据到华为云IoT平台
     * 数据流：小程序位置 -> mobile_001 -> vehicle_001 -> web_001 -> 系统 -> WebSocket -> 前端地图
     * 
     * 实现逻辑：
     * 1. 通过mobile_001设备发布位置数据到vehicle_001订阅的主题
     * 2. vehicle_001接收后转发到web_001的主题
     * 3. web_001接收后推送到系统，通过WebSocket发送到前端地图
     */
    private void publishUserLocationToHuaweiCloud(Long userId, String userName, Map<String, Object> locationData) {
        try {
            // 检查mobile_001设备是否已连接
            if (!multiDeviceMqttService.isDeviceConnected("mobile_001")) {
                log.warn("[小程序位置] mobile_001设备未连接，尝试连接设备");
                // 尝试连接设备
                multiDeviceMqttService.connectDevice("mobile_001");
                // 等待连接建立
                Thread.sleep(1000);
                
                if (!multiDeviceMqttService.isDeviceConnected("mobile_001")) {
                    log.error("[小程序位置] mobile_001设备连接失败，无法上传位置到华为云IoT");
                    return;
                }
            }

            // 构建位置数据（使用userId作为vehicleId标识，前端会识别为小程序位置）
            Map<String, Object> locationMessage = new HashMap<>();
            locationMessage.put("deviceId", "6961b5c87f2e6c302f48db15_mobile_001"); // mobile_001的设备ID
            locationMessage.put("deviceName", "mobile_001");
            locationMessage.put("vehicleId", userId); // 使用userId作为标识（前端会识别为小程序位置）
            locationMessage.put("vehicleNo", userName); // 使用用户名作为显示名称
            locationMessage.put("latitude", locationData.get("latitude"));
            locationMessage.put("longitude", locationData.get("longitude"));
            if (locationData.get("address") != null) {
                locationMessage.put("address", locationData.get("address"));
            }
            if (locationData.get("speed") != null) {
                locationMessage.put("speed", locationData.get("speed"));
            }
            if (locationData.get("direction") != null) {
                locationMessage.put("direction", locationData.get("direction"));
            }
            if (locationData.get("accuracy") != null) {
                locationMessage.put("accuracy", locationData.get("accuracy"));
            }
            locationMessage.put("timestamp", locationData.get("timestamp"));
            locationMessage.put("source", "miniprogram"); // 标识来源为小程序
            locationMessage.put("userId", userId); // 用户ID
            locationMessage.put("userName", userName); // 用户名

            // 构建主题：mobile_001 发布位置的主题（vehicle_001 会订阅此主题）
            String mobileDeviceId = multiDeviceMqttService.getLoadedDeviceId("mobile_001");
            if (mobileDeviceId == null || mobileDeviceId.isBlank()) {
                throw new RuntimeException("未加载 mobile_001 设备密钥，无法发布小程序位置");
            }
            String topic = String.format("/%s/%s/user/location", instanceId, mobileDeviceId);

            // 转换为JSON字符串
            String payload = objectMapper.writeValueAsString(locationMessage);

            // 通过mobile_001设备发布消息
            // vehicle_001会订阅mobile_001的主题，接收后转发到web_001
            multiDeviceMqttService.publishToDevice("mobile_001", topic, payload, 1);

            log.info("[小程序位置] 用户 {} ({}) 位置已通过mobile_001上传到华为云IoT，主题: {}, 位置: ({}, {})", 
                    userName, userId, topic, locationData.get("latitude"), locationData.get("longitude"));
        } catch (Exception e) {
            log.error("[小程序位置] 发布位置数据到华为云IoT失败", e);
            throw new RuntimeException("发布位置数据到华为云IoT失败: " + e.getMessage(), e);
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取车辆统计", description = "获取车辆统计信息")
    public Result<VehicleStatistics> getVehicleStatistics() {
        VehicleStatistics statistics = vehicleService.getVehicleStatistics();
        return Result.success(statistics);
    }
}