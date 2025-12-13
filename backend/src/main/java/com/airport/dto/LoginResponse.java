package com.airport.dto;

import lombok.Data;

/**
 * 登录响应DTO
 * 
 * @author Corkedmzx
 */
@Data
public class LoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * Token类型
     */
    private String tokenType = "Bearer";

    /**
     * 用户信息
     */
    private UserInfo user;

    /**
     * Token过期时间
     */
    private Long expiresIn;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private String email;
        private String avatar;
        private java.util.List<String> roles;
        private java.util.List<String> permissions;
    }
}