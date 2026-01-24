package com.airport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 明确指定静态资源路径，避免拦截API路由
        // 只处理 /static/** 和 /public/** 等明确的静态资源路径
        // 注意：不配置默认的静态资源处理，避免拦截API路由
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/");
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