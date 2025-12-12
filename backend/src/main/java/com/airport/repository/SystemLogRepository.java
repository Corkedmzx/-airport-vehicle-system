package com.airport.repository;

import com.airport.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 系统日志数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    /**
     * 根据日志级别统计数量
     * 
     * @param level 日志级别
     * @return 数量
     */
    long countByLevel(String level);

    /**
     * 根据时间范围统计数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 数量
     */
    long countByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}

