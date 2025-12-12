package com.airport.service.impl;

import com.airport.dto.TaskStatistics;
import com.airport.entity.DispatchTask;
import com.airport.repository.DispatchTaskRepository;
import com.airport.service.DispatchTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 调度任务服务实现
 * 
 * @author Corkedmzx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DispatchTaskServiceImpl implements DispatchTaskService {

    private final DispatchTaskRepository taskRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DispatchTask> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DispatchTask> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DispatchTask> getTaskByNo(String taskNo) {
        DispatchTask task = taskRepository.findByTaskNo(taskNo);
        return Optional.ofNullable(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchTask> getTasksByStatus(Integer status) {
        return taskRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchTask> getTasksByVehicleId(Long vehicleId) {
        return taskRepository.findByAssignedVehicleId(vehicleId);
    }

    @Override
    public DispatchTask createTask(DispatchTask task) {
        // 生成任务编号
        if (task.getTaskNo() == null || task.getTaskNo().trim().isEmpty()) {
            String taskNo = generateTaskNo();
            task.setTaskNo(taskNo);
        }

        // 检查任务编号是否已存在
        if (taskRepository.findByTaskNo(task.getTaskNo()) != null) {
            throw new RuntimeException("任务编号已存在");
        }

        // 设置默认值
        if (task.getStatus() == null) {
            task.setStatus(1); // 待分配
        }
        if (task.getPriority() == null) {
            task.setPriority(2); // 中等优先级
        }
        if (task.getProgress() == null) {
            task.setProgress(java.math.BigDecimal.ZERO);
        }

        return taskRepository.save(task);
    }

    @Override
    public DispatchTask updateTask(Long id, DispatchTask task) {
        DispatchTask existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        // 更新字段
        existingTask.setTaskName(task.getTaskName());
        existingTask.setTaskType(task.getTaskType());
        existingTask.setPriority(task.getPriority());
        existingTask.setDescription(task.getDescription());
        existingTask.setStartLocation(task.getStartLocation());
        existingTask.setEndLocation(task.getEndLocation());
        existingTask.setStartTime(task.getStartTime());
        existingTask.setEndTime(task.getEndTime());
        existingTask.setStatus(task.getStatus());
        existingTask.setProgress(task.getProgress());
        existingTask.setRemark(task.getRemark());

        return taskRepository.save(existingTask);
    }

    @Override
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Override
    public DispatchTask assignTask(Long taskId, Long vehicleId, Long driverId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 1) {
            throw new RuntimeException("只能分配待分配状态的任务");
        }

        task.setAssignedVehicleId(vehicleId);
        task.setAssignedDriverId(driverId);
        task.setStatus(2); // 已分配
        task.setActualStartTime(LocalDateTime.now());

        DispatchTask updatedTask = taskRepository.save(task);
        
        log.info("任务 {} 已分配给车辆 {} 和司机 {}", 
                task.getTaskNo(), vehicleId, driverId);

        return updatedTask;
    }

    @Override
    public DispatchTask startTask(Long taskId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 2) {
            throw new RuntimeException("只能开始已分配状态的任务");
        }

        task.setStatus(3); // 执行中
        task.setActualStartTime(LocalDateTime.now());

        DispatchTask updatedTask = taskRepository.save(task);
        
        log.info("任务 {} 开始执行", task.getTaskNo());

        return updatedTask;
    }

    @Override
    public DispatchTask completeTask(Long taskId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 3) {
            throw new RuntimeException("只能完成执行中的任务");
        }

        task.setStatus(4); // 已完成
        task.setActualEndTime(LocalDateTime.now());
        task.setProgress(java.math.BigDecimal.valueOf(100));

        DispatchTask updatedTask = taskRepository.save(task);
        
        log.info("任务 {} 已完成", task.getTaskNo());

        return updatedTask;
    }

    @Override
    public DispatchTask cancelTask(Long taskId, String reason) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() == 4) {
            throw new RuntimeException("已完成的任务不能取消");
        }

        task.setStatus(5); // 已取消
        task.setRemark(reason);

        DispatchTask updatedTask = taskRepository.save(task);
        
        log.info("任务 {} 已取消，原因: {}", task.getTaskNo(), reason);

        return updatedTask;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchTask> getPendingTasks() {
        return taskRepository.findPendingTasks();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchTask> getInProgressTasks() {
        return taskRepository.findInProgressTasks();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchTask> getTasksByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return taskRepository.findByTimeRange(startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatistics getTaskStatistics() {
        List<Object[]> stats = taskRepository.countTasksByStatus();
        
        TaskStatistics statistics = new TaskStatistics();
        statistics.setTotalCount(0L);
        statistics.setPendingCount(0L);
        statistics.setAssignedCount(0L);
        statistics.setInProgressCount(0L);
        statistics.setCompletedCount(0L);
        statistics.setCancelledCount(0L);
        statistics.setExceptionCount(0L);

        for (Object[] stat : stats) {
            Integer status = (Integer) stat[0];
            Long count = (Long) stat[1];
            
            statistics.setTotalCount(statistics.getTotalCount() + count);
            
            switch (status) {
                case 1: // 待分配
                    statistics.setPendingCount(count);
                    break;
                case 2: // 已分配
                    statistics.setAssignedCount(count);
                    break;
                case 3: // 执行中
                    statistics.setInProgressCount(count);
                    break;
                case 4: // 已完成
                    statistics.setCompletedCount(count);
                    break;
                case 5: // 已取消
                    statistics.setCancelledCount(count);
                    break;
                case 6: // 异常
                    statistics.setExceptionCount(count);
                    break;
            }
        }

        // 计算今日任务
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        Long todayTasks = taskRepository.countTodayTasks(startOfDay, endOfDay);
        statistics.setTodayCount(todayTasks);

        return statistics;
    }

    /**
     * 生成任务编号
     */
    private String generateTaskNo() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 这里可以添加序号生成的逻辑
        return "TASK" + dateStr + System.currentTimeMillis() % 10000;
    }
}