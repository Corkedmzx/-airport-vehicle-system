# MQTT + 华为云IoT 集成完整教程

## 目录

1. [概述](#概述)
2. [准备工作](#准备工作)
3. [华为云IoT平台配置](#华为云iot平台配置)
4. [后端配置](#后端配置)
5. [设备激活指南](#设备激活指南) - 重要：如何激活设备
6. [手机定位器配置](#手机定位器配置)
7. [前端地图显示](#前端地图显示)
8. [测试验证](#测试验证)
9. [常见问题](#常见问题)

---

## 概述

本教程将指导您完成以下功能：
- 使用华为云IoT平台作为MQTT Broker
- 通过MQTT接收手机定位器发送的位置数据
- 在系统地图监控页面实时显示车辆位置

### 数据流程

```
手机定位器 → MQTT (华为云IoT) → 后端MQTT客户端 → 车辆位置服务 → WebSocket → 前端地图页面
```

### 备选：规则引擎「HTTP 推送」到本服务

当华为侧**不向应用 MQTT 订阅投递**设备上行时，可在控制台配置**数据转发**到本服务：

- **URL**：`POST https://<你的域名>/api/mqtt/iot-forward-location`（本地调试可用 ngrok 等隧道，**勿将临时域名写入仓库**）
- **鉴权**：与控制台「转发目标」中 Token 一致的环境变量 **`HUAWEI_IOT_FORWARD_WEBHOOK_SECRET`**（见 `backend/.env.example`）。华为勾选鉴权后，请求头为 **`timestamp`、`nonce`、`signature`**（SHA256，规则见华为文档「HTTP/HTTPS推送基于Token认证」）；本服务亦兼容调试用请求头 **`X-IoT-Webhook-Token`** 与密钥明文相同。
- **数据库**：`vehicle.gps_device_id` 必须与上报的华为 **device_id** 完全一致（含产品 ID 前缀）。
- **与 MQTT 关系**：两条链路可并存；地图与入库逻辑一致。

```
硬件/手机 → 华为 IoT → HTTP 推送 → 后端 /mqtt/iot-forward-location → 车辆位置服务 → WebSocket → 前端地图
```

---

## 准备工作

### 1. 所需工具和账号

- 华为云账号（已开通IoTDA服务）
- MQTTX客户端（用于测试）
- 手机（Android/iOS，用于定位）
- 后端服务已启动
- 前端服务已启动

### 2. 检查依赖

确认 `backend/pom.xml` 中已包含MQTT客户端依赖：

```xml
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>
```

---

## 华为云IoT平台配置

### 当前实例信息

根据您提供的设备信息，当前配置如下：

- **实例名称**：`YourProjectName`
- **实例ID**：`your-instance-id-here`
- **MQTT接入地址**：`ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883`

### 已注册设备信息

| 设备名称 | 设备ID | 设备密钥 | 用途 |
|---------|--------|---------|------|
| 车辆定位器01 | `your_device_id_vehicle_001` | `your_device_secret_here` | 车辆位置上报 |
| 手机监控端 | `your_device_id_mobile_001` | `your_device_secret_here` | 手机定位 |
| 网页监控端 | `your_device_id_web_001` | `your_device_secret_here` | 网页监控 |

### MQTT连接信息

**车辆定位器01的MQTT连接参数**：

- **MQTT Broker地址**：`ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883`
- **客户端ID**：`your_device_id_vehicle_001`
- **用户名**：`your_device_id_vehicle_001`
- **密码**：`your_device_secret_here`
- **上报主题**：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/location`
- **下发主题**：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/command`

**说明**：
- `$oc` 是固定前缀
- `your-instance-id-here` 是实例ID（在主题中作为project_id使用）
- `your_device_id_vehicle_001` 是设备ID
- `/user/` 是用户自定义主题前缀

---

## 后端配置

### 步骤1：配置环境变量

在 `backend/src/main/resources/application.yml` 中已添加MQTT配置，现在需要设置环境变量。

**重要**：环境变量只在**当前PowerShell会话**中有效。如果关闭窗口，需要重新设置。启动后端服务时，必须在**同一个PowerShell窗口**中执行。

**推荐**：使用 `.env` 文件自动加载环境变量，详见 [环境变量配置指南](./environment-configuration.md)。

**Windows PowerShell**（直接复制执行）：

```powershell
# 启用MQTT客户端
$env:HUAWEI_IOT_MQTT_ENABLED="true"

# MQTT Broker地址（使用实际的接入地址）
$env:HUAWEI_IOT_MQTT_BROKER="ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883"

# MQTT客户端ID（车辆定位器设备ID）
$env:HUAWEI_IOT_MQTT_CLIENT_ID="your_device_id_vehicle_001"

# MQTT用户名（设备ID）
$env:HUAWEI_IOT_MQTT_USERNAME="your_device_id_vehicle_001"

# MQTT密码（设备密钥）
$env:HUAWEI_IOT_MQTT_PASSWORD="your_device_secret_here"

# 订阅主题（实例ID + 设备ID）
$env:HUAWEI_IOT_MQTT_TOPIC="$oc/your-instance-id-here/your_device_id_vehicle_001/user/location"

# QoS等级（0, 1, 2），推荐使用1
$env:HUAWEI_IOT_MQTT_QOS="1"
```

#### Linux/macOS

```bash
export HUAWEI_IOT_MQTT_ENABLED="true"
export HUAWEI_IOT_MQTT_BROKER="ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883"
export HUAWEI_IOT_MQTT_CLIENT_ID="your_device_id_vehicle_001"
export HUAWEI_IOT_MQTT_USERNAME="your_device_id_vehicle_001"
export HUAWEI_IOT_MQTT_PASSWORD="your_device_secret_here"
export HUAWEI_IOT_MQTT_TOPIC="$oc/your-instance-id-here/your_device_id_vehicle_001/user/location"
export HUAWEI_IOT_MQTT_QOS="1"
```

### 步骤2：配置车辆GPS设备ID

在数据库中，将车辆的 `gps_device_id` 字段设置为华为云IoT的设备ID：

```sql
-- 更新车辆的GPS设备ID（使用车辆定位器01的设备ID）
UPDATE vehicle 
SET gps_device_id = 'your_device_id_vehicle_001' 
WHERE vehicle_no = '京A12345';

-- 或者更新所有车辆（根据实际情况选择）
-- UPDATE vehicle SET gps_device_id = 'your_device_id_vehicle_001' WHERE id = 1;
```

**重要**：`gps_device_id` 必须与华为云IoT的设备ID一致，系统才能正确关联车辆。

**示例**：如果车辆"京A12345"要使用车辆定位器01，则设置：
```sql
UPDATE vehicle 
SET gps_device_id = 'your_device_id_vehicle_001' 
WHERE vehicle_no = '京A12345';
```

### 步骤3：重启后端服务

配置完成后，重启后端服务：

```bash
cd backend
mvn spring-boot:run
```

查看日志，确认MQTT连接成功：

```
INFO  c.a.s.i.MqttClientServiceImpl - MQTT客户端初始化成功，Broker: ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883
INFO  c.a.s.i.MqttClientServiceImpl - MQTT连接成功，Broker: ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883
INFO  c.a.s.i.MqttClientServiceImpl - MQTT订阅主题成功: $oc/your-instance-id-here/your_device_id_vehicle_001/user/location, QoS: 1
```

**重要说明**：
- 后端MQTT客户端连接成功**不会激活设备**
- 后端只是**订阅**主题，用于**接收**设备上报的数据
- 要激活设备，需要使用**MQTTX**或**手机应用**，以**设备的Client ID**连接并发送消息
- 详细步骤请查看 [设备激活完整指南](device-activation-guide.md)

---

## 设备激活指南

**重要**：设备在华为云IoT平台中显示为"未激活"状态，需要设备**首次连接MQTT Broker**才能激活。

### 设备激活原理

华为云IoT设备激活的条件：
1. 设备已注册（已在控制台创建）
2. 使用**设备的Client ID**连接MQTT Broker
3. 连接成功并发送至少一条消息

**重要区别**：
- **后端MQTT客户端**：作为**订阅者**连接，**不会激活设备**
- **设备连接**（MQTTX或手机应用）：使用**设备ID作为Client ID**连接，**会激活设备**

### 快速激活步骤

#### 方法1：使用MQTTX激活（推荐，最简单）

1. **打开MQTTX**，使用已配置的"车辆定位器01"连接
2. **点击连接**，等待连接成功（状态显示为Connected）
3. **发送一条消息**：
   - **Topic**：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/location`
   - **Payload**（JSON格式）：
     ```json
     {
       "longitude": 116.5842,
       "latitude": 40.0801,
       "address": "首都机场T3航站楼",
       "speed": 0,
       "direction": 0
     }
     ```
4. **验证激活**：
   - 刷新华为云IoT控制台的设备列表
   - "车辆定位器1"的状态应该从"未激活"变为"激活"

#### 方法2：后端自动连接（需要修改代码）

如果希望后端连接时自动激活设备，需要修改`MqttClientServiceImpl`，使用设备的Client ID连接。

**注意**：当前实现中，后端MQTT客户端主要用于**接收**消息，不会激活设备。设备激活需要设备本身连接。

### 详细说明

> 📖 **完整指南**：查看 [设备激活完整指南](device-activation-guide.md) 获取详细的激活步骤和故障排除方法。

---

## 手机定位器配置

### 方案1：使用MQTTX测试（推荐用于测试）

MQTTX是一个MQTT客户端工具，可以模拟手机定位器发送数据。

**详细使用指南**：查看 [MQTTX使用详细指南](mqttx-usage-guide.md) 获取完整的使用说明和示例。

#### 步骤1：下载并安装MQTTX

1. 访问官网：https://mqttx.app/
2. 下载对应操作系统的版本：
   - **Windows**：下载 `.exe` 安装包
   - **Mac**：下载 `.dmg` 安装包
   - **Linux**：下载 `.AppImage` 或使用包管理器安装
3. 安装完成后启动MQTTX

#### 步骤2：创建MQTT连接

1. **打开MQTTX**，点击左侧的 **+ New Connection** 按钮（或点击顶部菜单 **Connections** → **New Connection**）

2. **填写连接信息**：

   **基本信息**：
   - **Name**：`车辆定位器01`（连接名称，可自定义）
   - **Client ID**：`your_device_id_vehicle_001`（设备ID）

   **服务器信息**：
   - **Host**：`your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com`
   - **Port**：`8883`
   - **Protocol**：选择 `mqtts://`（MQTT over SSL/TLS）

   **用户认证**：
   - **Username**：`your_device_id_vehicle_001`（设备ID）
   - **Password**：`your_device_secret_here`（设备密钥）

   **SSL/TLS设置**：
   - 点击 **SSL/TLS** 标签页
   - 启用 **SSL/TLS** 开关
   - **Certificate**：选择 **CA signed server**（CA签名服务器证书）
   - 其他选项保持默认

3. **保存连接**：
   - 点击右下角的 **Connect** 按钮
   - 连接成功后，连接状态会显示为 **Connected**（绿色）

#### 步骤3：订阅主题（可选，用于接收平台下发消息）

1. 连接成功后，在右侧消息区域，点击 **New Subscription**
2. 填写订阅信息：
   - **Topic**：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/command`
   - **QoS**：选择 `1`
3. 点击 **Confirm** 完成订阅

#### 步骤4：发送位置数据

1. **创建消息**：
   - 在MQTTX主界面，点击底部的 **New Message** 按钮
   - 或点击消息输入框

2. **填写消息内容**：

   **主题（Topic）**：
   ```
   $oc/your-instance-id-here/your_device_id_vehicle_001/user/location
   ```
   - 格式：`$oc/{实例ID}/{设备ID}/user/location`
   - 实例ID：`your-instance-id-here`
   - 设备ID：`your_device_id_vehicle_001`

   **消息内容（Payload）**：
   - 选择格式：**JSON**
   - 输入以下内容：

   ```json
   {
     "longitude": 116.5842,
     "latitude": 40.0801,
     "address": "首都机场T3航站楼",
     "speed": 45.5,
     "direction": 90.0
   }
   ```

   **QoS设置**：
   - 选择 **QoS 1**（推荐，确保消息至少送达一次）

3. **发送消息**：
   - 点击右下角的 **Send** 按钮
   - 消息发送成功后，会在消息列表中显示

4. **验证发送结果**：
   - 查看后端日志，确认收到消息：
     ```
     INFO - 收到MQTT消息，主题: $oc/.../user/location
     INFO - 处理位置更新成功，车辆: 京A12345, 设备ID: ...
     ```
   - 检查数据库，确认车辆位置已更新：
     ```sql
     SELECT vehicle_no, location_longitude, location_latitude, last_update_time 
     FROM vehicle 
     WHERE gps_device_id = 'your_device_id_vehicle_001';
     ```

#### 步骤5：定时发送位置数据（模拟实时定位）

1. **设置定时发送**：
   - 在消息输入框下方，找到 **Timing** 选项
   - 启用 **Timing** 开关
   - 设置发送间隔：例如 `30` 秒
   - 设置发送次数：例如 `100` 次（或选择 **Unlimited** 无限发送）

2. **修改位置数据**：
   - 每次发送前，可以修改 `longitude` 和 `latitude` 值，模拟车辆移动
   - 例如：
     ```json
     {
       "longitude": 116.5842,
       "latitude": 40.0801,
       "address": "首都机场T3航站楼",
       "speed": 45.5,
       "direction": 90.0
     }
     ```
     下次发送改为：
     ```json
     {
       "longitude": 116.5850,
       "latitude": 40.0805,
       "address": "首都机场T3航站楼附近",
       "speed": 50.0,
       "direction": 95.0
     }
     ```

#### MQTTX使用技巧

1. **保存连接配置**：
   - 连接配置会自动保存
   - 下次使用时，直接点击连接名称即可连接

2. **查看消息历史**：
   - 在消息列表中，可以查看所有发送和接收的消息
   - 点击消息可以查看详细信息

3. **批量测试**：
   - 可以创建多个连接，模拟多个设备
   - 使用不同的设备ID和密钥

4. **调试模式**：
   - 在连接设置中，可以启用 **Debug** 模式
   - 查看详细的MQTT协议交互日志

#### 常见问题

**Q: 连接失败，提示 "Connection refused"**
- 检查Host和Port是否正确
- 确认SSL/TLS已启用
- 检查网络连接

**Q: 连接失败，提示 "Not authorized"**
- 检查Username和Password是否正确
- 确认设备ID和密钥匹配

**Q: 消息发送成功但后端未收到**
- 检查Topic是否正确（注意实例ID和设备ID）
- 检查后端MQTT客户端是否已连接
- 查看后端日志确认订阅状态

**Q: 如何测试多个车辆？**
- 在数据库中为不同车辆设置不同的 `gps_device_id`
- 在MQTTX中创建多个连接，使用不同的设备ID
- 或者修改消息中的设备ID（需要后端支持通配符主题）

### 方案2：Android手机应用（实际部署）

#### 使用Eclipse Paho Android客户端

1. **添加依赖**（在 `app/build.gradle`）：

```gradle
dependencies {
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
}
```

2. **获取位置权限**（在 `AndroidManifest.xml`）：

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

3. **发送位置数据代码示例**：

```java
// MQTT连接配置（使用车辆定位器01的实际信息）
String broker = "ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883";
String clientId = "your_device_id_vehicle_001";
String username = "your_device_id_vehicle_001";
String password = "your_device_secret_here";
String topic = "$oc/your-instance-id-here/your_device_id_vehicle_001/user/location";

// 创建MQTT客户端
MqttConnectOptions options = new MqttConnectOptions();
options.setUserName(username);
options.setPassword(password.toCharArray());
options.setCleanSession(true);

MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
client.connect(options);

// 获取GPS位置
LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

// 构建JSON消息
JSONObject message = new JSONObject();
message.put("longitude", location.getLongitude());
message.put("latitude", location.getLatitude());
message.put("address", "当前位置");
message.put("speed", location.getSpeed() * 3.6); // 转换为km/h
message.put("direction", location.getBearing());

// 发送消息
MqttMessage mqttMessage = new MqttMessage(message.toString().getBytes());
mqttMessage.setQos(1);
client.publish(topic, mqttMessage);
```

### 方案3：iOS手机应用（实际部署）

#### 使用CocoaMQTT

1. **添加依赖**（使用CocoaPods）：

```ruby
pod 'CocoaMQTT'
```

2. **发送位置数据代码示例**：

```swift
import CocoaMQTT
import CoreLocation

// MQTT连接配置（使用车辆定位器01的实际信息）
let broker = "your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com"
let port: UInt16 = 8883
let clientID = "your_device_id_vehicle_001"
let username = "your_device_id_vehicle_001"
let password = "your_device_secret_here"
let topic = "$oc/your-instance-id-here/your_device_id_vehicle_001/user/location"

// 创建MQTT客户端
let mqtt = CocoaMQTT(clientID: clientID, host: broker, port: port)
mqtt.username = username
mqtt.password = password
mqtt.secureMQTT = true
mqtt.connect()

// 获取GPS位置
let locationManager = CLLocationManager()
locationManager.requestWhenInUseAuthorization()
let location = locationManager.location

// 构建JSON消息
let message: [String: Any] = [
    "longitude": location.coordinate.longitude,
    "latitude": location.coordinate.latitude,
    "address": "当前位置",
    "speed": location.speed * 3.6, // 转换为km/h
    "direction": location.course
]

let jsonData = try JSONSerialization.data(withJSONObject: message)
let jsonString = String(data: jsonData, encoding: .utf8)!

// 发送消息
mqtt.publish(topic, withString: jsonString, qos: .qos1)
```

---

## 前端地图显示

前端地图页面已集成WebSocket实时位置更新功能。

### 功能说明

1. **自动连接WebSocket**：页面加载时自动连接
2. **实时位置更新**：收到位置更新后自动刷新地图标记
3. **车辆列表显示**：显示所有有位置的车辆
4. **地图标记**：在地图上显示车辆位置

### 查看地图

1. 登录系统
2. 进入 **地图监控** 页面
3. 点击 **打开地图** 按钮，在新窗口查看车辆位置
4. 车辆位置会实时更新（通过WebSocket）

---

## 测试验证

### 测试步骤

1. **启动后端服务**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **启动前端服务**
   ```bash
   cd frontend
   npm run dev
   ```

3. **使用MQTTX发送测试数据**
   - 连接华为云IoT
   - 发送位置数据（参考上面的JSON格式）

4. **检查后端日志**
   ```
   INFO  - 收到MQTT消息，主题: $oc/.../user/location
   INFO  - 处理位置更新成功，车辆: 京A12345, 设备ID: ...
   ```

5. **检查前端地图**
   - 打开地图监控页面
   - 查看车辆位置是否更新

### 验证清单

- [ ] MQTT连接成功（后端日志）
- [ ] MQTT消息接收成功（后端日志）
- [ ] 车辆位置更新成功（数据库检查）
- [ ] WebSocket推送成功（前端控制台）
- [ ] 地图显示车辆位置（前端页面）

---

## 常见问题

### Q1: MQTT连接失败

**可能原因**：
- Broker地址错误
- 设备ID或密钥错误
- SSL证书问题

**解决方法**：
1. 检查环境变量配置
2. 确认设备ID和密钥正确
3. 检查网络连接

### Q2: 收到消息但车辆位置未更新

**可能原因**：
- 车辆表中没有对应的 `gps_device_id`
- 设备ID不匹配

**解决方法**：
```sql
-- 检查车辆的GPS设备ID
SELECT id, vehicle_no, gps_device_id FROM vehicle WHERE gps_device_id = 'your_device_id';

-- 如果没有，更新设备ID
UPDATE vehicle SET gps_device_id = 'your_device_id' WHERE vehicle_no = '京A12345';
```

### Q3: 前端地图不显示车辆

**可能原因**：
- 车辆没有位置数据（latitude/longitude为null）
- WebSocket未连接

**解决方法**：
1. 检查车辆是否有位置数据：
   ```sql
   SELECT vehicle_no, location_latitude, location_longitude FROM vehicle;
   ```
2. 检查前端WebSocket连接状态（浏览器控制台）

### Q4: MQTT消息格式错误

**错误示例**：
```
处理MQTT消息失败，主题: ...
```

**解决方法**：
确保消息格式为JSON，包含以下字段：
```json
{
  "longitude": 116.5842,
  "latitude": 40.0801,
  "address": "位置描述",
  "speed": 0.0,
  "direction": 0.0
}
```

### Q5: 华为云IoT主题格式

华为云IoT的主题格式：
- 设备上报：`$oc/{实例ID}/{device_id}/user/{自定义主题}`
- 平台下发：`$oc/{实例ID}/{device_id}/user/command`

**当前实例的实际主题示例**：
- 车辆定位器01上报：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/location`
- 车辆定位器01接收：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/command`

**注意**：
- `$oc` 是固定前缀
- `your-instance-id-here` 是实例ID（在主题中作为project_id使用）
- `your_device_id_vehicle_001` 是设备ID
- `/user/` 是用户自定义主题前缀

---

## 总结

完成以上步骤后，您应该能够：

1. 通过华为云IoT平台接收MQTT位置数据
2. 在系统中自动更新车辆位置
3. 在地图监控页面实时显示车辆位置

如果遇到问题，请检查：
- 后端日志（MQTT连接和消息处理）
- 数据库（车辆GPS设备ID配置）
- 前端控制台（WebSocket连接和消息接收）

---

## 相关文档

- [MQTT手机定位指南](mqtt-mobile-location-guide.md)
- [系统架构文档](architecture.md)
- [部署指南](deployment-guide.md)

---

**最后更新**：2026年1月
