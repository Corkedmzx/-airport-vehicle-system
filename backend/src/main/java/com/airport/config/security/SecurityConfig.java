package com.airport.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置
 * 
 * @author MiniMax Agent
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(csrf -> csrf.disable())
            
            // 启用CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 配置会话管理
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 允许公开访问的路径（同时支持带和不带/api前缀的路径）
                .requestMatchers(
                    // 认证相关
                    "/api/auth/**",
                    "/auth/**",
                    
                    // Spring Boot Actuator（带和不带/api前缀）
                    "/api/actuator",
                    "/api/actuator/**",
                    "/actuator",
                    "/actuator/**",
                    
                    // API文档和Swagger资源（带和不带/api前缀）
                    "/api/doc.html",
                    "/doc.html",
                    "/api/doc.html/**",
                    "/doc.html/**",
                    "/api/swagger-ui/**",
                    "/api/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/swagger-resources/**",
                    "/api/swagger-resources",
                    "/swagger-resources/**",
                    "/swagger-resources",
                    "/api/v2/api-docs",
                    "/v2/api-docs",
                    "/api/v3/api-docs",
                    "/v3/api-docs",
                    "/api/v3/api-docs/**",
                    "/v3/api-docs/**",
                    "/api/webjars/**",
                    "/api/webjars",
                    "/webjars/**",
                    "/webjars",
                    "/api/configuration/ui",
                    "/configuration/ui",
                    "/api/configuration/security",
                    "/configuration/security",
                    
                    // Druid监控（带和不带/api前缀）
                    "/api/druid",
                    "/druid",
                    "/api/druid/**",
                    "/druid/**",
                    "/api/druid/login.html",
                    "/druid/login.html",
                    "/api/druid/index.html",
                    "/druid/index.html",
                    
                    // H2控制台（开发用，带和不带/api前缀）
                    "/api/h2-console",
                    "/h2-console",
                    "/api/h2-console/**",
                    "/h2-console/**",
                    
                    // WebSocket（带和不带/api前缀）
                    "/api/ws",
                    "/ws",
                    "/api/ws/**",
                    "/ws/**",
                    "/websocket",
                    "/websocket/**",
                    
                    // 静态资源（带和不带/api前缀）
                    "/api/static/**",
                    "/static/**",
                    "/api/public/**",
                    "/public/**",
                    "/api/resources/**",
                    "/resources/**",
                    
                    // 根路径和错误页面
                    "/",
                    "/error",
                    "/error/**",
                    
                    // 网站图标
                    "/favicon.ico",
                    "/api/favicon.ico"
                ).permitAll()
                
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            
            // 添加自定义异常处理
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    response.getWriter().write("{\"code\":401,\"message\":\"未认证或认证已过期\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    response.getWriter().write("{\"code\":403,\"message\":\"权限不足\"}");
                })
            );

        // 添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:8080",
            "http://localhost:8081",
            "http://127.0.0.1:8080",
            "http://127.0.0.1:8081",
            "http://localhost:3000",  // 前端端口
            "http://127.0.0.1:3000"   // 前端端口
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Disposition",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}