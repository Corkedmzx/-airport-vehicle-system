package com.airport.service;

/**
 * 多设备MQTT服务接口
 * 支持同时连接多个华为云IoT设备，实现数据流转
 * 
 * 数据流转路径：
 * mobile_001 (手机定位) -> vehicle_001 (车辆定位器) -> web_001 (网页监控端) -> 系统
 * 
 * @author Corkedmzx
 */
public interface MultiDeviceMqttService {
    
    /**
     * 初始化所有设备连接
     */
    void initializeAllDevices();
    
    /**
     * 连接指定设备
     * 
     * @param deviceName 设备名称，例如：mobile_001, vehicle_001, web_001
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
     * @param topic 主题
     * @param payload 消息内容
     * @param qos QoS等级
     */
    void publishToDevice(String deviceName, String topic, String payload, int qos);
}
