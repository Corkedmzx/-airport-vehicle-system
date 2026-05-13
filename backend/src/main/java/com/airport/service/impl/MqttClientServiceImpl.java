package com.airport.service.impl;

import com.airport.config.HuaweiIotDeviceConfig;
import com.airport.service.MqttClientService;
import com.airport.utils.HuaweiIotPasswordGenerator;
import com.airport.websocket.VehicleLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * MQTT客户端服务实现
 * 连接华为云IoT平台，接收车辆定位数据
 * 
 * 华为云IoT使用动态密码机制：
 * - Client ID格式: {deviceId}_{deviceType}_{passwordSignatureType}_{timestamp}
 * - Username: 设备ID
 * - Password: HMAC-SHA256(deviceSecret, timestamp) 的十六进制字符串
 * 
 * 系统会自动从设备密钥文件中读取设备信息，无需手动配置
 * 
 * @author Corkedmzx
 */
/**
 * 单设备MQTT客户端服务（已废弃）
 * 请使用 MultiDeviceMqttServiceImpl 替代
 * 
 * @deprecated 使用 MultiDeviceMqttServiceImpl 替代
 */
@Slf4j
// @Service  // 已禁用，使用 MultiDeviceMqttServiceImpl
@RequiredArgsConstructor
public class MqttClientServiceImpl implements MqttClientService, MqttCallback {

    private final VehicleLocationService vehicleLocationService;
    private final ObjectMapper objectMapper;
    private final HuaweiIotDeviceConfig deviceConfig;

    @Value("${huawei.iot.mqtt.broker:}")
    private String broker;

    @Value("${huawei.iot.mqtt.device-id:}")
    private String deviceId;

    @Value("${huawei.iot.mqtt.device-secret:}")
    private String deviceSecret;

    @Value("${huawei.iot.mqtt.topic:}")
    private String topic;

    @Value("${huawei.iot.mqtt.qos:1}")
    private int qos;

    @Value("${huawei.iot.mqtt.enabled:false}")
    private boolean enabled;

    @Value("${huawei.iot.mqtt.connection-timeout:30}")
    private int connectionTimeout;

    @Value("${huawei.iot.mqtt.keep-alive:60}")
    private int keepAlive;

    private MqttClient mqttClient;
    private boolean connected = false;
    private boolean subscribed = false; // 标记是否已订阅，避免重复订阅

    /**
     * 初始化MQTT客户端
     */
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("MQTT客户端未启用，跳过初始化");
            return;
        }

        if (broker == null || broker.isEmpty()) {
            log.warn("MQTT Broker地址未配置，MQTT客户端将不会启动");
            return;
        }

        // 获取设备信息（优先从环境变量，其次从文件）
        String finalDeviceId = deviceId;
        String finalDeviceSecret = deviceSecret;

        // 如果环境变量或配置文件中没有，尝试从设备密钥文件读取
        if ((finalDeviceId == null || finalDeviceId.isEmpty()) ||
            (finalDeviceSecret == null || finalDeviceSecret.isEmpty())) {
            log.info("从设备密钥文件读取设备信息...");
            HuaweiIotDeviceConfig.DeviceInfo deviceInfo = deviceConfig.getDeviceInfo();
            
            if (deviceInfo != null) {
                if (finalDeviceId == null || finalDeviceId.isEmpty()) {
                    finalDeviceId = deviceInfo.getDeviceId();
                }
                if (finalDeviceSecret == null || finalDeviceSecret.isEmpty()) {
                    finalDeviceSecret = deviceInfo.getSecret();
                }
                log.info("从文件读取设备信息成功，Device ID: {}", finalDeviceId);
            } else {
                log.error("无法获取设备信息，MQTT客户端将不会启动");
                log.error("请设置环境变量 HUAWEI_IOT_MQTT_DEVICE_ID 和 HUAWEI_IOT_MQTT_DEVICE_SECRET");
                log.error("或在配置文件中设置 huawei.iot.mqtt.device-id 和 huawei.iot.mqtt.device-secret");
                log.error("或在设备密钥目录中放置设备密钥文件");
                return;
            }
        }

        if (finalDeviceId == null || finalDeviceId.isEmpty()) {
            log.warn("设备ID未配置，MQTT客户端将不会启动");
            return;
        }

        if (finalDeviceSecret == null || finalDeviceSecret.isEmpty()) {
            log.warn("设备密钥未配置，MQTT客户端将不会启动");
            return;
        }

        try {
            // 生成动态连接信息（每次连接时生成新的Client ID和Password）
            HuaweiIotPasswordGenerator.MqttConnectionInfo connectionInfo = 
                    HuaweiIotPasswordGenerator.generateConnectionInfo(finalDeviceId, finalDeviceSecret);

            log.info("生成的连接信息 - Device ID: {}, Client ID: {}, Username: {}, Timestamp: {}, Password: {}...", 
                    finalDeviceId,
                    connectionInfo.getClientId(), 
                    connectionInfo.getUsername(), 
                    connectionInfo.getTimestamp(),
                    connectionInfo.getPassword().length() > 16 ? connectionInfo.getPassword().substring(0, 16) + "..." : connectionInfo.getPassword());

            // 创建MQTT客户端（使用动态生成的Client ID）
            mqttClient = new MqttClient(broker, connectionInfo.getClientId(), new MemoryPersistence());
            mqttClient.setCallback(this);

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(connectionInfo.getUsername());
            options.setPassword(connectionInfo.getPassword().toCharArray());
            options.setConnectionTimeout(connectionTimeout);
            options.setKeepAliveInterval(keepAlive);
            options.setAutomaticReconnect(true); // 自动重连
            // 华为云IoT：由于Client ID包含时间戳会变化，使用cleanSession=true
            // 这样每次连接都是新会话，避免重复订阅问题
            options.setCleanSession(true);
            
            // 重置订阅状态（新连接需要重新订阅）
            subscribed = false;

            // 连接
            connect(options);

            log.info("MQTT客户端初始化成功，Broker: {}, Device ID: {}, Timestamp: {}", 
                    broker, finalDeviceId, connectionInfo.getTimestamp());
        } catch (Exception e) {
            log.error("MQTT客户端初始化失败", e);
        }
    }

    @Override
    public void connect() {
        if (mqttClient == null) {
            log.warn("MQTT客户端未初始化");
            return;
        }

        try {
            if (!mqttClient.isConnected()) {
                // 获取设备信息
                String finalDeviceId = deviceId;
                String finalDeviceSecret = deviceSecret;

                if ((finalDeviceId == null || finalDeviceId.isEmpty()) ||
                    (finalDeviceSecret == null || finalDeviceSecret.isEmpty())) {
                    HuaweiIotDeviceConfig.DeviceInfo deviceInfo = deviceConfig.getDeviceInfo();
                    if (deviceInfo != null) {
                        if (finalDeviceId == null || finalDeviceId.isEmpty()) {
                            finalDeviceId = deviceInfo.getDeviceId();
                        }
                        if (finalDeviceSecret == null || finalDeviceSecret.isEmpty()) {
                            finalDeviceSecret = deviceInfo.getSecret();
                        }
                    } else {
                        log.error("无法获取设备信息，连接失败");
                        return;
                    }
                }

                // 每次连接时重新生成动态密码（因为时间戳会变化）
                HuaweiIotPasswordGenerator.MqttConnectionInfo connectionInfo = 
                        HuaweiIotPasswordGenerator.generateConnectionInfo(finalDeviceId, finalDeviceSecret);

                // 如果Client ID已变化，需要重新创建MQTT客户端
                if (!mqttClient.getClientId().equals(connectionInfo.getClientId())) {
                    log.info("Client ID已变化，重新创建MQTT客户端。旧: {}, 新: {}", 
                            mqttClient.getClientId(), connectionInfo.getClientId());
                    
                    // 断开旧连接
                    if (mqttClient.isConnected()) {
                        mqttClient.disconnect();
                    }
                    
                    // 创建新客户端
                    mqttClient = new MqttClient(broker, connectionInfo.getClientId(), new MemoryPersistence());
                    mqttClient.setCallback(this);
                }

                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(connectionInfo.getUsername());
                options.setPassword(connectionInfo.getPassword().toCharArray());
                options.setConnectionTimeout(connectionTimeout);
                options.setKeepAliveInterval(keepAlive);
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);

                connect(options);
            }
        } catch (Exception e) {
            log.error("MQTT连接失败", e);
            connected = false;
        }
    }

    /**
     * 使用指定选项连接
     */
    private void connect(MqttConnectOptions options) {
        if (mqttClient == null) {
            log.warn("MQTT客户端未初始化");
            return;
        }

        try {
            if (!mqttClient.isConnected()) {
                mqttClient.connect(options);
                connected = true;
                log.info("MQTT连接成功，Broker: {}, Client ID: {}", broker, mqttClient.getClientId());

                // 等待连接完全建立后再订阅主题
                // 华为云IoT可能需要一些时间来完成连接握手
                Thread.sleep(1000);
                
                // 再次检查连接状态
                if (mqttClient.isConnected()) {
                    // 订阅主题（使用异步方式，避免阻塞应用启动）
                    if (topic != null && !topic.isEmpty() && !subscribed) {
                        subscribeWithRetry(topic, qos);
                    }
                } else {
                    log.warn("MQTT连接状态异常，跳过订阅主题");
                }
            }
        } catch (MqttException e) {
            log.error("MQTT连接失败", e);
            connected = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待连接建立时被中断", e);
        }
    }

    @Override
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                connected = false;
                log.info("MQTT连接已断开");
            } catch (MqttException e) {
                log.error("MQTT断开连接失败", e);
            }
        }
    }

    @Override
    public void subscribe(String topic, int qos) {
        subscribeWithRetry(topic, qos, 0);
    }

    /**
     * 带重试机制的订阅方法
     * 
     * @param topic 主题
     * @param qos QoS等级
     */
    private void subscribeWithRetry(String topic, int qos) {
        subscribeWithRetry(topic, qos, 0);
    }

    /**
     * 带重试机制的订阅方法
     * 
     * @param topic 主题
     * @param qos QoS等级
     * @param retryCount 当前重试次数
     */
    private void subscribeWithRetry(String topic, int qos, int retryCount) {
        if (mqttClient == null) {
            log.warn("MQTT客户端未初始化，无法订阅主题: {}", topic);
            return;
        }

        if (!mqttClient.isConnected()) {
            log.warn("MQTT客户端未连接，无法订阅主题: {}", topic);
            // 如果未连接，延迟后重试
            if (retryCount < 3) {
                log.info("将在3秒后重试订阅主题: {}", topic);
                try {
                    Thread.sleep(3000);
                    if (mqttClient.isConnected()) {
                        subscribeWithRetry(topic, qos, retryCount + 1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("重试订阅时被中断", e);
                }
            }
            return;
        }

        // 如果已经订阅过，跳过
        if (subscribed) {
            log.debug("主题已订阅，跳过: {}", topic);
            return;
        }

        try {
            // 使用同步订阅（subscribe方法返回void，异常会直接抛出）
            mqttClient.subscribe(topic, qos);
            subscribed = true;
            log.info("MQTT订阅主题成功: {}, QoS: {}", topic, qos);
        } catch (MqttException e) {
            int reasonCode = e.getReasonCode();
            log.error("MQTT订阅主题失败: {}, 错误代码: {}, 原因: {}", 
                    topic, reasonCode, e.getMessage());
            
            // 根据错误代码提供详细的错误信息和建议
            switch (reasonCode) {
                case MqttException.REASON_CODE_NOT_AUTHORIZED:
                    log.error("订阅主题权限被拒绝");
                    log.error("   请检查：");
                    log.error("   1. 设备是否有订阅该主题的权限");
                    log.error("   2. 主题格式是否正确: {}", topic);
                    log.error("   3. 是否需要在华为云IoT平台配置订阅权限");
                    log.warn("   订阅失败不会阻止应用启动，但无法接收MQTT消息");
                    break;
                case MqttException.REASON_CODE_CLIENT_NOT_CONNECTED:
                    log.error("MQTT客户端未连接");
                    log.error("   连接可能已断开，将尝试重新连接");
                    connected = false;
                    // 延迟后重试
                    if (retryCount < 3) {
                        scheduleRetry(topic, qos, retryCount + 1);
                    }
                    break;
                case 128: // MQTT 错误代码 128 通常表示订阅失败
                    log.error("订阅主题失败（错误代码128）");
                    log.error("   可能原因：");
                    log.error("   1. 主题格式错误或无效: {}", topic);
                    log.error("   2. 华为云IoT主题格式: /{实例ID}/{设备ID}/user/{自定义主题} 或 $oc/{实例ID}/{设备ID}/user/{自定义主题}");
                    log.error("   3. 设备没有订阅该主题的权限");
                    log.error("   4. 可能是重复订阅（如果cleanSession=false且之前已订阅）");
                    log.warn("   订阅失败不会阻止应用启动，但无法接收MQTT消息");
                    log.warn("   建议：检查华为云IoT平台的设备权限配置");
                    // 对于错误代码128，不重试（可能是权限问题）
                    break;
                default:
                    log.error("未知错误，错误代码: {}", reasonCode);
                    log.error("   错误消息: {}", e.getMessage());
                    if (e.getCause() != null) {
                        log.error("   根本原因: {}", e.getCause().getMessage());
                    }
                    // 对于其他错误，延迟后重试
                    if (retryCount < 3) {
                        scheduleRetry(topic, qos, retryCount + 1);
                    }
            }
        } catch (Exception e) {
            log.error("MQTT订阅主题时发生未知异常: {}", topic, e);
            // 对于未知异常，延迟后重试
            if (retryCount < 3) {
                scheduleRetry(topic, qos, retryCount + 1);
            }
        }
    }

    /**
     * 延迟后重试订阅
     */
    private void scheduleRetry(String topic, int qos, int retryCount) {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待5秒后重试
                if (mqttClient != null && mqttClient.isConnected()) {
                    log.info("重试订阅主题 (第{}次): {}", retryCount, topic);
                    subscribeWithRetry(topic, qos, retryCount);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("重试订阅时被中断", e);
            }
        }).start();
    }

    @Override
    public void unsubscribe(String topic) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT客户端未连接，无法取消订阅主题: {}", topic);
            return;
        }

        try {
            mqttClient.unsubscribe(topic);
            log.info("MQTT取消订阅主题成功: {}", topic);
        } catch (MqttException e) {
            log.error("MQTT取消订阅主题失败: {}", topic, e);
        }
    }

    @Override
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected() && connected;
    }

    /**
     * MQTT消息到达回调
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            log.debug("收到MQTT消息，主题: {}, 内容: {}", topic, payload);

            // 解析消息
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            // 提取设备ID和位置数据
            String deviceId = extractDeviceId(topic, data);
            if (deviceId == null || deviceId.isEmpty()) {
                log.warn("无法从MQTT消息中提取设备ID，主题: {}, 数据: {}", topic, data);
                return;
            }

            // 处理位置更新
            Map<String, Object> locationData = extractLocationData(data);
            vehicleLocationService.processLocationUpdate(deviceId, locationData);

            log.debug("处理MQTT位置更新成功，设备ID: {}", deviceId);
        } catch (Exception e) {
            log.error("处理MQTT消息失败，主题: {}", topic, e);
        }
    }

    /**
     * 连接丢失回调
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT连接丢失", cause);
        connected = false;
        subscribed = false; // 连接丢失后，重置订阅状态

        // 自动重连（每次重连时重新生成动态密码）
        log.info("MQTT客户端将自动尝试重连（使用新的动态密码）...");
        new Thread(() -> {
            try {
                // 延迟重连，避免频繁重连
                Thread.sleep(5000);
                connect();
                
                // 重连成功后，重新订阅主题
                if (mqttClient != null && mqttClient.isConnected()) {
                    Thread.sleep(1000); // 等待连接稳定
                    if (topic != null && !topic.isEmpty()) {
                        subscribed = false; // 重置订阅状态，允许重新订阅
                        subscribeWithRetry(topic, qos);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("重连等待被中断", e);
            } catch (Exception e) {
                log.error("自动重连失败", e);
            }
        }).start();
    }

    /**
     * 消息发送完成回调
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 本服务只接收消息，不发送消息，此方法不需要实现
    }

    /**
     * 从主题或数据中提取设备ID
     * 华为云IoT主题格式支持两种：
     * 1. 带$oc前缀: $oc/{实例ID}/{device_id}/user/{topic}
     * 2. 不带前缀: /{实例ID}/{device_id}/user/{topic}
     * 示例:
     *   - $oc/{实例ID}/{device_id}/user/location
     *   - /{实例ID}/{device_id}/user/location
     */
    private String extractDeviceId(String topic, Map<String, Object> data) {
        // 方式1: 从主题中提取（华为云IoT标准格式）
        if (topic != null && topic.contains("/")) {
            String[] parts = topic.split("/");
            // 支持两种格式：
            // 格式1: $oc/{实例ID}/{device_id}/... (parts[0] = "$oc", parts[1] = 实例ID, parts[2] = 设备ID)
            // 格式2: /{实例ID}/{device_id}/... (parts[0] = "", parts[1] = 实例ID, parts[2] = 设备ID)
            if (parts.length >= 3) {
                if (parts[0].equals("$oc")) {
                    // 格式1: $oc/{实例ID}/{device_id}/...
                    return parts[2];
                } else if (parts[0].isEmpty() && parts.length >= 3) {
                    // 格式2: /{实例ID}/{device_id}/... (第一个元素是空字符串，因为以/开头)
                    return parts[2];
                }
            }
        }

        // 方式2: 从消息数据中提取
        Object deviceIdObj = data.get("deviceId");
        if (deviceIdObj != null) {
            return deviceIdObj.toString();
        }

        // 方式3: 从消息数据中提取（其他可能的字段名）
        Object deviceIdObj2 = data.get("device_id");
        if (deviceIdObj2 != null) {
            return deviceIdObj2.toString();
        }

        return null;
    }

    /**
     * 从MQTT消息中提取位置数据
     * 支持多种数据格式
     */
    private Map<String, Object> extractLocationData(Map<String, Object> data) {
        // 如果数据中直接包含位置信息
        if (data.containsKey("longitude") && data.containsKey("latitude")) {
            return data;
        }

        // 如果位置信息在嵌套对象中
        Object servicesObj = data.get("services");
        if (servicesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> services = (Map<String, Object>) servicesObj;
            
            // 查找位置服务（华为云IoT格式）
            for (Object serviceObj : services.values()) {
                if (serviceObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> service = (Map<String, Object>) serviceObj;
                    Object propertiesObj = service.get("properties");
                    if (propertiesObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> properties = (Map<String, Object>) propertiesObj;
                        if (properties.containsKey("longitude") || properties.containsKey("latitude")) {
                            return properties;
                        }
                    }
                }
            }
        }

        // 如果位置信息在data字段中
        Object dataObj = data.get("data");
        if (dataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> locationData = (Map<String, Object>) dataObj;
            if (locationData.containsKey("longitude") || locationData.containsKey("latitude")) {
                return locationData;
            }
        }

        // 如果位置信息在location字段中
        Object locationObj = data.get("location");
        if (locationObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> locationData = (Map<String, Object>) locationObj;
            if (locationData.containsKey("longitude") || locationData.containsKey("latitude")) {
                return locationData;
            }
        }

        // 返回原始数据（让VehicleLocationService处理）
        return data;
    }

    /**
     * 销毁时断开连接
     */
    @PreDestroy
    public void destroy() {
        disconnect();
    }
}
