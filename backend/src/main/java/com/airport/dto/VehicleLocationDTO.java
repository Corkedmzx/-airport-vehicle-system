package com.airport.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆位置信息DTO
 * 
 * @author MiniMax Agent
 */
@Data
public class VehicleLocationDTO {

    /**
     * 车辆ID
     */
    private Long vehicleId;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 海拔(m)
     */
    private BigDecimal altitude;

    /**
     * 速度(km/h)
     */
    private BigDecimal speed;

    /**
     * 方向角(度)
     */
    private BigDecimal direction;

    /**
     * 精度(m)
     */
    private BigDecimal accuracy;

    /**
     * 定位时间
     */
    private LocalDateTime locationTime;

    /**
     * 位置地址
     */
    private String address;
}