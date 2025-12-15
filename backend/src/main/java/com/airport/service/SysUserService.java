package com.airport.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.airport.dto.LoginResponse;
import com.airport.entity.SysUser;

import java.util.List;

/**
 * 用户服务接口
 * 
 * @author Corkedmzx
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

    /**
     * 分页查询用户
     * 
     * @param search 搜索关键词
     * @param status 状态
     * @param role 角色
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    Page<SysUser> findUsers(String search, Integer status, String role, Pageable pageable);

    /**
     * 根据ID获取用户
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    SysUser getUserById(Long id);

    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    List<SysUser> getAllUsers();

    /**
     * 根据手机号查找用户
     * 
     * @param phone 手机号
     * @return 用户信息
     */
    SysUser findByPhone(String phone);

    /**
     * 重置密码（用于账户找回）
     * 
     * @param userId 用户ID
     * @param newPassword 新密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 更新用户角色
     * 
     * @param userId 用户ID
     * @param roleCode 角色代码
     */
    void updateUserRole(Long userId, String roleCode);

    /**
     * 验证密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 是否匹配
     */
    boolean verifyPassword(Long userId, String password);
}