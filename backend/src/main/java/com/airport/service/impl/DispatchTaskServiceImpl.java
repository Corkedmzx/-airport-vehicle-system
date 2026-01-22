package com.airport.service.impl;

import com.airport.dto.TaskStatistics;
import com.airport.entity.DispatchTask;
import com.airport.entity.SysUser;
import com.airport.entity.SysUserRole;
import com.airport.entity.Vehicle;
import com.airport.repository.DispatchTaskRepository;
import com.airport.repository.SysUserRepository;
import com.airport.repository.SysUserRoleRepository;
import com.airport.repository.SysRoleRepository;
import com.airport.repository.SysRolePermissionRepository;
import com.airport.repository.SysPermissionRepository;
import com.airport.repository.VehicleRepository;
import com.airport.service.DispatchTaskService;
import com.airport.service.EmailService;
import com.airport.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

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
    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysPermissionRepository permissionRepository;
    private final EmailService emailService;
    private final MessageService messageService;

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

        // 生成唯一的任务编号
        if (task.getTaskNo() == null || task.getTaskNo().trim().isEmpty()) {
            String taskNo;
            int maxAttempts = 10; // 最多尝试10次
            int attempts = 0;
            do {
                taskNo = generateTaskNo();
                attempts++;
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("无法生成唯一的任务编号，请稍后重试");
                }
            } while (taskRepository.findByTaskNo(taskNo) != null);
            task.setTaskNo(taskNo);
        } else {
            // 如果提供了任务编号，检查是否已存在
            DispatchTask existingTask = taskRepository.findByTaskNo(task.getTaskNo());
            if (existingTask != null && !existingTask.getId().equals(task.getId())) {
                throw new RuntimeException("任务编号已存在: " + task.getTaskNo());
            }
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
    public DispatchTask assignTask(Long taskId, Long vehicleId, Long driverId, Long dispatcherId) {
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

        // 先保存数据库，确保事务提交
        DispatchTask updatedTask = taskRepository.saveAndFlush(task);
        
        log.info("任务 {} 已分配给车辆 {} 和司机 {}，车辆状态已更新为已分配", 
                task.getTaskNo(), vehicleId, driverId);

        // 获取调度员信息
        SysUser dispatcher = dispatcherId != null ? userRepository.findById(dispatcherId).orElse(null) : null;
        String dispatcherName = dispatcher != null ? 
            (dispatcher.getRealName() != null ? dispatcher.getRealName() : dispatcher.getUsername()) : "系统";
        String dispatcherRole = "调度员"; // 默认角色，可以从用户角色中获取

        // 如果指定了司机，异步发送邮件通知和站内信（不阻塞主流程）
        if (driverId != null) {
            SysUser driver = userRepository.findById(driverId).orElse(null);
            if (driver != null) {
                // 发送邮件通知
                if (driver.getEmail() != null && !driver.getEmail().trim().isEmpty()) {
                    // 使用异步方法发送邮件，不阻塞响应
                    emailService.sendDriverTaskAssignmentEmailAsync(
                        driver.getEmail(),
                        task.getTaskNo(),
                        task.getTaskName(),
                        task.getTaskType(),
                        task.getPriority(),
                        task.getStartLocation(),
                        task.getEndLocation(),
                        task.getStartTime(),
                        vehicle.getVehicleNo(),
                        vehicle.getBrand(),
                        vehicle.getModel(),
                        dispatcherName,
                        dispatcherRole
                    );
                    log.info("任务分配邮件发送任务已提交，司机邮箱: {}", driver.getEmail());
                }
                
                // 发送站内信通知
                String messageTitle = String.format("任务分配通知 - %s", task.getTaskNo());
                String messageContent = String.format(
                    "您已被分配了一个新的驾驶任务：\n\n" +
                    "任务编号：%s\n" +
                    "任务名称：%s\n" +
                    "任务类型：%s\n" +
                    "优先级：%s\n" +
                    "起始位置：%s\n" +
                    "目标位置：%s\n" +
                    "开始时间：%s\n" +
                    "分配车辆：%s\n" +
                    "分配人：%s（%s）\n\n" +
                    "请及时查看任务详情并前往指定地点执行任务。",
                    task.getTaskNo(),
                    task.getTaskName(),
                    task.getTaskType(),
                    getPriorityText(task.getPriority()),
                    task.getStartLocation(),
                    task.getEndLocation(),
                    task.getStartTime() != null ? task.getStartTime().toString() : "未指定",
                    vehicle.getVehicleNo(),
                    dispatcherName,
                    dispatcherRole
                );
                
                messageService.createMessagesForUsers(
                    List.of(driverId),
                    messageTitle,
                    messageContent,
                    "task_assignment",
                    "task",
                    task.getPriority() >= 3 ? "high" : task.getPriority() == 2 ? "medium" : "low",
                    task.getId(),
                    "task"
                );
                log.info("任务分配站内信已发送给司机: {}", driver.getUsername());
            }
        }

        return updatedTask;
    }
    
    private String getPriorityText(Integer priority) {
        if (priority == null) return "未知";
        switch (priority) {
            case 1: return "低";
            case 2: return "中";
            case 3: return "高";
            case 4: return "紧急";
            default: return "未知";
        }
    }

    @Override
    public DispatchTask assignTaskWithDriver(Long taskId, Long vehicleId, String driverUsername, Long dispatcherId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 1) {
            throw new RuntimeException("只能分配待分配状态的任务");
        }

        // 检查车辆是否存在
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("车辆不存在"));
        
        // 检查车辆状态是否为正常（1），只有正常状态的车辆才能分配任务
        if (vehicle.getStatus() != 1) {
            throw new RuntimeException("只能为正常状态的车辆分配任务");
        }

        // 查找司机（通过用户名）
        SysUser driver = userRepository.findByUsername(driverUsername)
                .orElseThrow(() -> new RuntimeException("司机不存在: " + driverUsername));

        if (driver.getStatus() == null || driver.getStatus() != 1) {
            throw new RuntimeException("司机已被禁用，无法分配任务");
        }

        // 分配任务
        task.setAssignedVehicleId(vehicleId);
        task.setAssignedDriverId(driver.getId());
        task.setStatus(2); // 已分配
        task.setActualStartTime(LocalDateTime.now());

        // 先保存数据库，确保事务提交
        DispatchTask updatedTask = taskRepository.saveAndFlush(task);
        
        log.info("任务 {} 已分配给车辆 {} (车牌: {}) 和司机 {} (ID: {})", 
                task.getTaskNo(), vehicleId, vehicle.getVehicleNo(), driverUsername, driver.getId());

        // 获取调度员信息
        SysUser dispatcher = dispatcherId != null ? userRepository.findById(dispatcherId).orElse(null) : null;
        String dispatcherName = dispatcher != null ? 
            (dispatcher.getRealName() != null ? dispatcher.getRealName() : dispatcher.getUsername()) : "系统";
        String dispatcherRole = "调度员"; // 默认角色

        // 向司机邮箱异步发送任务分配邮件通知（不阻塞主流程）
        if (driver.getEmail() != null && !driver.getEmail().trim().isEmpty()) {
            // 使用异步方法发送邮件，不阻塞响应
            emailService.sendDriverTaskAssignmentEmailAsync(
                driver.getEmail(),
                task.getTaskNo(),
                task.getTaskName(),
                task.getTaskType(),
                task.getPriority(),
                task.getStartLocation(),
                task.getEndLocation(),
                task.getStartTime(),
                vehicle.getVehicleNo(),
                vehicle.getBrand(),
                vehicle.getModel(),
                dispatcherName,
                dispatcherRole
            );
            log.info("任务分配邮件发送任务已提交（异步），司机邮箱: {}", driver.getEmail());
        } else {
            log.warn("司机 {} 没有邮箱地址，跳过发送邮件通知", driverUsername);
        }
        
        // 发送站内信通知
        String messageTitle = String.format("任务分配通知 - %s", task.getTaskNo());
        String messageContent = String.format(
            "您已被分配了一个新的驾驶任务：\n\n" +
            "任务编号：%s\n" +
            "任务名称：%s\n" +
            "任务类型：%s\n" +
            "优先级：%s\n" +
            "起始位置：%s\n" +
            "目标位置：%s\n" +
            "开始时间：%s\n" +
            "分配车辆：%s\n" +
            "分配人：%s（%s）\n\n" +
            "请及时查看任务详情并前往指定地点执行任务。",
            task.getTaskNo(),
            task.getTaskName(),
            task.getTaskType(),
            getPriorityText(task.getPriority()),
            task.getStartLocation(),
            task.getEndLocation(),
            task.getStartTime() != null ? task.getStartTime().toString() : "未指定",
            vehicle.getVehicleNo(),
            dispatcherName,
            dispatcherRole
        );
        
        messageService.createMessagesForUsers(
            List.of(driver.getId()),
            messageTitle,
            messageContent,
            "task_assignment",
            "task",
            task.getPriority() >= 3 ? "high" : task.getPriority() == 2 ? "medium" : "low",
            task.getId(),
            "task"
        );
        log.info("任务分配站内信已发送给司机: {}", driverUsername);

        return updatedTask;
    }

    @Override
    public DispatchTask unassignTask(Long taskId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 2) {
            throw new RuntimeException("只能取消分配已分配状态的任务");
        }

        // 在清除分配信息前，先获取司机/维修员和车辆信息用于发送邮件
        SysUser driver = null;
        SysUser maintenance = null;
        Vehicle vehicle = null;
        boolean isMaintenanceTask = false;
        
        if (task.getAssignedDriverId() != null) {
            driver = userRepository.findById(task.getAssignedDriverId()).orElse(null);
        }
        if (task.getAssignedUserId() != null) {
            // 检查是否是维修任务（通过assignedUserId分配）
            maintenance = userRepository.findById(task.getAssignedUserId()).orElse(null);
            if (maintenance != null) {
                // 检查是否是维修员角色
                isMaintenanceTask = userRoleRepository.findByUserId(maintenance.getId()).stream()
                        .anyMatch(ur -> {
                            var roleOpt = roleRepository.findById(ur.getRoleId());
                            return roleOpt.isPresent() && "MAINTENANCE".equals(roleOpt.get().getRoleCode());
                        });
            }
        }
        if (task.getAssignedVehicleId() != null) {
            vehicle = vehicleRepository.findById(task.getAssignedVehicleId()).orElse(null);
        }

        // 清除分配信息
        task.setAssignedVehicleId(null);
        task.setAssignedDriverId(null);
        task.setAssignedUserId(null);
        task.setStatus(1); // 恢复为待分配状态
        task.setActualStartTime(null);

        // 保存任务信息用于邮件发送（在清除分配信息前）
        String taskNo = task.getTaskNo();
        String taskName = task.getTaskName();
        String taskType = task.getTaskType();
        Integer priority = task.getPriority();
        String startLocation = task.getStartLocation();
        String endLocation = task.getEndLocation();
        java.time.LocalDateTime startTime = task.getStartTime();
        String vehicleNo = vehicle != null ? vehicle.getVehicleNo() : "未知车辆";
        String driverEmail = driver != null ? driver.getEmail() : null;
        String maintenanceEmail = maintenance != null ? maintenance.getEmail() : null;

        // 先保存数据库，确保事务提交
        DispatchTask updatedTask = taskRepository.saveAndFlush(task);
        
        log.info("任务 {} 已取消分配，恢复为待分配状态", taskNo);

        // 如果之前有分配司机，异步发送取消分配邮件通知（不阻塞主流程）
        if (driverEmail != null && !driverEmail.trim().isEmpty() && !isMaintenanceTask) {
            // 使用异步方法发送邮件，不阻塞响应
            emailService.sendTaskUnassignmentEmailAsync(
                driverEmail,
                taskNo,
                taskName,
                taskType,
                priority,
                startLocation,
                endLocation,
                startTime,
                vehicleNo,
                "管理员取消了任务分配"
            );
            log.info("任务取消分配邮件发送任务已提交（异步），司机邮箱: {}", driverEmail);
            
            // 发送站内信通知
            String messageTitle = String.format("任务取消分配通知 - %s", taskNo);
            String messageContent = String.format(
                "您被分配的任务已被取消：\n\n" +
                "任务编号：%s\n" +
                "任务名称：%s\n" +
                "任务类型：%s\n" +
                "原计划开始时间：%s\n" +
                "原分配车辆：%s\n" +
                "取消原因：管理员取消了任务分配\n\n" +
                "任务已恢复为待分配状态，您无需执行此任务。",
                taskNo,
                taskName,
                taskType,
                startTime != null ? startTime.toString() : "未指定",
                vehicleNo
            );
            
            messageService.createMessagesForUsers(
                List.of(driver.getId()),
                messageTitle,
                messageContent,
                "task_assignment",
                "task",
                priority >= 3 ? "high" : priority == 2 ? "medium" : "low",
                task.getId(),
                "task"
            );
            log.info("任务取消分配站内信已发送给司机: {}", driver.getUsername());
        }
        
        // 如果之前有分配维修员，异步发送取消分配邮件和站内信通知
        if (isMaintenanceTask && maintenanceEmail != null && !maintenanceEmail.trim().isEmpty()) {
            // 发送邮件通知（使用司机任务取消邮件模板，内容类似）
            emailService.sendTaskUnassignmentEmailAsync(
                maintenanceEmail,
                taskNo,
                taskName,
                taskType,
                priority,
                startLocation,
                endLocation,
                startTime,
                vehicleNo,
                "管理员取消了任务分配"
            );
            log.info("维修任务取消分配邮件发送任务已提交（异步），维修员邮箱: {}", maintenanceEmail);
            
            // 发送站内信通知
            String messageTitle = String.format("维修任务取消分配通知 - %s", taskNo);
            String messageContent = String.format(
                "您被分配的维修任务已被取消：\n\n" +
                "任务编号：%s\n" +
                "任务名称：%s\n" +
                "任务类型：%s\n" +
                "原计划开始时间：%s\n" +
                "原分配车辆：%s\n" +
                "取消原因：管理员取消了任务分配\n\n" +
                "任务已恢复为待分配状态，您无需执行此任务。",
                taskNo,
                taskName,
                taskType,
                startTime != null ? startTime.toString() : "未指定",
                vehicleNo
            );
            
            messageService.createMessagesForUsers(
                List.of(maintenance.getId()),
                messageTitle,
                messageContent,
                "task_assignment",
                "maintenance",
                priority >= 3 ? "high" : priority == 2 ? "medium" : "low",
                task.getId(),
                "task"
            );
            log.info("维修任务取消分配站内信已发送给维修员: {}", maintenance.getUsername());
        }

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
        
        // 更新车辆状态（如果任务关联了车辆）
        if (task.getAssignedVehicleId() != null) {
            try {
                Vehicle vehicle = vehicleRepository.findById(task.getAssignedVehicleId()).orElse(null);
                if (vehicle != null) {
                    // 如果车辆状态是正常（1），任务开始执行时车辆状态保持正常
                    // 如果车辆状态是维修中（2），说明是维护任务，保持维修中状态
                    // 这里不需要改变车辆状态，因为车辆状态应该由任务类型决定
                    log.info("任务 {} 开始执行，关联车辆: {} (状态: {})", 
                            task.getTaskNo(), vehicle.getVehicleNo(), vehicle.getStatus());
                }
            } catch (Exception e) {
                log.error("更新车辆状态失败，任务编号: {}", task.getTaskNo(), e);
            }
        }
        
        log.info("任务 {} 开始执行", task.getTaskNo());

        return updatedTask;
    }

    @Override
    public DispatchTask completeTask(Long taskId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        // 允许完成已分配（2）或执行中（3）状态的任务
        if (task.getStatus() != 2 && task.getStatus() != 3) {
            throw new RuntimeException("只能完成已分配或执行中的任务");
        }

        task.setStatus(4); // 已完成
        task.setActualEndTime(LocalDateTime.now());
        task.setProgress(java.math.BigDecimal.valueOf(100));

        DispatchTask updatedTask = taskRepository.save(task);
        
        // 根据任务类型更新车辆状态
        if (task.getAssignedVehicleId() != null) {
            try {
                Vehicle vehicle = vehicleRepository.findById(task.getAssignedVehicleId()).orElse(null);
                if (vehicle != null) {
                    // 如果是维护调度任务，无论车辆当前状态如何，都更新为正常（1）
                    if ("维护调度".equals(task.getTaskType())) {
                        int oldStatus = vehicle.getStatus();
                        vehicle.setStatus(1); // 正常
                        vehicleRepository.save(vehicle);
                        log.info("维护调度任务完成，车辆 {} 状态已从 {} 更新为正常（1），任务编号: {}", 
                                vehicle.getVehicleNo(), oldStatus, task.getTaskNo());
                    } else {
                        // 对于普通调度任务，车辆状态保持正常（1），不需要更新
                        log.info("任务 {} 已完成，关联车辆: {} (状态: {})", 
                                task.getTaskNo(), vehicle.getVehicleNo(), vehicle.getStatus());
                    }
                }
            } catch (Exception e) {
                log.error("更新车辆状态失败，任务编号: {}", task.getTaskNo(), e);
            }
        }
        
        log.info("任务 {} 已完成", task.getTaskNo());

        // 发送站内信通知给调度员（有task:assign权限的用户）
        try {
            List<Long> dispatcherIds = getUsersWithPermission("task:assign");
            if (!dispatcherIds.isEmpty()) {
                SysUser driver = task.getAssignedDriverId() != null ? 
                    userRepository.findById(task.getAssignedDriverId()).orElse(null) : null;
                String driverName = driver != null ? 
                    (driver.getRealName() != null ? driver.getRealName() : driver.getUsername()) : "未知司机";
                
                Vehicle vehicle = task.getAssignedVehicleId() != null ?
                    vehicleRepository.findById(task.getAssignedVehicleId()).orElse(null) : null;
                String vehicleNo = vehicle != null ? vehicle.getVehicleNo() : "未知车辆";
                
                String messageTitle = String.format("任务完成通知 - %s", task.getTaskNo());
                String messageContent = String.format(
                    "任务已完成：\n\n" +
                    "任务编号：%s\n" +
                    "任务名称：%s\n" +
                    "执行司机：%s\n" +
                    "执行车辆：%s\n" +
                    "完成时间：%s\n\n" +
                    "请及时查看任务详情。",
                    task.getTaskNo(),
                    task.getTaskName(),
                    driverName,
                    vehicleNo,
                    updatedTask.getActualEndTime() != null ? updatedTask.getActualEndTime().toString() : "未指定"
                );
                
                messageService.createMessagesForUsers(
                    dispatcherIds,
                    messageTitle,
                    messageContent,
                    "task_completion",
                    "task",
                    "normal",
                    task.getId(),
                    "task"
                );
                log.info("任务完成站内信已发送给{}个调度员", dispatcherIds.size());
            }
        } catch (Exception e) {
            log.error("发送任务完成站内信失败", e);
        }

        return updatedTask;
    }
    
    /**
     * 获取有指定权限的用户ID列表
     */
    private List<Long> getUsersWithPermission(String permissionCode) {
        List<Long> userIds = new ArrayList<>();
        
        try {
            // 查找权限ID
            permissionRepository.findByPermissionCode(permissionCode)
                    .ifPresent(permission -> {
                        // 查找所有拥有该权限的角色
                        List<com.airport.entity.SysRolePermission> rolePermissions = rolePermissionRepository.findAll()
                                .stream()
                                .filter(rp -> rp.getPermissionId().equals(permission.getId()))
                                .collect(java.util.stream.Collectors.toList());

                        // 查找所有拥有这些角色的用户
                        for (com.airport.entity.SysRolePermission rp : rolePermissions) {
                            List<SysUserRole> userRoles = userRoleRepository.findAll()
                                    .stream()
                                    .filter(ur -> ur.getRoleId().equals(rp.getRoleId()))
                                    .collect(java.util.stream.Collectors.toList());

                            for (SysUserRole userRole : userRoles) {
                                SysUser user = userRepository.findById(userRole.getUserId()).orElse(null);
                                if (user != null && user.getStatus() == 1) {
                                    if (!userIds.contains(user.getId())) {
                                        userIds.add(user.getId());
                                    }
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("获取有权限的用户失败: permissionCode={}", permissionCode, e);
        }

        return userIds;
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

    @Override
    public DispatchTask assignTaskToMaintenance(Long taskId, Long vehicleId, Long maintenanceId, Long dispatcherId) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 1) {
            throw new RuntimeException("只能分配待分配状态的任务");
        }

        // 检查车辆是否存在
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("车辆不存在"));

        // 查找维修员
        SysUser maintenance = userRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("维修员不存在"));

        if (maintenance.getStatus() == null || maintenance.getStatus() != 1) {
            throw new RuntimeException("维修员已被禁用，无法分配任务");
        }

        // 检查维修员是否有MAINTENANCE角色
        boolean isMaintenance = userRoleRepository.findByUserId(maintenanceId).stream()
                .anyMatch(ur -> {
                    var role = roleRepository.findById(ur.getRoleId());
                    return role.isPresent() && "MAINTENANCE".equals(role.get().getRoleCode());
                });
        if (!isMaintenance) {
            throw new RuntimeException("指定用户不是维修员角色");
        }

        // 分配任务（维护调度任务分配给维修员，使用assignedUserId字段）
        task.setAssignedVehicleId(vehicleId);
        task.setAssignedUserId(maintenanceId);
        task.setStatus(2); // 已分配
        task.setActualStartTime(LocalDateTime.now());

        // 更新车辆状态为维修中（2）
        vehicle.setStatus(2);
        vehicleRepository.save(vehicle);
        log.info("车辆 {} 状态已更新为维修中（2），任务编号: {}", vehicle.getVehicleNo(), task.getTaskNo());

        // 先保存数据库，确保事务提交
        DispatchTask updatedTask = taskRepository.saveAndFlush(task);
        
        log.info("维护调度任务 {} 已分配给车辆 {} (车牌: {}) 和维修员 {} (ID: {})", 
                task.getTaskNo(), vehicleId, vehicle.getVehicleNo(), maintenance.getUsername(), maintenanceId);

        // 获取调度员信息
        SysUser dispatcher = dispatcherId != null ? userRepository.findById(dispatcherId).orElse(null) : null;
        String dispatcherName = dispatcher != null ? 
            (dispatcher.getRealName() != null ? dispatcher.getRealName() : dispatcher.getUsername()) : "系统";
        String dispatcherRole = "调度员"; // 默认角色，可以从用户角色中获取
        if (dispatcher != null) {
            var roles = userRoleRepository.findByUserId(dispatcherId);
            if (!roles.isEmpty()) {
                var roleOpt = roleRepository.findById(roles.get(0).getRoleId());
                if (roleOpt.isPresent()) {
                    dispatcherRole = roleOpt.get().getRoleName();
                }
            }
        }

        // 如果指定了维修员，异步发送邮件通知和站内信（不阻塞主流程）
        if (maintenance.getEmail() != null && !maintenance.getEmail().trim().isEmpty()) {
            // 使用异步方法发送邮件，不阻塞响应
            emailService.sendMaintenanceTaskAssignmentEmailAsync(
                maintenance.getEmail(),
                task.getTaskNo(),
                task.getTaskName(),
                task.getTaskType(),
                task.getPriority(),
                task.getStartLocation(),
                task.getEndLocation(),
                task.getStartTime(),
                vehicle.getVehicleNo(),
                vehicle.getBrand(),
                vehicle.getModel(),
                task.getDescription() != null ? task.getDescription() : "",
                dispatcherName,
                dispatcherRole
            );
            log.info("维修任务分配邮件发送任务已提交，维修员邮箱: {}", maintenance.getEmail());
        }
        
        // 发送站内信通知
        String messageTitle = String.format("维修任务分配通知 - %s", task.getTaskNo());
        String messageContent = String.format(
            "您已被分配了一个新的维修任务：\n\n" +
            "任务编号：%s\n" +
            "任务名称：%s\n" +
            "任务类型：%s\n" +
            "优先级：%s\n" +
            "车辆位置：%s\n" +
            "维修地点：%s\n" +
            "开始时间：%s\n" +
            "车辆信息：%s %s (车牌号: %s)\n" +
            "%s" +
            "分配人：%s（%s）\n\n" +
            "请及时查看任务详情并前往指定地点执行维修任务。",
            task.getTaskNo(),
            task.getTaskName(),
            task.getTaskType(),
            getPriorityText(task.getPriority()),
            task.getStartLocation(),
            task.getEndLocation(),
            task.getStartTime() != null ? task.getStartTime().toString() : "未指定",
            vehicle.getBrand() != null ? vehicle.getBrand() : "",
            vehicle.getModel() != null ? vehicle.getModel() : "",
            vehicle.getVehicleNo(),
            task.getDescription() != null && !task.getDescription().trim().isEmpty() 
                ? String.format("任务描述：%s\n", task.getDescription()) : "",
            dispatcherName,
            dispatcherRole
        );
        
        messageService.createMessagesForUsers(
            List.of(maintenanceId),
            messageTitle,
            messageContent,
            "task_assignment",
            "maintenance",
            task.getPriority() >= 3 ? "high" : task.getPriority() == 2 ? "medium" : "low",
            task.getId(),
            "task"
        );
        log.info("维修任务分配站内信已发送给维修员: {}", maintenance.getUsername());

        return updatedTask;
    }

    @Override
    public DispatchTask assignTaskToMaintenanceWithUsername(Long taskId, Long vehicleId, String maintenanceUsername, String dispatcherUsername) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 1) {
            throw new RuntimeException("只能分配待分配状态的任务");
        }

        // 检查车辆是否存在
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("车辆不存在"));

        // 查找维修员（通过用户名）
        SysUser maintenance = userRepository.findByUsername(maintenanceUsername)
                .orElseThrow(() -> new RuntimeException("维修员不存在: " + maintenanceUsername));

        if (maintenance.getStatus() == null || maintenance.getStatus() != 1) {
            throw new RuntimeException("维修员已被禁用，无法分配任务");
        }

        // 检查维修员是否有MAINTENANCE角色
        boolean isMaintenance = userRoleRepository.findByUserId(maintenance.getId()).stream()
                .anyMatch(ur -> {
                    var role = roleRepository.findById(ur.getRoleId());
                    return role.isPresent() && "MAINTENANCE".equals(role.get().getRoleCode());
                });
        if (!isMaintenance) {
            throw new RuntimeException("指定用户不是维修员角色");
        }

        // 查找调度员（通过用户名）
        SysUser dispatcher = userRepository.findByUsername(dispatcherUsername)
                .orElseThrow(() -> new RuntimeException("调度员不存在: " + dispatcherUsername));

        // 分配任务
        task.setAssignedVehicleId(vehicleId);
        task.setAssignedUserId(maintenance.getId());
        task.setStatus(2); // 已分配
        task.setActualStartTime(LocalDateTime.now());

        // 先保存数据库，确保事务提交
        DispatchTask updatedTask = taskRepository.saveAndFlush(task);
        
        log.info("维护调度任务 {} 已分配给车辆 {} (车牌: {}) 和维修员 {} (ID: {})", 
                task.getTaskNo(), vehicleId, vehicle.getVehicleNo(), maintenanceUsername, maintenance.getId());

        // 获取调度员信息
        String dispatcherName = dispatcher.getRealName() != null ? dispatcher.getRealName() : dispatcher.getUsername();
        String dispatcherRole = "调度员"; // 默认角色
        var roles = userRoleRepository.findByUserId(dispatcher.getId());
        if (!roles.isEmpty()) {
            var roleOpt = roleRepository.findById(roles.get(0).getRoleId());
            if (roleOpt.isPresent()) {
                dispatcherRole = roleOpt.get().getRoleName();
            }
        }

        // 如果指定了维修员，异步发送邮件通知和站内信（不阻塞主流程）
        if (maintenance.getEmail() != null && !maintenance.getEmail().trim().isEmpty()) {
            // 使用异步方法发送邮件，不阻塞响应
            emailService.sendMaintenanceTaskAssignmentEmailAsync(
                maintenance.getEmail(),
                task.getTaskNo(),
                task.getTaskName(),
                task.getTaskType(),
                task.getPriority(),
                task.getStartLocation(),
                task.getEndLocation(),
                task.getStartTime(),
                vehicle.getVehicleNo(),
                vehicle.getBrand(),
                vehicle.getModel(),
                task.getDescription() != null ? task.getDescription() : "",
                dispatcherName,
                dispatcherRole
            );
            log.info("维修任务分配邮件发送任务已提交（异步），维修员邮箱: {}", maintenance.getEmail());
        } else {
            log.warn("维修员 {} 没有邮箱地址，跳过发送邮件通知", maintenanceUsername);
        }
        
        // 发送站内信通知
        String messageTitle = String.format("维修任务分配通知 - %s", task.getTaskNo());
        String messageContent = String.format(
            "您已被分配了一个新的维修任务：\n\n" +
            "任务编号：%s\n" +
            "任务名称：%s\n" +
            "任务类型：%s\n" +
            "优先级：%s\n" +
            "车辆位置：%s\n" +
            "维修地点：%s\n" +
            "开始时间：%s\n" +
            "车辆信息：%s %s (车牌号: %s)\n" +
            "%s" +
            "分配人：%s（%s）\n\n" +
            "请及时查看任务详情并前往指定地点执行维修任务。",
            task.getTaskNo(),
            task.getTaskName(),
            task.getTaskType(),
            getPriorityText(task.getPriority()),
            task.getStartLocation(),
            task.getEndLocation(),
            task.getStartTime() != null ? task.getStartTime().toString() : "未指定",
            vehicle.getBrand() != null ? vehicle.getBrand() : "",
            vehicle.getModel() != null ? vehicle.getModel() : "",
            vehicle.getVehicleNo(),
            task.getDescription() != null && !task.getDescription().trim().isEmpty() 
                ? String.format("任务描述：%s\n", task.getDescription()) : "",
            dispatcherName,
            dispatcherRole
        );
        
        messageService.createMessagesForUsers(
            List.of(maintenance.getId()),
            messageTitle,
            messageContent,
            "task_assignment",
            "maintenance",
            task.getPriority() >= 3 ? "high" : task.getPriority() == 2 ? "medium" : "low",
            task.getId(),
            "task"
        );
        log.info("维修任务分配站内信已发送给维修员: {}", maintenanceUsername);

        return updatedTask;
    }

    @Override
    public DispatchTask assignTaskToUser(Long taskId, String username) {
        DispatchTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (task.getStatus() != 1) {
            throw new RuntimeException("只能分配待分配状态的任务");
        }

        // 查找用户
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用，无法分配任务");
        }

        // 检查用户邮箱
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            log.warn("用户 {} 没有邮箱，无法发送邮件通知", username);
        }

        // 分配任务给用户
        task.setAssignedUserId(user.getId());
        task.setStatus(2); // 已分配
        task.setActualStartTime(LocalDateTime.now());

        DispatchTask updatedTask = taskRepository.save(task);

        log.info("任务 {} 已分配给用户 {} (ID: {})", task.getTaskNo(), username, user.getId());

        // 发送邮件通知
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            try {
                emailService.sendTaskAssignmentEmail(
                    user.getEmail(),
                    task.getTaskNo(),
                    task.getTaskName(),
                    task.getTaskType(),
                    task.getPriority(),
                    task.getStartLocation(),
                    task.getEndLocation(),
                    task.getStartTime()
                );
                log.info("任务分配邮件已发送到用户邮箱: {}", user.getEmail());
            } catch (Exception e) {
                log.error("发送任务分配邮件失败，用户邮箱: {}", user.getEmail(), e);
                // 邮件发送失败不影响任务分配，只记录日志
            }
        } else {
            log.warn("用户 {} 没有邮箱地址，跳过发送邮件通知", username);
        }

        return updatedTask;
    }

    /**
     * 生成任务编号
     * 格式：TASK + 日期(yyyyMMdd) + 序号(0001-9999)
     * 使用时间戳的毫秒数后4位作为序号，避免并发冲突
     */
    private String generateTaskNo() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用当前时间戳的毫秒数后4位 + 随机数，确保唯一性
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 100); // 0-99的随机数
        String sequence = String.format("%04d", (timestamp % 10000 + random) % 10000);
        return "TASK" + dateStr + sequence;
    }
}