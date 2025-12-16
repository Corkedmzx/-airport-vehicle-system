package com.airport.websocket;

import com.airport.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆位置WebSocket处理器
 * 用于实时推送车辆位置更新
 * 
 * @author Corkedmzx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleLocationWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    
    // 存储所有连接的WebSocket会话，key为车辆ID，value为会话集合
    private final Map<Long, WebSocketSession> vehicleSessions = new ConcurrentHashMap<>();
    
    // 存储用户会话，key为用户名，value为会话集合
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getTokenFromSession(session);
        if (token == null || !validateToken(token)) {
            log.warn("WebSocket连接失败：无效的token");
            session.close(CloseStatus.BAD_DATA.withReason("无效的认证token"));
            return;
        }
        
        String username = jwtUtils.getUsernameFromToken(token);
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        userSessions.put(username, session);
        log.info("WebSocket连接已建立，用户: {}, 用户ID: {}", username, userId);
        
        // 发送连接成功消息
        sendMessage(session, createMessage("CONNECTED", Map.of("message", "连接成功")));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            
            String type = (String) data.get("type");
            
            switch (type) {
                case "PING":
                    // 心跳检测
                    sendMessage(session, createMessage("PONG", Map.of("timestamp", System.currentTimeMillis())));
                    break;
                case "SUBSCRIBE_VEHICLE":
                    // 订阅车辆位置更新
                    Long vehicleId = Long.valueOf(data.get("data").toString());
                    vehicleSessions.put(vehicleId, session);
                    log.info("用户订阅车辆位置更新，车辆ID: {}", vehicleId);
                    sendMessage(session, createMessage("SUBSCRIBED", Map.of("vehicleId", vehicleId)));
                    break;
                case "UNSUBSCRIBE_VEHICLE":
                    // 取消订阅
                    Long unsubVehicleId = Long.valueOf(data.get("data").toString());
                    vehicleSessions.remove(unsubVehicleId);
                    log.info("用户取消订阅车辆位置更新，车辆ID: {}", unsubVehicleId);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendMessage(session, createMessage("ERROR", Map.of("message", "消息处理失败")));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 清理会话
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        vehicleSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        log.info("WebSocket连接已关闭: {}", status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        vehicleSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }

    /**
     * 发送车辆位置更新消息给所有订阅的用户
     */
    public void broadcastVehicleLocationUpdate(Long vehicleId, Map<String, Object> locationData) {
        WebSocketSession session = vehicleSessions.get(vehicleId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, createMessage("VEHICLE_LOCATION_UPDATE", locationData));
            } catch (Exception e) {
                log.error("广播车辆位置更新失败，车辆ID: {}", vehicleId, e);
            }
        }
        
        // 也发送给所有连接的用户（用于地图监控页面）
        userSessions.values().forEach(s -> {
            if (s.isOpen()) {
                try {
                    sendMessage(s, createMessage("VEHICLE_LOCATION_UPDATE", locationData));
                } catch (Exception e) {
                    log.error("发送车辆位置更新给用户失败", e);
                }
            }
        });
    }

    /**
     * 发送告警通知
     */
    public void broadcastAlert(Map<String, Object> alertData) {
        userSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, createMessage("ALERT_NOTIFICATION", alertData));
                } catch (Exception e) {
                    log.error("发送告警通知失败", e);
                }
            }
        });
    }

    /**
     * 发送任务状态更新
     */
    public void broadcastTaskUpdate(Map<String, Object> taskData) {
        userSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, createMessage("TASK_STATUS_UPDATE", taskData));
                } catch (Exception e) {
                    log.error("发送任务状态更新失败", e);
                }
            }
        });
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    /**
     * 创建消息
     */
    private String createMessage(String type, Map<String, Object> data) {
        try {
            Map<String, Object> message = Map.of(
                "type", type,
                "data", data,
                "timestamp", System.currentTimeMillis()
            );
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("创建消息失败", e);
            return "{\"type\":\"ERROR\",\"data\":{\"message\":\"消息创建失败\"}}";
        }
    }

    /**
     * 从会话中获取token
     */
    private String getTokenFromSession(WebSocketSession session) {
        // 从查询参数中获取token
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }

    /**
     * 验证token
     */
    private boolean validateToken(String token) {
        try {
            String username = jwtUtils.getUsernameFromToken(token);
            return jwtUtils.validateToken(token, username);
        } catch (Exception e) {
            return false;
        }
    }
}

