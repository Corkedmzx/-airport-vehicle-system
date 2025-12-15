package com.airport.repository;

import com.airport.entity.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统配置Repository
 * 
 * @author Corkedmzx
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, Long> {

    /**
     * 根据配置键查找配置
     */
    Optional<SystemConfigEntity> findByConfigKey(String configKey);
}

