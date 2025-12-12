package com.airport.dto;

import lombok.Data;

/**
 * 车辆信息DTO
 * 
 * @author MiniMax Agent
 */
@Data
public class VehicleDTO {

    /**
     * 车辆ID
     */
    private Long id;

    /**
     * 车牌号
     */
    private String vehicleNo;

    /**
     * 车辆类型
     */
    private String vehicleType;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 型号
     */
    private String model;

    /**
     * 颜色
     */
    private String color;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 当前位置
     */
    private String currentLocation;

    /**
     * 最后更新时间
     */
    private String lastUpdateTime;

    /**
     * GPS设备ID
     */
    private String gpsDeviceId;
}