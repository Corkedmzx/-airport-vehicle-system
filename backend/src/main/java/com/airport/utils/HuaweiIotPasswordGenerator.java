package com.airport.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 华为云IoT MQTT密码生成工具
 * 
 * 华为云IoT平台使用动态密码机制：
 * - Client ID格式: {deviceId}_{deviceType}_{passwordSignatureType}_{timestamp}
 *   例如: {deviceId}_0_0_2026011517（deviceId 为控制台完整设备 ID）
 * - Username: 设备ID
 * - Password: HMAC-SHA256(deviceSecret, timestamp) 的十六进制字符串
 * 
 * @author Corkedmzx
 */
@Slf4j
public class HuaweiIotPasswordGenerator {

    /**
     * 设备类型：0表示普通设备
     */
    private static final int DEVICE_TYPE = 0;

    /**
     * 密码签名类型：
     * 0 - 不验证timestamp（密码仅基于secret）
     * 1 - 验证timestamp（密码基于secret和timestamp）
     * 华为云IoT标准认证使用1
     */
    private static final int PASSWORD_SIGNATURE_TYPE = 1;

    /**
     * 时间戳格式：YYYYMMDDHH（年月日时）
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneId.of("UTC"));

    /**
     * 生成当前时间戳（UTC时间，格式：YYYYMMDDHH）
     * 
     * @return 时间戳字符串，例如：2026011517
     */
    public static String generateTimestamp() {
        return TIMESTAMP_FORMATTER.format(Instant.now());
    }

    /**
     * 生成Client ID
     * 格式: {deviceId}_{deviceType}_{passwordSignatureType}_{timestamp}
     * 
     * @param deviceId 设备ID
     * @return Client ID，例如：{你的设备ID}_0_0_2026011517
     */
    public static String generateClientId(String deviceId) {
        String timestamp = generateTimestamp();
        return String.format("%s_%d_%d_%s", deviceId, DEVICE_TYPE, PASSWORD_SIGNATURE_TYPE, timestamp);
    }

    /**
     * 生成MQTT连接密码
     * 使用HMAC-SHA256算法，以timestamp作为key，对deviceSecret进行加密
     * 
     * ⚠️ 重要：华为云IoT要求使用timestamp作为key，deviceSecret作为message
     * 算法：HMAC-SHA256(timestamp, deviceSecret)
     * 
     * @param deviceSecret 设备密钥（作为message）
     * @param timestamp 时间戳（格式：YYYYMMDDHH，作为key）
     * @return 密码的十六进制字符串（小写）
     */
    public static String generatePassword(String deviceSecret, String timestamp) {
        try {
            // 创建HMAC-SHA256实例
            Mac mac = Mac.getInstance("HmacSHA256");
            
            // ⚠️ 关键：使用timestamp作为key，deviceSecret作为message
            // 华为云IoT文档明确说明：以timestamp作为key，对deviceSecret进行加密
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    timestamp.getBytes(StandardCharsets.UTF_8),  // timestamp作为key
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);

            // 计算HMAC-SHA256：HMAC-SHA256(timestamp, deviceSecret)
            byte[] hash = mac.doFinal(deviceSecret.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串（小写）
            String password = bytesToHex(hash).toLowerCase();
            
            log.debug("生成MQTT密码 - DeviceSecret: {}..., Timestamp: {}, Password: {}...", 
                    deviceSecret.length() > 8 ? deviceSecret.substring(0, 8) : deviceSecret,
                    timestamp,
                    password.length() > 16 ? password.substring(0, 16) : password);
            
            return password;
        } catch (Exception e) {
            log.error("生成MQTT密码失败", e);
            throw new RuntimeException("生成MQTT密码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成MQTT连接密码（使用当前时间戳）
     * 
     * @param deviceSecret 设备密钥
     * @return 密码的十六进制字符串
     */
    public static String generatePassword(String deviceSecret) {
        String timestamp = generateTimestamp();
        return generatePassword(deviceSecret, timestamp);
    }

    /**
     * 生成完整的MQTT连接信息
     * 
     * @param deviceId 设备ID
     * @param deviceSecret 设备密钥
     * @return MQTT连接信息对象
     */
    public static MqttConnectionInfo generateConnectionInfo(String deviceId, String deviceSecret) {
        String timestamp = generateTimestamp();
        String clientId = generateClientId(deviceId, timestamp);
        String password = generatePassword(deviceSecret, timestamp);

        return new MqttConnectionInfo(clientId, deviceId, password, timestamp);
    }

    /**
     * 生成Client ID（指定时间戳）
     * 
     * @param deviceId 设备ID
     * @param timestamp 时间戳
     * @return Client ID
     */
    private static String generateClientId(String deviceId, String timestamp) {
        return String.format("%s_%d_%d_%s", deviceId, DEVICE_TYPE, PASSWORD_SIGNATURE_TYPE, timestamp);
    }

    /**
     * 将字节数组转换为十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * MQTT连接信息
     */
    public static class MqttConnectionInfo {
        private final String clientId;
        private final String username;
        private final String password;
        private final String timestamp;

        public MqttConnectionInfo(String clientId, String username, String password, String timestamp) {
            this.clientId = clientId;
            this.username = username;
            this.password = password;
            this.timestamp = timestamp;
        }

        public String getClientId() {
            return clientId;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("MqttConnectionInfo{clientId='%s', username='%s', password='%s', timestamp='%s'}", 
                    clientId, username, "***", timestamp);
        }
    }
}
