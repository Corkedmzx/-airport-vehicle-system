package com.airport.controller;

import com.airport.dto.Result;
import com.airport.dto.VehicleDTO;
import com.airport.dto.VehicleLocationDTO;
import com.airport.dto.VehicleStatistics;
import com.airport.entity.Vehicle;
import com.airport.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 车辆管理控制器
 * 
 * @author MiniMax Agent
 */
@Slf4j
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "车辆管理", description = "车辆信息管理相关接口")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "获取车辆列表", description = "获取所有车辆信息")
    public Result<List<Vehicle>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return Result.success(vehicles);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取车辆详情", description = "根据ID获取车辆详细信息")
    public Result<Vehicle> getVehicleById(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id) {
        Optional<Vehicle> vehicle = vehicleService.getVehicleById(id);
        if (vehicle.isPresent()) {
            return Result.success(vehicle.get());
        } else {
            return Result.notFound("车辆不存在");
        }
    }

    @GetMapping("/by-number/{vehicleNo}")
    @Operation(summary = "根据车牌号获取车辆", description = "根据车牌号获取车辆信息")
    public Result<Vehicle> getVehicleByNo(
            @Parameter(description = "车牌号", required = true) 
            @PathVariable String vehicleNo) {
        Optional<Vehicle> vehicle = vehicleService.getVehicleByNo(vehicleNo);
        if (vehicle.isPresent()) {
            return Result.success(vehicle.get());
        } else {
            return Result.notFound("车辆不存在");
        }
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "根据状态获取车辆", description = "根据车辆状态获取车辆列表")
    public Result<List<Vehicle>> getVehiclesByStatus(
            @Parameter(description = "车辆状态:0-停用,1-正常,2-维修中,3-故障", required = true) 
            @PathVariable Integer status) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByStatus(status);
        return Result.success(vehicles);
    }

    @GetMapping("/active")
    @Operation(summary = "获取正常车辆", description = "获取所有正常状态的车辆")
    public Result<List<Vehicle>> getActiveVehicles() {
        List<Vehicle> vehicles = vehicleService.getActiveVehicles();
        return Result.success(vehicles);
    }

    @GetMapping("/by-type/{vehicleTypeId}")
    @Operation(summary = "根据类型获取车辆", description = "根据车辆类型获取车辆列表")
    public Result<List<Vehicle>> getVehiclesByType(
            @Parameter(description = "车辆类型ID", required = true) 
            @PathVariable Long vehicleTypeId) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByType(vehicleTypeId);
        return Result.success(vehicles);
    }

    @PostMapping
    @Operation(summary = "创建车辆", description = "创建新的车辆记录")
    public Result<Vehicle> createVehicle(@RequestBody Vehicle vehicle) {
        try {
            Vehicle createdVehicle = vehicleService.createVehicle(vehicle);
            return Result.success("车辆创建成功", createdVehicle);
        } catch (Exception e) {
            log.error("创建车辆失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新车辆", description = "更新车辆信息")
    public Result<Vehicle> updateVehicle(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id,
            @RequestBody Vehicle vehicle) {
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle);
            return Result.success("车辆更新成功", updatedVehicle);
        } catch (Exception e) {
            log.error("更新车辆失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除车辆", description = "删除车辆记录")
    public Result<String> deleteVehicle(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id) {
        try {
            vehicleService.deleteVehicle(id);
            return Result.success("车辆删除成功");
        } catch (Exception e) {
            log.error("删除车辆失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "更新车辆位置", description = "更新车辆实时位置信息")
    public Result<Vehicle> updateVehicleLocation(
            @Parameter(description = "车辆ID", required = true) 
            @PathVariable Long id,
            @RequestBody VehicleLocationDTO locationDTO) {
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicleLocation(id, locationDTO);
            return Result.success("位置更新成功", updatedVehicle);
        } catch (Exception e) {
            log.error("更新车辆位置失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取车辆统计", description = "获取车辆统计信息")
    public Result<VehicleStatistics> getVehicleStatistics() {
        VehicleStatistics statistics = vehicleService.getVehicleStatistics();
        return Result.success(statistics);
    }
}