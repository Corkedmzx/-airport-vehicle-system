package com.airport.dto;

import lombok.Data;

/**
 * 密码找回响应DTO
 * 
 * @author Corkedmzx
 */
@Data
public class ForgotPasswordResponse {
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户ID
     */
    private Long userId;
}

