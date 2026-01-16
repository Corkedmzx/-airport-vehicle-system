package com.airport.service;

/**
 * MQTT客户端服务接口
 * 用于连接华为云IoT平台，接收车辆定位数据
 * 
 * @author Corkedmzx
 */
public interface MqttClientService {
    
    /**
     * 连接MQTT Broker
     */
    void connect();
    
    /**
     * 断开MQTT连接
     */
    void disconnect();
    
    /**
     * 订阅主题
     * 
     * @param topic 主题名称
     * @param qos 服务质量等级 (0, 1, 2)
     */
    void subscribe(String topic, int qos);
    
    /**
     * 取消订阅主题
     * 
     * @param topic 主题名称
     */
    void unsubscribe(String topic);
    
    /**
     * 检查连接状态
     * 
     * @return 是否已连接
     */
    boolean isConnected();
}
