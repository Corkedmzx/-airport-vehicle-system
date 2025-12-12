package com.airport.repository;

import com.airport.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 车辆数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * 根据车牌号查找车辆
     * 
     * @param vehicleNo 车牌号
     * @return 车辆信息
     */
    Optional<Vehicle> findByVehicleNo(String vehicleNo);

    /**
     * 根据GPS设备ID查找车辆
     * 
     * @param gpsDeviceId GPS设备ID
     * @return 车辆信息
     */
    Optional<Vehicle> findByGpsDeviceId(String gpsDeviceId);

    /**
     * 根据车辆类型查找车辆
     * 
     * @param vehicleTypeId 车辆类型ID
     * @return 车辆列表
     */
    List<Vehicle> findByVehicleTypeId(Long vehicleTypeId);

    /**
     * 根据状态查找车辆
     * 
     * @param status 车辆状态
     * @return 车辆列表
     */
    List<Vehicle> findByStatus(Integer status);

    /**
     * 查找正常状态的车辆
     * 
     * @return 正常车辆列表
     */
    @Query("SELECT v FROM Vehicle v WHERE v.status = 1")
    List<Vehicle> findActiveVehicles();

    /**
     * 根据位置范围查找车辆
     * 
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @return 车辆列表
     */
    @Query("SELECT v FROM Vehicle v WHERE v.locationLongitude BETWEEN :minLng AND :maxLng " +
           "AND v.locationLatitude BETWEEN :minLat AND :maxLat")
    List<Vehicle> findByLocationRange(@Param("minLng") Double minLng, 
                                     @Param("maxLng") Double maxLng,
                                     @Param("minLat") Double minLat, 
                                     @Param("maxLat") Double maxLat);

    /**
     * 查找最近更新的车辆
     * 
     * @param sinceTime 从此时间开始
     * @return 车辆列表
     */
    @Query("SELECT v FROM Vehicle v WHERE v.lastUpdateTime > :sinceTime ORDER BY v.lastUpdateTime DESC")
    List<Vehicle> findRecentlyUpdated(@Param("sinceTime") LocalDateTime sinceTime);

    /**
     * 根据状态和类型查找车辆
     * 
     * @param status 状态
     * @param vehicleTypeId 车辆类型ID
     * @return 车辆列表
     */
    List<Vehicle> findByStatusAndVehicleTypeId(Integer status, Long vehicleTypeId);

    /**
     * 统计各状态车辆数量
     * 
     * @return 状态统计
     */
    @Query("SELECT v.status, COUNT(v) FROM Vehicle v GROUP BY v.status")
    List<Object[]> countVehiclesByStatus();
}