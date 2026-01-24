package com.airport.service;

import com.airport.dto.DispatchTaskDTO;
import com.airport.dto.TaskStatistics;
import com.airport.entity.DispatchTask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 调度任务服务接口
 * 
 * @author Corkedmzx
 */
public interface DispatchTaskService {

    /**
     * 获取所有任务列表
     * 
     * @return 任务列表
     */
    List<DispatchTask> getAllTasks();

    /**
     * 根据ID获取任务
     * 
     * @param id 任务ID
     * @return 任务信息
     */
    Optional<DispatchTask> getTaskById(Long id);

    /**
     * 根据任务编号获取任务
     * 
     * @param taskNo 任务编号
     * @return 任务信息
     */
    Optional<DispatchTask> getTaskByNo(String taskNo);

    /**
     * 根据状态获取任务
     * 
     * @param status 任务状态
     * @return 任务列表
     */
    List<DispatchTask> getTasksByStatus(Integer status);

    /**
     * 根据车辆ID获取任务
     * 
     * @param vehicleId 车辆ID
     * @return 任务列表
     */
    List<DispatchTask> getTasksByVehicleId(Long vehicleId);
    
    /**
     * 查找用户正在执行的任务（状态为2-已分配或3-执行中）
     * 
     * @param userId 用户ID
     * @return 任务列表
     */
    List<DispatchTask> findActiveTasksByUserId(Long userId);

    /**
     * 创建任务
     * 
     * @param task 任务信息
     * @return 创建的任务
     */
    DispatchTask createTask(DispatchTask task);

    /**
     * 更新任务
     * 
     * @param id 任务ID
     * @param task 任务信息
     * @return 更新后的任务
     */
    DispatchTask updateTask(Long id, DispatchTask task);

    /**
     * 删除任务
     * 
     * @param id 任务ID
     */
    void deleteTask(Long id);

    /**
     * 分配任务给车辆
     * 
     * @param taskId 任务ID
     * @param vehicleId 车辆ID
     * @param driverId 司机ID
     * @param dispatcherId 调度员ID（可选，用于记录和通知）
     * @return 更新后的任务
     */
    DispatchTask assignTask(Long taskId, Long vehicleId, Long driverId, Long dispatcherId);

    /**
     * 分配任务给车辆和司机（通过用户名）
     * 
     * @param taskId 任务ID
     * @param vehicleId 车辆ID
     * @param driverUsername 司机用户名
     * @param dispatcherId 调度员ID（可选，用于记录和通知）
     * @return 更新后的任务
     */
    DispatchTask assignTaskWithDriver(Long taskId, Long vehicleId, String driverUsername, Long dispatcherId);

    /**
     * 分配任务给用户
     * 
     * @param taskId 任务ID
     * @param username 用户名
     * @return 更新后的任务
     */
    DispatchTask assignTaskToUser(Long taskId, String username);

    /**
     * 分配维护调度任务给维修员
     * 
     * @param taskId 任务ID
     * @param vehicleId 车辆ID（需要维修的车辆）
     * @param maintenanceId 维修员ID
     * @param dispatcherId 调度员ID（用于记录和通知）
     * @return 更新后的任务
     */
    DispatchTask assignTaskToMaintenance(Long taskId, Long vehicleId, Long maintenanceId, Long dispatcherId);

    /**
     * 分配维护调度任务给维修员（通过用户名）
     * 
     * @param taskId 任务ID
     * @param vehicleId 车辆ID（需要维修的车辆）
     * @param maintenanceUsername 维修员用户名
     * @param dispatcherUsername 调度员用户名
     * @return 更新后的任务
     */
    DispatchTask assignTaskToMaintenanceWithUsername(Long taskId, Long vehicleId, String maintenanceUsername, String dispatcherUsername);

    /**
     * 取消分配任务（将已分配的任务恢复为待分配状态）
     * 
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    DispatchTask unassignTask(Long taskId);

    /**
     * 开始执行任务
     * 
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    DispatchTask startTask(Long taskId);

    /**
     * 完成任务
     * 
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    DispatchTask completeTask(Long taskId);

    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @param reason 取消原因
     * @return 更新后的任务
     */
    DispatchTask cancelTask(Long taskId, String reason);

    /**
     * 获取待分配任务
     * 
     * @return 待分配任务列表
     */
    List<DispatchTask> getPendingTasks();

    /**
     * 获取进行中任务
     * 
     * @return 进行中任务列表
     */
    List<DispatchTask> getInProgressTasks();

    /**
     * 获取时间范围内的任务
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务列表
     */
    List<DispatchTask> getTasksByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取任务统计数据
     * 
     * @return 统计数据
     */
    TaskStatistics getTaskStatistics();

    /**
     * 重新发送已完成的任务（复制任务并生成新任务编号）
     * 
     * @param taskId 原任务ID
     * @return 新创建的任务
     */
    DispatchTask resendTask(Long taskId);

    /**
     * 获取当前用户的任务列表（包括分配给司机和维修员的任务）
     * 
     * @param userId 用户ID
     * @return 任务列表（只返回未完成的任务，状态不为4-已完成和5-已取消）
     */
    List<DispatchTask> getMyTasks(Long userId);
}