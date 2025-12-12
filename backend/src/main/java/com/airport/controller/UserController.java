package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.SysUser;
import com.airport.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息管理相关接口")
public class UserController {

    private final SysUserService userService;

    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    public Result<Page<SysUser>> getUsers(
            @Parameter(description = "页码", required = false) 
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", required = false) 
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "搜索关键词", required = false) 
            @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态", required = false) 
            @RequestParam(required = false) Integer status) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SysUser> users = userService.findUsers(keyword, status, null, pageable);
            return Result.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public Result<SysUser> getUserById(
            @Parameter(description = "用户ID", required = true) 
            @PathVariable Long id) {
        try {
            SysUser user = userService.getUserById(id);
            return Result.success(user);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error("获取用户详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    public Result<SysUser> createUser(@RequestBody SysUser user) {
        try {
            SysUser createdUser = userService.createUser(user);
            return Result.success("用户创建成功", createdUser);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    public Result<SysUser> updateUser(
            @Parameter(description = "用户ID", required = true) 
            @PathVariable Long id,
            @RequestBody SysUser user) {
        try {
            user.setId(id);
            SysUser updatedUser = userService.updateUser(user);
            return Result.success("用户更新成功", updatedUser);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除用户")
    public Result<String> deleteUser(
            @Parameter(description = "用户ID", required = true) 
            @PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success("用户删除成功");
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    public Result<SysUser> updateUserStatus(
            @Parameter(description = "用户ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "状态:0-禁用,1-启用", required = true) 
            @RequestParam Integer status) {
        try {
            SysUser user = userService.getUserById(id);
            user.setStatus(status);
            SysUser updatedUser = userService.updateUser(user);
            return Result.success("用户状态更新成功", updatedUser);
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置密码", description = "重置用户密码")
    public Result<String> resetPassword(
            @Parameter(description = "用户ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "新密码", required = true) 
            @RequestParam String newPassword) {
        try {
            SysUser user = userService.getUserById(id);
            user.setPassword(newPassword);
            userService.updateUser(user);
            return Result.success("密码重置成功");
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取用户统计", description = "获取用户统计数据")
    public Result<Object> getUserStatistics() {
        try {
            List<SysUser> allUsers = userService.getAllUsers();
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().filter(u -> u.getStatus() == 1).count();
            
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", activeUsers);
            stats.put("onlineUsers", 0L); // TODO: 实现在线用户统计
            stats.put("onlineRate", totalUsers > 0 ? (activeUsers * 100 / totalUsers) : 0);
            stats.put("totalRoles", 5L); // TODO: 从角色表获取
            stats.put("adminUsers", allUsers.stream().filter(u -> "admin".equals(u.getUsername())).count());
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取用户统计失败", e);
            return Result.error("获取用户统计失败: " + e.getMessage());
        }
    }
}

