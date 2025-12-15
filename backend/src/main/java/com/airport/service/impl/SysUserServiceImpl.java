package com.airport.service.impl;

import com.airport.dto.LoginResponse;
import com.airport.entity.SysUser;
import com.airport.entity.SysRole;
import com.airport.entity.SysUserRole;
import com.airport.entity.SysRolePermission;
import com.airport.entity.SysPermission;
import com.airport.repository.SysUserRepository;
import com.airport.repository.SysRoleRepository;
import com.airport.repository.SysUserRoleRepository;
import com.airport.repository.SysRolePermissionRepository;
import com.airport.repository.SysPermissionRepository;
import com.airport.service.SysUserService;
import com.airport.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 * 
 * @author Corkedmzx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SysUserServiceImpl implements SysUserService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysPermissionRepository permissionRepository;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LoginResponse login(String username, String password) {
        // 查找用户
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未注册或用户名错误"));

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用，请联系管理员");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误，请重新输入");
        }

        // 生成Token
        String token = jwtUtils.generateToken(username, user.getId());

        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn((long) jwtUtils.getJwtExpiration());

        // 获取用户角色和权限（在非事务方法中查询，避免事务回滚问题）
        List<String> roleCodes = new java.util.ArrayList<>();
        List<String> permissionCodes = new java.util.ArrayList<>();
        
        try {
            java.util.Map<String, java.util.List<String>> roleAndPermissions = getUserRolesAndPermissions(user.getId());
            roleCodes = roleAndPermissions.get("roles");
            permissionCodes = roleAndPermissions.get("permissions");
        } catch (Exception e) {
            log.warn("获取用户角色和权限时出错，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            // 即使获取角色/权限失败，也允许登录，只是角色和权限列表为空
        }

        // 设置用户信息
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRoles(roleCodes);
        userInfo.setPermissions(permissionCodes);
        response.setUser(userInfo);

        log.info("用户 {} 登录成功", username);
        
        // 更新最后登录时间（异步处理，避免影响登录响应）
        try {
            updateLastLoginTime(user.getId());
        } catch (Exception e) {
            log.warn("更新最后登录时间失败: {}", e.getMessage());
            // 不影响登录流程
        }
        
        return response;
    }

    @Override
    public SysUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public SysUser createUser(SysUser user) {
        // 检查用户名是否已存在
        if (existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (user.getEmail() != null && existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        // 保存用户
        SysUser savedUser = userRepository.save(user);

        // 为新注册用户分配"查看者"角色（VIEWER）
        try {
            SysRole viewerRole = roleRepository.findByRoleCode("VIEWER")
                    .orElseThrow(() -> new RuntimeException("查看者角色不存在，请先初始化数据库"));
            
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(savedUser.getId());
            userRole.setRoleId(viewerRole.getId());
            userRoleRepository.save(userRole);
            
            log.info("为新用户 {} 分配了查看者角色", savedUser.getUsername());
        } catch (Exception e) {
            log.warn("为新用户分配角色失败: {}", e.getMessage());
            // 不抛出异常，允许用户创建成功，但需要管理员后续分配角色
        }

        return savedUser;
    }

    @Override
    public SysUser updateUser(SysUser user) {
        SysUser existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户名是否已存在（如果修改了用户名）
        if (!existingUser.getUsername().equals(user.getUsername()) && existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在（如果修改了邮箱）
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail()) && existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 更新字段
        existingUser.setRealName(user.getRealName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAvatar(user.getAvatar());
        existingUser.setStatus(user.getStatus());

        return userRepository.save(existingUser);
    }

    @Override
    public void updateUserRole(Long userId, String roleCode) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 查找角色
        SysRole role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + roleCode));

        // 删除用户原有的所有角色
        userRoleRepository.deleteByUserId(userId);

        // 添加新角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);

        log.info("用户 {} 的角色已更新为 {}", user.getUsername(), roleCode);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("用户 {} 密码修改成功", user.getUsername());
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SysUser getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public Page<SysUser> findUsers(String search, Integer status, String role, Pageable pageable) {
        Specification<SysUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim() + "%";
                Predicate usernamePredicate = cb.like(root.get("username"), searchPattern);
                Predicate realNamePredicate = cb.like(root.get("realName"), searchPattern);
                Predicate emailPredicate = cb.like(root.get("email"), searchPattern);
                predicates.add(cb.or(usernamePredicate, realNamePredicate, emailPredicate));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return userRepository.findAll(spec, pageable);
    }

    /**
     * 获取用户角色和权限（在非事务方法中执行，避免事务回滚问题）
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED, noRollbackFor = Exception.class)
    public java.util.Map<String, java.util.List<String>> getUserRolesAndPermissions(Long userId) {
        List<String> roleCodes = new java.util.ArrayList<>();
        List<String> permissionCodes = new java.util.ArrayList<>();
        
        try {
            List<SysUserRole> userRoles = userRoleRepository.findByUserId(userId);
            if (userRoles != null && !userRoles.isEmpty()) {
                // 获取角色代码
                roleCodes = userRoles.stream()
                        .map(ur -> {
                            try {
                                return roleRepository.findById(ur.getRoleId());
                            } catch (Exception e) {
                                log.warn("查询角色失败，角色ID: {}, 错误: {}", ur.getRoleId(), e.getMessage());
                                return java.util.Optional.<SysRole>empty();
                            }
                        })
                        .filter(java.util.Optional::isPresent)
                        .map(opt -> opt.get().getRoleCode())
                        .collect(Collectors.toList());
                
                // 获取权限代码
                for (SysUserRole userRole : userRoles) {
                    try {
                        List<SysRolePermission> rolePermissions = rolePermissionRepository.findByRoleId(userRole.getRoleId());
                        if (rolePermissions != null) {
                            for (SysRolePermission rp : rolePermissions) {
                                try {
                                    permissionRepository.findById(rp.getPermissionId())
                                            .ifPresent(permission -> {
                                                if (!permissionCodes.contains(permission.getPermissionCode())) {
                                                    permissionCodes.add(permission.getPermissionCode());
                                                }
                                            });
                                } catch (Exception e) {
                                    log.warn("查询权限失败，权限ID: {}, 错误: {}", rp.getPermissionId(), e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("查询角色权限失败，角色ID: {}, 错误: {}", userRole.getRoleId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取用户角色和权限时出错，用户ID: {}", userId, e);
            throw e;
        }
        
        java.util.Map<String, java.util.List<String>> result = new java.util.HashMap<>();
        result.put("roles", roleCodes);
        result.put("permissions", permissionCodes);
        return result;
    }

    /**
     * 更新最后登录时间
     */
    @Transactional
    public void updateLastLoginTime(Long userId) {
        try {
            SysUser user = userRepository.findById(userId)
                    .orElse(null);
            if (user != null) {
                user.setLastLoginTime(java.time.LocalDateTime.now());
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("更新最后登录时间失败，用户ID: {}", userId, e);
            // 不抛出异常，避免影响登录流程
        }
    }

    @Override
    public SysUser findByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("未找到该手机号对应的用户"));
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public boolean verifyPassword(Long userId, String password) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return passwordEncoder.matches(password, user.getPassword());
    }
}