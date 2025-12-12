package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * @author MiniMax Agent
 */
@Data
@Entity
@Table(name = "sys_user")
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码(加密)
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * 邮箱
     */
    @Column(name = "email", unique = true, length = 100)
    private String email;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 真实姓名
     */
    @Column(name = "real_name", length = 50)
    private String realName;

    /**
     * 头像
     */
    @Column(name = "avatar", length = 255)
    private String avatar;

    /**
     * 状态:0-禁用,1-启用
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
}