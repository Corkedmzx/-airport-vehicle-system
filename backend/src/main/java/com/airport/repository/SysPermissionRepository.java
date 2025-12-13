package com.airport.repository;

import com.airport.entity.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 权限数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface SysPermissionRepository extends JpaRepository<SysPermission, Long> {

    /**
     * 根据权限编码查找权限
     * 
     * @param permissionCode 权限编码
     * @return 权限信息
     */
    Optional<SysPermission> findByPermissionCode(String permissionCode);
}

