package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色权限关联实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "sys_role_permission")
@EqualsAndHashCode(callSuper = true)
public class SysRolePermission extends BaseEntity {

    /**
     * 角色ID
     */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * 权限ID
     */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;
}

