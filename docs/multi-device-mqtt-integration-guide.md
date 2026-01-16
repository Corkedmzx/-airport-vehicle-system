# 多设备MQTT数据流转集成指南

## 概述

本系统实现了多设备MQTT数据流转功能，支持从手机定位到车辆定位器，再到网页监控端，最终显示在系统地图监控部分。

## 数据流转路径

```
手机定位 (mobile_001) 
    ↓ (MQTT发布位置数据)
车辆定位器 (vehicle_001) 
    ↓ (MQTT转发位置数据)
网页监控端 (web_001) 
    ↓ (MQTT接收位置数据)
后端系统 
    ↓ (WebSocket推送)
前端地图监控
```

## 架构设计

### 设备角色

1. **mobile_001** (手机定位)
   - 角色：数据源
   - 功能：获取本机位置信息，通过MQTT发布到华为云IoT平台
   - 主题：`/{instanceId}/your_device_id_mobile_001/user/location`

2. **vehicle_001** (车辆定位器)
   - 角色：数据中转
   - 功能：订阅 mobile_001 的位置数据，转发到 web_001
   - 订阅主题：`/{instanceId}/your_device_id_mobile_001/user/location`
   - 发布主题：`/{instanceId}/your_device_id_web_001/user/location`

3. **web_001** (网页监控端)
   - 角色：数据接收
   - 功能：订阅 vehicle_001 转发的数据，推送到后端系统
   - 订阅主题：`/{instanceId}/your_device_id_web_001/user/location`

### 后端实现

系统使用 `MultiDeviceMqttServiceImpl` 实现多设备MQTT连接：

- 同时连接 vehicle_001 和 web_001
- vehicle_001 订阅 mobile_001 的位置主题
- vehicle_001 接收消息后，转发到 web_001 的主题
- web_001 订阅自己的主题，接收 vehicle_001 转发的数据
- 后端通过 web_001 接收位置数据，通过 WebSocket 推送到前端

## 配置说明

### 环境变量配置

在 `backend/.env` 文件中配置以下环境变量：

```bash
# 启用MQTT客户端
HUAWEI_IOT_MQTT_ENABLED=true

# MQTT Broker地址
HUAWEI_IOT_MQTT_BROKER=ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883

# 设备密钥文件目录
HUAWEI_IOT_MQTT_DEVICE_KEY_PATH=/path/to/device-keys

# 华为云IoT实例ID
HUAWEI_IOT_MQTT_INSTANCE_ID=your-instance-id-here
```

### 设备密钥文件

系统会自动从 `device-key-path` 目录读取设备密钥文件：

- `DEVICES-KEY-your_device_id_vehicle_001.txt`
- `DEVICES-KEY-your_device_id_web_001.txt`

文件格式（JSON）：
```json
{
    "device_id": "your_device_id_vehicle_001",
    "secret": "your_device_secret_here"
}
```

## 前端地图显示

### 百度地图集成

前端使用百度地图API显示实际地图：

1. 在 `frontend/index.html` 中引入百度地图API：
```html
<script type="text/javascript" src="https://api.map.baidu.com/api?v=3.0&ak=YOUR_BAIDU_MAP_AK"></script>
```

2. 申请百度地图AK：
   - 访问：https://lbsyun.baidu.com/apiconsole/key
   - 创建应用，获取AK
   - 替换 `index.html` 中的 `YOUR_BAIDU_MAP_AK`

3. 地图功能：
   - 实时显示车辆位置标记
   - 根据车辆状态显示不同颜色的标记
   - 点击标记查看车辆详细信息
   - 自动调整地图视野以包含所有车辆

### 车辆标记颜色

- 绿色：正常运行
- 橙色：维修中
- 红色：故障
- 灰色：停用

## 数据格式

### MQTT消息格式

位置数据JSON格式：
```json
{
    "longitude": 116.5842,
    "latitude": 40.0801,
    "address": "北京市首都机场",
    "speed": 60,
    "direction": 90,
    "timestamp": "2026-01-15T20:00:00Z"
}
```

### WebSocket消息格式

前端接收的位置更新消息：
```json
{
    "type": "vehicle_location",
    "data": {
        "vehicleId": "1",
        "vehicleNo": "京A12345",
        "longitude": 116.5842,
        "latitude": 40.0801,
        "address": "北京市首都机场",
        "speed": 60
    }
}
```

## 使用流程

### 1. 配置设备密钥

确保设备密钥文件已放置在指定目录：
```
/path/to/device-keys/
├── DEVICES-KEY-your_device_id_vehicle_001.txt
└── DEVICES-KEY-your_device_id_web_001.txt
```

### 2. 配置环境变量

在 `backend/.env` 文件中配置MQTT相关环境变量。

### 3. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

系统会自动：
- 连接 vehicle_001 和 web_001
- 订阅相应的主题
- 开始接收位置数据

### 4. 配置前端地图

1. 申请百度地图AK
2. 在 `frontend/index.html` 中替换AK
3. 启动前端服务：
```bash
cd frontend
npm run dev
```

### 5. 测试数据流转

1. 使用手机（mobile_001）发布位置数据到华为云IoT平台
2. 查看后端日志，确认 vehicle_001 接收到数据并转发
3. 查看后端日志，确认 web_001 接收到数据
4. 查看前端地图，确认车辆位置已更新

## 故障排查

### 设备连接失败

1. 检查设备密钥文件是否存在且格式正确
2. 检查环境变量配置是否正确
3. 查看后端日志，确认设备连接状态

### 订阅主题失败

1. 检查主题格式是否正确（不使用 `$oc` 前缀）
2. 检查设备是否有订阅该主题的权限
3. 查看华为云IoT平台的设备权限配置

### 地图不显示

1. 检查百度地图AK是否正确配置
2. 检查网络连接，确认可以访问百度地图API
3. 查看浏览器控制台，确认是否有错误信息

### 位置数据不更新

1. 检查WebSocket连接是否正常
2. 检查后端是否接收到MQTT消息
3. 查看后端日志，确认数据流转是否正常

## 注意事项

1. **设备密钥安全**：设备密钥文件包含敏感信息，不要提交到Git仓库
2. **百度地图AK**：需要申请百度地图AK才能使用地图功能
3. **主题格式**：华为云IoT主题格式不使用 `$oc` 前缀
4. **动态密码**：华为云IoT使用动态密码机制，系统会自动生成

## 相关文档

- [MQTT + 华为云IoT 集成指南](./mqtt-huawei-iot-integration-guide.md)
- [设备激活指南](./device-activation-guide.md)
- [MQTTX 使用指南](./mqttx-usage-guide.md)
- [故障排除指南](./troubleshooting-mqtt-connection.md)
- [环境变量配置指南](./environment-configuration.md)