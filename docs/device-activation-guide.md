# 华为云IoT设备激活完整指南

## 目录

1. [设备激活原理](#设备激活原理)
2. [后端MQTT客户端配置](#后端mqtt客户端配置)
3. [使用MQTTX激活设备](#使用mqttx激活设备)
4. [验证设备激活](#验证设备激活)
5. [常见问题](#常见问题)

---

## 设备激活原理

### 什么是设备激活？

华为云IoT平台中，设备状态分为：
- **未激活**：设备已注册但从未连接过MQTT Broker
- **激活**：设备已成功连接过MQTT Broker（至少一次）
- **在线**：设备当前正在连接MQTT Broker
- **离线**：设备已断开连接

### 设备激活的条件

设备要激活，必须满足以下条件：

1. **设备已注册**：在华为云IoT控制台已创建设备
2. **MQTT连接成功**：使用正确的设备ID和密钥连接到MQTT Broker
3. **发送至少一条消息**：连接成功后发送至少一条消息到平台

**重要**：设备激活是**自动的**，只要设备成功连接并发送消息，平台会自动将设备状态从"未激活"变为"激活"。

---

## 后端MQTT客户端配置

### 步骤1：设置环境变量

在PowerShell中执行以下命令（**在同一会话中执行**）：

```powershell
# 启用MQTT客户端
$env:HUAWEI_IOT_MQTT_ENABLED="true"

# MQTT Broker地址
$env:HUAWEI_IOT_MQTT_BROKER="ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883"

# MQTT客户端ID（必须是车辆定位器01的设备ID）
$env:HUAWEI_IOT_MQTT_CLIENT_ID="your_device_id_vehicle_001"

# MQTT用户名（设备ID）
$env:HUAWEI_IOT_MQTT_USERNAME="your_device_id_vehicle_001"

# MQTT密码（设备密钥）
$env:HUAWEI_IOT_MQTT_PASSWORD="your_device_secret_here"

# 订阅主题（用于接收设备上报的位置数据）
$env:HUAWEI_IOT_MQTT_TOPIC="$oc/your-instance-id-here/your_device_id_vehicle_001/user/location"

# QoS等级
$env:HUAWEI_IOT_MQTT_QOS="1"

# 验证配置
Write-Host "=== MQTT配置 ===" -ForegroundColor Green
Write-Host "Broker: $env:HUAWEI_IOT_MQTT_BROKER" -ForegroundColor Cyan
Write-Host "Client ID: $env:HUAWEI_IOT_MQTT_CLIENT_ID" -ForegroundColor Cyan
Write-Host "Topic: $env:HUAWEI_IOT_MQTT_TOPIC" -ForegroundColor Cyan
```

**重要提示**：
- 这些环境变量只在**当前PowerShell会话**中有效
- 如果关闭PowerShell窗口，需要重新设置
- 启动后端服务时，必须在**同一个PowerShell窗口**中执行

### 步骤2：配置数据库车辆GPS设备ID

在MySQL中执行：

```sql
-- 查看现有车辆
SELECT id, vehicle_no, gps_device_id FROM vehicle;

-- 将车辆的GPS设备ID设置为车辆定位器01的设备ID
-- 假设车辆"京A12345"要使用车辆定位器01
UPDATE vehicle 
SET gps_device_id = 'your_device_id_vehicle_001' 
WHERE vehicle_no = '京A12345';

-- 验证更新结果
SELECT id, vehicle_no, gps_device_id 
FROM vehicle 
WHERE gps_device_id = 'your_device_id_vehicle_001';
```

**说明**：
- `gps_device_id` 必须与华为云IoT的设备ID完全一致
- 一个设备ID只能关联一辆车（数据库有唯一约束）
- 如果车辆表中没有数据，需要先创建车辆记录

### 步骤3：启动后端服务

在**设置了环境变量的同一个PowerShell窗口**中：

```powershell
cd backend
mvn spring-boot:run
```

### 步骤4：检查后端日志

启动后，查看日志输出，应该看到：

```
INFO  c.a.s.i.MqttClientServiceImpl - MQTT客户端初始化成功，Broker: ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883
INFO  c.a.s.i.MqttClientServiceImpl - MQTT连接成功，Broker: ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883
INFO  c.a.s.i.MqttClientServiceImpl - MQTT订阅主题成功: $oc/your-instance-id-here/your_device_id_vehicle_001/user/location, QoS: 1
```

**如果看到这些日志，说明后端MQTT客户端已成功连接！**

---

## 使用MQTTX激活设备

### 为什么需要MQTTX？

后端MQTT客户端只是**订阅**主题接收消息，不会**发送**消息。要激活设备，需要设备**主动连接并发送消息**。

MQTTX可以模拟设备，发送消息到华为云IoT平台，从而激活设备。

### 步骤1：配置MQTTX连接

**重要**：要激活设备，Client ID **必须是设备的ID**，不能使用MQTTX自动生成的Client ID！

#### 配置步骤：

1. **打开MQTTX连接配置**：
   - 点击左侧的"车辆定位器01"连接
   - 或点击连接名称进入编辑模式

2. **检查并修改Client ID**（最关键）：
   - 找到 **Client ID** 字段
   - **必须手动输入**：`your_device_id_vehicle_001`
   - **不要使用**MQTTX自动生成的Client ID（如 `mqttx_78f93f8`）
   - **必须使用**设备的实际ID

3. **检查其他配置**：
   - **Name**：车辆定位器01（连接名称，可自定义）
   - **Host**：`your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com`
   - **Port**：`8883`
   - **Protocol**：`mqtts://`（MQTT over SSL/TLS）
   - **Username**：`your_device_id_vehicle_001`（设备ID，与Client ID相同）
   - **Password**：`your_device_secret_here`（设备密钥）
   - **SSL/TLS**：已启用
   - **Certificate Type**：CA signed server certificate

4. **保存配置**：
   - 点击右上角的 **保存** 按钮（或按 Ctrl+S）

### 步骤2：连接MQTT Broker

1. 在MQTTX中，点击 **连接** 按钮（或点击连接名称）
2. 等待连接成功，状态显示为 **Connected**（绿色圆点）

**连接成功后，设备状态会从"未激活"变为"激活"！**

### 步骤3：发送位置数据（激活设备）

连接成功后，发送一条消息来激活设备：

1. **创建消息**：
   - 在MQTTX底部，找到消息输入区域
   - 点击 **New Message** 或直接在输入框中输入

2. **填写主题（Topic）**：
   ```
   $oc/your-instance-id-here/your_device_id_vehicle_001/user/location
   ```

3. **填写消息内容（Payload）**：
   - 选择格式：**JSON**
   - 输入内容：
   ```json
   {
     "longitude": 116.5842,
     "latitude": 40.0801,
     "address": "首都机场T3航站楼",
     "speed": 0,
     "direction": 0
   }
   ```

4. **设置QoS**：
   - 选择 **QoS 1**

5. **发送消息**：
   - 点击 **Send** 按钮
   - 消息发送成功后，会在消息列表中显示

### 步骤4：验证设备激活

1. **在华为云IoT控制台查看**：
   - 刷新设备列表页面
   - 查看"车辆定位器1"的状态
   - 应该从"未激活"变为"激活"（或"在线"）

2. **查看后端日志**：
   - 应该看到收到MQTT消息的日志：
     ```
     INFO - 收到MQTT消息，主题: $oc/.../user/location
     INFO - 处理位置更新成功，车辆: 京A12345, 设备ID: your_device_id_vehicle_001
     ```

3. **检查数据库**：
   ```sql
   SELECT vehicle_no, location_longitude, location_latitude, last_update_time 
   FROM vehicle 
   WHERE gps_device_id = 'your_device_id_vehicle_001';
   ```
   - 应该看到位置数据已更新

---

## 验证设备激活

### 方法1：在华为云IoT控制台查看

1. 登录华为云IoT控制台
2. 进入 **设备** → **所有设备**
3. 查看"车辆定位器1"的状态：
   - **激活**：设备已成功连接过（绿色）
   - **在线**：设备当前正在连接（绿色，带圆点）
   - **未激活**：设备从未连接过（灰色）

### 方法2：查看后端日志

后端MQTT客户端连接成功后，会输出：

```
INFO  c.a.s.i.MqttClientServiceImpl - MQTT连接成功，Broker: ssl://...
INFO  c.a.s.i.MqttClientServiceImpl - MQTT订阅主题成功: $oc/.../user/location, QoS: 1
```

**注意**：后端MQTT客户端连接**不会**激活设备，因为它是作为**订阅者**连接的，不是作为**设备**连接的。

要激活设备，必须使用**设备的Client ID**连接（即MQTTX中使用`your_device_id_vehicle_001`作为Client ID）。

### 方法3：使用MQTTX查看连接状态

在MQTTX中：
- 连接成功后，连接名称旁边会显示绿色圆点
- 状态显示为 **Connected**

---

## 完整配置流程总结

### 第一步：配置后端环境变量

```powershell
# 在PowerShell中执行（一次性设置）
$env:HUAWEI_IOT_MQTT_ENABLED="true"
$env:HUAWEI_IOT_MQTT_BROKER="ssl://your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883"
$env:HUAWEI_IOT_MQTT_CLIENT_ID="your_device_id_vehicle_001"
$env:HUAWEI_IOT_MQTT_USERNAME="your_device_id_vehicle_001"
$env:HUAWEI_IOT_MQTT_PASSWORD="your_device_secret_here"
$env:HUAWEI_IOT_MQTT_TOPIC="$oc/your-instance-id-here/your_device_id_vehicle_001/user/location"
$env:HUAWEI_IOT_MQTT_QOS="1"
```

### 第二步：配置数据库

```sql
-- 将车辆的GPS设备ID设置为车辆定位器01的设备ID
UPDATE vehicle 
SET gps_device_id = 'your_device_id_vehicle_001' 
WHERE vehicle_no = '京A12345';
```

### 第三步：启动后端服务

```powershell
# 在同一个PowerShell窗口中
cd backend
mvn spring-boot:run
```

### 第四步：使用MQTTX连接并发送消息

1. 打开MQTTX
2. 连接"车辆定位器01"（使用您已配置的连接）
3. 发送位置数据（参考上面的步骤3）

### 第五步：验证

1. 华为云IoT控制台：设备状态变为"激活"
2. 后端日志：收到MQTT消息
3. 数据库：车辆位置已更新
4. 前端地图：显示车辆位置

---

## 常见问题

### Q1: 后端连接成功，但设备还是"未激活"

**原因**：
- 后端MQTT客户端使用的是**订阅者**身份，不是**设备**身份
- 设备激活需要**设备本身**连接（使用设备的Client ID）

**解决方法**：
- 使用MQTTX，以**设备的Client ID**（`your_device_id_vehicle_001`）连接
- 连接成功后，设备会自动激活

### Q2: MQTTX连接失败，提示"Bad User Name or Password"

**最常见原因**：Client ID使用了MQTTX自动生成的值，而不是设备的实际ID

**解决方法**：

1. **检查Client ID**（最重要）：
   - 打开MQTTX连接配置
   - 找到 **Client ID** 字段
   - **必须手动输入**：`your_device_id_vehicle_001`
   - 如果显示 `mqttx_xxxxx` 或类似自动生成的值，**必须删除并手动输入设备ID**
   - Client ID必须与设备ID完全一致

2. **检查其他配置**：
   - Host：`your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com`
   - Port：`8883`
   - Protocol：`mqtts://`
   - Username：`your_device_id_vehicle_001`（必须与Client ID相同）
   - Password：`your_device_secret_here`
   - SSL/TLS：已启用
   - Certificate Type：CA signed server

3. **保存并重试**：
   - 点击保存按钮
   - 重新点击连接按钮

**关键点**：
- Client ID = Username = 设备ID = `your_device_id_vehicle_001`
- 这三个值必须完全一致！

### Q3: 设备激活后，后端收不到消息

**可能原因**：
- 后端未连接或连接失败
- 主题不匹配
- 后端未订阅主题

**解决方法**：
1. 检查后端日志，确认MQTT连接成功
2. 检查订阅的主题是否正确：
   ```
   $oc/your-instance-id-here/your_device_id_vehicle_001/user/location
   ```
3. 确认MQTTX发送的主题与后端订阅的主题一致

### Q4: 后端MQTT客户端连接失败

**可能原因**：
- 环境变量未设置
- Broker地址错误
- 设备ID或密钥错误

**解决方法**：
1. 检查环境变量是否设置：
   ```powershell
   Get-ChildItem Env: | Where-Object { $_.Name -like "HUAWEI_IOT_MQTT*" }
   ```
2. 确认所有环境变量都已设置
3. 检查Broker地址是否正确
4. 检查设备ID和密钥是否正确

### Q5: 数据库车辆位置未更新

**可能原因**：
- 车辆的`gps_device_id`未设置或设置错误
- 设备ID不匹配

**解决方法**：
1. 检查车辆的GPS设备ID：
   ```sql
   SELECT vehicle_no, gps_device_id 
   FROM vehicle 
   WHERE vehicle_no = '京A12345';
   ```
2. 确认`gps_device_id`与MQTT消息中的设备ID一致
3. 如果不一致，更新：
   ```sql
   UPDATE vehicle 
   SET gps_device_id = 'your_device_id_vehicle_001' 
   WHERE vehicle_no = '京A12345';
   ```

---

## 重要说明

### 后端MQTT客户端 vs 设备连接

**后端MQTT客户端**：
- 作用：**订阅**主题，接收设备上报的位置数据
- Client ID：可以是任意值（建议使用设备ID）
- 不会激活设备

**设备连接**（MQTTX模拟）：
- 作用：**发送**位置数据到平台
- Client ID：**必须是设备的ID**（`your_device_id_vehicle_001`）
- **会激活设备**

### 设备激活流程

```
1. 设备（MQTTX）使用设备ID连接MQTT Broker
   ↓
2. 连接成功 → 设备状态变为"激活"
   ↓
3. 设备发送位置数据到主题
   ↓
4. 后端MQTT客户端接收消息
   ↓
5. 更新数据库车辆位置
   ↓
6. 通过WebSocket推送给前端
   ↓
7. 前端地图显示车辆位置
```

---

## 快速检查清单

完成以下步骤后，设备应该已激活：

- [ ] 后端环境变量已设置（在同一PowerShell会话中）
- [ ] 数据库车辆`gps_device_id`已配置
- [ ] 后端服务已启动，MQTT连接成功（查看日志）
- [ ] MQTTX已配置并连接成功（使用设备ID作为Client ID）
- [ ] MQTTX已发送至少一条位置数据
- [ ] 华为云IoT控制台显示设备状态为"激活"
- [ ] 后端日志显示收到MQTT消息
- [ ] 数据库车辆位置已更新
- [ ] 前端地图显示车辆位置

---

**相关文档**：
- [MQTT + 华为云IoT集成指南](mqtt-huawei-iot-integration-guide.md)
- [MQTTX使用详细指南](mqttx-usage-guide.md)

---

**最后更新**：2026年1月
