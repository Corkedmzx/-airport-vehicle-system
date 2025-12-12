package com.airport.controller;

import com.airport.dto.Result;
import com.airport.dto.TaskStatistics;
import com.airport.dto.VehicleStatistics;
import com.airport.service.DispatchTaskService;
import com.airport.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
}