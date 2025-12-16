package com.airport.websocket;

import java.util.Map;

/**
 * 车辆位置服务接口
 * 用于处理传感器发送的位置数据
 * 
 * @author Corkedmzx
 */
public interface VehicleLocationService {
    
    /**
     * 处理位置更新
     * 
     * @param deviceId 设备ID
     * @param locationData 位置数据，包含：
     *                    - vehicleId: 车辆ID（可选）
     *                    - longitude: 经度
     *                    - latitude: 纬度
     *                    - address: 地址（可选）
     *                    - speed: 速度（可选）
     *                    - direction: 方向（可选）
     */
    void processLocationUpdate(String deviceId, Map<String, Object> locationData);
}

