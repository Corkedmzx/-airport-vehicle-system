package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调度任务实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "dispatch_task")
@EqualsAndHashCode(callSuper = true)
public class DispatchTask extends BaseEntity {

    /**
     * 任务编号
     */
    @Column(name = "task_no", nullable = false, unique = true, length = 20)
    private String taskNo;

    /**
     * 任务名称
     */
    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    /**
     * 任务类型:常规调度,紧急调度,维护调度
     */
    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType;

    /**
     * 优先级:1-低,2-中,3-高,4-紧急
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 2;

    /**
     * 任务描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 起始位置
     */
    @Column(name = "start_location", nullable = false, length = 200)
    private String startLocation;

    /**
     * 目标位置
     */
    @Column(name = "end_location", nullable = false, length = 200)
    private String endLocation;

    /**
     * 开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 预计结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 实际开始时间
     */
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间
     */
    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    /**
     * 分配车辆ID
     */
    @Column(name = "assigned_vehicle_id")
    private Long assignedVehicleId;

    /**
     * 分配司机ID
     */
    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    /**
     * 状态:1-待分配,2-已分配,3-执行中,4-已完成,5-已取消,6-异常
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 完成进度(%)
     */
    @Column(name = "progress", precision = 5, scale = 2)
    private BigDecimal progress = BigDecimal.ZERO;

    /**
     * 备注
     */
    @Column(name = "remark")
    private String remark;
}