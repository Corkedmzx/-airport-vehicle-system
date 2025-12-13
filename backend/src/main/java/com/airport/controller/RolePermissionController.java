package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.SysRole;
import com.airport.entity.SysRolePermission;
import com.airport.repository.SysPermissionRepository;
import com.airport.repository.SysRolePermissionRepository;
import com.airport.repository.SysRoleRepository;
import com.airport.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/role-permissions")
@RequiredArgsConstructor
@Tag(name = "角色权限管理", description = "角色权限配置相关接口")
public class RolePermissionController {

    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final JwtUtils jwtUtils;

    @Data
    public static class RolePermissionDTO {
        private Long roleId;
        private String roleName;
        private String roleCode;
        private List<String> permissions;
    }

    @GetMapping("/roles")
    @Operation(summary = "获取所有角色", description = "获取系统中所有角色")
    public Result<List<SysRole>> getAllRoles() {
        try {
            List<SysRole> roles = roleRepository.findAll();
            return Result.success(roles);
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return Result.error("获取角色列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/roles/{roleId}/permissions")
    @Operation(summary = "获取角色权限", description = "获取指定角色的所有权限")
    public Result<RolePermissionDTO> getRolePermissions(
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long roleId) {
        try {
            SysRole role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("角色不存在"));

            List<SysRolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
            List<String> permissionCodes = rolePermissions.stream()
                    .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                    .filter(java.util.Optional::isPresent)
                    .map(opt -> opt.get().getPermissionCode())
                    .collect(Collectors.toList());

            RolePermissionDTO dto = new RolePermissionDTO();
            dto.setRoleId(role.getId());
            dto.setRoleName(role.getRoleName());
            dto.setRoleCode(role.getRoleCode());
            dto.setPermissions(permissionCodes);

            return Result.success(dto);
        } catch (Exception e) {
            log.error("获取角色权限失败", e);
            return Result.error("获取角色权限失败: " + e.getMessage());
        }
    }

    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "更新角色权限", description = "更新指定角色的权限配置（仅admin用户可操作）")
    public Result<String> updateRolePermissions(
            @Parameter(description = "角色ID", required = true)
            @PathVariable Long roleId,
            @RequestBody List<String> permissionCodes,
            HttpServletRequest request) {
        try {
            // 验证是否是admin用户
            String username = getCurrentUsername(request);
            if (username == null || !"admin".equals(username)) {
                log.warn("用户 {} 尝试修改角色权限，但无权限", username);
                return Result.forbidden("只有系统管理员（admin）可以修改角色权限");
            }

            SysRole role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("角色不存在"));

            // 防止修改ADMIN角色的权限
            if ("ADMIN".equals(role.getRoleCode())) {
                log.warn("用户 {} 尝试修改ADMIN角色权限，已阻止", username);
                return Result.error("系统管理员（ADMIN）角色的权限不可修改，以防止误操作");
            }

            // 删除原有权限
            rolePermissionRepository.deleteByRoleId(roleId);

            // 添加新权限
            for (String permissionCode : permissionCodes) {
                permissionRepository.findByPermissionCode(permissionCode)
                        .ifPresent(permission -> {
                            SysRolePermission rolePermission = new SysRolePermission();
                            rolePermission.setRoleId(roleId);
                            rolePermission.setPermissionId(permission.getId());
                            rolePermissionRepository.save(rolePermission);
                        });
            }

            log.info("用户 {} 更新了角色 {} 的权限", username, role.getRoleName());
            return Result.success("角色权限更新成功");
        } catch (Exception e) {
            log.error("更新角色权限失败", e);
            return Result.error("更新角色权限失败: " + e.getMessage());
        }
    }

    /**
     * 从请求头中获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
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

    @GetMapping("/permissions")
    @Operation(summary = "获取所有权限", description = "获取系统中所有权限")
    public Result<List<com.airport.entity.SysPermission>> getAllPermissions() {
        try {
            List<com.airport.entity.SysPermission> permissions = permissionRepository.findAll();
            return Result.success(permissions);
        } catch (Exception e) {
            log.error("获取权限列表失败", e);
            return Result.error("获取权限列表失败: " + e.getMessage());
        }
    }
}

