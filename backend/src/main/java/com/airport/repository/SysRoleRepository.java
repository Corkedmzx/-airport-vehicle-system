package com.airport.repository;

import com.airport.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    /**
     * 根据角色编码查找角色
     * 
     * @param roleCode 角色编码
     * @return 角色信息
     */
    Optional<SysRole> findByRoleCode(String roleCode);
}

