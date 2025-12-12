package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.Alert;
import com.airport.repository.AlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "确认告警", description = "确认处理告警")
    public Result<Alert> acknowledgeAlert(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "处理人", required = false) 
            @RequestParam(required = false) String assignee) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            
            alert.setStatus("processing");
            alert.setAcknowledged(true);
            alert.setAcknowledgedTime(LocalDateTime.now());
            if (assignee != null) {
                alert.setAssignee(assignee);
            }
            
            Alert updatedAlert = alertRepository.save(alert);
            return Result.success("告警已确认", updatedAlert);
        } catch (Exception e) {
            log.error("确认告警失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "解决告警", description = "标记告警为已解决")
    public Result<Alert> resolveAlert(
            @Parameter(description = "告警ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "处理说明", required = false) 
            @RequestParam(required = false) String notes) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警不存在"));
            
            alert.setStatus("resolved");
            alert.setResolvedTime(LocalDateTime.now());
            if (notes != null) {
                alert.setResolutionNotes(notes);
            }
            
            Alert updatedAlert = alertRepository.save(alert);
            return Result.success("告警已解决", updatedAlert);
        } catch (Exception e) {
            log.error("解决告警失败", e);
            return Result.error(e.getMessage());
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

