# MQTT手机定位上传方案

## 概述

本方案说明如何通过MQTT协议将手机定位数据上传至系统，并在地图监控部分实时显示。

## 架构设计

### 数据流程

```
手机APP (GPS定位)
    ↓ MQTT发布
MQTT Broker (如：Mosquitto, EMQX, HiveMQ)
    ↓ MQTT订阅
后端服务 (Spring Boot)
    ↓ 处理位置数据
数据库 (vehicle_location表)
    ↓ WebSocket推送
前端地图监控页面 (实时显示)
```

## 方案一：使用本地MQTT Broker（推荐）

### 1. 安装MQTT Broker

#### Windows - 使用Mosquitto

1. 下载安装：https://mosquitto.org/download/
2. 配置文件 `mosquitto.conf`：
```conf
# 监听端口
listener 1883
allow_anonymous true

# 日志
log_dest file C:/mosquitto/log/mosquitto.log
log_type all
```

3. 启动服务：
```bash
mosquitto -c mosquitto.conf
```

#### Linux - 使用Mosquitto

```bash
# Ubuntu/Debian
sudo apt-get install mosquitto mosquitto-clients

# 启动服务
sudo systemctl start mosquitto
sudo systemctl enable mosquitto
```

### 2. 后端集成MQTT客户端

#### 2.1 添加依赖

在 `backend/pom.xml` 中添加：

```xml
<!-- Eclipse Paho MQTT客户端 -->
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>
```

#### 2.2 创建MQTT配置类

创建 `backend/src/main/java/com/airport/config/MqttConfig.java`：

```java
package com.airport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfig {
    private String brokerUrl = "tcp://localhost:1883";
    private String clientId = "airport-vehicle-system";
    private String username;
    private String password;
    private String locationTopic = "vehicle/location/+"; // 订阅所有车辆位置主题
    private int qos = 1; // 服务质量等级
}
```

#### 2.3 创建MQTT服务

创建 `backend/src/main/java/com/airport/service/MqttLocationService.java`：

```java
package com.airport.service;

import com.airport.config.MqttConfig;
import com.airport.dto.VehicleLocationDTO;
import com.airport.entity.Vehicle;
import com.airport.repository.VehicleRepository;
import com.airport.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class MqttLocationService implements MqttCallback {

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private ObjectMapper objectMapper;

    private MqttClient mqttClient;

    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(
                mqttConfig.getBrokerUrl(),
                mqttConfig.getClientId(),
                new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);
            
            if (mqttConfig.getUsername() != null && !mqttConfig.getUsername().isEmpty()) {
                options.setUserName(mqttConfig.getUsername());
                options.setPassword(mqttConfig.getPassword().toCharArray());
            }

            mqttClient.setCallback(this);
            mqttClient.connect(options);

            // 订阅位置主题
            mqttClient.subscribe(mqttConfig.getLocationTopic(), mqttConfig.getQos());
            log.info("MQTT客户端已连接，订阅主题: {}", mqttConfig.getLocationTopic());

        } catch (Exception e) {
            log.error("MQTT客户端初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (Exception e) {
            log.error("MQTT客户端关闭失败", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT连接丢失，尝试重连...", cause);
        // 实现重连逻辑
        reconnect();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());
            log.debug("收到MQTT消息，主题: {}, 内容: {}", topic, payload);

            // 解析主题获取车辆ID
            // 主题格式: vehicle/location/{vehicleId}
            String[] topicParts = topic.split("/");
            if (topicParts.length < 3) {
                log.warn("无效的主题格式: {}", topic);
                return;
            }
            String vehicleIdStr = topicParts[2];
            Long vehicleId = Long.parseLong(vehicleIdStr);

            // 解析位置数据
            LocationData locationData = objectMapper.readValue(payload, LocationData.class);

            // 创建位置DTO
            VehicleLocationDTO locationDTO = new VehicleLocationDTO();
            locationDTO.setVehicleId(vehicleId);
            locationDTO.setLongitude(new BigDecimal(locationData.getLongitude()));
            locationDTO.setLatitude(new BigDecimal(locationData.getLatitude()));
            if (locationData.getAltitude() != null) {
                locationDTO.setAltitude(new BigDecimal(locationData.getAltitude()));
            }
            if (locationData.getSpeed() != null) {
                locationDTO.setSpeed(new BigDecimal(locationData.getSpeed()));
            }
            if (locationData.getDirection() != null) {
                locationDTO.setDirection(new BigDecimal(locationData.getDirection()));
            }
            if (locationData.getAccuracy() != null) {
                locationDTO.setAccuracy(new BigDecimal(locationData.getAccuracy()));
            }
            locationDTO.setLocationTime(LocalDateTime.now());

            // 更新车辆位置
            vehicleService.updateVehicleLocation(vehicleId, locationDTO);
            log.info("车辆位置已更新，车辆ID: {}, 经度: {}, 纬度: {}", 
                    vehicleId, locationData.getLongitude(), locationData.getLatitude());

        } catch (Exception e) {
            log.error("处理MQTT位置消息失败", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 消息发送完成回调（发布消息时使用）
    }

    private void reconnect() {
        // 实现重连逻辑
        int maxRetries = 5;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                Thread.sleep(5000 * (retryCount + 1)); // 递增延迟
                mqttClient.reconnect();
                mqttClient.subscribe(mqttConfig.getLocationTopic(), mqttConfig.getQos());
                log.info("MQTT重连成功");
                return;
            } catch (Exception e) {
                retryCount++;
                log.warn("MQTT重连失败，重试次数: {}/{}", retryCount, maxRetries);
            }
        }
        log.error("MQTT重连失败，已达到最大重试次数");
    }

    // 位置数据内部类
    private static class LocationData {
        private Double longitude;
        private Double latitude;
        private Double altitude;
        private Double speed;
        private Double direction;
        private Double accuracy;
        private String timestamp;

        // Getters and Setters
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getAltitude() { return altitude; }
        public void setAltitude(Double altitude) { this.altitude = altitude; }
        public Double getSpeed() { return speed; }
        public void setSpeed(Double speed) { this.speed = speed; }
        public Double getDirection() { return direction; }
        public void setDirection(Double direction) { this.direction = direction; }
        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
```

#### 2.4 配置application.yml

在 `application.yml` 中添加：

```yaml
# MQTT配置
mqtt:
  broker-url: ${MQTT_BROKER_URL:tcp://localhost:1883}  # MQTT Broker地址
  client-id: ${MQTT_CLIENT_ID:airport-vehicle-system}  # 客户端ID
  username: ${MQTT_USERNAME:}  # 用户名（可选）
  password: ${MQTT_PASSWORD:}  # 密码（可选）
  location-topic: vehicle/location/+  # 位置主题（+为通配符）
  qos: 1  # 服务质量等级（0, 1, 2）
```

### 3. 手机APP端实现

#### 3.1 Android示例（使用Eclipse Paho）

```kotlin
// 添加依赖 build.gradle
// implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
// implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'

class MqttLocationPublisher {
    private var mqttClient: MqttAndroidClient? = null
    
    fun connect(brokerUrl: String, clientId: String) {
        mqttClient = MqttAndroidClient(context, brokerUrl, clientId)
        
        val options = MqttConnectOptions()
        options.isCleanSession = true
        
        mqttClient?.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "连接成功")
            }
            
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "连接失败", exception)
            }
        })
    }
    
    fun publishLocation(vehicleId: Long, location: Location) {
        val topic = "vehicle/location/$vehicleId"
        val payload = JSONObject().apply {
            put("longitude", location.longitude)
            put("latitude", location.latitude)
            put("altitude", location.altitude)
            put("speed", location.speed * 3.6) // m/s转km/h
            put("direction", location.bearing)
            put("accuracy", location.accuracy)
            put("timestamp", System.currentTimeMillis())
        }.toString()
        
        val message = MqttMessage(payload.toByteArray())
        message.qos = 1
        
        mqttClient?.publish(topic, message, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "位置发布成功")
            }
            
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "位置发布失败", exception)
            }
        })
    }
}
```

#### 3.2 iOS示例（使用CocoaMQTT）

```swift
// 添加依赖 Podfile
// pod 'CocoaMQTT'

import CocoaMQTT

class MqttLocationPublisher {
    var mqtt: CocoaMQTT?
    
    func connect(brokerHost: String, port: UInt16, clientId: String) {
        mqtt = CocoaMQTT(clientID: clientId, host: brokerHost, port: port)
        mqtt?.connect()
    }
    
    func publishLocation(vehicleId: Int64, location: CLLocation) {
        let topic = "vehicle/location/\(vehicleId)"
        let payload: [String: Any] = [
            "longitude": location.coordinate.longitude,
            "latitude": location.coordinate.latitude,
            "altitude": location.altitude,
            "speed": location.speed * 3.6, // m/s转km/h
            "direction": location.course,
            "accuracy": location.horizontalAccuracy,
            "timestamp": Int64(Date().timeIntervalSince1970 * 1000)
        ]
        
        let jsonData = try? JSONSerialization.data(withJSONObject: payload)
        let jsonString = String(data: jsonData!, encoding: .utf8)!
        
        mqtt?.publish(topic, withString: jsonString, qos: .qos1)
    }
}
```

## 方案二：使用云MQTT服务

### 推荐服务

1. **EMQX Cloud**：https://www.emqx.com/zh/cloud
2. **HiveMQ Cloud**：https://www.hivemq.com/mqtt-cloud-broker/
3. **AWS IoT Core**：https://aws.amazon.com/iot-core/
4. **阿里云IoT平台**：https://iot.console.aliyun.com/

### 配置示例（EMQX Cloud）

```yaml
mqtt:
  broker-url: tcp://your-instance.emqx.cloud:1883
  username: your-username
  password: your-password
  location-topic: vehicle/location/+
  qos: 1
```

## 数据格式

### MQTT消息格式

**主题**：`vehicle/location/{vehicleId}`

**消息体（JSON）**：
```json
{
  "longitude": 116.5842,
  "latitude": 40.0801,
  "altitude": 35.5,
  "speed": 45.0,
  "direction": 90.0,
  "accuracy": 10.0,
  "timestamp": 1704067200000
}
```

### 字段说明

- `longitude`: 经度（必填）
- `latitude`: 纬度（必填）
- `altitude`: 海拔（米，可选）
- `speed`: 速度（km/h，可选）
- `direction`: 方向角（度，0-360，可选）
- `accuracy`: 精度（米，可选）
- `timestamp`: 时间戳（毫秒，可选，默认使用服务器时间）

## 前端地图监控

系统已实现地图监控功能，位置数据更新后会自动通过WebSocket推送到前端：

1. **后端处理**：MQTT消息 → 数据库更新 → WebSocket推送
2. **前端接收**：WebSocket消息 → 地图标记更新 → 实时显示

前端无需修改，系统会自动在地图上显示车辆位置。

## 测试

### 1. 使用MQTT客户端测试

```bash
# 安装mosquitto客户端
# Windows: 下载 https://mosquitto.org/download/
# Linux: sudo apt-get install mosquitto-clients

# 发布测试消息
mosquitto_pub -h localhost -p 1883 -t "vehicle/location/1" -m '{
  "longitude": 116.5842,
  "latitude": 40.0801,
  "speed": 45.0,
  "direction": 90.0
}'
```

### 2. 验证数据

1. 检查数据库 `vehicle_location` 表是否有新记录
2. 检查前端地图是否显示车辆位置
3. 查看后端日志确认MQTT消息接收

## 安全建议

1. **启用MQTT认证**：配置用户名和密码
2. **使用TLS/SSL**：生产环境使用 `ssl://` 协议
3. **主题权限控制**：限制客户端只能发布/订阅特定主题
4. **数据加密**：敏感数据在传输前加密

## 常见问题

### Q: 手机APP如何获取车辆ID？

A: 车辆ID可以通过以下方式获取：
- 用户登录后，后端返回关联的车辆ID
- 通过车牌号查询车辆ID
- 在APP中配置车辆ID

### Q: 如何处理网络断开重连？

A: MQTT客户端会自动重连，后端服务会缓存未发送的消息。

### Q: 位置更新频率如何控制？

A: 在手机APP中控制GPS采样频率，建议：
- 静止状态：30秒更新一次
- 移动状态：5-10秒更新一次

## 总结

通过MQTT协议可以实现手机定位数据的实时上传，系统会自动处理并在地图监控页面显示。该方案具有以下优势：

1. **实时性**：MQTT低延迟，适合实时位置更新
2. **可靠性**：QoS保证消息送达
3. **扩展性**：支持多设备并发
4. **简单性**：实现简单，易于维护
