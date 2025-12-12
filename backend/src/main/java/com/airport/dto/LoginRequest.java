package com.airport.dto;

import lombok.Data;

/**
 * 登录请求DTO
 * 
 * @author Corkedmzx
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 验证码
     */
    private String captcha;

    /**
     * 验证码key
     */
    private String captchaKey;

    /**
     * 邮箱（注册时使用）
     */
    private String email;

    /**
     * 手机号（注册时使用）
     */
    private String phone;
}