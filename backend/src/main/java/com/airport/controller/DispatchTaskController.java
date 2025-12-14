package com.airport.controller;

import com.airport.dto.Result;
import com.airport.dto.TaskStatistics;
import com.airport.entity.DispatchTask;
import com.airport.service.DispatchTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 调度任务管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "调度任务", description = "调度任务管理相关接口")
public class DispatchTaskController {

    private final DispatchTaskService taskService;

    @GetMapping
    @Operation(summary = "获取任务列表", description = "获取所有调度任务")
    public Result<List<DispatchTask>> getAllTasks() {
        List<DispatchTask> tasks = taskService.getAllTasks();
        return Result.success(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情", description = "根据ID获取任务详细信息")
    public Result<DispatchTask> getTaskById(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id) {
        Optional<DispatchTask> task = taskService.getTaskById(id);
        if (task.isPresent()) {
            return Result.success(task.get());
        } else {
            return Result.notFound("任务不存在");
        }
    }

    @GetMapping("/by-number/{taskNo}")
    @Operation(summary = "根据任务编号获取任务", description = "根据任务编号获取任务信息")
    public Result<DispatchTask> getTaskByNo(
            @Parameter(description = "任务编号", required = true) 
            @PathVariable String taskNo) {
        Optional<DispatchTask> task = taskService.getTaskByNo(taskNo);
        if (task.isPresent()) {
            return Result.success(task.get());
        } else {
            return Result.notFound("任务不存在");
        }
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "根据状态获取任务", description = "根据任务状态获取任务列表")
    public Result<List<DispatchTask>> getTasksByStatus(
            @Parameter(description = "任务状态", required = true) 
            @PathVariable Integer status) {
        List<DispatchTask> tasks = taskService.getTasksByStatus(status);
        return Result.success(tasks);
    }

    @GetMapping("/by-vehicle/{vehicleId}")
    @Operation(summary = "根据车辆获取任务", description = "根据车辆ID获取关联的任务")
    public Result<List<DispatchTask>> getTasksByVehicle(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long vehicleId) {
        List<DispatchTask> tasks = taskService.getTasksByVehicleId(vehicleId);
        return Result.success(tasks);
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待分配任务", description = "获取所有待分配状态的任务")
    public Result<List<DispatchTask>> getPendingTasks() {
        List<DispatchTask> tasks = taskService.getPendingTasks();
        return Result.success(tasks);
    }

    @GetMapping("/in-progress")
    @Operation(summary = "获取进行中任务", description = "获取所有进行中的任务")
    public Result<List<DispatchTask>> getInProgressTasks() {
        List<DispatchTask> tasks = taskService.getInProgressTasks();
        return Result.success(tasks);
    }

    @GetMapping("/by-time-range")
    @Operation(summary = "获取时间范围内的任务", description = "根据时间范围获取任务")
    public Result<List<DispatchTask>> getTasksByTimeRange(
            @Parameter(description = "开始时间", required = true) 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true) 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<DispatchTask> tasks = taskService.getTasksByTimeRange(startTime, endTime);
        return Result.success(tasks);
    }

    @PostMapping
    @Operation(summary = "创建任务", description = "创建新的调度任务")
    public Result<DispatchTask> createTask(@RequestBody DispatchTask task) {
        try {
            DispatchTask createdTask = taskService.createTask(task);
            return Result.success("任务创建成功", createdTask);
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务", description = "更新任务信息")
    public Result<DispatchTask> updateTask(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id,
            @RequestBody DispatchTask task) {
        try {
            DispatchTask updatedTask = taskService.updateTask(id, task);
            return Result.success("任务更新成功", updatedTask);
        } catch (Exception e) {
            log.error("更新任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务", description = "删除任务记录")
    public Result<String> deleteTask(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return Result.success("任务删除成功");
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "分配任务", description = "将任务分配给车辆和司机")
    public Result<DispatchTask> assignTask(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "车辆ID", required = true) 
            @RequestParam Long vehicleId,
            @Parameter(description = "司机ID", required = false) 
            @RequestParam(required = false) Long driverId) {
        try {
            DispatchTask updatedTask = taskService.assignTask(id, vehicleId, driverId);
            return Result.success("任务分配成功", updatedTask);
        } catch (Exception e) {
            log.error("分配任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/start")
    @Operation(summary = "开始任务", description = "开始执行任务")
    public Result<DispatchTask> startTask(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id) {
        try {
            DispatchTask updatedTask = taskService.startTask(id);
            return Result.success("任务开始成功", updatedTask);
        } catch (Exception e) {
            log.error("开始任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "完成任务", description = "标记任务为已完成")
    public Result<DispatchTask> completeTask(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id) {
        try {
            DispatchTask updatedTask = taskService.completeTask(id);
            return Result.success("任务完成成功", updatedTask);
        } catch (Exception e) {
            log.error("完成任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消任务", description = "取消任务执行")
    public Result<DispatchTask> cancelTask(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "取消原因", required = false) 
            @RequestParam(required = false) String reason) {
        try {
            DispatchTask updatedTask = taskService.cancelTask(id, reason);
            return Result.success("任务取消成功", updatedTask);
        } catch (Exception e) {
            log.error("取消任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "重新发送任务", description = "复制已完成的任务并生成新任务编号")
    public Result<DispatchTask> resendTask(
            @Parameter(description = "任务ID", required = true) 
            @PathVariable Long id) {
        try {
            DispatchTask newTask = taskService.resendTask(id);
            return Result.success("任务重新发送成功", newTask);
        } catch (Exception e) {
            log.error("重新发送任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取任务统计", description = "获取任务统计信息")
    public Result<TaskStatistics> getTaskStatistics() {
        TaskStatistics statistics = taskService.getTaskStatistics();
        return Result.success(statistics);
    }
}