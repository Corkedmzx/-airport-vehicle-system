package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 告警实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "alert")
@EqualsAndHashCode(callSuper = true)
public class Alert extends BaseEntity {

    /**
     * 告警标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 告警描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 告警级别:high-高,medium-中,low-低
     */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /**
     * 告警类别:vehicle_fault-车辆故障,task_timeout-任务超时,system_error-系统错误,safety_alert-安全告警
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * 关联车辆ID
     */
    @Column(name = "vehicle_id")
    private Long vehicleId;

    /**
     * 关联任务ID
     */
    @Column(name = "task_id")
    private Long taskId;

    /**
     * 处理状态:unprocessed-未处理,processing-处理中,resolved-已解决
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "unprocessed";

    /**
     * 处理人
     */
    @Column(name = "assignee", length = 50)
    private String assignee;

    /**
     * 处理时间
     */
    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    /**
     * 处理说明
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * 是否已确认
     */
    @Column(name = "acknowledged", nullable = false)
    private Boolean acknowledged = false;

    /**
     * 确认时间
     */
    @Column(name = "acknowledged_time")
    private LocalDateTime acknowledgedTime;
}

