package com.airport.config;

import com.airport.config.security.AirportSecurityProperties;
import com.airport.websocket.SensorWebSocketHandler;
import com.airport.websocket.VehicleLocationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket处理器配置
 * 注册WebSocket处理器
 * 
 * @author Corkedmzx
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketHandlerConfig implements WebSocketConfigurer {

    private final VehicleLocationWebSocketHandler vehicleLocationWebSocketHandler;
    private final SensorWebSocketHandler sensorWebSocketHandler;
    private final AirportSecurityProperties securityProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = securityProperties.getCorsAllowedOriginPatterns()
                .toArray(new String[0]);

        registry.addHandler(vehicleLocationWebSocketHandler, "/ws/vehicles")
                .setAllowedOriginPatterns(origins);

        registry.addHandler(sensorWebSocketHandler, "/ws/sensor")
                .setAllowedOriginPatterns(origins);
    }
}

