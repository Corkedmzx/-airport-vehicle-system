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
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            
            // 根据异常类型返回不同的状态码
            if (e.getMessage().contains("用户名") || e.getMessage().contains("密码")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Result.unauthorized(e.getMessage()));
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "系统健康检查", description = "检查认证系统是否正常")
    public Result<String> health() {
        return Result.success("认证系统运行正常");
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户退出登录")
    public Result<String> logout() {
        // 这里可以实现Token黑名单机制
        return Result.success("登出成功");
    }
}