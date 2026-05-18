package com.airport.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关可配置项（生产默认收紧管理端点与 CORS）
 */
@Data
@ConfigurationProperties(prefix = "airport.security")
public class AirportSecurityProperties {

    /**
     * 为 true 时开放 Actuator、Swagger/Knife4j、Druid、H2 等管理/调试端点（仅建议 dev 环境）
     */
    private boolean exposeAdminEndpoints = false;

    /**
     * CORS 允许的来源（Spring {@code allowedOriginPatterns}）
     */
    private List<String> corsAllowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            "http://localhost:8081",
            "http://127.0.0.1:8081"
    ));
}
