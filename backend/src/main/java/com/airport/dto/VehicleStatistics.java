package com.airport.dto;

import lombok.Data;

/**
 * 车辆统计数据
 * 
 * @author Corkedmzx
 */
@Data
public class VehicleStatistics {
    
    /**
     * 车辆总数
     */
    private Long totalCount;
    
    /**
     * 正常车辆数
     */
    private Long activeCount;
    
    /**
     * 维修中车辆数
     */
    private Long maintenanceCount;
    
    /**
     * 故障车辆数
     */
    private Long faultCount;
    
    /**
     * 停用车辆数
     */
    private Long offlineCount;
}