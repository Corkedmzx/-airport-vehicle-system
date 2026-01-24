package com.airport.repository;

import com.airport.entity.DispatchTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 调度任务数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface DispatchTaskRepository extends JpaRepository<DispatchTask, Long> {

    /**
     * 根据任务编号查找任务
     * 
     * @param taskNo 任务编号
     * @return 任务信息
     */
    DispatchTask findByTaskNo(String taskNo);

    /**
     * 根据分配车辆查找任务
     * 
     * @param vehicleId 车辆ID
     * @return 任务列表
     */
    List<DispatchTask> findByAssignedVehicleId(Long vehicleId);

    /**
     * 根据状态查找任务
     * 
     * @param status 任务状态
     * @return 任务列表
     */
    List<DispatchTask> findByStatus(Integer status);

    /**
     * 根据优先级查找任务
     * 
     * @param priority 优先级
     * @return 任务列表
     */
    List<DispatchTask> findByPriorityOrderByStartTimeAsc(Integer priority);

    /**
     * 查找待分配的任务
     * 
     * @return 待分配任务列表
     */
    @Query("SELECT t FROM DispatchTask t WHERE t.status = 1 ORDER BY t.priority DESC, t.startTime ASC")
    List<DispatchTask> findPendingTasks();

    /**
     * 查找进行中的任务
     * 
     * @return 进行中任务列表
     */
    @Query("SELECT t FROM DispatchTask t WHERE t.status = 3 ORDER BY t.actualStartTime ASC")
    List<DispatchTask> findInProgressTasks();

    /**
     * 查找时间范围内的任务
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务列表
     */
    @Query("SELECT t FROM DispatchTask t WHERE t.startTime BETWEEN :startTime AND :endTime " +
           "ORDER BY t.startTime ASC")
    List<DispatchTask> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查找司机的任务
     * 
     * @param driverId 司机ID
     * @return 任务列表
     */
    List<DispatchTask> findByAssignedDriverIdOrderByStartTimeDesc(Long driverId);
    
    /**
     * 根据分配的用户ID查找任务（按开始时间降序）
     * 
     * @param userId 用户ID
     * @return 任务列表
     */
    List<DispatchTask> findByAssignedUserIdOrderByStartTimeDesc(Long userId);
    
    /**
     * 查找用户正在执行的任务（状态为2-已分配或3-执行中）
     * 
     * @param driverId 司机ID
     * @return 任务列表
     */
    @Query("SELECT t FROM DispatchTask t WHERE (t.assignedDriverId = :userId OR t.assignedUserId = :userId) AND t.status IN (2, 3) ORDER BY t.actualStartTime DESC")
    List<DispatchTask> findActiveTasksByUserId(@Param("userId") Long userId);

    /**
     * 查找分配给指定用户的任务（包括司机任务和维修员任务）
     * 查询条件：assignedDriverId = userId OR assignedUserId = userId
     * 
     * @param userId 用户ID
     * @return 任务列表（按开始时间降序）
     */
    @Query("SELECT t FROM DispatchTask t WHERE (t.assignedDriverId = :userId OR t.assignedUserId = :userId) " +
           "AND t.status != 4 AND t.status != 5 ORDER BY t.startTime DESC")
    List<DispatchTask> findMyTasks(@Param("userId") Long userId);

    /**
     * 统计各状态任务数量
     * 
     * @return 状态统计
     */
    @Query("SELECT t.status, COUNT(t) FROM DispatchTask t GROUP BY t.status")
    List<Object[]> countTasksByStatus();

    /**
     * 查找超时的任务
     * 
     * @param currentTime 当前时间
     * @return 超时任务列表
     */
    @Query("SELECT t FROM DispatchTask t WHERE t.status IN (2, 3) AND t.endTime < :currentTime")
    List<DispatchTask> findOverdueTasks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 统计今日任务
     * 
     * @param startOfDay 一天开始时间
     * @param endOfDay 一天结束时间
     * @return 任务数量
     */
    @Query("SELECT COUNT(t) FROM DispatchTask t WHERE t.createTime BETWEEN :startOfDay AND :endOfDay")
    Long countTodayTasks(@Param("startOfDay") LocalDateTime startOfDay, 
                        @Param("endOfDay") LocalDateTime endOfDay);
}