package com.airport.service.impl;

import com.airport.dto.TaskStatistics;
import com.airport.entity.DispatchTask;
import com.airport.entity.Vehicle;
import com.airport.repository.DispatchTaskRepository;
import com.airport.repository.VehicleRepository;
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
    private final VehicleRepository vehicleRepository;

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
        // 验证必填字段
        if (task.getTaskName() == null || task.getTaskName().trim().isEmpty()) {
            throw new RuntimeException("任务名称不能为空");
        }
        if (task.getTaskType() == null || task.getTaskType().trim().isEmpty()) {
            throw new RuntimeException("任务类型不能为空");
        }
        if (task.getStartLocation() == null || task.getStartLocation().trim().isEmpty()) {
            throw new RuntimeException("起始位置不能为空");
        }
        if (task.getEndLocation() == null || task.getEndLocation().trim().isEmpty()) {
            throw new RuntimeException("目标位置不能为空");
        }
        if (task.getStartTime() == null) {
            throw new RuntimeException("开始时间不能为空");
        }

        // 生成任务编号
        if (task.getTaskNo() == null || task.getTaskNo().trim().isEmpty()) {
            String taskNo = generateTaskNo();
            task.setTaskNo(taskNo);
        }

        // 检查任务编号是否已存在
        DispatchTask existingTask = taskRepository.findByTaskNo(task.getTaskNo());
        if (existingTask != null) {
            // 如果已存在，重新生成
            String taskNo = generateTaskNo();
            task.setTaskNo(taskNo);
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

        // 验证必填字段
        if (task.getTaskName() != null && !task.getTaskName().trim().isEmpty()) {
            existingTask.setTaskName(task.getTaskName());
        }
        if (task.getTaskType() != null && !task.getTaskType().trim().isEmpty()) {
            existingTask.setTaskType(task.getTaskType());
        }
        if (task.getPriority() != null) {
            existingTask.setPriority(task.getPriority());
        }
        if (task.getDescription() != null) {
            existingTask.setDescription(task.getDescription());
        }
        if (task.getStartLocation() != null && !task.getStartLocation().trim().isEmpty()) {
            existingTask.setStartLocation(task.getStartLocation());
        }
        if (task.getEndLocation() != null && !task.getEndLocation().trim().isEmpty()) {
            existingTask.setEndLocation(task.getEndLocation());
        }
        if (task.getStartTime() != null) {
            existingTask.setStartTime(task.getStartTime());
        }
        if (task.getEndTime() != null) {
            existingTask.setEndTime(task.getEndTime());
        }
        if (task.getStatus() != null) {
            existingTask.setStatus(task.getStatus());
        }
        if (task.getProgress() != null) {
            existingTask.setProgress(task.getProgress());
        }
        if (task.getRemark() != null) {
            existingTask.setRemark(task.getRemark());
        }

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

        // 检查车辆是否存在，但不改变车辆状态
        // 车辆状态应该保持为1（正常），任务分配通过任务表的assignedVehicleId来关联
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("车辆不存在"));
        
        // 检查车辆状态是否为正常（1），只有正常状态的车辆才能分配任务
        if (vehicle.getStatus() != 1) {
            throw new RuntimeException("只能为正常状态的车辆分配任务");
        }
        
        // 不改变车辆状态，保持为1（正常）
        // 任务分配通过任务表的assignedVehicleId字段来关联

        task.setAssignedVehicleId(vehicleId);
        task.setAssignedDriverId(driverId);
        task.setStatus(2); // 已分配
        task.setActualStartTime(LocalDateTime.now());

        DispatchTask updatedTask = taskRepository.save(task);
        
        log.info("任务 {} 已分配给车辆 {} 和司机 {}，车辆状态已更新为已分配", 
                task.getTaskNo(), vehicleId, driverId);

        return updatedTask;
    }

    @Override
    public DispatchTask unassignTask(Long taskId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 2) {
            throw new RuntimeException("只能取消分配已分配状态的任务");
        }

        // 清除分配信息
        task.setAssignedVehicleId(null);
        task.setAssignedDriverId(null);
        task.setStatus(1); // 恢复为待分配状态
        task.setActualStartTime(null);

        DispatchTask updatedTask = taskRepository.save(task);
        
        log.info("任务 {} 已取消分配，恢复为待分配状态", task.getTaskNo());

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

    @Override
    public DispatchTask resendTask(Long taskId) {
        DispatchTask originalTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        // 只有已完成的任务才能重新发送
        if (originalTask.getStatus() != 4) {
            throw new RuntimeException("只能重新发送已完成的任务");
        }

        // 创建新任务，复制原任务的信息
        DispatchTask newTask = new DispatchTask();
        newTask.setTaskName(originalTask.getTaskName());
        newTask.setTaskType(originalTask.getTaskType());
        newTask.setPriority(originalTask.getPriority());
        newTask.setDescription(originalTask.getDescription());
        newTask.setStartLocation(originalTask.getStartLocation());
        newTask.setEndLocation(originalTask.getEndLocation());
        newTask.setStartTime(LocalDateTime.now()); // 使用当前时间作为开始时间
        newTask.setStatus(1); // 待分配
        newTask.setProgress(java.math.BigDecimal.ZERO);
        newTask.setRemark("重新发送自任务: " + originalTask.getTaskNo());

        // 生成新的任务编号
        String newTaskNo = generateTaskNo();
        // 确保任务编号唯一
        while (taskRepository.findByTaskNo(newTaskNo) != null) {
            newTaskNo = generateTaskNo();
        }
        newTask.setTaskNo(newTaskNo);

        DispatchTask savedTask = taskRepository.save(newTask);
        
        log.info("任务 {} 已重新发送，新任务编号: {}", originalTask.getTaskNo(), newTaskNo);
        
        return savedTask;
    }

    /**
     * 生成任务编号
     */
    private String generateTaskNo() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 获取当天任务数量作为序号
        Long todayCount = taskRepository.countTodayTasks(
            LocalDateTime.now().toLocalDate().atStartOfDay(),
            LocalDateTime.now().toLocalDate().atStartOfDay().plusDays(1)
        );
        String sequence = String.format("%04d", (todayCount + 1) % 10000);
        return "TASK" + dateStr + sequence;
    }
}