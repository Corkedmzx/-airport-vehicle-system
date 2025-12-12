package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.SystemLog;
import com.airport.repository.SystemLogRepository;
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
 * 系统日志控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Tag(name = "系统日志", description = "系统日志管理相关接口")
public class SystemLogController {

    private final SystemLogRepository logRepository;

    @GetMapping
    @Operation(summary = "获取日志列表", description = "分页获取系统日志")
    public Result<Page<SystemLog>> getLogs(
            @Parameter(description = "页码", required = false) 
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", required = false) 
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "日志级别", required = false) 
            @RequestParam(required = false) String level,
            @Parameter(description = "日志类别", required = false) 
            @RequestParam(required = false) String category,
            @Parameter(description = "关键词", required = false) 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "开始时间", required = false) 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间", required = false) 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            
            // 这里简化处理，实际应该使用Specification进行复杂查询
            Page<SystemLog> logs = logRepository.findAll(pageable);
            
            // 如果有筛选条件，需要在前端或使用Specification进行筛选
            return Result.success(logs);
        } catch (Exception e) {
            log.error("获取日志列表失败", e);
            return Result.error("获取日志列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取日志详情", description = "根据ID获取日志详细信息")
    public Result<SystemLog> getLogById(
            @Parameter(description = "日志ID", required = true) 
            @PathVariable Long id) {
        try {
            SystemLog log = logRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("日志不存在"));
            return Result.success(log);
        } catch (Exception e) {
            log.error("获取日志详情失败", e);
            return Result.error("获取日志详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取日志统计", description = "获取日志统计数据")
    public Result<Map<String, Object>> getLogStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 统计各级别日志数量
            long totalLogs = logRepository.count();
            long errorLogs = logRepository.countByLevel("ERROR");
            long warnLogs = logRepository.countByLevel("WARN");
            long infoLogs = logRepository.countByLevel("INFO");
            
            // 统计今日日志
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            long todayLogs = logRepository.countByCreateTimeBetween(startOfDay, endOfDay);
            
            stats.put("totalLogs", totalLogs);
            stats.put("errorLogs", errorLogs);
            stats.put("warningLogs", warnLogs);
            stats.put("infoLogs", infoLogs);
            stats.put("todayLogs", todayLogs);
            stats.put("unreadErrors", errorLogs); // 简化处理
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            return Result.error("获取日志统计失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除日志", description = "删除指定日志")
    public Result<String> deleteLog(
            @Parameter(description = "日志ID", required = true) 
            @PathVariable Long id) {
        try {
            logRepository.deleteById(id);
            return Result.success("日志删除成功");
        } catch (Exception e) {
            log.error("删除日志失败", e);
            return Result.error("删除日志失败: " + e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "清空日志", description = "清空所有日志")
    public Result<String> clearLogs() {
        try {
            logRepository.deleteAll();
            return Result.success("日志已清空");
        } catch (Exception e) {
            log.error("清空日志失败", e);
            return Result.error("清空日志失败: " + e.getMessage());
        }
    }
}

