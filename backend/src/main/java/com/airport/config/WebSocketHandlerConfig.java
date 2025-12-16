package com.airport.config;

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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册车辆位置WebSocket处理器（用于前端连接）
        registry.addHandler(vehicleLocationWebSocketHandler, "/ws/vehicles")
                .setAllowedOriginPatterns("*");
        
        // 注册传感器WebSocket处理器（用于传感器设备连接）
        registry.addHandler(sensorWebSocketHandler, "/ws/sensor")
                .setAllowedOriginPatterns("*");
    }
}

