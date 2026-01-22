package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.VehicleReport;
import com.airport.service.VehicleReportService;
import com.airport.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆报告控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/vehicle-reports")
@RequiredArgsConstructor
@Tag(name = "车辆报告", description = "车辆问题报告相关接口")
public class VehicleReportController {

    private final VehicleReportService reportService;
    private final JwtUtils jwtUtils;

    /**
     * 从请求头中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtils.getUsernameFromToken(token);
                if (jwtUtils.validateToken(token, username)) {
                    // 这里需要根据username获取userId，简化处理，实际应该注入UserService
                    return jwtUtils.getUserIdFromToken(token);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
        }
        return null;
    }

    @PostMapping
    @Operation(summary = "创建车辆报告", description = "司机提交车辆问题报告")
    public Result<VehicleReport> createReport(
            @RequestBody VehicleReport report,
            HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.error("未认证或认证已过期");
            }
            
            // 设置报告人ID
            report.setReporterId(currentUserId);
            
            VehicleReport createdReport = reportService.createReport(report);
            return Result.success("报告提交成功", createdReport);
        } catch (Exception e) {
            log.error("创建车辆报告失败", e);
            return Result.error("创建报告失败: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "获取报告列表", description = "分页获取车辆报告列表")
    public Result<Page<VehicleReport>> getReports(
            @Parameter(description = "页码", required = false)
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", required = false)
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "车辆ID", required = false)
            @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "报告人ID", required = false)
            @RequestParam(required = false) Long reporterId,
            @Parameter(description = "状态", required = false)
            @RequestParam(required = false) String status) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<VehicleReport> reports = reportService.getReports(pageable, vehicleId, reporterId, status);
            return Result.success(reports);
        } catch (Exception e) {
            log.error("获取报告列表失败", e);
            return Result.error("获取报告列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取报告详情", description = "根据ID获取报告详细信息")
    public Result<VehicleReport> getReportById(
            @Parameter(description = "报告ID", required = true)
            @PathVariable Long id) {
        try {
            VehicleReport report = reportService.getReportById(id);
            return Result.success(report);
        } catch (Exception e) {
            log.error("获取报告详情失败", e);
            return Result.error("获取报告详情失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新报告状态", description = "更新报告处理状态（需要vehicle:update权限）")
    public Result<VehicleReport> updateReportStatus(
            @Parameter(description = "报告ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "状态", required = true)
            @RequestParam String status,
            @Parameter(description = "处理人ID", required = false)
            @RequestParam(required = false) Long handlerId,
            @Parameter(description = "处理备注", required = false)
            @RequestParam(required = false) String handlerNotes,
            HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return Result.error("未认证或认证已过期");
            }
            
            // 如果没有指定处理人，使用当前用户
            if (handlerId == null) {
                handlerId = currentUserId;
            }
            
            VehicleReport updatedReport = reportService.updateReportStatus(id, status, handlerId, handlerNotes);
            return Result.success("状态更新成功", updatedReport);
        } catch (Exception e) {
            log.error("更新报告状态失败", e);
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "获取车辆的报告列表", description = "根据车辆ID获取该车辆的所有报告")
    public Result<List<VehicleReport>> getReportsByVehicleId(
            @Parameter(description = "车辆ID", required = true)
            @PathVariable Long vehicleId) {
        try {
            List<VehicleReport> reports = reportService.getReportsByVehicleId(vehicleId);
            return Result.success(reports);
        } catch (Exception e) {
            log.error("获取车辆报告列表失败", e);
            return Result.error("获取报告列表失败: " + e.getMessage());
        }
    }
}
