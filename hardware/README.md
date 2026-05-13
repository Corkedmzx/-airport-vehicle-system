# ESP32-S3 GPS → 华为云 IoT MQTT → 机场车辆前端地图（ESP-IDF）

**本目录 `hardware/` 即 ESP-IDF 工程根目录**（含 `CMakeLists.txt`、`main/`）。请先 **`cd hardware`** 再执行 `idf.py`。

## 端到端数据流（主路径）

```
GPS(NMEA) → ESP32 UART → 解析经纬度
    → WiFi → MQTT/TLS(华为云 IoT) → 后端订阅 → VehicleLocationService → WebSocket → 前端百度地图
```

备用：**关闭 menuconfig 中的「启用 WiFi + 华为云 MQTT」** 时，仅在 USB 串口打印 JSON（调试用）。

### 与后端打通的检查项

1. **华为云**：设备已在控制台创建；MQTT 接入 URI、`mqtts://…:8883`、设备 ID、密钥与固件 `menuconfig` 一致。
2. **数据库**：`vehicle.gps_device_id` = 华为云 **设备 ID**（与 `docs/mqtt-huawei-iot-integration-guide.md` 一致）。
3. **后端**：`HUAWEI_IOT_MQTT_*` 环境变量已配置，能订阅设备上报主题。
4. **射频**：**ESP32-S3 WiFi 仅 2.4GHz**。若使用手机热点且设为 **仅 5GHz**，芯片**搜不到**该 SSID；请在手机里把热点改为 **2.4GHz** 或 **2.4GHz/5GHz 双频**后再配网。

### 用 `idf.py monitor` 如何判断有没有发坐标

正常顺序建议记住这几条日志：

1. **`GPS 模块串口已有数据输出`** — UART 收到任意字节（接线/波特率大致正确）。  
2. **`定位有效(RMC)` 或 `定位有效(GGA)`** — 已解析出经纬度（**GGA** 为补充：部分模块 RMC 仍为 V 时已有定位）。  
3. **`MQTT 上报成功 lon=… lat=…`** — 已向华为云该 topic 发出 JSON；若始终没有本条，地图不会动。  
4. **每 40 秒 `诊断: UART累计接收=…`** — 累计字节长期为 0 说明 GPS 未往 MCU 发数据（线序/波特率/模块供电）。

### MQTT 已连上但地图没有点

1. **固件侧**：要有 **`MQTT 上报成功 lon=… lat=…`**。若始终没有，多为 **无 GPS 有效解**（室内 RMC=V）；请到室外/窗边。  
2. **数据库**：至少一辆车 **`gps_device_id`** 必须等于华为控制台中的 **设备 ID**（与固件 `menuconfig` 里一致），例如：  
   `UPDATE vehicle SET gps_device_id = '<你在控制台复制的完整设备ID>' WHERE vehicle_no = '你的车牌';`  
   若只改了库未重启后端，刷新地图页；后端会将硬件上报的 **WGS84 转为 BD09** 再入库，与百度底图一致。  
3. **演示数据**：`database/init.sql` 里示例车辆可能使用 **远离当前定位** 的演示坐标；若地图中心在您本机或浏览器定位附近，示例绿点可能在视野外，可点「定位全部」或缩小地图；已正确绑定 **gps_device_id** 的车辆会出现在 **模块真实 GPS 附近**。  
4. **后端（重要）**：**vehicle_001** 默认在**同一 MQTT 连接**上既做 **mobile→web 中转**，又按 **`hardware-gps-device-ids` + `vehicle.gps_device_id`** 跨设备订阅硬件上行（可能受华为 ACL 限制）。若希望 **vehicle_001 只做中转、GPS 模块仅走 vehicle_002 且用独立连接**，在后端配置 **`huawei.iot.mqtt.hardware-gps-dedicated-client=true`**，并在设备密钥目录放置 **`DEVICES-KEY-*_vehicle_002.txt`**（与华为控制台 **vehicle_002** 一致），重启后端即可；此时 **`hardware-gps-mqtt-client-name`** 默认为 `vehicle_002`，可按文件名修改。**主题**：MQTT 里 **`$oc/{实例}/…` 与 `/{实例}/…` 是两个不同的 topic**；`hardware-gps-subscribe-oc-prefix` 默认 false。  
5. **华为云「消息跟踪」校验失败**：常与 **Topic/模型** 与控制台规则有关；只要 **MQTT 发布成功** 且后端能订阅到 **JSON 经纬度**，地图仍可更新（与控制台某条校验记录是否失败可并存）。

### 华为云 MQTT 账密（易错点）

控制台「MQTT 连接参数」里的 **Password 不是把设备密钥原样放进 MQTT**，而是按官方文档用 **HMAC-SHA256** 生成（密钥为 **UTC** 的 `YYYYMMDDHH`，消息为 **secret 字符串**）。**ClientId** 也必须为 `{device_id}_0_0_{同一时间戳}`，**UserName** 为 **device_id**。本仓库固件已在 `airport_connectivity.c` 中按 [华为文档](https://support.huaweicloud.com/devg-iothub/iot_02_0203.html) 自动计算；若仍失败，请核对 SNTP 已同步（日志里应在一段时间后出现 **MQTT 已连接华为云 Broker**）。

---

## 敏感信息与 Git（下载即可用，且不泄漏密钥）

| 文件 | 说明 |
|------|------|
| **`sdkconfig`** | 本地生成，含 WiFi/密钥；已在 **`.gitignore`**，**勿提交**。 |
| **`sdkconfig.defaults.example`** | **无敏感信息**的可提交模板，结构与真实配置一致；克隆后按此在 `menuconfig` 中填写。 |
| **`sdkconfig.defaults`** | 仓库内为 **占位符**（`REPLACE_WITH_*`），仅保证工程可配置、可编译；**勿把真密码写进将提交的版本。** |

真实 WiFi 密码、华为云设备密钥请只在 **本机** 通过 **`idf.py menuconfig`** 或 **未跟踪的 `sdkconfig`** 配置。

---

## 硬件接线（GPS）

| GPS E108-GN03D | ESP32-S3（menuconfig 可改） |
|----------------|----------------------------|
| VCC            | 5V 或 3.3V                 |
| GND            | GND                        |
| TXD            | GPIO10（ESP RX）           |
| RXD            | GPIO11（ESP TX）           |

## 构建与烧录

要求：**ESP-IDF 5.3+（含 6.x）**，已执行 `export.ps1` / `export.sh`，首次构建需联网拉取 `espressif/mqtt`（`main/idf_component.yml`）。

```bash
cd hardware
idf.py set-target esp32s3
idf.py menuconfig
# Airport 菜单：填写真实 WiFi（2.4GHz）、华为云 URI/实例/设备/密钥
idf.py build
idf.py -p <串口号> flash monitor   # Windows 下为「端口 COMx」，编号以设备管理器为准
```

若曾切换过联网/串口模式，建议清理后重配：`idf.py fullclean` 或删除 `build` 再 `set-target`。

## 预留接口

- **蓝牙**：`airport_ble_reserve_interface()` 占位，未启协议栈。
- **联网实现**：`main/airport_connectivity.c`，由 `CONFIG_AIRPORT_USE_WIFI_MQTT` 控制。

## 常见问题

- **`fullclean` 拒绝删除 `build`**：删除 `hardware/build` 目录后重试。
- **能连路由但连不上手机热点**：确认热点为 **2.4GHz**；ESP32-S3 不支持 5GHz-only 热点。
- **`mqtt` / `esp_driver_uart` 等组件错误**：见历史说明；IDF 6 需 `esp_driver_uart` 与组件注册表 `espressif/mqtt`。
- **地图无点**：查后端是否订阅到消息、库表 `gps_device_id` 是否与设备 ID 一致、浏览器 WebSocket 与地图页是否打开。

更多集成说明见项目内 **`docs/mqtt-huawei-iot-integration-guide.md`**。
