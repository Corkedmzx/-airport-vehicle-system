package com.airport.repository;

import com.airport.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关联数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {

    /**
     * 根据用户ID查找用户角色关联
     * 
     * @param userId 用户ID
     * @return 用户角色关联列表
     */
    List<SysUserRole> findByUserId(Long userId);

    /**
     * 根据用户ID删除所有角色关联
     * 
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}

