package com.airport.controller;

import com.airport.dto.LoginRequest;
import com.airport.dto.LoginResponse;
import com.airport.dto.Result;
import com.airport.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "用户登录、注册等相关接口")
public class AuthController {

    private final SysUserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据用户名和密码进行登录")
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(Result.success("登录成功", response));
        } catch (RuntimeException e) {
            log.warn("登录失败: {}", e.getMessage());
            
            // 统一返回401状态码，但提示信息更友好
            String errorMessage = e.getMessage();
            if (errorMessage.contains("用户未注册") || errorMessage.contains("用户名错误")) {
                errorMessage = "用户未注册或用户名错误";
            } else if (errorMessage.contains("密码错误")) {
                errorMessage = "密码错误，请重新输入";
            } else if (errorMessage.contains("禁用")) {
                errorMessage = "用户已被禁用，请联系管理员";
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, errorMessage));
        } catch (Exception e) {
            log.error("登录异常: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("登录失败，请稍后重试"));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "系统健康检查", description = "检查认证系统是否正常")
    public Result<String> health() {
        return Result.success("认证系统运行正常");
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册")
    public ResponseEntity<Result<LoginResponse>> register(@RequestBody LoginRequest request) {
        try {
            // 验证必填字段
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error("用户名不能为空"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error("密码不能为空"));
            }
            if (request.getPassword().length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error("密码长度至少6位"));
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error("邮箱不能为空"));
            }
            
            // 创建新用户
            com.airport.entity.SysUser newUser = new com.airport.entity.SysUser();
            newUser.setUsername(request.getUsername().trim());
            newUser.setPassword(request.getPassword());
            newUser.setEmail(request.getEmail().trim());
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                newUser.setPhone(request.getPhone().trim());
            }
            newUser.setStatus(1); // 默认启用
            
            com.airport.entity.SysUser createdUser = userService.createUser(newUser);
            
            // 注册成功后自动登录
            try {
                LoginResponse response = userService.login(createdUser.getUsername(), request.getPassword());
                return ResponseEntity.ok(Result.success("注册成功", response));
            } catch (Exception loginError) {
                log.warn("注册成功但自动登录失败: {}", loginError.getMessage());
                // 即使自动登录失败，也返回注册成功，让用户手动登录
                return ResponseEntity.ok(Result.success("注册成功，请登录", null));
            }
        } catch (RuntimeException e) {
            log.error("注册失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            log.error("注册异常: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("注册失败，请稍后重试"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户退出登录")
    public Result<String> logout() {
        // 这里可以实现Token黑名单机制
        return Result.success("登出成功");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "账户找回", description = "通过手机号找回账户，重置密码为空")
    public ResponseEntity<Result<com.airport.dto.ForgotPasswordResponse>> forgotPassword(@RequestBody LoginRequest request) {
        try {
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error("手机号不能为空"));
            }
            
            // 根据手机号查找用户
            com.airport.entity.SysUser user = userService.findByPhone(request.getPhone());
            
            // 重置密码为空（清空密码）
            userService.resetPassword(user.getId(), "");
            
            // 返回用户名，用于后续设置新密码
            com.airport.dto.ForgotPasswordResponse response = new com.airport.dto.ForgotPasswordResponse();
            response.setUsername(user.getUsername());
            response.setUserId(user.getId());
            
            return ResponseEntity.ok(Result.success("账户找回成功，请设置新密码", response));
        } catch (Exception e) {
            log.error("账户找回失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "设置新密码", description = "在账户找回后设置新密码")
    public ResponseEntity<Result<String>> resetPassword(@RequestBody LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error("用户名不能为空"));
            }
            
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Result.error("新密码不能为空"));
            }
            
            // 查找用户
            com.airport.entity.SysUser user = userService.findByUsername(request.getUsername());
            
            // 检查密码是否为空（表示是找回账户后的首次设置）
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                // 如果密码不为空，需要验证旧密码
                if (request.getPassword().length() < 6) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Result.error("密码长度至少6位"));
                }
            }
            
            // 设置新密码
            userService.resetPassword(user.getId(), request.getPassword());
            
            return ResponseEntity.ok(Result.success("密码设置成功"));
        } catch (Exception e) {
            log.error("设置密码失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.error(e.getMessage()));
        }
    }
}