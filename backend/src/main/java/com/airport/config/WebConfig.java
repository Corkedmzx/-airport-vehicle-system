package com.airport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
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

    /**
     * 配置RestTemplate Bean，用于HTTP请求（百度地图API代理）
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 连接超时5秒
        factory.setReadTimeout(10000); // 读取超时10秒
        return new RestTemplate(factory);
    }
}