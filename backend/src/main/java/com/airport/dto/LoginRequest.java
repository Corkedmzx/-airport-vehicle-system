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
}