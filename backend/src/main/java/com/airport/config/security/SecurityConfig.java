package com.airport.config.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置
 *
 * @author Corkedmzx
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AirportSecurityProperties.class)
public class SecurityConfig {

    private static final String[] ADMIN_ENDPOINT_PATTERNS = {
            "/api/actuator", "/api/actuator/**", "/actuator", "/actuator/**",
            "/api/doc.html", "/doc.html", "/api/doc.html/**", "/doc.html/**",
            "/api/swagger-ui/**", "/api/swagger-ui.html", "/swagger-ui/**", "/swagger-ui.html",
            "/api/swagger-resources/**", "/api/swagger-resources", "/swagger-resources/**", "/swagger-resources",
            "/api/v2/api-docs", "/v2/api-docs", "/api/v3/api-docs", "/v3/api-docs", "/api/v3/api-docs/**", "/v3/api-docs/**",
            "/api/webjars/**", "/api/webjars", "/webjars/**", "/webjars",
            "/api/configuration/ui", "/configuration/ui",
            "/api/configuration/security", "/configuration/security",
            "/api/druid", "/druid", "/api/druid/**", "/druid/**",
            "/api/druid/login.html", "/druid/login.html", "/api/druid/index.html", "/druid/index.html",
            "/api/h2-console", "/h2-console", "/api/h2-console/**", "/h2-console/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AirportSecurityProperties securityProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AirportSecurityProperties securityProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityProperties = securityProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(buildPublicMatchers()).permitAll()
                .anyRequest().authenticated()
            )
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

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private String[] buildPublicMatchers() {
        List<String> paths = new ArrayList<>(Arrays.asList(
                "/api/auth/**", "/auth/**",
                "/api/baidu-map/**", "/baidu-map/**",
                "/api/mqtt/iot-forward-location", "/mqtt/iot-forward-location",
                "/api/ws", "/ws", "/api/ws/**", "/ws/**",
                "/websocket", "/websocket/**",
                "/api/static/**", "/static/**",
                "/api/public/**", "/public/**",
                "/api/resources/**", "/resources/**",
                "/", "/error", "/error/**",
                "/favicon.ico", "/api/favicon.ico"
        ));
        if (securityProperties.isExposeAdminEndpoints()) {
            paths.addAll(Arrays.asList(ADMIN_ENDPOINT_PATTERNS));
        }
        return paths.toArray(new String[0]);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(new ArrayList<>(securityProperties.getCorsAllowedOriginPatterns()));
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
