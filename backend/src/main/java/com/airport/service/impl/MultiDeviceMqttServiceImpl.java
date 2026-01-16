package com.airport.service.impl;

import com.airport.config.HuaweiIotDeviceConfig;
import com.airport.service.MultiDeviceMqttService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多设备MQTT服务实现
 * 
 * 数据流转路径：
 * mobile_001 (手机定位) -> vehicle_001 (车辆定位器) -> web_001 (网页监控端) -> 系统
 * 
 * 实现逻辑：
 * 1. vehicle_001 订阅 mobile_001 的位置主题
 * 2. vehicle_001 接收消息后，转发到 web_001 的主题
 * 3. web_001 订阅自己的主题，接收位置数据
 * 4. 系统通过 web_001 接收位置数据，通过 WebSocket 推送到前端
 * 
 * @author Corkedmzx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiDeviceMqttServiceImpl implements MultiDeviceMqttService, MqttCallback {

    private final VehicleLocationService vehicleLocationService;
    private final ObjectMapper objectMapper;
    private final HuaweiIotDeviceConfig deviceConfig;

    @Value("${huawei.iot.mqtt.broker:}")
    private String broker;

    @Value("${huawei.iot.mqtt.device-key-path:e:/share/设备密钥}")
    private String deviceKeyPath;

    @Value("${huawei.iot.mqtt.enabled:false}")
    private boolean enabled;

    @Value("${huawei.iot.mqtt.connection-timeout:30}")
    private int connectionTimeout;

    @Value("${huawei.iot.mqtt.keep-alive:60}")
    private int keepAlive;

    @Value("${huawei.iot.mqtt.instance-id:a494d922-ff97-4873-bd0c-2d6b1a72086d}")
    private String instanceId;

    // 设备客户端映射：deviceName -> MqttClient
    private final Map<String, MqttClient> deviceClients = new ConcurrentHashMap<>();
    
    // 设备信息映射：deviceName -> DeviceInfo
    private final Map<String, DeviceInfo> deviceInfos = new ConcurrentHashMap<>();
    
    // 设备连接状态映射：deviceName -> connected
    private final Map<String, Boolean> deviceConnected = new ConcurrentHashMap<>();

    /**
     * 设备信息
     */
    private static class DeviceInfo {
        String deviceId;
        String secret;
        String subscribeTopic;  // 订阅的主题
        String publishTopic;    // 发布的主题（用于转发）
    }

    /**
     * 初始化所有设备连接
     */
    @PostConstruct
    @Override
    public void initializeAllDevices() {
        if (!enabled) {
            log.info("MQTT客户端未启用，跳过多设备初始化");
            return;
        }

        if (broker == null || broker.isEmpty()) {
            log.warn("MQTT Broker地址未配置，多设备MQTT客户端将不会启动");
            return;
        }

        log.info("开始初始化多设备MQTT连接...");

        // 加载所有设备信息
        loadAllDeviceInfos();

        // 连接 vehicle_001（中转设备，订阅 mobile_001，转发到 web_001）
        connectDevice("vehicle_001");

        // 连接 web_001（接收 vehicle_001 转发的数据）
        connectDevice("web_001");

        log.info("多设备MQTT初始化完成");
    }

    /**
     * 加载所有设备信息
     */
    private void loadAllDeviceInfos() {
        try {
            Path deviceKeyDir = Paths.get(deviceKeyPath);
            if (!Files.exists(deviceKeyDir)) {
                log.error("设备密钥目录不存在: {}", deviceKeyPath);
                return;
            }

            // 加载 vehicle_001
            DeviceInfo vehicleInfo = loadDeviceInfo("vehicle_001");
            if (vehicleInfo != null) {
                deviceInfos.put("vehicle_001", vehicleInfo);
                // vehicle_001 订阅 mobile_001 的位置主题
                vehicleInfo.subscribeTopic = String.format("/%s/%s/user/location", 
                        instanceId, "6961b5c87f2e6c302f48db15_mobile_001");
                // vehicle_001 转发到 web_001 的主题
                vehicleInfo.publishTopic = String.format("/%s/%s/user/location", 
                        instanceId, "6961b5c87f2e6c302f48db15_web_001");
            }

            // 加载 web_001
            DeviceInfo webInfo = loadDeviceInfo("web_001");
            if (webInfo != null) {
                deviceInfos.put("web_001", webInfo);
                // web_001 订阅自己的主题，接收 vehicle_001 转发的数据
                webInfo.subscribeTopic = String.format("/%s/%s/user/location", 
                        instanceId, webInfo.deviceId);
            }

        } catch (Exception e) {
            log.error("加载设备信息失败", e);
        }
    }

    /**
     * 从文件加载设备信息
     */
    private DeviceInfo loadDeviceInfo(String deviceName) {
        try {
            Path deviceKeyDir = Paths.get(deviceKeyPath);
            String fileNamePattern = "DEVICES-KEY-*_" + deviceName + ".txt";
            
            java.io.File[] files = deviceKeyDir.toFile().listFiles((dir, name) -> 
                name.startsWith("DEVICES-KEY-") && name.endsWith(".txt") && name.contains(deviceName));
            
            if (files == null || files.length == 0) {
                log.warn("未找到设备密钥文件: {}", deviceName);
                return null;
            }

            String jsonContent = Files.readString(files[0].toPath());
            Map<String, String> deviceData = objectMapper.readValue(jsonContent, Map.class);
            
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.deviceId = deviceData.get("device_id");
            deviceInfo.secret = deviceData.get("secret");
            
            log.info("加载设备信息成功: {} -> {}", deviceName, deviceInfo.deviceId);
            return deviceInfo;
        } catch (Exception e) {
            log.error("加载设备信息失败: {}", deviceName, e);
            return null;
        }
    }

    /**
     * 连接指定设备
     */
    @Override
    public void connectDevice(String deviceName) {
        DeviceInfo deviceInfo = deviceInfos.get(deviceName);
        if (deviceInfo == null) {
            log.error("设备信息不存在: {}", deviceName);
            return;
        }

        if (deviceClients.containsKey(deviceName) && deviceConnected.getOrDefault(deviceName, false)) {
            log.info("设备已连接: {}", deviceName);
            return;
        }

        try {
            // 生成动态连接信息
            HuaweiIotPasswordGenerator.MqttConnectionInfo connectionInfo = 
                    HuaweiIotPasswordGenerator.generateConnectionInfo(deviceInfo.deviceId, deviceInfo.secret);

            log.info("连接设备: {} (Device ID: {}, Client ID: {})", 
                    deviceName, deviceInfo.deviceId, connectionInfo.getClientId());

            // 创建MQTT客户端
            MqttClient mqttClient = new MqttClient(broker, connectionInfo.getClientId(), new MemoryPersistence());
            mqttClient.setCallback(new DeviceMqttCallback(deviceName));

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(connectionInfo.getUsername());
            options.setPassword(connectionInfo.getPassword().toCharArray());
            options.setConnectionTimeout(connectionTimeout);
            options.setKeepAliveInterval(keepAlive);
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            // 连接
            mqttClient.connect(options);
            deviceClients.put(deviceName, mqttClient);
            deviceConnected.put(deviceName, true);

            log.info("设备连接成功: {}", deviceName);

            // 等待连接稳定后订阅主题
            Thread.sleep(500);
            
            if (deviceInfo.subscribeTopic != null && !deviceInfo.subscribeTopic.isEmpty()) {
                subscribeTopic(deviceName, deviceInfo.subscribeTopic, 1);
            }

        } catch (Exception e) {
            log.error("连接设备失败: {}", deviceName, e);
            deviceConnected.put(deviceName, false);
        }
    }

    /**
     * 订阅主题
     */
    private void subscribeTopic(String deviceName, String topic, int qos) {
        MqttClient mqttClient = deviceClients.get(deviceName);
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("设备未连接，无法订阅主题: {} -> {}", deviceName, topic);
            return;
        }

        try {
            mqttClient.subscribe(topic, qos);
            log.info("设备订阅主题成功: {} -> {}, QoS: {}", deviceName, topic, qos);
        } catch (MqttException e) {
            log.error("设备订阅主题失败: {} -> {}, 错误: {}", deviceName, topic, e.getMessage(), e);
        }
    }

    /**
     * 断开指定设备连接
     */
    @Override
    public void disconnectDevice(String deviceName) {
        MqttClient mqttClient = deviceClients.get(deviceName);
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                deviceConnected.put(deviceName, false);
                log.info("设备断开连接: {}", deviceName);
            } catch (MqttException e) {
                log.error("断开设备连接失败: {}", deviceName, e);
            }
        }
    }

    /**
     * 获取设备连接状态
     */
    @Override
    public boolean isDeviceConnected(String deviceName) {
        MqttClient mqttClient = deviceClients.get(deviceName);
        return mqttClient != null && mqttClient.isConnected() && deviceConnected.getOrDefault(deviceName, false);
    }

    /**
     * 发布消息到指定设备的主题
     */
    @Override
    public void publishToDevice(String deviceName, String topic, String payload, int qos) {
        MqttClient mqttClient = deviceClients.get(deviceName);
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("设备未连接，无法发布消息: {} -> {}", deviceName, topic);
            return;
        }

        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            mqttClient.publish(topic, message);
            log.debug("设备发布消息成功: {} -> {}", deviceName, topic);
        } catch (MqttException e) {
            log.error("设备发布消息失败: {} -> {}", deviceName, topic, e);
        }
    }

    /**
     * 设备MQTT回调类
     */
    private class DeviceMqttCallback implements MqttCallback {
        private final String deviceName;

        public DeviceMqttCallback(String deviceName) {
            this.deviceName = deviceName;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                log.debug("设备收到消息: {} -> 主题: {}, 内容: {}", deviceName, topic, payload);

                // 解析消息
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);

                // 根据设备类型处理消息
                DeviceInfo deviceInfo = deviceInfos.get(deviceName);
                if (deviceInfo == null) {
                    return;
                }

                if ("vehicle_001".equals(deviceName)) {
                    // vehicle_001 接收 mobile_001 的位置数据，转发到 web_001
                    handleVehicleForward(data, deviceInfo);
                } else if ("web_001".equals(deviceName)) {
                    // web_001 接收 vehicle_001 转发的数据，推送到系统
                    handleWebReceive(topic, data);
                }

            } catch (Exception e) {
                log.error("处理设备消息失败: {} -> {}", deviceName, topic, e);
            }
        }

        /**
         * vehicle_001 转发消息到 web_001
         */
        private void handleVehicleForward(Map<String, Object> data, DeviceInfo deviceInfo) {
            try {
                // 转发到 web_001 的主题
                String forwardTopic = deviceInfo.publishTopic;
                if (forwardTopic != null && !forwardTopic.isEmpty()) {
                    String payload = objectMapper.writeValueAsString(data);
                    publishToDevice("vehicle_001", forwardTopic, payload, 1);
                    log.debug("vehicle_001 转发消息到 web_001: {}", forwardTopic);
                }
            } catch (Exception e) {
                log.error("vehicle_001 转发消息失败", e);
            }
        }

        /**
         * web_001 接收消息，推送到系统
         */
        private void handleWebReceive(String topic, Map<String, Object> data) {
            try {
                // 提取设备ID和位置数据
                String deviceId = extractDeviceId(topic, data);
                if (deviceId == null || deviceId.isEmpty()) {
                    log.warn("⚠️ [web_001] 无法从消息中提取设备ID，主题: {}", topic);
                    return;
                }

                // 检查是否是PC位置
                String source = getStringValue(data, "source");
                String deviceName = getStringValue(data, "deviceName");
                boolean isPCLocation = "pc_browser".equals(source) || "pc_location".equals(deviceName);
                
                if (isPCLocation) {
                    Double latitude = getDoubleValue(data, "latitude");
                    Double longitude = getDoubleValue(data, "longitude");
                    log.info("[web_001] 收到PC位置消息，设备ID: {}, 位置: ({}, {}), 主题: {}", 
                            deviceId, latitude, longitude, topic);
                }

                // 处理位置更新
                Map<String, Object> locationData = extractLocationData(data);
                vehicleLocationService.processLocationUpdate(deviceId, locationData);
                
                if (isPCLocation) {
                    log.info("[web_001] PC位置处理成功，已推送到VehicleLocationService");
                } else {
                    log.debug("web_001 处理位置更新成功，设备ID: {}", deviceId);
                }
            } catch (Exception e) {
                log.error("[web_001] 处理消息失败", e);
            }
        }
        
        /**
         * 获取String值
         */
        private String getStringValue(Map<String, Object> data, String key) {
            Object value = data.get(key);
            return value != null ? value.toString() : null;
        }
        
        /**
         * 获取Double值
         */
        private Double getDoubleValue(Map<String, Object> data, String key) {
            Object value = data.get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(value.toString());
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            log.warn("设备连接丢失: {}", deviceName, cause);
            deviceConnected.put(deviceName, false);

            // 自动重连
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    connectDevice(deviceName);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // 不需要实现
        }
    }

    /**
     * 从主题或数据中提取设备ID
     */
    private String extractDeviceId(String topic, Map<String, Object> data) {
        // 从主题中提取
        if (topic != null && topic.contains("/")) {
            String[] parts = topic.split("/");
            if (parts.length >= 3) {
                if (parts[0].equals("$oc") || parts[0].isEmpty()) {
                    // 格式: $oc/{实例ID}/{device_id}/... 或 /{实例ID}/{device_id}/...
                    return parts[2];
                }
            }
        }

        // 从消息数据中提取
        Object deviceIdObj = data.get("deviceId");
        if (deviceIdObj != null) {
            return deviceIdObj.toString();
        }

        Object deviceIdObj2 = data.get("device_id");
        if (deviceIdObj2 != null) {
            return deviceIdObj2.toString();
        }

        return null;
    }

    /**
     * 从MQTT消息中提取位置数据
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

        // 返回原始数据
        return data;
    }

    /**
     * 销毁时断开所有连接
     */
    @PreDestroy
    public void destroy() {
        log.info("断开所有设备连接...");
        deviceClients.forEach((deviceName, client) -> {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (MqttException e) {
                log.error("断开设备连接失败: {}", deviceName, e);
            }
        });
        deviceClients.clear();
        deviceConnected.clear();
    }

    // MqttCallback 接口方法（用于兼容，实际使用 DeviceMqttCallback）
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // 不会调用，使用 DeviceMqttCallback
    }

    @Override
    public void connectionLost(Throwable cause) {
        // 不会调用，使用 DeviceMqttCallback
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 不会调用，使用 DeviceMqttCallback
    }
}
