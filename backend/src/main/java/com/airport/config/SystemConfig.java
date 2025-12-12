package com.airport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * 系统配置类
 * 
 * @author Corkedmzx
 */
@Configuration
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class SystemConfig {

    /**
     * 统一时钟配置，便于测试
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}