package com.airport.service;

import com.airport.entity.Vehicle;
import com.airport.dto.VehicleDTO;
import com.airport.dto.VehicleLocationDTO;
import com.airport.dto.VehicleStatistics;

import java.util.List;
import java.util.Optional;

/**
 * 车辆服务接口
 * 
 * @author MiniMax Agent
 */
public interface VehicleService {

    /**
     * 获取所有车辆列表
     * 
     * @return 车辆列表
     */
    List<Vehicle> getAllVehicles();

    /**
     * 根据ID获取车辆
     * 
     * @param id 车辆ID
     * @return 车辆信息
     */
    Optional<Vehicle> getVehicleById(Long id);

    /**
     * 根据车牌号获取车辆
     * 
     * @param vehicleNo 车牌号
     * @return 车辆信息
     */
    Optional<Vehicle> getVehicleByNo(String vehicleNo);

    /**
     * 根据状态获取车辆
     * 
     * @param status 车辆状态
     * @return 车辆列表
     */
    List<Vehicle> getVehiclesByStatus(Integer status);

    /**
     * 创建车辆
     * 
     * @param vehicle 车辆信息
     * @return 创建的车辆
     */
    Vehicle createVehicle(Vehicle vehicle);

    /**
     * 更新车辆
     * 
     * @param id 车辆ID
     * @param vehicle 车辆信息
     * @return 更新后的车辆
     */
    Vehicle updateVehicle(Long id, Vehicle vehicle);

    /**
     * 删除车辆
     * 
     * @param id 车辆ID
     */
    void deleteVehicle(Long id);

    /**
     * 更新车辆位置
     * 
     * @param vehicleId 车辆ID
     * @param locationDTO 位置信息
     * @return 更新后的车辆
     */
    Vehicle updateVehicleLocation(Long vehicleId, VehicleLocationDTO locationDTO);

    /**
     * 获取正常状态的车辆
     * 
     * @return 正常车辆列表
     */
    List<Vehicle> getActiveVehicles();

    /**
     * 根据车辆类型获取车辆
     * 
     * @param vehicleTypeId 车辆类型ID
     * @return 车辆列表
     */
    List<Vehicle> getVehiclesByType(Long vehicleTypeId);

    /**
     * 获取车辆统计数据
     * 
     * @return 统计数据
     */
    VehicleStatistics getVehicleStatistics();
}