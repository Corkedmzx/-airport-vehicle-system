package com.airport.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调度任务DTO
 * 
 * @author Corkedmzx
 */
@Data
public class DispatchTaskDTO {

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务类型描述
     */
    private String taskTypeDesc;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 优先级描述
     */
    private String priorityDesc;

    /**
     * 描述
     */
    private String description;

    /**
     * 起始位置
     */
    private String startLocation;

    /**
     * 目标位置
     */
    private String endLocation;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 预计结束时间
     */
    private LocalDateTime endTime;

    /**
     * 分配车辆
     */
    private String assignedVehicle;

    /**
     * 分配司机
     */
    private String assignedDriver;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 完成进度(%)
     */
    private BigDecimal progress;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}