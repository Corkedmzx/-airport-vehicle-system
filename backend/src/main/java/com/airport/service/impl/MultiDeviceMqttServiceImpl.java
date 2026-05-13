package com.airport.service.impl;

import com.airport.config.HardwareGpsDeviceRegistry;
import com.airport.config.HuaweiIotDeviceConfig;
import com.airport.repository.VehicleRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多设备MQTT服务实现
 * 
 * 数据流转路径：
 * mobile_001（小程序）→ vehicle_001（仅中转：订阅 mobile、转发 web）→ web_001 → 系统。
 * 硬件 GPS：独立 MQTT 连接 {@code hardware-gps-mqtt-client-name}（如 vehicle_002）与中转 {@code vehicle_001} 并存；
 * 因华为 IoTDA 通常不把「本设备 user 上行」投递给「同设备身份」的订阅端，实际上行由 {@code vehicle_001} 跨设备只读订阅消费，
 * 与小程序→web 转发在回调中分支隔离，不经同一业务转发链。
 * PC 浏览器位置由 {@link com.airport.controller.MqttLocationController} 使用 web_001 直连发布到 web 主题，不经 vehicle_001。
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
    private final HardwareGpsDeviceRegistry hardwareGpsDeviceRegistry;
    private final VehicleRepository vehicleRepository;

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

    /**
     * 是否把 MySQL 中 vehicle.gps_device_id 合并进 MQTT 订阅列表（推荐开启：新增车辆定位器时仅需库表 + 华为创建设备后重启后端）。
     */
    @Value("${huawei.iot.mqtt.hardware-gps-subscribe-from-database:true}")
    private boolean hardwareGpsSubscribeFromDatabase;

    /**
     * 是否订阅带 {@code $oc/} 前缀的硬件上行 Topic。华为侧常因 ACL 拒绝导致失败；
     * 与固件、消息跟踪一致的无前缀路径 {@code /{实例}/{设备ID}/user/location} 即可消费，默认关闭。
     */
    @Value("${huawei.iot.mqtt.hardware-gps-subscribe-oc-prefix:false}")
    private boolean hardwareGpsSubscribeOcPrefix;

    /**
     * 为 true 时：额外建立独立 MQTT 连接（见 {@link #hardwareGpsMqttClientName} 密钥，与 ESP 设备身份对应）；
     * 华为侧本机上行往往不由「同身份订阅」投递，故 vehicle_001 仍会跨设备只读订阅硬件 Topic，回调中与 mobile→web 转发严格分支隔离。
     */
    @Value("${huawei.iot.mqtt.hardware-gps-dedicated-client:true}")
    private boolean hardwareGpsDedicatedClient;

    /**
     * 独立硬件 MQTT 在密钥目录中的逻辑名，对应 {@code DEVICES-KEY-*_{name}.txt}，一般为华为上的 GPS 设备 vehicle_002。
     */
    @Value("${huawei.iot.mqtt.hardware-gps-mqtt-client-name:vehicle_002}")
    private String hardwareGpsMqttClientName;

    /** 启动时解析得到的、参与 hardware 上行 Topic 匹配的设备 ID 集合（跨设备订阅模式用） */
    private volatile Set<String> hardwareGpsSubscriptionIds = Set.of();

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
        /** 额外订阅（如 ESP 硬件 vehicle_002 上行），由同一 MQTT 连接消费 */
        List<String> extraSubscribeTopics = new ArrayList<>();
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

        // 连接 mobile_001（小程序位置发布设备）
        connectDevice("mobile_001");

        // 连接 vehicle_001（中转：订阅 mobile_001，转发 web_001）
        connectDevice("vehicle_001");

        if (hardwareGpsDedicatedClient && deviceInfos.containsKey(hardwareGpsMqttClientName)) {
            connectDevice(hardwareGpsMqttClientName);
        }

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

            // 加载 mobile_001（小程序位置发布设备）
            DeviceInfo mobileInfo = loadDeviceInfo("mobile_001");
            if (mobileInfo != null) {
                deviceInfos.put("mobile_001", mobileInfo);
                // mobile_001 只发布数据，不订阅（所有小程序用户共享此设备）
                log.info("mobile_001 设备已加载，用于小程序位置发布");
            }

            // 独立硬件密钥预加载（与 vehicle_001 解析共用，避免重复读文件）
            DeviceInfo gpsDedicatedFile = hardwareGpsDedicatedClient ? loadDeviceInfo(hardwareGpsMqttClientName) : null;

            // 加载 vehicle_001
            DeviceInfo vehicleInfo = loadDeviceInfo("vehicle_001");
            if (vehicleInfo != null) {
                deviceInfos.put("vehicle_001", vehicleInfo);

                if (hardwareGpsDedicatedClient) {
                    /*
                     * 华为 IoTDA：设备 A 上报到 /{实例}/A/user/… 时，通常不会把该上行 MQTT 投递给「仍以 A 身份订阅的同一路径」，
                     * 故独立连接 vehicle_002 仅保持与 ESP 一致的设备会话；实际上行由 vehicle_001 跨设备只读订阅（与小程序转发分支隔离）。
                     */
                    vehicleInfo.extraSubscribeTopics.clear();
                    Set<String> hwIds = resolveHardwareGpsDeviceIdsForSubscription(vehicleInfo.deviceId);
                    if (gpsDedicatedFile != null && gpsDedicatedFile.deviceId != null && !gpsDedicatedFile.deviceId.isBlank()) {
                        hwIds.add(gpsDedicatedFile.deviceId.trim());
                    }
                    this.hardwareGpsSubscriptionIds = Collections.unmodifiableSet(new LinkedHashSet<>(hwIds));
                    for (String hid : hwIds) {
                        if (hardwareGpsSubscribeOcPrefix) {
                            vehicleInfo.extraSubscribeTopics.add(
                                    String.format("$oc/%s/%s/user/location", instanceId, hid));
                        }
                        vehicleInfo.extraSubscribeTopics.add(
                                String.format("/%s/%s/user/location", instanceId, hid));
                    }
                    if (!hwIds.isEmpty()) {
                        log.info("hardware-gps-dedicated-client=true：vehicle_001 额外只读订阅 {} 路硬件 GPS 上行 device_id={}（与 mobile→web 转发在回调中隔离）；"
                                        + "独立连接「{}」另行建立（不订阅本机上行，避免华为侧零投递）。",
                                hwIds.size(), hwIds, hardwareGpsMqttClientName);
                    } else {
                        this.hardwareGpsSubscriptionIds = Set.of();
                        log.warn("未解析到任何硬件 GPS device_id；请配置 vehicle.gps_device_id 或 hardware-gps-device-ids，并放置 DEVICES-KEY-*_{}.txt。",
                                hardwareGpsMqttClientName);
                    }
                } else {
                    Set<String> hwIds = resolveHardwareGpsDeviceIdsForSubscription(vehicleInfo.deviceId);
                    this.hardwareGpsSubscriptionIds = Collections.unmodifiableSet(new LinkedHashSet<>(hwIds));
                    for (String hid : hwIds) {
                        if (hardwareGpsSubscribeOcPrefix) {
                            vehicleInfo.extraSubscribeTopics.add(
                                    String.format("$oc/%s/%s/user/location", instanceId, hid));
                        }
                        vehicleInfo.extraSubscribeTopics.add(
                                String.format("/%s/%s/user/location", instanceId, hid));
                    }
                    if (!hwIds.isEmpty()) {
                        log.info("vehicle_001 将额外订阅 {} 个硬件 GPS 设备 MQTT 上行，设备 ID 列表: {}（$oc 前缀: {}）",
                                hwIds.size(), hwIds, hardwareGpsSubscribeOcPrefix ? "开" : "关");
                    } else {
                        log.warn("未解析到任何「独立硬件」GPS 设备 ID（已排除与 vehicle_001 相同的 device_id）。"
                                + "ESP 若使用 vehicle_002 等其它设备上报，请在 vehicle.gps_device_id 或 huawei.iot.mqtt.hardware-gps-device-ids 中配置该完整 ID；"
                                + "或启用 hardware-gps-dedicated-client 并为 {} 准备密钥文件后重启。", hardwareGpsMqttClientName);
                    }
                }
            }

            if (hardwareGpsDedicatedClient) {
                if (gpsDedicatedFile != null) {
                    deviceInfos.put(hardwareGpsMqttClientName, gpsDedicatedFile);
                    gpsDedicatedFile.publishTopic = null;
                    gpsDedicatedFile.extraSubscribeTopics.clear();
                    if (deviceInfos.containsKey("vehicle_001")) {
                        gpsDedicatedFile.subscribeTopic = null;
                        log.info("独立硬件 MQTT「{}」仅建立会话（不订阅本机 user 上行）；上行由 vehicle_001 跨设备订阅消费。",
                                hardwareGpsMqttClientName);
                    } else {
                        gpsDedicatedFile.subscribeTopic = String.format("/%s/%s/user/location", instanceId, gpsDedicatedFile.deviceId);
                        if (hardwareGpsSubscribeOcPrefix) {
                            gpsDedicatedFile.extraSubscribeTopics.add(
                                    String.format("$oc/%s/%s/user/location", instanceId, gpsDedicatedFile.deviceId));
                        }
                        this.hardwareGpsSubscriptionIds = Collections.singleton(gpsDedicatedFile.deviceId);
                        log.warn("未加载 vehicle_001，硬件 GPS 回退为「{}」自订阅本机上行。", hardwareGpsMqttClientName);
                    }
                } else {
                    if (!deviceInfos.containsKey("vehicle_001")) {
                        this.hardwareGpsSubscriptionIds = Set.of();
                    }
                    log.error("已开启 hardware-gps-dedicated-client，但未在目录 {} 找到 DEVICES-KEY-*_{}.txt；"
                                    + "若已配置 vehicle_001，仍可依赖库表/配置中的硬件 device_id 由 vehicle_001 订阅。",
                            deviceKeyPath, hardwareGpsMqttClientName);
                }
            }

            // 加载 web_001
            DeviceInfo webInfo = loadDeviceInfo("web_001");
            if (webInfo != null) {
                deviceInfos.put("web_001", webInfo);
                // web_001 订阅自己的主题，接收 vehicle_001 转发的数据
                webInfo.subscribeTopic = String.format("/%s/%s/user/location", 
                        instanceId, webInfo.deviceId);
            }

            linkVehicle001BridgeTopics();

        } catch (Exception e) {
            log.error("加载设备信息失败", e);
        }
    }

    /**
     * vehicle_001 订阅 mobile 的上行主题、向 web 的 user/location 转发；device_id 一律来自各设备 DEVICES-KEY 文件，避免硬编码。
     */
    private void linkVehicle001BridgeTopics() {
        DeviceInfo bridge = deviceInfos.get("vehicle_001");
        if (bridge == null) {
            return;
        }
        DeviceInfo mobile = deviceInfos.get("mobile_001");
        DeviceInfo web = deviceInfos.get("web_001");
        if (mobile != null) {
            bridge.subscribeTopic = String.format("/%s/%s/user/location", instanceId, mobile.deviceId);
        } else {
            log.warn("未加载 mobile_001 密钥，vehicle_001 无法配置订阅主题");
        }
        if (web != null) {
            bridge.publishTopic = String.format("/%s/%s/user/location", instanceId, web.deviceId);
        } else {
            log.warn("未加载 web_001 密钥，vehicle_001 无法配置转发发布主题");
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
            mqttClient.setCallback(new DeviceMqttCallback(deviceName, mqttClient));
            deviceClients.put(deviceName, mqttClient);

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(connectionInfo.getUsername());
            options.setPassword(connectionInfo.getPassword().toCharArray());
            options.setConnectionTimeout(connectionTimeout);
            options.setKeepAliveInterval(keepAlive);
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            // 连接（connectComplete 会再次订阅，此处 put 须在 connect 前以便回调内能取到 client）
            mqttClient.connect(options);
            deviceConnected.put(deviceName, true);

            log.info("设备连接成功: {}", deviceName);

            // 等待连接稳定后订阅主题
            Thread.sleep(500);
            subscribeAllTopicsForDevice(deviceName);

        } catch (Exception e) {
            log.error("连接设备失败: {}", deviceName, e);
            deviceConnected.put(deviceName, false);
            MqttClient stale = deviceClients.remove(deviceName);
            if (stale != null) {
                try {
                    if (stale.isConnected()) {
                        stale.disconnectForcibly();
                    }
                } catch (Exception closeEx) {
                    log.debug("清理失败连接: {}", deviceName, closeEx);
                }
            }
        }
    }

    /**
     * 为某连接订阅其主 topic 与 extra（首次连接与 Paho 自动重连后均需调用；cleanSession=true 时重连不会保留订阅）。
     */
    private void subscribeAllTopicsForDevice(String deviceName) {
        DeviceInfo deviceInfo = deviceInfos.get(deviceName);
        if (deviceInfo == null) {
            return;
        }
        if (deviceInfo.subscribeTopic != null && !deviceInfo.subscribeTopic.isEmpty()) {
            subscribeTopic(deviceName, deviceInfo.subscribeTopic, 1);
        }
        for (String extra : deviceInfo.extraSubscribeTopics) {
            if (extra != null && !extra.isEmpty()) {
                subscribeTopic(deviceName, extra, 1);
            }
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
            if (topic != null && topic.startsWith("$oc/")) {
                log.warn("设备订阅 $oc 主题被拒（可忽略，见 hardware-gps-subscribe-oc-prefix）: {} -> {} — {}",
                        deviceName, topic, e.getMessage());
            } else {
                log.error("设备订阅主题失败: {} -> {}, 错误: {}", deviceName, topic, e.getMessage(), e);
            }
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
     * 已加载设备的华为 device_id（来自密钥 JSON），未加载时返回 null。
     */
    @Override
    public String getLoadedDeviceId(String deviceName) {
        DeviceInfo info = deviceInfos.get(deviceName);
        return info != null ? info.deviceId : null;
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
     * 设备 MQTT 回调：使用 {@link MqttCallbackExtended}，在 Paho 自动重连后重新订阅（cleanSession=true 时订阅会丢失）。
     */
    private class DeviceMqttCallback implements MqttCallbackExtended {
        private final String deviceName;
        private final MqttClient client;

        public DeviceMqttCallback(String deviceName, MqttClient client) {
            this.deviceName = deviceName;
            this.client = client;
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (!client.isConnected()) {
                return;
            }
            deviceConnected.put(deviceName, true);
            if (reconnect) {
                log.warn("MQTT 已自动重连（{}），因 cleanSession=true 将重新订阅全部 Topic: {}", deviceName, serverURI);
            }
            subscribeAllTopicsForDevice(deviceName);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                log.debug("设备收到消息: {} -> 主题: {}, 内容: {}", deviceName, topic, payload);
                if ("vehicle_001".equals(deviceName) && topic != null && topic.contains("/user/location")) {
                    log.info("vehicle_001 MQTT 收包 topic={} hardwareRoute={} bytes={}",
                            topic, topicMatchesHardwareGpsUplink(topic), payload.length());
                }

                // 解析消息
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);

                // 根据设备类型处理消息
                DeviceInfo deviceInfo = deviceInfos.get(deviceName);
                if (deviceInfo == null) {
                    return;
                }

                if (hardwareGpsMqttClientName.equals(deviceName)) {
                    // 多数情况下华为不向「同设备身份」投递本机上行；若仍能收到则照常处理，避免与 vehicle_001 只读订阅重复时可依赖幂等写库
                    log.info("[{}] 收到 GPS 上行 topic={}, 推送调度服务", deviceName, topic);
                    deliverLocationToWebSocket(topic, data);
                    return;
                }

                if ("vehicle_001".equals(deviceName)) {
                    // ESP 等硬件直连：上行由本连接订阅，直接入库并推送地图（不经 mobile 转发）
                    if (topicMatchesHardwareGpsUplink(topic)) {
                        log.info("[vehicle_001] 收到硬件 GPS 上行 topic={}, 推送调度服务", topic);
                        deliverLocationToWebSocket(topic, data);
                        return;
                    }
                    // vehicle_001 接收 mobile_001 的位置数据，转发到 web_001
                    handleVehicleForward(data, deviceInfo);
                } else if ("web_001".equals(deviceName)) {
                    // web_001 接收 vehicle_001 转发的数据，推送到系统
                    deliverLocationToWebSocket(topic, data);
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
         * web_001 / 硬件 GPS 上行共用：写入车辆位置并 WebSocket 推送前端地图
         */
        private void deliverLocationToWebSocket(String topic, Map<String, Object> data) {
            try {
                String deviceId = extractDeviceId(topic, data);
                if (deviceId == null || deviceId.isEmpty()) {
                    log.warn("⚠️ 无法从消息中提取设备ID，主题: {}", topic);
                    return;
                }

                String source = getStringValue(data, "source");
                String dn = getStringValue(data, "deviceName");
                boolean isPCLocation = "pc_browser".equals(source) || "pc_location".equals(dn);

                if (isPCLocation) {
                    Double latitude = getDoubleValue(data, "latitude");
                    Double longitude = getDoubleValue(data, "longitude");
                    log.info("[MQTT位置] PC消息 deviceId={} ({}, {}) topic={}", deviceId, latitude, longitude, topic);
                }

                Map<String, Object> locationData = extractLocationData(data);
                vehicleLocationService.processLocationUpdate(deviceId, locationData);

                if (isPCLocation) {
                    log.info("[MQTT位置] PC位置已推送 VehicleLocationService");
                } else {
                    log.debug("[MQTT位置] 处理成功 deviceId={}", deviceId);
                }
            } catch (Exception e) {
                log.error("[MQTT位置] 处理失败 topic={}", topic, e);
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
            log.warn("设备连接丢失: {}（已开启 Paho automaticReconnect，将由客户端自动重连并在 connectComplete 中重新订阅）",
                    deviceName, cause);
            deviceConnected.put(deviceName, false);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // 不需要实现
        }
    }

    /**
     * 配置文件 {@code hardware-gps-device-ids} +（可选）库表 {@code gps_device_id} 合并后的硬件订阅目标 device_id 集合。
     *
     * @param mqttBridgeVehicle001DeviceId 密钥文件中 vehicle_001 的华为 device_id；与之一致的 ID 不得作为「独立硬件」再订阅
     */
    private Set<String> resolveHardwareGpsDeviceIdsForSubscription(String mqttBridgeVehicle001DeviceId) {
        Set<String> ids = new LinkedHashSet<>();
        for (String raw : hardwareGpsDeviceRegistry.getConfiguredDeviceIds()) {
            addNormalizedHardwareGpsId(ids, raw, "配置 hardware-gps-device-ids", mqttBridgeVehicle001DeviceId);
        }
        if (hardwareGpsSubscribeFromDatabase) {
            for (String id : vehicleRepository.findDistinctNonBlankGpsDeviceIds()) {
                addNormalizedHardwareGpsId(ids, id, "vehicle.gps_device_id", mqttBridgeVehicle001DeviceId);
            }
        }
        return ids;
    }

    /**
     * 常见笔误 vihecle → vehicle。
     * 若纠正后与 vehicle_001 中转账号 device_id 相同（例如库中误存 …_vihecle_001），则跳过订阅并打 ERROR，避免误以为已订阅到 ESP 的 vehicle_002。
     */
    private void addNormalizedHardwareGpsId(Set<String> target, String raw, String source, String mqttBridgeVehicle001DeviceId) {
        if (raw == null) {
            return;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return;
        }
        String toAdd = t.contains("vihecle") ? t.replace("vihecle", "vehicle") : t;
        if (!toAdd.equals(t)) {
            log.warn("检测到华为设备 ID 拼写 vihecle（应为 vehicle），来源: {}，已纠正为 {}；请同步修正数据库或配置。", source, toAdd);
        }
        if (mqttBridgeVehicle001DeviceId != null && mqttBridgeVehicle001DeviceId.equals(toAdd)) {
            log.error("已跳过将「硬件 GPS」订阅到 device_id={}：它与 MQTT 中转连接 vehicle_001 为同一设备，无法收到 ESP 在其它设备（如 …_vehicle_002）上的上报。"
                    + " 来源: {}。请在 vehicle 表把对应车辆的 gps_device_id 改为华为控制台里 GPS 模块的完整设备 ID（与固件 menuconfig 一致）。",
                    toAdd, source);
            return;
        }
        target.add(toAdd);
    }

    /** Topic 是否属于已登记的某台硬件 GPS 直连上报 */
    private boolean topicMatchesHardwareGpsUplink(String topic) {
        if (topic == null || topic.isEmpty()) {
            return false;
        }
        for (String id : hardwareGpsSubscriptionIds) {
            if (id != null && topic.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从主题或数据中提取设备ID
     */
    private String extractDeviceId(String topic, Map<String, Object> data) {
        // 从主题中提取（兼容有/无前导斜杠、$oc 前缀）
        if (topic != null && topic.contains("/")) {
            String[] parts = topic.split("/");
            java.util.List<String> segs = new java.util.ArrayList<>();
            for (String p : parts) {
                if (p != null && !p.isEmpty()) {
                    segs.add(p);
                }
            }
            if (segs.size() >= 3 && "$oc".equals(segs.get(0))) {
                return segs.get(2);
            }
            if (segs.size() >= 2) {
                // /{instanceId}/{deviceId}/user/... 或 {instanceId}/{deviceId}/user/...
                return segs.get(1);
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
