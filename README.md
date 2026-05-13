# 机场车辆监控与调度系统

## 项目概述

基于Java+SpringBoot的机场车辆监控与调度系统，实现对机场内各类车辆的实时监管与智能调度，提升车辆利用效率，保障运行安全。系统采用前后端分离架构，提供完整的RBAC权限控制、实时监控、智能调度和数据分析功能。

## 技术栈

- **后端**: Java 17 + SpringBoot 3.2 + Spring Security + Spring Data JPA + JWT
- **数据库**: MySQL 8.0 + Redis (缓存)
- **前端**: Vue.js 3 + TypeScript + Element Plus + Vite + Pinia + ECharts
- **小程序**: 微信小程序（支持司机和维修员移动端操作）
- **构建工具**: Maven (后端) + npm (前端)
- **API文档**: Swagger/OpenAPI 3 (Knife4j)
- **实时通信**: WebSocket (实时定位、消息推送)
- **其他**: BCrypt密码加密、响应式设计

## 核心功能模块

### 1. 用户认证与权限管理
- 用户注册与登录：JWT Token认证机制，支持小程序端注册（手机号必填）
- 注册通知：新用户注册后自动发送站内信通知ADMIN角色管理员
- 密码管理：BCrypt加密存储，支持修改密码
- 角色权限控制(RBAC)：7种角色，细粒度权限控制
- 权限刷新：权限更新后可在个人资料页面刷新，无需重新登录
- 个人资料管理：查看和修改个人信息、头像、密码、权限信息

### 2. 车辆管理
- 车辆信息管理：车辆CRUD操作
- 车辆状态监控：正常运行、维护中、故障、离线等状态
- 车辆类型管理：行李车、摆渡车、货运车、清洁车、维修车、巡逻车
- 车辆详情：查看车辆详细信息、历史记录

### 3. 任务调度
- 任务创建：创建调度任务，设置优先级，自动生成唯一任务编号
- 任务分配：手动分配任务到车辆，取消分配功能
- 任务状态管理：待分配 → 已分配 → 执行中 → 已完成
- 任务完成：司机和维修员可以确认完成任务（需要task:complete权限）
  - 支持小程序接口：`POST /api/tasks/confirm-complete`
  - 支持完成说明保存
  - 自动更新车辆状态和发送通知
- 我的任务：小程序端可获取分配给当前用户的任务列表（`GET /api/tasks?action=my-tasks`）
- 调度中心：实时调度概览、可用车辆展示、任务分配界面
- 车辆状态自动更新：任务分配/完成时自动更新车辆状态

### 4. 实时监控
- 车辆实时监控：实时显示车辆位置、状态、速度等信息
- WebSocket实时定位：前端通过WebSocket连接接收实时位置更新
- 传感器接口预留：提供WebSocket端点供传感器设备连接
- 监控统计：在线车辆数、运行中任务数、实时数据刷新

### 5. 地图监控
- 实际地图显示：使用百度地图API显示实际地图
- 实时车辆标记：在地图上实时显示车辆位置，根据状态显示不同颜色
- PC位置监控：支持PC浏览器位置获取和显示（WGS84坐标系，自动转换为BD09）
- 小程序位置监控：支持微信小程序用户位置上传和显示（GCJ02坐标系，自动转换为BD09）
- 位置标记区分：PC位置和小程序位置使用不同颜色和样式的标记，信息窗口清晰区分
- 坐标系统一：所有位置数据统一转换为BD09坐标系（百度地图标准）显示
- 多设备MQTT数据流转：手机定位 → 车辆定位器 → 网页监控端 → 系统地图
- 华为云「数据转发」HTTP 推送（可选）：平台 `POST` 至 `/api/mqtt/iot-forward-location`，与 MQTT 链路二选一或并存；需配置 `HUAWEI_IOT_FORWARD_WEBHOOK_SECRET`（与控制台 Token 一致），鉴权为华为 `timestamp`/`nonce`/`signature` 签名（详见 `docs/mqtt-huawei-iot-integration-guide.md`）
- 自动视野调整：自动调整地图视野以包含所有车辆和用户位置

### 6. 统计分析
- 仪表盘统计：车辆总数、活跃车辆数、任务统计、任务完成率
- 车辆使用情况排行：车辆使用率统计和排名
- 任务效率统计：任务完成效率分析
- 数据可视化：ECharts图表展示

### 7. 告警管理
- 告警规则配置：创建和管理告警规则
- 告警记录：查看告警历史记录
- 告警处理：告警确认和处理
- 告警关联：告警可关联车辆报告和调度任务
- 邮件通知：维修员可向管理员发送报告邮件，管理员可发送任务分配邮件

### 8. 系统管理
- Redis缓存：使用Redis作为缓存层，提升系统性能
- 系统配置：车辆位置更新间隔、自动任务分配开关、地图服务提供商选择
- 权限设置：角色权限配置，支持权限更新后实时刷新
- 邮件配置：查看已配置邮箱的用户列表，支持邮件测试发送
- 系统日志：操作日志查看和管理

## 快速开始

### 1. 配置数据库

推荐使用 **`backend/.env`**（从 `backend/.env.example` 复制）管理密码与密钥；本地 `backend/src/main/resources/application.yml` 默认 **已被 `.gitignore` 忽略**，不会进入 Git，新克隆仓库时可复制 `application-example.yml` 为 `application.yml` 再按需修改。

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE airport_vehicle_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# 导入初始化脚本
mysql -u root -p airport_vehicle_system < database/init.sql
```

### 3. 配置环境变量

在 `backend` 目录下复制示例并编辑（**勿将填好后的 `.env` 提交到 Git**）：

```bash
cd backend
copy .env.example .env
# Linux/macOS: cp .env.example .env
```

`.env` 中至少配置数据库与 JWT；启用华为 IoT 时再补 MQTT 与 HTTP 转发项，例如：

```env
# MySQL数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=airport_vehicle_system
DB_USERNAME=app_user
DB_PASSWORD=your-password

# JWT配置
JWT_SECRET=your-jwt-secret-key

# 华为云IoT MQTT配置（可选）
HUAWEI_IOT_MQTT_ENABLED=true
HUAWEI_IOT_MQTT_BROKER=ssl://your-broker:8883
HUAWEI_IOT_MQTT_INSTANCE_ID=your-instance-id

# 华为「数据转发」HTTP 推送鉴权（可选，与控制台「转发目标」Token 一致，须符合华为长度与字符集要求）
HUAWEI_IOT_FORWARD_WEBHOOK_SECRET=your-token-3-32-chars-alnum

# 百度地图配置（可选）
BAIDU_MAP_AK=your-baidu-map-ak
```

完整占位说明见 **`backend/.env.example`**。

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在：http://localhost:8080/api

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：http://localhost:3000

### 6. 默认账号

- **用户名**: admin
- **密码**: admin123
- **角色**: 系统管理员（拥有所有权限）

## API文档

启动后端后，访问 Swagger API 文档：
- **Knife4j UI**: http://localhost:8080/api/doc.html
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html

## 权限说明

### 角色权限矩阵

| 功能模块 | ADMIN | DISPATCHER | OPERATOR | MONITOR | VIEWER | DRIVER | MAINTENANCE |
|---------|-------|------------|----------|---------|--------|--------|-------------|
| 用户管理 | 是 | 否 | 是 | 否 | 否 | 否 | 否 |
| 车辆管理 | 是 | 否 | 是 | 否 | 否 | 否 | 是 |
| 任务创建 | 是 | 是 | 否 | 否 | 否 | 否 | 否 |
| 任务分配 | 是 | 是 | 否 | 否 | 否 | 否 | 否 |
| 完成任务 | 是 | 是 | 否 | 否 | 否 | 是 | 是 |
| 实时监控 | 是 | 是 | 是 | 是 | 是 | 否 | 否 |
| 地图监控 | 是 | 是 | 是 | 是 | 是 | 否 | 否 |
| 统计分析 | 是 | 是 | 是 | 是 | 是 | 否 | 否 |
| 告警管理 | 是 | 否 | 否 | 是 | 否 | 否 | 是 |
| 系统配置 | 是 | 否 | 否 | 否 | 否 | 否 | 否 |

## 主要特性

1. **完整的RBAC权限控制**: 前后端双重验证，细粒度权限管理，支持权限实时刷新
2. **小程序支持**: 提供完整的微信小程序接口，支持司机和维修员移动端操作
3. **WebSocket实时定位**: 车辆位置实时推送，支持订阅/取消订阅
4. **Redis缓存** (可选): 提升系统性能，缓存热点数据，自动降级机制
5. **实时数据监控**: 车辆状态、任务进度实时更新，自动刷新机制
6. **智能任务调度**: 优先级排序、自动分配建议，唯一任务编号生成
7. **车辆状态自动管理**: 根据任务状态自动更新车辆状态（正常/维修中/故障）
8. **邮件通知系统**: 支持多种邮件通知类型，邮件测试功能
9. **多地图支持**: 支持百度、高德、腾讯三种地图供应商
10. **响应式设计**: 适配不同屏幕尺寸
11. **多设备MQTT数据流转**: 支持手机定位、车辆定位器、网页监控端数据流转

## 相关仓库

### 微信小程序端

- **GitHub**: [airport-vehicle-system-miniprogarm](https://github.com/Corkedmzx/airport-vehicle-system-miniprogarm.git)
- **Gitee**: [airport-vehicle-system-miniprogarm](https://gitee.com/Corkedmzx/airport-vehicle-system-miniprogarm.git)

小程序端提供司机和维修员移动端操作，支持任务管理、位置上传、问题反馈等功能。

## 文档

### 核心文档
- [架构设计文档](docs/architecture.md) - 系统架构设计和技术选型
- [部署指南](docs/deployment-guide.md) - 详细的部署步骤和配置说明

### 嵌入式终端（ESP32）
- [ESP32 固件（ESP-IDF）](hardware/README.md) - GPS → 华为云 MQTT → 前端地图；**在 `hardware/` 下**执行 `idf.py`；敏感项见 `hardware/.gitignore` 与 **`hardware/sdkconfig.defaults.example`（可提交的无密钥模板）**

### 功能文档
- [功能更新文档](docs/feature-updates.md) - 最新功能更新和改进说明
- [小程序接口使用指南](docs/miniprogram-api-guide.md) - 小程序端API接口使用说明和集成指南
- [环境变量配置指南](docs/environment-configuration.md) - `.env` 与 **`backend/.env.example`** 说明
- [MQTT + 华为云IoT集成指南](docs/mqtt-huawei-iot-integration-guide.md) - MQTT定位数据接入和地图显示
- [多设备MQTT数据流转指南](docs/multi-device-mqtt-integration-guide.md) - 多设备数据流转配置
- [设备激活指南](docs/device-activation-guide.md) - 华为云IoT设备激活
- [MQTTX使用指南](docs/mqttx-usage-guide.md) - MQTTX客户端工具使用说明
- [百度地图配置指南](docs/baidu-map-server-proxy-guide.md) - 百度地图服务端代理配置
- [百度地图浏览器AK指南](docs/create-browser-ak-guide.md) - 创建浏览器端AK
- [邮件配置指南](docs/email-configuration.md) - 邮件发送功能配置

### 测试与安全
- MQTT 与连接问题可参考 [部署指南](docs/deployment-guide.md) 及 [MQTT 集成指南](docs/mqtt-huawei-iot-integration-guide.md)

## 安全与 Git 提交注意

提交代码前请确认 **未包含** 下列内容（仓库已通过 `.gitignore` 尽量排除，合并前仍建议自检）：

| 类型 | 说明 |
|------|------|
| `backend/.env` | 真实数据库密码、JWT、华为 Token、百度 AK 等 |
| `backend/logs/`、`*.log` | 运行日志可能含请求参数、路径或脱敏不全的密钥片段 |
| `backend/target/`、`backend/out/` | Maven 编译产物，体积大且可能含打包进 jar 的本地配置 |
| `**/设备密钥/`、`*.pem`、`*.key` | 华为设备密钥文件、证书 |
| 本地 `application.yml` | 若含非占位符敏感项（该文件默认不跟踪）；团队共享请只用 `application-example.yml` + 环境变量 |
| ngrok 临时域名、生产内网地址 | 可写进个人笔记，避免写进对外文档的固定示例 |

可提交的模板：**`backend/.env.example`**（无真实密钥）、**`application-example.yml`**。生产环境密钥请使用 CI/宿主机的密钥管理或环境注入，勿写入仓库。

## 作者

Corkedmzx

## 许可证

MIT License
