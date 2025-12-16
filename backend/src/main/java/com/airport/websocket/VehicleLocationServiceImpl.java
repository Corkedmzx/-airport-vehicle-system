package com.airport.websocket;

import com.airport.dto.VehicleLocationDTO;
import com.airport.entity.Vehicle;
import com.airport.repository.VehicleRepository;
import com.airport.service.VehicleService;
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
    public void processLocationUpdate(String deviceId, Map<String, Object> locationData) {
        try {
            // 根据设备ID查找车辆
            Vehicle vehicle = vehicleRepository.findByGpsDeviceId(deviceId)
                    .orElse(null);
            
            if (vehicle == null) {
                log.warn("未找到设备ID对应的车辆: {}", deviceId);
                return;
            }
            
            // 构建位置DTO
            VehicleLocationDTO locationDTO = new VehicleLocationDTO();
            Double longitude = getDoubleValue(locationData, "longitude");
            Double latitude = getDoubleValue(locationData, "latitude");
            locationDTO.setLongitude(longitude != null ? BigDecimal.valueOf(longitude) : null);
            locationDTO.setLatitude(latitude != null ? BigDecimal.valueOf(latitude) : null);
            locationDTO.setAddress(getStringValue(locationData, "address"));
            
            // 更新车辆位置
            Vehicle updatedVehicle = vehicleService.updateVehicleLocation(vehicle.getId(), locationDTO);
            
            // 通过WebSocket广播位置更新
            Map<String, Object> broadcastData = Map.of(
                "vehicleId", updatedVehicle.getId(),
                "vehicleNo", updatedVehicle.getVehicleNo(),
                "longitude", locationDTO.getLongitude(),
                "latitude", locationDTO.getLatitude(),
                "address", locationDTO.getAddress() != null ? locationDTO.getAddress() : "",
                "speed", getDoubleValue(locationData, "speed", 0.0),
                "direction", getDoubleValue(locationData, "direction", 0.0),
                "timestamp", System.currentTimeMillis()
            );
            
            webSocketHandler.broadcastVehicleLocationUpdate(updatedVehicle.getId(), broadcastData);
            
            log.debug("处理位置更新成功，车辆: {}, 设备ID: {}", updatedVehicle.getVehicleNo(), deviceId);
        } catch (Exception e) {
            log.error("处理位置更新失败，设备ID: {}", deviceId, e);
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

