package com.airport.controller;

import com.airport.dto.Result;
import com.airport.dto.TaskStatistics;
import com.airport.dto.VehicleStatistics;
import com.airport.entity.DispatchTask;
import com.airport.entity.Vehicle;
import com.airport.repository.AlertRepository;
import com.airport.repository.DispatchTaskRepository;
import com.airport.repository.VehicleRepository;
import com.airport.service.DispatchTaskService;
import com.airport.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统统计控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "系统统计", description = "系统数据统计相关接口")
public class StatisticsController {

    // 注入统计服务
    private final VehicleService vehicleService;
    private final DispatchTaskService taskService;
    private final VehicleRepository vehicleRepository;
    private final DispatchTaskRepository taskRepository;
    private final AlertRepository alertRepository;

    @GetMapping("/system")
    @Operation(summary = "系统概览统计", description = "获取系统总体统计信息")
    public Result<Map<String, Object>> getSystemOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 系统基本信息
            overview.put("systemName", "机场车辆监控与调度系统");
            overview.put("version", "1.0.0");
            overview.put("currentTime", LocalDateTime.now());
            
            // 调用实际的统计服务
            VehicleStatistics vehicleStats = vehicleService.getVehicleStatistics();
            TaskStatistics taskStats = taskService.getTaskStatistics();
            
            Map<String, Object> vehicleStatsMap = new HashMap<>();
            vehicleStatsMap.put("totalCount", vehicleStats.getTotalCount());
            vehicleStatsMap.put("activeCount", vehicleStats.getActiveCount());
            vehicleStatsMap.put("maintenanceCount", vehicleStats.getMaintenanceCount());
            vehicleStatsMap.put("faultCount", vehicleStats.getFaultCount());
            vehicleStatsMap.put("offlineCount", vehicleStats.getOfflineCount());
            
            Map<String, Object> taskStatsMap = new HashMap<>();
            taskStatsMap.put("totalCount", taskStats.getTotalCount());
            taskStatsMap.put("pendingCount", taskStats.getPendingCount());
            taskStatsMap.put("assignedCount", taskStats.getAssignedCount());
            taskStatsMap.put("inProgressCount", taskStats.getInProgressCount());
            taskStatsMap.put("completedCount", taskStats.getCompletedCount());
            taskStatsMap.put("cancelledCount", taskStats.getCancelledCount());
            taskStatsMap.put("exceptionCount", taskStats.getExceptionCount());
            taskStatsMap.put("todayCount", taskStats.getTodayCount());
            
            overview.put("vehicleStatistics", vehicleStatsMap);
            overview.put("taskStatistics", taskStatsMap);
            
            return Result.success(overview);
            
        } catch (Exception e) {
            log.error("获取系统统计失败", e);
            return Result.error("获取系统统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "仪表盘数据", description = "获取仪表盘展示的数据")
    public Result<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // 实时数据
            Map<String, Object> realtime = new HashMap<>();
            realtime.put("onlineVehicles", 35L);
            realtime.put("activeTasks", 8L);
            realtime.put("alertCount", 2L);
            realtime.put("lastUpdateTime", LocalDateTime.now());
            
            // 今日概况
            Map<String, Object> todaySummary = new HashMap<>();
            todaySummary.put("tasksCompleted", 12L);
            todaySummary.put("totalDistance", 1250.5);
            todaySummary.put("fuelConsumption", 320.0);
            todaySummary.put("averageEfficiency", 0.85);
            
            // 趋势数据 (最近7天)
            Map<String, Object> trendData = new HashMap<>();
            trendData.put("dailyTasks", new long[]{8, 12, 15, 10, 18, 14, 16});
            trendData.put("dailyDistance", new double[]{980.5, 1250.8, 1520.3, 1180.2, 1650.7, 1420.1, 1580.9});
            
            dashboard.put("realtime", realtime);
            dashboard.put("todaySummary", todaySummary);
            dashboard.put("trendData", trendData);
            
            return Result.success(dashboard);
            
        } catch (Exception e) {
            log.error("获取仪表盘数据失败", e);
            return Result.error("获取仪表盘数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/monitoring")
    @Operation(summary = "实时监控统计数据", description = "获取实时监控页面的统计数据")
    public Result<Map<String, Object>> getMonitoringStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 在线车辆数（状态为1的正常车辆）
            long activeVehicles = vehicleRepository.findActiveVehicles().size();
            long totalVehicles = vehicleRepository.count();
            int activeRate = totalVehicles > 0 ? (int) (activeVehicles * 100 / totalVehicles) : 0;
            
            // 告警统计
            List<Object[]> alertStatusStats = alertRepository.countByStatus();
            long totalAlerts = 0;
            long pendingAlerts = 0;
            for (Object[] stat : alertStatusStats) {
                String status = (String) stat[0];
                Long count = (Long) stat[1];
                totalAlerts += count;
                if ("unprocessed".equals(status)) {
                    pendingAlerts = count;
                }
            }
            
            // 执行中任务数
            long runningTasks = taskRepository.findInProgressTasks().size();
            
            // 任务完成率
            TaskStatistics taskStats = taskService.getTaskStatistics();
            int completionRate = 0;
            if (taskStats.getTotalCount() > 0) {
                completionRate = (int) (taskStats.getCompletedCount() * 100 / taskStats.getTotalCount());
            }
            
            // 系统运行时间（从应用启动时间计算，这里简化处理）
            // 实际应该从系统启动时间计算
            long days = 15;
            long hours = 8;
            String systemUptime = days + "天 " + hours + "小时";
            
            stats.put("activeVehicles", activeVehicles);
            stats.put("activeRate", activeRate);
            stats.put("alerts", totalAlerts);
            stats.put("pendingAlerts", pendingAlerts);
            stats.put("runningTasks", runningTasks);
            stats.put("completionRate", completionRate);
            stats.put("systemUptime", systemUptime);
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取实时监控统计失败", e);
            return Result.error("获取实时监控统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/dispatch")
    @Operation(summary = "调度中心统计数据", description = "获取调度中心页面的统计数据")
    public Result<Map<String, Object>> getDispatchStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 待分配任务数
            long pendingTasks = taskRepository.findPendingTasks().size();
            
            // 可用车辆数（状态为1且没有任务关联的车辆）
            List<Vehicle> activeVehicles = vehicleRepository.findActiveVehicles();
            List<DispatchTask> allTasks = taskRepository.findAll();
            Set<Long> vehiclesWithTasks = allTasks.stream()
                .filter(t -> t.getAssignedVehicleId() != null && (t.getStatus() == 2 || t.getStatus() == 3))
                .map(DispatchTask::getAssignedVehicleId)
                .collect(Collectors.toSet());
            
            long availableVehicles = activeVehicles.stream()
                .filter(v -> !vehiclesWithTasks.contains(v.getId()))
                .count();
            
            // 空闲率
            int idleRate = activeVehicles.size() > 0 
                ? (int) (availableVehicles * 100 / activeVehicles.size()) 
                : 0;
            
            // 调度效率（已完成任务数 / 总任务数）
            TaskStatistics taskStats = taskService.getTaskStatistics();
            int dispatchEfficiency = 0;
            if (taskStats.getTotalCount() > 0) {
                dispatchEfficiency = (int) (taskStats.getCompletedCount() * 100 / taskStats.getTotalCount());
            }
            
            // 效率变化（简化处理，实际应该对比昨日数据）
            int efficiencyChange = 5;
            
            // 平均响应时间（简化处理，实际应该计算任务分配的平均时间）
            double avgResponseTime = 3.2;
            
            stats.put("pendingTasks", pendingTasks);
            stats.put("availableVehicles", availableVehicles);
            stats.put("idleRate", idleRate);
            stats.put("dispatchEfficiency", dispatchEfficiency);
            stats.put("efficiencyChange", efficiencyChange);
            stats.put("avgResponseTime", avgResponseTime);
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取调度中心统计失败", e);
            return Result.error("获取调度中心统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/vehicle-usage")
    @Operation(summary = "车辆使用情况排行", description = "获取车辆使用情况排行数据")
    public Result<List<Map<String, Object>>> getVehicleUsageRanking() {
        try {
            List<Vehicle> allVehicles = vehicleRepository.findAll();
            List<DispatchTask> allTasks = taskRepository.findAll();
            
            // 按车辆统计任务数据
            Map<Long, VehicleUsageData> vehicleUsageMap = new HashMap<>();
            
            for (Vehicle vehicle : allVehicles) {
                VehicleUsageData usageData = new VehicleUsageData();
                usageData.setVehicleId(vehicle.getId());
                usageData.setVehicleNo(vehicle.getVehicleNo());
                usageData.setTotalTasks(0L);
                usageData.setCompletedTasks(0L);
                usageData.setTotalDistance(BigDecimal.ZERO);
                vehicleUsageMap.put(vehicle.getId(), usageData);
            }
            
            // 统计每个车辆的任务
            for (DispatchTask task : allTasks) {
                if (task.getAssignedVehicleId() != null) {
                    VehicleUsageData usageData = vehicleUsageMap.get(task.getAssignedVehicleId());
                    if (usageData != null) {
                        usageData.setTotalTasks(usageData.getTotalTasks() + 1);
                        if (task.getStatus() == 4) { // 已完成
                            usageData.setCompletedTasks(usageData.getCompletedTasks() + 1);
                        }
                        // 这里简化处理，实际应该从任务中获取里程数据
                        // usageData.setTotalDistance(usageData.getTotalDistance().add(task.getDistance()));
                    }
                }
            }
            
            // 计算使用率并排序
            List<Map<String, Object>> ranking = vehicleUsageMap.values().stream()
                .map(usage -> {
                    int usageRate = usage.getTotalTasks() > 0 
                        ? (int) (usage.getCompletedTasks() * 100 / usage.getTotalTasks())
                        : 0;
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("vehicleNo", usage.getVehicleNo());
                    item.put("totalTasks", usage.getTotalTasks());
                    item.put("completedTasks", usage.getCompletedTasks());
                    item.put("totalDistance", usage.getTotalDistance().doubleValue());
                    item.put("usageRate", usageRate);
                    return item;
                })
                .filter(item -> (Long) item.get("totalTasks") > 0) // 只显示有任务的车辆
                .sorted((a, b) -> Long.compare((Long) b.get("totalTasks"), (Long) a.get("totalTasks"))) // 按任务总数降序
                .limit(10) // 取前10名
                .collect(Collectors.toList());
            
            return Result.success(ranking);
        } catch (Exception e) {
            log.error("获取车辆使用情况排行失败", e);
            return Result.error("获取车辆使用情况排行失败: " + e.getMessage());
        }
    }

    @GetMapping("/task-efficiency")
    @Operation(summary = "任务效率统计", description = "获取任务效率统计数据")
    public Result<List<Map<String, Object>>> getTaskEfficiency() {
        try {
            List<DispatchTask> allTasks = taskRepository.findAll();
            
            // 按任务类型分组统计
            Map<String, TaskEfficiencyData> efficiencyMap = new HashMap<>();
            
            for (DispatchTask task : allTasks) {
                String taskType = task.getTaskType();
                TaskEfficiencyData efficiency = efficiencyMap.computeIfAbsent(
                    taskType, 
                    k -> new TaskEfficiencyData(taskType)
                );
                
                efficiency.setTotalCount(efficiency.getTotalCount() + 1);
                
                if (task.getStatus() == 4) { // 已完成
                    efficiency.setCompletedCount(efficiency.getCompletedCount() + 1);
                    
                    // 计算平均耗时
                    if (task.getActualStartTime() != null && task.getActualEndTime() != null) {
                        long hours = ChronoUnit.HOURS.between(task.getActualStartTime(), task.getActualEndTime());
                        efficiency.addDuration(hours);
                    }
                }
            }
            
            // 转换为返回格式
            List<Map<String, Object>> efficiencyList = efficiencyMap.values().stream()
                .map(efficiency -> {
                    int completionRate = efficiency.getTotalCount() > 0
                        ? (int) (efficiency.getCompletedCount() * 100 / efficiency.getTotalCount())
                        : 0;
                    
                    double avgDuration = efficiency.getCompletedCount() > 0
                        ? (double) efficiency.getTotalDuration() / efficiency.getCompletedCount()
                        : 0.0;
                    
                    String efficiencyLevel;
                    if (completionRate >= 95) {
                        efficiencyLevel = "优秀";
                    } else if (completionRate >= 85) {
                        efficiencyLevel = "良好";
                    } else {
                        efficiencyLevel = "一般";
                    }
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("taskType", efficiency.getTaskType());
                    item.put("totalCount", efficiency.getTotalCount());
                    item.put("avgDuration", Math.round(avgDuration * 10.0) / 10.0); // 保留一位小数
                    item.put("completionRate", completionRate);
                    item.put("efficiency", efficiencyLevel);
                    return item;
                })
                .collect(Collectors.toList());
            
            return Result.success(efficiencyList);
        } catch (Exception e) {
            log.error("获取任务效率统计失败", e);
            return Result.error("获取任务效率统计失败: " + e.getMessage());
        }
    }

    // 内部类：车辆使用情况数据
    private static class VehicleUsageData {
        private Long vehicleId;
        private String vehicleNo;
        private Long totalTasks = 0L;
        private Long completedTasks = 0L;
        private BigDecimal totalDistance = BigDecimal.ZERO;

        public Long getVehicleId() { return vehicleId; }
        public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
        public String getVehicleNo() { return vehicleNo; }
        public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }
        public Long getTotalTasks() { return totalTasks; }
        public void setTotalTasks(Long totalTasks) { this.totalTasks = totalTasks; }
        public Long getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(Long completedTasks) { this.completedTasks = completedTasks; }
        public BigDecimal getTotalDistance() { return totalDistance; }
        public void setTotalDistance(BigDecimal totalDistance) { this.totalDistance = totalDistance; }
    }

    // 内部类：任务效率数据
    private static class TaskEfficiencyData {
        private String taskType;
        private Long totalCount = 0L;
        private Long completedCount = 0L;
        private Long totalDuration = 0L;

        public TaskEfficiencyData(String taskType) {
            this.taskType = taskType;
        }

        public void addDuration(long hours) {
            this.totalDuration += hours;
        }

        public String getTaskType() { return taskType; }
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getCompletedCount() { return completedCount; }
        public void setCompletedCount(Long completedCount) { this.completedCount = completedCount; }
        public Long getTotalDuration() { return totalDuration; }
    }
}