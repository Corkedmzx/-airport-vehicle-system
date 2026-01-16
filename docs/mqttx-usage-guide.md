# MQTTX 使用详细指南

## 目录

1. [MQTTX简介](#mqttx简介)
2. [下载和安装](#下载和安装)
3. [创建连接](#创建连接)
4. [发送消息](#发送消息)
5. [订阅主题](#订阅主题)
6. [定时发送](#定时发送)
7. [实际应用示例](#实际应用示例)
8. [常见问题](#常见问题)

---

## MQTTX简介

MQTTX 是一个跨平台的 MQTT 客户端工具，支持 Windows、Mac 和 Linux。它提供了友好的图形界面，可以方便地测试 MQTT 连接和消息收发。

**主要功能**：
- 连接多个 MQTT Broker
- 发送和接收 MQTT 消息
- 支持 SSL/TLS 加密连接
- 支持定时发送消息
- 消息历史记录
- 支持多种消息格式（JSON、Text、Hex等）

---

## 下载和安装

### Windows

1. 访问官网：https://mqttx.app/
2. 点击 **Download for Windows**
3. 下载 `.exe` 安装包
4. 运行安装程序，按提示完成安装
5. 启动 MQTTX

### Mac

1. 访问官网：https://mqttx.app/
2. 点击 **Download for macOS**
3. 下载 `.dmg` 安装包
4. 打开 `.dmg` 文件，将 MQTTX 拖拽到 Applications 文件夹
5. 启动 MQTTX（首次启动需要在"系统偏好设置"中允许）

### Linux

1. 访问官网：https://mqttx.app/
2. 点击 **Download for Linux**
3. 下载 `.AppImage` 文件
4. 添加执行权限：
   ```bash
   chmod +x MQTTX-x.x.x.AppImage
   ```
5. 运行：
   ```bash
   ./MQTTX-x.x.x.AppImage
   ```

---

## 创建连接

### 步骤1：打开新建连接对话框

1. 启动 MQTTX
2. 点击左侧的 **+ New Connection** 按钮
3. 或点击顶部菜单 **Connections** → **New Connection**

### 步骤2：填写基本信息

在 **General** 标签页：

- **Name**：`车辆定位器01`（连接名称，可自定义）
- **Client ID**：`your_device_id_vehicle_001`（**必须手动输入设备ID**，不能使用MQTTX自动生成的值）

**重要提示**：
- MQTTX可能会自动生成Client ID（如 `mqttx_78f93f80`）
- **必须删除自动生成的值，手动输入设备的实际ID**
- Client ID必须与设备ID完全一致，否则连接会失败并提示"Bad User Name or Password"

### 步骤3：配置服务器信息

在 **Broker** 标签页：

- **Host**：`your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com`
- **Port**：`8883`
- **Protocol**：选择 `mqtts://`（MQTT over SSL/TLS）

### 步骤4：配置用户认证

在 **User Credentials** 标签页：

- **Username**：`your_device_id_vehicle_001`（设备ID）
- **Password**：`your_device_secret_here`（设备密钥）

### 步骤5：配置SSL/TLS

在 **SSL/TLS** 标签页：

1. 启用 **SSL/TLS** 开关
2. **Certificate**：选择 **CA signed server**（CA签名服务器证书）
3. 其他选项保持默认

### 步骤6：保存并连接

1. 点击右下角的 **Connect** 按钮
2. 连接成功后，连接状态会显示为 **Connected**（绿色）
3. 连接配置会自动保存，下次可以直接使用

---

## 发送消息

### 基本发送

1. **创建消息**：
   - 在MQTTX主界面，点击底部的 **New Message** 按钮
   - 或直接在消息输入框中输入

2. **填写主题（Topic）**：
   ```
   $oc/your-instance-id-here/your_device_id_vehicle_001/user/location
   ```

3. **选择消息格式**：
   - 点击 **Format** 下拉菜单
   - 选择 **JSON**

4. **输入消息内容**：
   ```json
   {
     "longitude": 116.5842,
     "latitude": 40.0801,
     "address": "首都机场T3航站楼",
     "speed": 45.5,
     "direction": 90.0
   }
   ```

5. **设置QoS**：
   - 选择 **QoS 1**（推荐）

6. **发送消息**：
   - 点击右下角的 **Send** 按钮
   - 消息发送成功后，会在消息列表中显示

### 消息格式说明

**必需字段**：
- `longitude`：经度（Double类型）
- `latitude`：纬度（Double类型）

**可选字段**：
- `address`：地址描述（String类型）
- `speed`：速度，单位 km/h（Double类型）
- `direction`：方向角，单位 度（Double类型）

**示例消息**：

```json
{
  "longitude": 116.5842,
  "latitude": 40.0801,
  "address": "首都机场T3航站楼",
  "speed": 45.5,
  "direction": 90.0
}
```

---

## 订阅主题

### 订阅平台下发主题

1. **创建订阅**：
   - 连接成功后，在右侧消息区域，点击 **New Subscription** 按钮

2. **填写订阅信息**：
   - **Topic**：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/command`
   - **QoS**：选择 `1`

3. **确认订阅**：
   - 点击 **Confirm** 按钮
   - 订阅成功后，会在订阅列表中显示

4. **接收消息**：
   - 当平台下发消息时，会在消息列表中显示
   - 消息会标注为 **Received**

---

## 定时发送

### 设置定时发送

1. **创建消息**（参考上面的"发送消息"步骤）

2. **启用定时发送**：
   - 在消息输入框下方，找到 **Timing** 选项
   - 启用 **Timing** 开关

3. **配置定时参数**：
   - **Interval**：发送间隔（秒），例如 `30`（每30秒发送一次）
   - **Times**：发送次数，例如 `100`（发送100次）
   - 或选择 **Unlimited**（无限发送，直到手动停止）

4. **开始定时发送**：
   - 点击 **Send** 按钮
   - 定时发送开始后，会在消息列表中显示发送状态

5. **停止定时发送**：
   - 点击消息列表中的 **Stop** 按钮
   - 或断开连接

### 模拟车辆移动

在定时发送时，可以修改位置数据，模拟车辆移动：

**第1次发送**：
```json
{
  "longitude": 116.5842,
  "latitude": 40.0801,
  "address": "首都机场T3航站楼",
  "speed": 45.5,
  "direction": 90.0
}
```

**第2次发送**（30秒后）：
```json
{
  "longitude": 116.5850,
  "latitude": 40.0805,
  "address": "首都机场T3航站楼附近",
  "speed": 50.0,
  "direction": 95.0
}
```

**第3次发送**（60秒后）：
```json
{
  "longitude": 116.5860,
  "latitude": 40.0810,
  "address": "机场高速",
  "speed": 55.0,
  "direction": 100.0
}
```

---

## 实际应用示例

### 示例1：发送单个位置点

**场景**：测试车辆位置更新功能

1. 连接MQTTX（使用车辆定位器01的配置）
2. 发送消息：
   - **Topic**：`$oc/your-instance-id-here/your_device_id_vehicle_001/user/location`
   - **Payload**：
     ```json
     {
       "longitude": 116.5842,
       "latitude": 40.0801,
       "address": "首都机场T3航站楼",
       "speed": 0,
       "direction": 0
     }
     ```
3. 检查后端日志，确认收到消息
4. 检查数据库，确认车辆位置已更新
5. 打开前端地图页面，查看车辆位置

### 示例2：模拟车辆行驶轨迹

**场景**：测试实时位置跟踪功能

1. 连接MQTTX
2. 设置定时发送：
   - **Interval**：`10` 秒
   - **Times**：`30` 次（模拟5分钟的行驶）
3. 每次发送前修改位置数据，模拟车辆移动
4. 在前端地图页面观察车辆位置实时更新

### 示例3：测试多个车辆

**场景**：同时测试多个车辆的位置上报

1. **创建多个连接**：
   - 连接1：车辆定位器01
   - 连接2：手机监控端（使用不同的设备ID和密钥）
   - 连接3：网页监控端（使用不同的设备ID和密钥）

2. **配置不同的车辆**：
   ```sql
   -- 车辆1使用定位器01
   UPDATE vehicle SET gps_device_id = 'your_device_id_vehicle_001' WHERE vehicle_no = '京A12345';
   
   -- 车辆2使用手机监控端
   UPDATE vehicle SET gps_device_id = 'your_device_id_mobile_001' WHERE vehicle_no = '京B67890';
   ```

3. **同时发送位置数据**：
   - 在每个连接中发送不同车辆的位置
   - 在地图页面查看所有车辆的位置

---

## 常见问题

### Q1: 连接失败，提示 "Connection refused"

**可能原因**：
- Host或Port配置错误
- SSL/TLS未启用
- 网络连接问题

**解决方法**：
1. 检查Host：`your-broker.st1.iotda-device.cn-north-4.myhuaweicloud.com`
2. 检查Port：`8883`
3. 确认Protocol选择：`mqtts://`
4. 确认SSL/TLS已启用
5. 检查网络连接

### Q2: 连接失败，提示 "Not authorized"

**可能原因**：
- Username或Password错误
- 设备ID和密钥不匹配

**解决方法**：
1. 检查Username：`your_device_id_vehicle_001`
2. 检查Password：`your_device_secret_here`
3. 确认设备ID和密钥来自同一个设备

### Q3: 消息发送成功但后端未收到

**可能原因**：
- Topic不正确
- 后端MQTT客户端未连接
- 后端未订阅该主题

**解决方法**：
1. 检查Topic格式：
   ```
   $oc/your-instance-id-here/your_device_id_vehicle_001/user/location
   ```
2. 检查后端日志，确认MQTT连接状态
3. 检查后端日志，确认主题订阅状态
4. 确认后端环境变量配置正确

### Q4: 如何查看消息历史

**方法**：
1. 在MQTTX主界面，消息列表会显示所有发送和接收的消息
2. 点击消息可以查看详细信息
3. 可以导出消息历史（右键消息 → Export）

### Q5: 如何测试多个设备

**方法**：
1. 创建多个连接，每个连接使用不同的设备ID和密钥
2. 在数据库中为不同车辆配置不同的 `gps_device_id`
3. 在不同连接中发送不同车辆的位置数据

### Q6: 定时发送如何停止

**方法**：
1. 点击消息列表中的 **Stop** 按钮
2. 或断开连接
3. 或关闭MQTTX

### Q7: 消息格式错误

**错误示例**：
```json
{
  "longitude": "116.5842",  // 错误：应该是数字，不是字符串
  "latitude": "40.0801"     // 错误：应该是数字，不是字符串
}
```

**正确格式**：
```json
{
  "longitude": 116.5842,    // 正确：数字类型
  "latitude": 40.0801       // 正确：数字类型
}
```

---

## 总结

使用MQTTX可以方便地测试MQTT连接和消息收发。主要步骤：

1. 下载并安装MQTTX
2. 创建连接（使用设备ID和密钥）
3. 发送位置数据（JSON格式）
4. 验证后端接收和前端显示

**推荐工作流程**：
1. 使用MQTTX测试单个位置点
2. 确认后端和前端正常工作
3. 使用定时发送模拟实时定位
4. 在实际手机应用中实现定位功能

---

**相关文档**：
- [MQTT + 华为云IoT集成指南](mqtt-huawei-iot-integration-guide.md)
- [系统架构文档](architecture.md)

---

**最后更新**：2026年1月
