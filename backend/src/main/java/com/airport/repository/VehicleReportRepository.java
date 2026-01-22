package com.airport.repository;

import com.airport.entity.VehicleReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 车辆报告数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface VehicleReportRepository extends JpaRepository<VehicleReport, Long>, JpaSpecificationExecutor<VehicleReport> {

    /**
     * 根据车辆ID查找报告
     */
    List<VehicleReport> findByVehicleIdOrderByCreateTimeDesc(Long vehicleId);

    /**
     * 根据报告人ID查找报告
     */
    Page<VehicleReport> findByReporterIdOrderByCreateTimeDesc(Long reporterId, Pageable pageable);

    /**
     * 根据处理人ID查找报告
     */
    Page<VehicleReport> findByHandlerIdOrderByCreateTimeDesc(Long handlerId, Pageable pageable);

    /**
     * 根据状态查找报告
     */
    Page<VehicleReport> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);

    /**
     * 查找所有报告（分页）
     */
    Page<VehicleReport> findAllByOrderByCreateTimeDesc(Pageable pageable);

    /**
     * 根据车辆ID和状态查找报告
     */
    List<VehicleReport> findByVehicleIdAndStatusOrderByCreateTimeDesc(Long vehicleId, String status);
}
