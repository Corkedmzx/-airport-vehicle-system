package com.airport.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.airport.dto.LoginResponse;
import com.airport.entity.SysUser;

/**
 * 用户服务接口
 * 
 * @author MiniMax Agent
 */
public interface SysUserService {

    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录响应
     */
    LoginResponse login(String username, String password);

    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    SysUser findByUsername(String username);

    /**
     * 创建用户
     * 
     * @param user 用户信息
     * @return 创建的用户
     */
    SysUser createUser(SysUser user);

    /**
     * 更新用户
     * 
     * @param user 用户信息
     * @return 更新后的用户
     */
    SysUser updateUser(SysUser user);

    /**
     * 删除用户
     * 
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    Page<SysUser> findUsers(String search, Integer status, String role, Pageable pageable);
}