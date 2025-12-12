package com.airport.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具类 - 用于生成和验证BCrypt加密密码
 */
@Component
public class PasswordUtils {
    
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 加密密码
     */
    public static String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * 验证密码
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 判断密码是否为BCrypt格式
     */
    public static boolean isBCrypt(String password) {
        return password != null && password.startsWith("$2a$");
    }
    
    /**
     * 主方法：用于生成加密密码（可直接运行）
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("请提供要加密的密码作为参数");
            System.out.println("示例: java PasswordUtils admin123");
            System.out.println("\n生成常用测试密码:");
            generateTestPasswords();
            return;
        }
        
        String password = args[0];
        String encoded = encode(password);
        
        System.out.println("\n=================================");
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt加密后: " + encoded);
        System.out.println("验证结果: " + matches(password, encoded));
        System.out.println("=================================");
        
        System.out.println("\nSQL更新语句:");
        System.out.println("UPDATE sys_user SET password = '" + encoded + "' WHERE username = 'your_username';");
    }
    
    private static void generateTestPasswords() {
        System.out.println("常用测试密码的BCrypt加密:");
        System.out.println("--------------------------");
        
        String[] passwords = {"admin123", "password", "123456", "test123", "user123"};
        
        for (String pass : passwords) {
            String encoded = encode(pass);
            System.out.println("\n密码: " + pass);
            System.out.println("加密: " + encoded);
            System.out.println("SQL: UPDATE sys_user SET password = '" + encoded + "' WHERE username = 'xxx';");
        }
    }
}