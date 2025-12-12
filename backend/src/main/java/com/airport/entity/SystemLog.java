package com.airport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

// backend/src/main/java/com/airport/entity/SystemLog.java
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "system_log")
public class SystemLog extends BaseEntity {
    
    @Column(name = "level", nullable = false)
    private String level; // ERROR, WARN, INFO, DEBUG
    
    @Column(name = "category", nullable = false)
    private String category; // OPERATION, SYSTEM, ERROR, AUDIT
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "module")
    private String module;
    
    @Column(name = "request_url")
    private String requestUrl;
    
    @Column(name = "request_method")
    private String requestMethod;
    
    @Column(name = "execution_time")
    private Long executionTime;
    
    @Column(name = "exception", columnDefinition = "TEXT")
    private String exception;
}