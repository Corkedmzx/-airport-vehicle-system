package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆实体类
 * 
 * @author MiniMax Agent
 */
@Data
@Entity
@Table(name = "vehicle")
@EqualsAndHashCode(callSuper = true)
public class Vehicle extends BaseEntity {

    /**
     * 车牌号
     */
    @Column(name = "vehicle_no", nullable = false, unique = true, length = 20)
    private String vehicleNo;

    /**
     * 车辆类型ID
     */
    @Column(name = "vehicle_type_id", nullable = false)
    private Long vehicleTypeId;

    /**
     * 品牌
     */
    @Column(name = "brand", length = 50)
    private String brand;

    /**
     * 型号
     */
    @Column(name = "model", length = 50)
    private String model;

    /**
     * 颜色
     */
    @Column(name = "color", length = 20)
    private String color;

    /**
     * 发动机号
     */
    @Column(name = "engine_no", length = 50)
    private String engineNo;

    /**
     * 车架号
     */
    @Column(name = "vin", length = 50)
    private String vin;

    /**
     * 购买日期
     */
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /**
     * 里程数(km)
     */
    @Column(name = "mileage", precision = 10, scale = 2)
    private BigDecimal mileage = BigDecimal.ZERO;

    /**
     * 油箱容量(L)
     */
    @Column(name = "fuel_capacity", precision = 5, scale = 2)
    private BigDecimal fuelCapacity;

    /**
     * 当前油量(L)
     */
    @Column(name = "current_fuel", precision = 5, scale = 2)
    private BigDecimal currentFuel;

    /**
     * GPS设备ID
     */
    @Column(name = "gps_device_id", unique = true, length = 50)
    private String gpsDeviceId;

    /**
     * 状态:0-停用,1-正常,2-维修中,3-故障
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 当前位置经度
     */
    @Column(name = "location_longitude", precision = 10, scale = 7)
    private BigDecimal locationLongitude;

    /**
     * 当前位置纬度
     */
    @Column(name = "location_latitude", precision = 10, scale = 7)
    private BigDecimal locationLatitude;

    /**
     * 当前位置地址
     */
    @Column(name = "location_address", length = 255)
    private String locationAddress;

    /**
     * 最后位置更新时间
     */
    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;
}