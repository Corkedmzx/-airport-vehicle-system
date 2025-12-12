package com.airport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 
 * @author Corkedmzx
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 路径重定向
        registry.addRedirectViewController("/", "/doc.html");
        registry.addRedirectViewController("/api", "/api/doc.html");
        registry.addRedirectViewController("/swagger-ui", "/doc.html");
        registry.addRedirectViewController("/api/swagger-ui", "/api/doc.html");
        registry.addRedirectViewController("/swagger-ui.html", "/doc.html");
        registry.addRedirectViewController("/api/swagger-ui.html", "/api/doc.html");
    }
}