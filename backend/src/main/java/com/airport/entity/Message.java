package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内信实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "message")
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseEntity {

    /**
     * 接收用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 消息标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 消息内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 消息类型:task_assignment-任务分配,task_completion-任务完成,alert-告警,maintenance-维修,system-系统通知
     */
    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;

    /**
     * 消息类别（与邮件类型对应）
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 是否已读:0-未读,1-已读
     */
    @Column(name = "`read`", nullable = false)
    private Boolean read = false;

    /**
     * 阅读时间
     */
    @Column(name = "read_time")
    private LocalDateTime readTime;

    /**
     * 关联ID（如任务ID、告警ID等）
     */
    @Column(name = "related_id")
    private Long relatedId;

    /**
     * 关联类型:task,alert,vehicle等
     */
    @Column(name = "related_type", length = 50)
    private String relatedType;

    /**
     * 优先级:high-高,medium-中,low-低,normal-普通
     */
    @Column(name = "priority", length = 20)
    private String priority = "normal";

    /**
     * 扩展数据(JSON格式)
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
