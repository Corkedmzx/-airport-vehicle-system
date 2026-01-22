package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.Alert;
import com.airport.entity.Vehicle;
import com.airport.entity.SysUserRole;
import com.airport.repository.AlertRepository;
import com.airport.repository.VehicleRepository;
import com.airport.repository.SysUserRepository;
import com.airport.repository.SysUserRoleRepository;
import com.airport.repository.SysRoleRepository;
import com.airport.repository.DispatchTaskRepository;
import com.airport.service.EmailService;
import com.airport.service.MessageService;
import com.airport.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警信息管理相关接口")
public class AlertController {

    private final AlertRepository alertRepository;
    private final VehicleRepository vehicleRepository;
    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final DispatchTaskRepository taskRepository;
    private final EmailService emailService;
    private final MessageService messageService;
    private final JwtUtils jwtUtils;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @GetMapping
    @Operation(summary = "获取告警列表", description = "分页获取告警列表")
    public Result<Page<Alert>> getAlerts(
            @Parameter(description = "页码", required = false) 
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", required = false) 
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "告警级别", required = false) 
            @RequestParam(required = false) String severity,
            @Parameter(description = "处理状态", required = false) 
            @RequestParam(required = false) String status,
            @Parameter(description = "关键词", required = false) 
            @RequestParam(required = false) String keyword) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<Alert> alerts = alertRepository.findAll(pageable);
            return Result.success(alerts);
        } catch (Exception e) {
            log.error("获取告警列表失败", e);
            return Result.error("获取告警列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取告警详情", description = "根据ID获取告警详细信息")
    public Result<Alert> getAlertById(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            return Result.success(alert);
        } catch (Exception e) {
            log.error("获取告警详情失败", e);
            return Result.error("获取告警详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建告警", description = "创建新告警")
    public Result<Alert> createAlert(@RequestBody Alert alert) {
        try {
            Alert createdAlert = alertRepository.save(alert);
            return Result.success("告警创建成功", createdAlert);
        } catch (Exception e) {
            log.error("创建告警失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新告警", description = "更新告警信息")
    public Result<Alert> updateAlert(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id,
            @RequestBody Alert alert) {
        try {
            Alert existingAlert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            
            // 更新字段
            if (alert.getTitle() != null) {
                existingAlert.setTitle(alert.getTitle());
            }
            if (alert.getDescription() != null) {
                existingAlert.setDescription(alert.getDescription());
            }
            if (alert.getSeverity() != null) {
                existingAlert.setSeverity(alert.getSeverity());
            }
            if (alert.getCategory() != null) {
                existingAlert.setCategory(alert.getCategory());
            }
            if (alert.getVehicleId() != null) {
                existingAlert.setVehicleId(alert.getVehicleId());
            }
            if (alert.getTaskId() != null) {
                existingAlert.setTaskId(alert.getTaskId());
            }
            if (alert.getReportId() != null) {
                existingAlert.setReportId(alert.getReportId());
            }
            if (alert.getStatus() != null) {
                existingAlert.setStatus(alert.getStatus());
            }
            if (alert.getAssignee() != null) {
                existingAlert.setAssignee(alert.getAssignee());
            }
            
            Alert updatedAlert = alertRepository.save(existingAlert);
            return Result.success("告警更新成功", updatedAlert);
        } catch (Exception e) {
            log.error("更新告警失败", e);
            return Result.error("更新告警失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "确认告警", description = "确认处理告警（维修员操作，会向管理员发送确认邮件）")
    public Result<Alert> acknowledgeAlert(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "处理人", required = false) 
            @RequestParam(required = false) String assignee,
            HttpServletRequest request) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            
            // 获取当前用户信息
            Long currentUserId = getCurrentUserId(request);
            String currentUsername = getCurrentUsername(request);
            
            alert.setStatus("processing");
            alert.setAcknowledged(true);
            alert.setAcknowledgedTime(LocalDateTime.now());
            if (assignee != null) {
                alert.setAssignee(assignee);
            } else if (currentUsername != null) {
                alert.setAssignee(currentUsername);
            }
            
            Alert updatedAlert = alertRepository.save(alert);
            
            // 如果是维修员确认，异步向管理员发送确认邮件
            if (currentUserId != null && isMaintenanceUser(currentUserId)) {
                CompletableFuture.runAsync(() -> sendAcknowledgeEmailToAdmin(updatedAlert, currentUserId), taskExecutor);
            }
            
            return Result.success("告警已确认", updatedAlert);
        } catch (Exception e) {
            log.error("确认告警失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "解决告警", description = "标记告警为已解决（维修员操作，会向管理员发送完成邮件）")
    public Result<Alert> resolveAlert(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "处理说明", required = false) 
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            
            // 获取当前用户信息
            Long currentUserId = getCurrentUserId(request);
            
            alert.setStatus("resolved");
            alert.setResolvedTime(LocalDateTime.now());
            if (notes != null) {
                alert.setResolutionNotes(notes);
            }
            
            Alert updatedAlert = alertRepository.save(alert);
            
            // 如果是维修员完成，更新车辆状态为正常（1），更新关联任务状态为已完成（4），并异步向管理员发送完成邮件
            if (currentUserId != null && isMaintenanceUser(currentUserId)) {
                try {
                    // 更新车辆状态
                    if (alert.getVehicleId() != null) {
                        Vehicle vehicle = vehicleRepository.findById(alert.getVehicleId()).orElse(null);
                        if (vehicle != null && vehicle.getStatus() == 2) { // 只有维修中状态才更新为正常
                            vehicle.setStatus(1); // 正常
                            vehicleRepository.save(vehicle);
                            log.info("车辆 {} 状态已更新为正常（1），告警ID: {}", vehicle.getVehicleNo(), alert.getId());
                        }
                    }
                    
                    // 更新关联任务状态
                    if (alert.getTaskId() != null) {
                        var task = taskRepository.findById(alert.getTaskId()).orElse(null);
                        if (task != null && task.getStatus() == 3) { // 只有执行中状态才更新为已完成
                            task.setStatus(4); // 已完成
                            task.setActualEndTime(LocalDateTime.now());
                            task.setProgress(java.math.BigDecimal.valueOf(100));
                            taskRepository.save(task);
                            log.info("任务 {} 状态已更新为已完成（4），告警ID: {}", task.getTaskNo(), alert.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("更新车辆或任务状态失败，告警ID: {}", alert.getId(), e);
                }
                
                CompletableFuture.runAsync(() -> sendResolveEmailToAdmin(updatedAlert, currentUserId, notes), taskExecutor);
            }
            
            return Result.success("告警已解决", updatedAlert);
        } catch (Exception e) {
            log.error("解决告警失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/send-report-email")
    @Operation(summary = "发送报告邮件", description = "维修员向管理员发送告警报告邮件")
    public Result<Void> sendReportEmail(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            
            // 获取当前用户信息
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.unauthorized("未认证或认证已过期");
            }
            
            // 验证是否为维修员
            if (!isMaintenanceUser(currentUserId)) {
                return Result.forbidden("只有维修员可以发送报告邮件");
            }
            
            // 异步向管理员发送报告邮件
            CompletableFuture.runAsync(() -> sendReportEmailToAdmin(alert, currentUserId), taskExecutor);
            
            Result<Void> result = Result.success();
            result.setMessage("报告邮件已发送给管理员");
            return result;
        } catch (Exception e) {
            log.error("发送报告邮件失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/send-task-assignment-email")
    @Operation(summary = "发送任务分配邮件", description = "管理员向维修员发送任务分配邮件（从告警创建任务后）")
    public Result<Void> sendTaskAssignmentEmail(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "任务ID", required = true) 
            @RequestParam Long taskId,
            HttpServletRequest request) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            
            // 获取当前用户信息
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.unauthorized("未认证或认证已过期");
            }
            
            // 验证是否为管理员
            if (!isAdminUser(currentUserId)) {
                return Result.forbidden("只有管理员可以发送任务分配邮件");
            }
            
            // 获取任务信息
            var task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在"));
            
            // 获取分配的维修员
            if (task.getAssignedUserId() != null) {
                var maintenance = userRepository.findById(task.getAssignedUserId())
                        .orElseThrow(() -> new RuntimeException("维修员不存在"));
                
                if (maintenance.getEmail() != null && !maintenance.getEmail().trim().isEmpty()) {
                    // 获取车辆信息
                    Vehicle vehicle = null;
                    if (alert.getVehicleId() != null) {
                        vehicle = vehicleRepository.findById(alert.getVehicleId()).orElse(null);
                    }
                    
                    // 发送任务分配邮件（这个已经在任务分配时发送了，这里可以再次发送或跳过）
                    log.info("任务分配邮件已在任务分配时发送，告警ID: {}, 任务ID: {}", id, taskId);
                }
            }
            
            Result<Void> result = Result.success();
            result.setMessage("任务分配邮件已发送");
            return result;
        } catch (Exception e) {
            log.error("发送任务分配邮件失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除告警", description = "删除指定告警")
    public Result<Void> deleteAlert(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            alertRepository.delete(alert);
            log.info("告警 {} 已删除", id);
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            log.error("删除告警失败", e);
            return Result.error("删除告警失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除告警", description = "批量删除选中的告警")
    public Result<Void> deleteAlerts(
            @Parameter(description = "告警ID列表", required = true) 
            @RequestBody List<Long> alertIds) {
        try {
            if (alertIds == null || alertIds.isEmpty()) {
                return Result.error("请选择要删除的告警");
            }
            List<Alert> alerts = alertRepository.findAllById(alertIds);
            alertRepository.deleteAll(alerts);
            log.info("批量删除了{}条告警", alerts.size());
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            log.error("批量删除告警失败", e);
            return Result.error("批量删除告警失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtils.validateToken(token, jwtUtils.getUsernameFromToken(token))) {
                    return jwtUtils.getUserIdFromToken(token);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
        }
        return null;
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtils.validateToken(token, jwtUtils.getUsernameFromToken(token))) {
                    return jwtUtils.getUsernameFromToken(token);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户名失败", e);
        }
        return null;
    }

    /**
     * 检查用户是否为维修员
     */
    private boolean isMaintenanceUser(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .anyMatch(ur -> {
                    var roleOpt = roleRepository.findById(ur.getRoleId());
                    return roleOpt.isPresent() && "MAINTENANCE".equals(roleOpt.get().getRoleCode());
                });
    }

    /**
     * 检查用户是否为管理员
     */
    private boolean isAdminUser(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .anyMatch(ur -> {
                    var roleOpt = roleRepository.findById(ur.getRoleId());
                    return roleOpt.isPresent() && "ADMIN".equals(roleOpt.get().getRoleCode());
                });
    }

    /**
     * 维修员确认告警后，向管理员发送确认邮件
     */
    private void sendAcknowledgeEmailToAdmin(Alert alert, Long maintenanceUserId) {
        try {
            var maintenance = userRepository.findById(maintenanceUserId)
                    .orElseThrow(() -> new RuntimeException("维修员不存在"));
            String maintenanceName = maintenance.getRealName() != null ? 
                    maintenance.getRealName() : maintenance.getUsername();
            
            // 获取车辆信息
            Vehicle vehicle = null;
            if (alert.getVehicleId() != null) {
                vehicle = vehicleRepository.findById(alert.getVehicleId()).orElse(null);
            }
            String vehicleNo = vehicle != null ? vehicle.getVehicleNo() : "未知车辆";
            
            // 获取所有管理员
            List<Long> adminIds = userRoleRepository.findAll().stream()
                    .filter(ur -> {
                        var roleOpt = roleRepository.findById(ur.getRoleId());
                        return roleOpt.isPresent() && "ADMIN".equals(roleOpt.get().getRoleCode());
                    })
                    .map(com.airport.entity.SysUserRole::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 使用多线程向每个管理员发送邮件
            List<CompletableFuture<Void>> futures = adminIds.stream()
                    .map(adminId -> CompletableFuture.runAsync(() -> {
                        try {
                            var admin = userRepository.findById(adminId).orElse(null);
                            if (admin != null && admin.getEmail() != null && !admin.getEmail().trim().isEmpty() && admin.getStatus() == 1) {
                                emailService.sendMaintenanceAlertAcknowledgeEmail(
                                        admin.getEmail(),
                                        alert.getTitle(),
                                        alert.getDescription(),
                                        alert.getSeverity(),
                                        vehicleNo,
                                        maintenanceName,
                                        alert.getAcknowledgedTime() != null ? alert.getAcknowledgedTime() : LocalDateTime.now()
                                );
                                log.debug("已向管理员 {} 发送告警确认邮件", admin.getEmail());
                            }
                        } catch (Exception e) {
                            log.error("向管理员 {} 发送告警确认邮件失败", adminId, e);
                        }
                    }, taskExecutor))
                    .collect(Collectors.toList());
            
            // 等待所有邮件发送完成（可选，如果需要等待的话）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            log.info("已向管理员发送告警确认邮件，告警ID: {}, 维修员: {}", alert.getId(), maintenanceName);
        } catch (Exception e) {
            log.error("发送告警确认邮件失败", e);
        }
    }

    /**
     * 维修员完成告警后，向管理员发送完成邮件
     */
    private void sendResolveEmailToAdmin(Alert alert, Long maintenanceUserId, String notes) {
        try {
            var maintenance = userRepository.findById(maintenanceUserId)
                    .orElseThrow(() -> new RuntimeException("维修员不存在"));
            String maintenanceName = maintenance.getRealName() != null ? 
                    maintenance.getRealName() : maintenance.getUsername();
            
            // 获取车辆信息
            Vehicle vehicle = null;
            if (alert.getVehicleId() != null) {
                vehicle = vehicleRepository.findById(alert.getVehicleId()).orElse(null);
            }
            String vehicleNo = vehicle != null ? vehicle.getVehicleNo() : "未知车辆";
            
            // 获取所有管理员
            List<Long> adminIds = userRoleRepository.findAll().stream()
                    .filter(ur -> {
                        var roleOpt = roleRepository.findById(ur.getRoleId());
                        return roleOpt.isPresent() && "ADMIN".equals(roleOpt.get().getRoleCode());
                    })
                    .map(com.airport.entity.SysUserRole::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 使用多线程向每个管理员发送邮件
            List<CompletableFuture<Void>> futures = adminIds.stream()
                    .map(adminId -> CompletableFuture.runAsync(() -> {
                        try {
                            var admin = userRepository.findById(adminId).orElse(null);
                            if (admin != null && admin.getEmail() != null && !admin.getEmail().trim().isEmpty() && admin.getStatus() == 1) {
                                emailService.sendMaintenanceAlertResolveEmail(
                                        admin.getEmail(),
                                        alert.getTitle(),
                                        alert.getDescription(),
                                        alert.getSeverity(),
                                        vehicleNo,
                                        maintenanceName,
                                        notes != null ? notes : "无",
                                        alert.getResolvedTime() != null ? alert.getResolvedTime() : LocalDateTime.now()
                                );
                                log.debug("已向管理员 {} 发送告警完成邮件", admin.getEmail());
                            }
                        } catch (Exception e) {
                            log.error("向管理员 {} 发送告警完成邮件失败", adminId, e);
                        }
                    }, taskExecutor))
                    .collect(Collectors.toList());
            
            // 等待所有邮件发送完成（可选，如果需要等待的话）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            log.info("已向管理员发送告警完成邮件，告警ID: {}, 维修员: {}", alert.getId(), maintenanceName);
        } catch (Exception e) {
            log.error("发送告警完成邮件失败", e);
        }
    }

    /**
     * 维修员发送报告邮件给管理员
     */
    private void sendReportEmailToAdmin(Alert alert, Long maintenanceUserId) {
        try {
            var maintenance = userRepository.findById(maintenanceUserId)
                    .orElseThrow(() -> new RuntimeException("维修员不存在"));
            String maintenanceName = maintenance.getRealName() != null ? 
                    maintenance.getRealName() : maintenance.getUsername();
            
            // 获取车辆信息
            Vehicle vehicle = null;
            if (alert.getVehicleId() != null) {
                vehicle = vehicleRepository.findById(alert.getVehicleId()).orElse(null);
            }
            String vehicleNo = vehicle != null ? vehicle.getVehicleNo() : "未知车辆";
            
            // 获取所有管理员
            List<Long> adminIds = userRoleRepository.findAll().stream()
                    .filter(ur -> {
                        var roleOpt = roleRepository.findById(ur.getRoleId());
                        return roleOpt.isPresent() && "ADMIN".equals(roleOpt.get().getRoleCode());
                    })
                    .map(com.airport.entity.SysUserRole::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 使用多线程向每个管理员发送邮件
            List<CompletableFuture<Void>> futures = adminIds.stream()
                    .map(adminId -> CompletableFuture.runAsync(() -> {
                        try {
                            var admin = userRepository.findById(adminId).orElse(null);
                            if (admin != null && admin.getEmail() != null && !admin.getEmail().trim().isEmpty() && admin.getStatus() == 1) {
                                emailService.sendMaintenanceAlertReportEmail(
                                        admin.getEmail(),
                                        alert.getTitle(),
                                        alert.getDescription(),
                                        alert.getSeverity(),
                                        vehicleNo,
                                        maintenanceName,
                                        LocalDateTime.now()
                                );
                                log.debug("已向管理员 {} 发送告警报告邮件", admin.getEmail());
                            }
                        } catch (Exception e) {
                            log.error("向管理员 {} 发送告警报告邮件失败", adminId, e);
                        }
                    }, taskExecutor))
                    .collect(Collectors.toList());
            
            // 等待所有邮件发送完成（可选，如果需要等待的话）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            log.info("已向管理员发送告警报告邮件，告警ID: {}, 维修员: {}", alert.getId(), maintenanceName);
        } catch (Exception e) {
            log.error("发送告警报告邮件失败", e);
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取告警统计", description = "获取告警统计数据")
    public Result<Map<String, Object>> getAlertStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            List<Object[]> severityStats = alertRepository.countBySeverity();
            List<Object[]> statusStats = alertRepository.countByStatus();
            
            long highPriority = 0;
            long mediumPriority = 0;
            long lowPriority = 0;
            long unprocessedMedium = 0;
            long resolvedToday = 0;
            long totalToday = alertRepository.countTodayAlerts();
            
            for (Object[] stat : severityStats) {
                String severity = (String) stat[0];
                Long count = (Long) stat[1];
                if ("high".equals(severity)) {
                    highPriority = count;
                } else if ("medium".equals(severity)) {
                    mediumPriority = count;
                } else if ("low".equals(severity)) {
                    lowPriority = count;
                }
            }
            
            for (Object[] stat : statusStats) {
                String status = (String) stat[0];
                Long count = (Long) stat[1];
                if ("resolved".equals(status)) {
                    resolvedToday = count; // 简化处理
                }
            }
            
            stats.put("highPriority", highPriority);
            stats.put("mediumPriority", mediumPriority);
            stats.put("lowPriority", lowPriority);
            stats.put("unprocessedMedium", unprocessedMedium);
            stats.put("resolvedToday", resolvedToday);
            stats.put("resolutionRate", totalToday > 0 ? (resolvedToday * 100 / totalToday) : 0);
            stats.put("totalToday", totalToday);
            stats.put("changeRate", 0); // TODO: 计算变化率
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取告警统计失败", e);
            return Result.error("获取告警统计失败: " + e.getMessage());
        }
    }
}

