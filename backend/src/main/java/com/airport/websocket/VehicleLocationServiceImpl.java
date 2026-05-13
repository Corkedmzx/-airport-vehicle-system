package com.airport.websocket;

import com.airport.dto.VehicleLocationDTO;
import com.airport.entity.Vehicle;
import com.airport.repository.VehicleRepository;
import com.airport.service.VehicleService;
import com.airport.utils.ChinaCoordinateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 车辆位置服务实现
 * 处理传感器发送的实时定位数据
 * 
 * @author Corkedmzx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleLocationServiceImpl implements VehicleLocationService {

    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final VehicleLocationWebSocketHandler webSocketHandler;

    @Override
    public boolean processLocationUpdate(String deviceId, Map<String, Object> locationData) {
        try {
            // 检查是否是PC位置（通过source或deviceName字段判断）
            String source = getStringValue(locationData, "source");
            String deviceName = getStringValue(locationData, "deviceName");
            boolean isPCLocation = "pc_browser".equals(source) || "pc_location".equals(deviceName);
            
            if (isPCLocation) {
                // 处理PC位置（不关联车辆，直接通过WebSocket推送）
                Double longitude = getDoubleValue(locationData, "longitude");
                Double latitude = getDoubleValue(locationData, "latitude");
                
                if (longitude == null || latitude == null) {
                    log.warn("PC位置数据不完整，缺少longitude或latitude");
                    return false;
                }
                
                // 构建PC位置广播数据
                Map<String, Object> broadcastData = new java.util.HashMap<>();
                broadcastData.put("vehicleId", null); // PC位置没有车辆ID
                broadcastData.put("vehicleNo", getStringValue(locationData, "vehicleNo")); // 使用vehicleNo字段（"PC位置"）
                broadcastData.put("longitude", longitude);
                broadcastData.put("latitude", latitude);
                broadcastData.put("address", getStringValue(locationData, "address"));
                broadcastData.put("speed", getDoubleValue(locationData, "speed", 0.0));
                broadcastData.put("direction", getDoubleValue(locationData, "direction", 0.0));
                broadcastData.put("accuracy", getDoubleValue(locationData, "accuracy"));
                broadcastData.put("timestamp", getLongValue(locationData, "timestamp", System.currentTimeMillis()));
                broadcastData.put("source", "pc_browser");
                broadcastData.put("deviceName", "pc_location");
                
                // 通过WebSocket广播PC位置更新（使用deviceId作为标识）
                webSocketHandler.broadcastVehicleLocationUpdate(null, broadcastData);
                
                log.info("[PC位置] 处理PC位置更新成功，设备ID: {}, 位置: ({}, {}), 精度: {}米, 已通过WebSocket推送到前端", 
                        deviceId, latitude, longitude, getDoubleValue(locationData, "accuracy", 0.0));
                return true;
            }
            
            // 检查是否是小程序位置（通过source字段判断）
            // 注意：source变量已在上面定义，这里直接使用
            boolean isMiniprogramLocation = "miniprogram".equals(source);
            
            if (isMiniprogramLocation) {
                // 处理小程序位置（用户位置，不关联车辆，直接推送）
                Long userId = getLongValue(locationData, "userId");
                String userName = getStringValue(locationData, "userName");
                Double longitude = getDoubleValue(locationData, "longitude");
                Double latitude = getDoubleValue(locationData, "latitude");
                
                if (longitude == null || latitude == null) {
                    log.warn("小程序位置数据不完整，缺少longitude或latitude");
                    return false;
                }
                
                // 构建小程序位置广播数据（不更新车辆，直接推送用户位置）
                Map<String, Object> broadcastData = new java.util.HashMap<>();
                broadcastData.put("vehicleId", userId); // 使用userId作为标识（前端会识别为小程序位置）
                broadcastData.put("vehicleNo", userName != null ? userName : ("用户" + userId)); // 使用用户名作为显示名称
                broadcastData.put("longitude", longitude);
                broadcastData.put("latitude", latitude);
                broadcastData.put("address", getStringValue(locationData, "address"));
                broadcastData.put("speed", getDoubleValue(locationData, "speed"));
                broadcastData.put("direction", getDoubleValue(locationData, "direction"));
                broadcastData.put("accuracy", getDoubleValue(locationData, "accuracy"));
                broadcastData.put("timestamp", getLongValue(locationData, "timestamp", System.currentTimeMillis()));
                broadcastData.put("source", "miniprogram");
                broadcastData.put("deviceName", "mobile_001");
                broadcastData.put("userId", userId);
                broadcastData.put("userName", userName);
                
                // 通过WebSocket广播小程序位置更新（使用userId作为标识）
                webSocketHandler.broadcastVehicleLocationUpdate(userId, broadcastData);
                
                log.info("[小程序位置] 处理小程序位置更新成功，用户: {} ({}), 位置: ({}, {}), 已通过WebSocket推送到前端", 
                        userName != null ? userName : ("用户" + userId), userId, latitude, longitude);
                return true;
            }
            
            // 处理车辆位置更新（通过设备ID查找车辆）
            Vehicle vehicle = vehicleRepository.findByGpsDeviceId(deviceId)
                    .orElse(null);
            if (vehicle == null && deviceId != null && deviceId.contains("_vehicle_")) {
                String typoId = deviceId.replace("_vehicle_", "_vihecle_");
                vehicle = vehicleRepository.findByGpsDeviceId(typoId).orElse(null);
                if (vehicle != null) {
                    log.warn("库中 gps_device_id 为笔误 {}，已按 MQTT 设备 {} 匹配到车辆 id={}；请将 gps_device_id 更新为与华为控制台一致的正确字符串。",
                            typoId, deviceId, vehicle.getId());
                }
            }

            if (vehicle == null) {
                log.warn("未找到设备ID对应的车辆: {}（请在 vehicle 表将某车的 gps_device_id 设为与华为 IoT 设备 ID 完全一致，含 productId 前缀）", deviceId);
                return false;
            }
            /*
             * NMEA/华为直连上报通常为 WGS84；百度地图需 BD09。
             * 若载荷已标明 BD09/GCJ02 则不再按 WGS84 转换（便于仿真或非 GPS 源）。
             */
            VehicleLocationDTO locationDTO = new VehicleLocationDTO();
            Double longitude = getDoubleValue(locationData, "longitude");
            Double latitude = getDoubleValue(locationData, "latitude");
            String coordSys = getStringValue(locationData, "coordinateSystem");
            boolean explicitBd09 = "BD09".equalsIgnoreCase(coordSys);
            boolean explicitGcj02 = "GCJ02".equalsIgnoreCase(coordSys);
            if (longitude != null && latitude != null) {
                if (explicitBd09) {
                    locationDTO.setLongitude(BigDecimal.valueOf(longitude));
                    locationDTO.setLatitude(BigDecimal.valueOf(latitude));
                } else if (explicitGcj02) {
                    double[] bd = ChinaCoordinateUtils.gcj02ToBd09(longitude, latitude);
                    locationDTO.setLongitude(BigDecimal.valueOf(bd[0]));
                    locationDTO.setLatitude(BigDecimal.valueOf(bd[1]));
                } else {
                    double[] bd09 = ChinaCoordinateUtils.wgs84ToBd09(longitude, latitude);
                    locationDTO.setLongitude(BigDecimal.valueOf(bd09[0]));
                    locationDTO.setLatitude(BigDecimal.valueOf(bd09[1]));
                }
            }
            locationDTO.setAddress(getStringValue(locationData, "address"));
            
            Vehicle updatedVehicle = vehicleService.updateVehicleLocation(vehicle.getId(), locationDTO);
            
            log.debug("处理位置更新成功，车辆: {}, 设备ID: {}", updatedVehicle.getVehicleNo(), deviceId);
            return true;
        } catch (Exception e) {
            log.error("处理位置更新失败，设备ID: {}", deviceId, e);
            return false;
        }
    }
    
    /**
     * 获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Long值（带默认值）
     */
    private Long getLongValue(Map<String, Object> data, String key, Long defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取Double值
     */
    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Double值（带默认值）
     */
    private Double getDoubleValue(Map<String, Object> data, String key, Double defaultValue) {
        Double value = getDoubleValue(data, key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取String值
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
}

