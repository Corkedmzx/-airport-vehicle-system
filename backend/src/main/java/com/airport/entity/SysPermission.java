package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "sys_permission")
@EqualsAndHashCode(callSuper = true)
public class SysPermission extends BaseEntity {

    /**
     * 权限名称
     */
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    /**
     * 权限编码
     */
    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    /**
     * 权限类型:menu-菜单,button-按钮,api-接口
     */
    @Column(name = "permission_type", length = 20)
    private String permissionType = "api";

    /**
     * 资源路径
     */
    @Column(name = "resource", length = 200)
    private String resource;

    /**
     * 描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 状态:0-禁用,1-启用
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
}

