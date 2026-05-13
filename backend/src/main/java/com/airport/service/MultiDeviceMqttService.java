package com.airport.service;

/**
 * 多设备 MQTT 服务：华为 IoT 多设备连接与发布。
 * <p>
 * 小程序链路：mobile_001 → vehicle_001（仅中转）→ web_001 → 系统。
 * 硬件 GPS：独立连接（如 vehicle_002）与 vehicle_001 并存；上行通常由 vehicle_001 跨设备只读订阅消费（华为 IoTDA 对本机上行订阅常无投递）。PC 位置由 web_001 直连发布。
 */
public interface MultiDeviceMqttService {

    /**
     * 初始化所有设备连接
     */
    void initializeAllDevices();

    /**
     * 连接指定设备
     *
     * @param deviceName 设备名称，例如：mobile_001, vehicle_001, web_001, vehicle_002
     */
    void connectDevice(String deviceName);

    /**
     * 断开指定设备连接
     *
     * @param deviceName 设备名称
     */
    void disconnectDevice(String deviceName);

    /**
     * 获取设备连接状态
     *
     * @param deviceName 设备名称
     * @return 是否已连接
     */
    boolean isDeviceConnected(String deviceName);

    /**
     * 发布消息到指定设备的主题
     *
     * @param deviceName 设备名称
     * @param topic      主题
     * @param payload    消息内容
     * @param qos        QoS 等级
     */
    void publishToDevice(String deviceName, String topic, String payload, int qos);

    /**
     * 从已加载的设备密钥中读取华为 device_id（如 web_001），未加载时返回 null。
     */
    String getLoadedDeviceId(String deviceName);
}
