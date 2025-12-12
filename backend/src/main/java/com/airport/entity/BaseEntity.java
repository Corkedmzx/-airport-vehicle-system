package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * 
 * @author Corkedmzx
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of = {"id"})
public abstract class BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}