package com.airport.websocket;

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
 * 传感器WebSocket处理器
 * 预留接口，用于接收传感器设备发送的实时定位数据
 * 
 * @author Corkedmzx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final VehicleLocationService vehicleLocationService;
    
    // 存储传感器会话，key为设备ID，value为会话
    private final Map<String, WebSocketSession> sensorSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String deviceId = getDeviceIdFromSession(session);
        if (deviceId == null || deviceId.isEmpty()) {
            log.warn("传感器连接失败：缺少设备ID");
            session.close(CloseStatus.BAD_DATA.withReason("缺少设备ID"));
            return;
        }
        
        sensorSessions.put(deviceId, session);
        log.info("传感器连接已建立，设备ID: {}", deviceId);
        
        // 发送连接成功消息
        sendMessage(session, createResponse("CONNECTED", Map.of("message", "连接成功", "deviceId", deviceId)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            
            String type = (String) data.getOrDefault("type", "UNKNOWN");
            
            switch (type) {
                case "LOCATION_UPDATE":
                    // 处理位置更新数据
                    handleLocationUpdate(session, data);
                    break;
                case "HEARTBEAT":
                    // 心跳检测
                    handleHeartbeat(session, data);
                    break;
                case "STATUS_REPORT":
                    // 状态报告
                    handleStatusReport(session, data);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
                    sendMessage(session, createResponse("ERROR", Map.of("message", "未知的消息类型")));
            }
        } catch (Exception e) {
            log.error("处理传感器消息失败", e);
            sendMessage(session, createResponse("ERROR", Map.of("message", "消息处理失败: " + e.getMessage())));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 清理会话
        sensorSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        log.info("传感器连接已关闭: {}", status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("传感器WebSocket传输错误", exception);
        sensorSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }

    /**
     * 处理位置更新
     */
    private void handleLocationUpdate(WebSocketSession session, Map<String, Object> data) {
        try {
            String deviceId = getDeviceIdFromSession(session);
            Map<String, Object> locationData = (Map<String, Object>) data.get("data");
            
            if (locationData == null) {
                sendMessage(session, createResponse("ERROR", Map.of("message", "位置数据为空")));
                return;
            }
            
            // 调用车辆位置服务处理位置更新
            vehicleLocationService.processLocationUpdate(deviceId, locationData);
            
            // 发送确认消息
            sendMessage(session, createResponse("LOCATION_UPDATED", Map.of("message", "位置更新成功")));
            
            log.debug("处理位置更新，设备ID: {}, 数据: {}", deviceId, locationData);
        } catch (Exception e) {
            log.error("处理位置更新失败", e);
            sendMessage(session, createResponse("ERROR", Map.of("message", "位置更新处理失败")));
        }
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(WebSocketSession session, Map<String, Object> data) {
        sendMessage(session, createResponse("PONG", Map.of("timestamp", System.currentTimeMillis())));
    }

    /**
     * 处理状态报告
     */
    private void handleStatusReport(WebSocketSession session, Map<String, Object> data) {
        String deviceId = getDeviceIdFromSession(session);
        log.info("收到状态报告，设备ID: {}, 数据: {}", deviceId, data);
        sendMessage(session, createResponse("STATUS_RECEIVED", Map.of("message", "状态报告已接收")));
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 创建响应消息
     */
    private String createResponse(String type, Map<String, Object> data) {
        try {
            Map<String, Object> response = Map.of(
                "type", type,
                "data", data,
                "timestamp", System.currentTimeMillis()
            );
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("创建响应消息失败", e);
            return "{\"type\":\"ERROR\",\"data\":{\"message\":\"消息创建失败\"}}";
        }
    }

    /**
     * 从会话中获取设备ID
     */
    private String getDeviceIdFromSession(WebSocketSession session) {
        // 从查询参数中获取设备ID
        String query = session.getUri().getQuery();
        if (query != null && query.contains("deviceId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("deviceId=")) {
                    return param.substring(9);
                }
            }
        }
        return null;
    }
}

