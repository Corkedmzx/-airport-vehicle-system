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
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private static final String ATTR_AUTHENTICATED = "authenticated";
    private static final long AUTH_TIMEOUT_SECONDS = 15L;

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService authScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-auth-timeout");
        t.setDaemon(true);
        return t;
    });

    private final Map<Long, WebSocketSession> vehicleSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 握手成功，等待 AUTH 帧，会话ID: {}", session.getId());
        session.getAttributes().put(ATTR_AUTHENTICATED, Boolean.FALSE);
        sendMessage(session, createMessage("AUTH_REQUIRED", Map.of("message", "请发送 AUTH 消息")));

        authScheduler.schedule(() -> {
            if (Boolean.FALSE.equals(session.getAttributes().get(ATTR_AUTHENTICATED)) && session.isOpen()) {
                try {
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("认证超时"));
                } catch (IOException e) {
                    log.debug("关闭未认证 WebSocket 失败: {}", e.getMessage());
                }
            }
        }, AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            String type = (String) data.get("type");

            if ("AUTH".equals(type)) {
                handleAuth(session, data);
                return;
            }

            if (!isAuthenticated(session)) {
                sendMessage(session, createMessage("ERROR", Map.of("message", "未认证")));
                return;
            }

            switch (type) {
                case "PING":
                    sendMessage(session, createMessage("PONG", Map.of("timestamp", System.currentTimeMillis())));
                    break;
                case "SUBSCRIBE_VEHICLE":
                    Long vehicleId = Long.valueOf(data.get("data").toString());
                    vehicleSessions.put(vehicleId, session);
                    log.info("用户订阅车辆位置更新，车辆ID: {}", vehicleId);
                    sendMessage(session, createMessage("SUBSCRIBED", Map.of("vehicleId", vehicleId)));
                    break;
                case "UNSUBSCRIBE_VEHICLE":
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

    private void handleAuth(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (isAuthenticated(session)) {
            return;
        }

        String token = extractTokenFromAuthPayload(data);
        if (token == null || !validateToken(token)) {
            log.warn("WebSocket AUTH 失败，会话ID: {}", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("无效的认证token"));
            return;
        }

        String username = jwtUtils.getUsernameFromToken(token);
        Long userId = jwtUtils.getUserIdFromToken(token);
        session.getAttributes().put(ATTR_AUTHENTICATED, Boolean.TRUE);
        userSessions.put(username, session);
        log.info("WebSocket 认证成功，用户: {}, 用户ID: {}, 会话ID: {}", username, userId, session.getId());
        sendMessage(session, createMessage("CONNECTED", Map.of("message", "连接成功")));
    }

    @SuppressWarnings("unchecked")
    private String extractTokenFromAuthPayload(Map<String, Object> data) {
        Object payload = data.get("data");
        if (payload instanceof Map<?, ?> map) {
            Object token = map.get("token");
            if (token != null) {
                return token.toString();
            }
        }
        if (payload instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }

    private boolean isAuthenticated(WebSocketSession session) {
        return Boolean.TRUE.equals(session.getAttributes().get(ATTR_AUTHENTICATED));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        vehicleSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        log.info("WebSocket连接已关闭: {}", status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (hasClosedChannelCause(exception)) {
            log.debug("WebSocket 通道已关闭（常见于应用停止）: {}", exception.getMessage());
        } else {
            log.error("WebSocket传输错误", exception);
        }
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        vehicleSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }

    private static boolean hasClosedChannelCause(Throwable t) {
        while (t != null) {
            if (t instanceof ClosedChannelException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    public void broadcastVehicleLocationUpdate(Long vehicleId, Map<String, Object> locationData) {
        if (vehicleId != null) {
            WebSocketSession session = vehicleSessions.get(vehicleId);
            if (session != null && session.isOpen()) {
                try {
                    sendMessage(session, createMessage("VEHICLE_LOCATION_UPDATE", locationData));
                } catch (Exception e) {
                    log.error("广播车辆位置更新失败，车辆ID: {}", vehicleId, e);
                }
            }
        }

        log.debug("广播位置更新给所有用户，位置数据: {}", locationData);
        int sentCount = 0;
        for (WebSocketSession s : userSessions.values()) {
            if (s.isOpen()) {
                try {
                    String message = createMessage("VEHICLE_LOCATION_UPDATE", locationData);
                    sendMessage(s, message);
                    sentCount++;
                } catch (Exception e) {
                    log.error("发送位置更新给用户失败，会话ID: {}", s.getId(), e);
                }
            }
        }
        log.info("位置更新已广播给 {} 个用户会话", sentCount);
    }

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

    private void sendMessage(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

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

    private boolean validateToken(String token) {
        try {
            String username = jwtUtils.getUsernameFromToken(token);
            return jwtUtils.validateToken(token, username);
        } catch (Exception e) {
            return false;
        }
    }
}
