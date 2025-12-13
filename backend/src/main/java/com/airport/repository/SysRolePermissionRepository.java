package com.airport.repository;

import com.airport.entity.SysRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色权限关联数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface SysRolePermissionRepository extends JpaRepository<SysRolePermission, Long> {

    /**
     * 根据角色ID查找角色权限关联
     * 
     * @param roleId 角色ID
     * @return 角色权限关联列表
     */
    List<SysRolePermission> findByRoleId(Long roleId);

    /**
     * 根据角色ID删除所有权限关联
     * 
     * @param roleId 角色ID
     */
    void deleteByRoleId(Long roleId);
}

