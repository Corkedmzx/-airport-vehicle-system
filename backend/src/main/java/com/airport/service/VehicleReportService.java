package com.airport.service;

import com.airport.entity.VehicleReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 车辆报告服务接口
 * 
 * @author Corkedmzx
 */
public interface VehicleReportService {

    /**
     * 创建车辆报告
     */
    VehicleReport createReport(VehicleReport report);

    /**
     * 获取报告列表（分页）
     */
    Page<VehicleReport> getReports(Pageable pageable, Long vehicleId, Long reporterId, String status);

    /**
     * 根据ID获取报告
     */
    VehicleReport getReportById(Long id);

    /**
     * 更新报告状态
     */
    VehicleReport updateReportStatus(Long id, String status, Long handlerId, String handlerNotes);

    /**
     * 根据车辆ID获取报告列表
     */
    List<VehicleReport> getReportsByVehicleId(Long vehicleId);
}
