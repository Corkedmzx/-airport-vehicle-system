package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警规则实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "alert_rule")
@EqualsAndHashCode(callSuper = true)
public class AlertRule extends BaseEntity {

    /**
     * 规则名称
     */
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /**
     * 规则类型:vehicle_fault-车辆故障,task_timeout-任务超时,system_error-系统错误,
     * safety_alert-安全告警,fuel_low-油量低,speed_exceed-速度超限
     */
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    /**
     * 条件类型:大于,小于,等于,范围
     */
    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    /**
     * 条件值
     */
    @Column(name = "condition_value", nullable = false, length = 200)
    private String conditionValue;

    /**
     * 告警严重程度:high-高,medium-中,low-低
     */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /**
     * 是否启用:0-禁用,1-启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 规则描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

