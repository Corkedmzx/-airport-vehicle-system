package com.airport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 机场车辆监控与调度系统 - 主应用程序
 * 
 * @author Corkedmzx
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
public class AirportVehicleSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirportVehicleSystemApplication.class, args);
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("""
                
                ========================================
                   机场车辆监控与调度系统启动成功！
                ========================================
                
                🌟 系统特色：
                • Java 17 + SpringBoot 3.2
                • MySQL 8.0 + Redis 缓存
                • JWT 认证 + Spring Security
                • WebSocket 实时监控
                • RESTful API 设计
                
                📊 管理端点：
                • 健康检查: http://localhost:8080/api/actuator/health
                • API文档: http://localhost:8080/api/doc.html
                • 监控面板: http://localhost:8080/api/druid/
                
                🕒 启动时间: {} 

                """.formatted(currentTime));
    }
}