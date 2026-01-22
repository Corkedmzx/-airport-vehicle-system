package com.airport.service.impl;

import com.airport.entity.VehicleReport;
import com.airport.entity.Vehicle;
import com.airport.entity.Alert;
import com.airport.repository.VehicleReportRepository;
import com.airport.repository.VehicleRepository;
import com.airport.repository.AlertRepository;
import com.airport.service.VehicleReportService;
import com.airport.service.MessageService;
import com.airport.repository.SysUserRepository;
import com.airport.repository.SysUserRoleRepository;
import com.airport.repository.SysRolePermissionRepository;
import com.airport.repository.SysPermissionRepository;
import com.airport.repository.SysRoleRepository;
import com.airport.entity.SysUser;
import com.airport.entity.SysUserRole;
import com.airport.entity.SysRolePermission;
import com.airport.entity.SysRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车辆报告服务实现类
 * 
 * @author Corkedmzx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleReportServiceImpl implements VehicleReportService {

    private final VehicleReportRepository reportRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertRepository alertRepository;
    private final MessageService messageService;
    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysRoleRepository roleRepository;

    @Override
    public VehicleReport createReport(VehicleReport report) {
        // 验证车辆是否存在
        Vehicle vehicle = vehicleRepository.findById(report.getVehicleId())
                .orElseThrow(() -> new RuntimeException("车辆不存在"));

        // 设置默认值
        if (report.getStatus() == null) {
            report.setStatus("pending");
        }
        if (report.getSeverity() == null) {
            report.setSeverity("medium");
        }

        VehicleReport savedReport = reportRepository.save(report);

        // 更新车辆状态为故障（3）
        vehicle.setStatus(3);
        vehicleRepository.save(vehicle);
        log.info("车辆 {} 状态已更新为故障（3），报告ID: {}", vehicle.getVehicleNo(), savedReport.getId());

        // 如果是故障类型的报告，自动创建告警
        if ("fault".equals(report.getReportType()) || "故障".equals(report.getReportType())) {
            createAlertFromReport(savedReport, vehicle);
        }

        // 异步通知相关人员
        // 如果是维修员提交的报告，通知管理员角色；如果是司机提交的报告，通知有vehicle:update权限的用户
        notifyRelevantUsers(savedReport, vehicle);

        log.info("车辆报告已创建: reportId={}, vehicleId={}, reporterId={}", 
                savedReport.getId(), report.getVehicleId(), report.getReporterId());
        
        return savedReport;
    }

    /**
     * 异步通知相关人员
     * 维修员提交的报告通知管理员角色，司机提交的报告通知有vehicle:update权限的用户
     */
    @Async
    private void notifyRelevantUsers(VehicleReport report, Vehicle vehicle) {
        try {
            // 获取报告人信息
            SysUser reporter = userRepository.findById(report.getReporterId()).orElse(null);
            if (reporter == null) {
                log.warn("报告人不存在，无法发送站内信通知");
                return;
            }

            String reporterName = reporter.getRealName() != null ? reporter.getRealName() : reporter.getUsername();
            
            // 检查报告人是否是维修员
            boolean isMaintenance = userRoleRepository.findByUserId(report.getReporterId()).stream()
                    .anyMatch(ur -> {
                        var roleOpt = roleRepository.findById(ur.getRoleId());
                        return roleOpt.isPresent() && "MAINTENANCE".equals(roleOpt.get().getRoleCode());
                    });

            // 构建消息内容
            String title = String.format("车辆问题报告 - %s", vehicle.getVehicleNo());
            String reporterRole = isMaintenance ? "维修员" : "司机";
            String content = String.format(
                "%s %s 提交了车辆问题报告：\n\n" +
                "车辆：%s (%s %s)\n" +
                "报告类型：%s\n" +
                "严重程度：%s\n" +
                "标题：%s\n" +
                "描述：%s\n\n" +
                "请及时处理该报告。",
                reporterRole,
                reporterName,
                vehicle.getVehicleNo(),
                vehicle.getBrand() != null ? vehicle.getBrand() : "",
                vehicle.getModel() != null ? vehicle.getModel() : "",
                getReportTypeText(report.getReportType()),
                getSeverityText(report.getSeverity()),
                report.getTitle(),
                report.getDescription()
            );

            if (isMaintenance) {
                // 维修员提交的报告，通知管理员角色
                messageService.createMessagesForRoles(
                    List.of("ADMIN"),
                    title,
                    content,
                    "vehicle_report",
                    report.getReportType(),
                    report.getSeverity().equals("urgent") ? "high" : report.getSeverity(),
                    report.getId(),
                    "vehicle_report"
                );
                log.info("已为管理员角色发送维修员车辆报告站内信通知");
            } else {
                // 司机提交的报告，通知有vehicle:update权限的用户
                List<Long> userIds = getUsersWithPermission("vehicle:update");
                
                if (userIds.isEmpty()) {
                    log.warn("没有找到有vehicle:update权限的用户，无法发送站内信通知");
                    return;
                }

                messageService.createMessagesForUsers(
                    userIds,
                    title,
                    content,
                    "vehicle_report",
                    report.getReportType(),
                    report.getSeverity().equals("urgent") ? "high" : report.getSeverity(),
                    report.getId(),
                    "vehicle_report"
                );
                log.info("已为{}个有vehicle:update权限的用户发送站内信通知", userIds.size());
            }
        } catch (Exception e) {
            log.error("通知相关人员失败", e);
        }
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
                        List<SysRolePermission> rolePermissions = rolePermissionRepository.findAll()
                                .stream()
                                .filter(rp -> rp.getPermissionId().equals(permission.getId()))
                                .collect(Collectors.toList());

                        // 查找所有拥有这些角色的用户
                        for (SysRolePermission rp : rolePermissions) {
                            List<SysUserRole> userRoles = userRoleRepository.findAll()
                                    .stream()
                                    .filter(ur -> ur.getRoleId().equals(rp.getRoleId()))
                                    .collect(Collectors.toList());

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

    /**
     * 从车辆报告创建告警
     */
    @Async
    private void createAlertFromReport(VehicleReport report, Vehicle vehicle) {
        try {
            Alert alert = new Alert();
            alert.setTitle(String.format("车辆故障告警 - %s", vehicle.getVehicleNo()));
            alert.setDescription(String.format(
                "车辆 %s (%s %s) 报告故障：\n\n" +
                "报告标题：%s\n" +
                "问题描述：%s\n" +
                "严重程度：%s\n" +
                "报告人ID：%s",
                vehicle.getVehicleNo(),
                vehicle.getBrand() != null ? vehicle.getBrand() : "",
                vehicle.getModel() != null ? vehicle.getModel() : "",
                report.getTitle(),
                report.getDescription(),
                getSeverityText(report.getSeverity()),
                report.getReporterId()
            ));
            
            // 根据报告的严重程度设置告警级别
            String alertSeverity = "medium";
            if ("urgent".equals(report.getSeverity()) || "critical".equals(report.getSeverity()) || "紧急".equals(report.getSeverity())) {
                alertSeverity = "high";
            } else if ("high".equals(report.getSeverity()) || "高".equals(report.getSeverity())) {
                alertSeverity = "high";
            } else if ("low".equals(report.getSeverity()) || "低".equals(report.getSeverity())) {
                alertSeverity = "low";
            }
            
            alert.setSeverity(alertSeverity);
            alert.setCategory("vehicle_fault");
            alert.setVehicleId(vehicle.getId());
            alert.setReportId(report.getId());
            alert.setStatus("unprocessed");
            
            Alert savedAlert = alertRepository.save(alert);
            log.info("已从车辆报告创建告警: alertId={}, reportId={}, vehicleId={}", 
                    savedAlert.getId(), report.getId(), vehicle.getId());
        } catch (Exception e) {
            log.error("从车辆报告创建告警失败: reportId={}", report.getId(), e);
        }
    }

    private String getReportTypeText(String reportType) {
        switch (reportType) {
            case "fault": return "故障";
            case "maintenance": return "维修需求";
            case "other": return "其他";
            default: return reportType;
        }
    }

    private String getSeverityText(String severity) {
        switch (severity) {
            case "low": return "低";
            case "medium": return "中";
            case "high": return "高";
            case "urgent": return "紧急";
            default: return severity;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleReport> getReports(Pageable pageable, Long vehicleId, Long reporterId, String status) {
        Specification<VehicleReport> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (vehicleId != null) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicleId));
            }
            if (reporterId != null) {
                predicates.add(cb.equal(root.get("reporterId"), reporterId));
            }
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return reportRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleReport getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("报告不存在"));
    }

    @Override
    public VehicleReport updateReportStatus(Long id, String status, Long handlerId, String handlerNotes) {
        VehicleReport report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("报告不存在"));

        report.setStatus(status);
        report.setHandlerId(handlerId);
        if (handlerNotes != null) {
            report.setHandlerNotes(handlerNotes);
        }
        if ("resolved".equals(status) || "closed".equals(status)) {
            report.setResolveTime(LocalDateTime.now());
        }

        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleReport> getReportsByVehicleId(Long vehicleId) {
        return reportRepository.findByVehicleIdOrderByCreateTimeDesc(vehicleId);
    }
}
