package com.airport.repository;

import com.airport.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 根据级别查找告警
     */
    List<Alert> findBySeverity(String severity);

    /**
     * 根据状态查找告警
     */
    List<Alert> findByStatus(String status);

    /**
     * 根据车辆ID查找告警
     */
    List<Alert> findByVehicleId(Long vehicleId);

    /**
     * 根据时间范围查找告警
     */
    @Query("SELECT a FROM Alert a WHERE a.createTime BETWEEN :startTime AND :endTime ORDER BY a.createTime DESC")
    List<Alert> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计今日告警
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE DATE(a.createTime) = CURRENT_DATE")
    Long countTodayAlerts();

    /**
     * 统计各级别告警数量
     */
    @Query("SELECT a.severity, COUNT(a) FROM Alert a GROUP BY a.severity")
    List<Object[]> countBySeverity();

    /**
     * 统计各状态告警数量
     */
    @Query("SELECT a.status, COUNT(a) FROM Alert a GROUP BY a.status")
    List<Object[]> countByStatus();
}

