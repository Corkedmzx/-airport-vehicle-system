package com.airport.controller;

import com.airport.dto.Result;
import com.airport.dto.UserDTO;
import com.airport.entity.SysUser;
import com.airport.entity.SysUserRole;
import com.airport.entity.SysRole;
import com.airport.repository.SysUserRoleRepository;
import com.airport.repository.SysRoleRepository;
import com.airport.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.airport.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

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
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final com.airport.utils.JwtUtils jwtUtils;

    /**
     * 从请求头中获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (org.springframework.util.StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtils.validateToken(token, jwtUtils.getUsernameFromToken(token))) {
                    return jwtUtils.getUsernameFromToken(token);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
        }
        return null;
    }

    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    public Result<Page<UserDTO>> getUsers(
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
            
            // 转换为UserDTO，包含角色信息
            List<UserDTO> userDTOs = users.getContent().stream()
                    .map(user -> {
                        List<String> roleCodes = userRoleRepository.findByUserId(user.getId()).stream()
                                .map(userRole -> roleRepository.findById(userRole.getRoleId()))
                                .filter(java.util.Optional::isPresent)
                                .map(opt -> opt.get().getRoleCode())
                                .collect(Collectors.toList());
                        return UserDTO.fromEntity(user, roleCodes);
                    })
                    .collect(Collectors.toList());
            
            Page<UserDTO> userDTOPage = new PageImpl<>(userDTOs, pageable, users.getTotalElements());
            return Result.success(userDTOPage);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public Result<UserDTO> getUserById(
            @Parameter(description = "用户ID", required = true) 
            @PathVariable Long id) {
        try {
            SysUser user = userService.getUserById(id);
            List<String> roleCodes = userRoleRepository.findByUserId(id).stream()
                    .map(userRole -> roleRepository.findById(userRole.getRoleId()))
                    .filter(java.util.Optional::isPresent)
                    .map(opt -> opt.get().getRoleCode())
                    .collect(Collectors.toList());
            UserDTO userDTO = UserDTO.fromEntity(user, roleCodes);
            return Result.success(userDTO);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error("获取用户详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户（仅admin可操作）")
    public Result<SysUser> createUser(@RequestBody SysUser user, HttpServletRequest request) {
        try {
            // 检查权限：只有admin可以创建用户
            String currentUsername = getCurrentUsername(request);
            if (currentUsername == null || !"admin".equals(currentUsername)) {
                log.warn("用户 {} 尝试创建用户，但无权限", currentUsername);
                return Result.error("只有系统管理员可以创建用户");
            }
            
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
            @RequestBody java.util.Map<String, Object> userData,
            HttpServletRequest request) {
        try {
            // 获取当前登录用户
            String currentUsername = getCurrentUsername(request);
            if (currentUsername == null) {
                return Result.error("未认证或认证已过期");
            }
            
            SysUser currentUser = userService.findByUsername(currentUsername);
            SysUser user = userService.getUserById(id);
            
            // 检查权限：只有admin可以修改其他用户，普通用户只能修改自己的基本信息
            boolean isAdmin = currentUser.getUsername().equals("admin");
            boolean isSelf = currentUser.getId().equals(id);
            
            // 如果修改角色，必须要有user:update权限且是admin
            if (userData.containsKey("role")) {
                if (!isAdmin) {
                    log.warn("用户 {} 尝试修改用户 {} 的角色，但无权限", currentUsername, user.getUsername());
                    return Result.error("只有系统管理员可以修改用户角色");
                }
                
                // 防止修改admin用户的角色
                if ("admin".equals(user.getUsername())) {
                    log.warn("用户 {} 尝试修改admin用户的角色，已阻止", currentUsername);
                    return Result.error("不能修改系统管理员（admin）的角色");
                }
            }
            
            // 如果修改状态，必须要有user:update权限且是admin
            if (userData.containsKey("status")) {
                if (!isAdmin) {
                    log.warn("用户 {} 尝试修改用户 {} 的状态，但无权限", currentUsername, user.getUsername());
                    return Result.error("只有系统管理员可以修改用户状态");
                }
            }
            
            // 普通用户只能修改自己的基本信息（姓名、邮箱、手机号、头像）
            if (!isAdmin && !isSelf) {
                log.warn("用户 {} 尝试修改其他用户 {} 的信息，但无权限", currentUsername, user.getUsername());
                return Result.error("只能修改自己的信息");
            }
            
            // 更新基本信息
            if (userData.containsKey("realName")) {
                user.setRealName((String) userData.get("realName"));
            }
            if (userData.containsKey("email")) {
                user.setEmail((String) userData.get("email"));
            }
            if (userData.containsKey("phone")) {
                user.setPhone((String) userData.get("phone"));
            }
            if (userData.containsKey("avatar")) {
                user.setAvatar((String) userData.get("avatar"));
            }
            if (userData.containsKey("status") && isAdmin) {
                Object statusObj = userData.get("status");
                if (statusObj instanceof Number) {
                    user.setStatus(((Number) statusObj).intValue());
                }
            }
            
            SysUser updatedUser = userService.updateUser(user);
            
            // 更新角色（如果提供了role字段且是admin）
            if (userData.containsKey("role") && isAdmin) {
                String roleCode = (String) userData.get("role");
                // 将前端的小写角色代码转换为大写的角色代码
                String upperRoleCode = roleCode.toUpperCase();
                // 处理特殊映射
                if ("ADMIN".equals(upperRoleCode)) {
                    upperRoleCode = "ADMIN";
                } else if ("OPERATOR".equals(upperRoleCode)) {
                    upperRoleCode = "OPERATOR";
                } else if ("VIEWER".equals(upperRoleCode)) {
                    upperRoleCode = "VIEWER";
                } else if ("DISPATCHER".equals(upperRoleCode)) {
                    upperRoleCode = "DISPATCHER";
                } else if ("DRIVER".equals(upperRoleCode)) {
                    upperRoleCode = "DRIVER";
                } else if ("MAINTENANCE".equals(upperRoleCode)) {
                    upperRoleCode = "MAINTENANCE";
                } else if ("MONITOR".equals(upperRoleCode)) {
                    upperRoleCode = "MONITOR";
                }
                userService.updateUserRole(id, upperRoleCode);
            }
            
            return Result.success("用户更新成功", updatedUser);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除用户（仅admin可操作）")
    public Result<String> deleteUser(
            @Parameter(description = "用户ID", required = true) 
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            // 检查权限：只有admin可以删除用户
            String currentUsername = getCurrentUsername(request);
            if (currentUsername == null || !"admin".equals(currentUsername)) {
                log.warn("用户 {} 尝试删除用户，但无权限", currentUsername);
                return Result.error("只有系统管理员可以删除用户");
            }
            
            SysUser user = userService.getUserById(id);
            // 防止删除admin用户
            if ("admin".equals(user.getUsername())) {
                log.warn("用户 {} 尝试删除admin用户，已阻止", currentUsername);
                return Result.error("不能删除系统管理员（admin）");
            }
            
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

