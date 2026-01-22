package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 车辆报告实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "vehicle_report")
@EqualsAndHashCode(callSuper = true)
public class VehicleReport extends BaseEntity {

    /**
     * 车辆ID
     */
    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    /**
     * 报告人ID（司机）
     */
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    /**
     * 报告类型:fault-故障,maintenance-维修需求,other-其他
     */
    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    /**
     * 报告标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 问题描述
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * 严重程度:low-低,medium-中,high-高,urgent-紧急
     */
    @Column(name = "severity", length = 20)
    private String severity = "medium";

    /**
     * 处理状态:pending-待处理,processing-处理中,resolved-已解决,closed-已关闭
     */
    @Column(name = "status", length = 20)
    private String status = "pending";

    /**
     * 处理人ID
     */
    @Column(name = "handler_id")
    private Long handlerId;

    /**
     * 处理备注
     */
    @Column(name = "handler_notes", columnDefinition = "TEXT")
    private String handlerNotes;

    /**
     * 解决时间
     */
    @Column(name = "resolve_time")
    private LocalDateTime resolveTime;
}
