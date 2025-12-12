package com.airport.service.impl;

import com.airport.entity.Vehicle;
import com.airport.dto.VehicleLocationDTO;
import com.airport.dto.VehicleStatistics;
import com.airport.repository.VehicleRepository;
import com.airport.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 车辆服务实现
 * 
 * @author MiniMax Agent
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> getVehicleByNo(String vehicleNo) {
        return vehicleRepository.findByVehicleNo(vehicleNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByStatus(Integer status) {
        return vehicleRepository.findByStatus(status);
    }

    @Override
    public Vehicle createVehicle(Vehicle vehicle) {
        // 检查车牌号是否已存在
        if (vehicleRepository.findByVehicleNo(vehicle.getVehicleNo()).isPresent()) {
            throw new RuntimeException("车牌号已存在");
        }

        // 检查GPS设备ID是否已存在
        if (vehicle.getGpsDeviceId() != null && 
            vehicleRepository.findByGpsDeviceId(vehicle.getGpsDeviceId()).isPresent()) {
            throw new RuntimeException("GPS设备ID已存在");
        }

        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle updateVehicle(Long id, Vehicle vehicle) {
        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("车辆不存在"));

        // 更新字段
        existingVehicle.setBrand(vehicle.getBrand());
        existingVehicle.setModel(vehicle.getModel());
        existingVehicle.setColor(vehicle.getColor());
        existingVehicle.setEngineNo(vehicle.getEngineNo());
        existingVehicle.setVin(vehicle.getVin());
        existingVehicle.setPurchaseDate(vehicle.getPurchaseDate());
        existingVehicle.setMileage(vehicle.getMileage());
        existingVehicle.setFuelCapacity(vehicle.getFuelCapacity());
        existingVehicle.setCurrentFuel(vehicle.getCurrentFuel());
        existingVehicle.setStatus(vehicle.getStatus());

        return vehicleRepository.save(existingVehicle);
    }

    @Override
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }

    @Override
    public Vehicle updateVehicleLocation(Long vehicleId, VehicleLocationDTO locationDTO) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("车辆不存在"));

        // 更新位置信息
        vehicle.setLocationLongitude(locationDTO.getLongitude());
        vehicle.setLocationLatitude(locationDTO.getLatitude());
        vehicle.setLocationAddress(locationDTO.getAddress());
        vehicle.setLastUpdateTime(LocalDateTime.now());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        // TODO: 这里可以发送WebSocket消息给前端，实时更新车辆位置
        
        log.info("车辆 {} 位置更新: ({}, {})", 
                updatedVehicle.getVehicleNo(), 
                locationDTO.getLongitude(), 
                locationDTO.getLatitude());

        return updatedVehicle;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getActiveVehicles() {
        return vehicleRepository.findActiveVehicles();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByType(Long vehicleTypeId) {
        return vehicleRepository.findByVehicleTypeId(vehicleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleStatistics getVehicleStatistics() {
        List<Object[]> stats = vehicleRepository.countVehiclesByStatus();
        
        VehicleStatistics statistics = new VehicleStatistics();
        statistics.setTotalCount(0L);
        statistics.setActiveCount(0L);
        statistics.setMaintenanceCount(0L);
        statistics.setFaultCount(0L);
        statistics.setOfflineCount(0L);

        for (Object[] stat : stats) {
            Integer status = (Integer) stat[0];
            Long count = (Long) stat[1];
            
            statistics.setTotalCount(statistics.getTotalCount() + count);
            
            switch (status) {
                case 1: // 正常
                    statistics.setActiveCount(count);
                    break;
                case 2: // 维修中
                    statistics.setMaintenanceCount(count);
                    break;
                case 3: // 故障
                    statistics.setFaultCount(count);
                    break;
                case 0: // 停用
                    statistics.setOfflineCount(count);
                    break;
            }
        }

        return statistics;
    }
}