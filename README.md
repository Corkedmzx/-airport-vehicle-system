# 机场车辆监控与调度系统

## 项目概述

基于Java+SpringBoot的机场车辆监控与调度系统，实现对机场内各类车辆的实时监管与智能调度，提升车辆利用效率，保障运行安全。系统采用前后端分离架构，提供完整的RBAC权限控制、实时监控、智能调度和数据分析功能。

## 技术栈

- **后端**: Java 17 + SpringBoot 3.2 + Spring Security + Spring Data JPA + JWT
- **数据库**: MySQL 8.0 + Redis (缓存)
- **前端**: Vue.js 3 + TypeScript + Element Plus + Vite + Pinia + ECharts
- **构建工具**: Maven (后端) + npm (前端)
- **API文档**: Swagger/OpenAPI 3 (Knife4j)
- **实时通信**: WebSocket (实时定位、消息推送)
- **其他**: BCrypt密码加密、响应式设计

## 核心功能模块

### 1. 用户认证与权限管理
- 用户注册与登录：JWT Token认证机制
- 密码管理：BCrypt加密存储，支持修改密码
- 角色权限控制(RBAC)：7种角色，细粒度权限控制
- 个人资料管理：查看和修改个人信息、头像、密码

### 2. 车辆管理
- 车辆信息管理：车辆CRUD操作
- 车辆状态监控：正常运行、维护中、故障、离线等状态
- 车辆类型管理：行李车、摆渡车、货运车、清洁车、维修车、巡逻车
- 车辆详情：查看车辆详细信息、历史记录

### 3. 任务调度
- 任务创建：创建调度任务，设置优先级
- 任务分配：手动分配任务到车辆，取消分配功能
- 任务状态管理：待分配 → 已分配 → 执行中 → 已完成
- 调度中心：实时调度概览、可用车辆展示、任务分配界面

### 4. 实时监控
- 车辆实时监控：实时显示车辆位置、状态、速度等信息
- WebSocket实时定位：前端通过WebSocket连接接收实时位置更新
- 传感器接口预留：提供WebSocket端点供传感器设备连接
- 监控统计：在线车辆数、运行中任务数、实时数据刷新

### 5. 地图监控
- 实际地图显示：使用百度地图API显示实际地图
- 实时车辆标记：在地图上实时显示车辆位置，根据状态显示不同颜色
- PC位置监控：支持PC浏览器位置获取和显示
- 多设备MQTT数据流转：手机定位 → 车辆定位器 → 网页监控端 → 系统地图
- 自动视野调整：自动调整地图视野以包含所有车辆

### 6. 统计分析
- 仪表盘统计：车辆总数、活跃车辆数、任务统计、任务完成率
- 车辆使用情况排行：车辆使用率统计和排名
- 任务效率统计：任务完成效率分析
- 数据可视化：ECharts图表展示

### 7. 告警管理
- 告警规则配置：创建和管理告警规则
- 告警记录：查看告警历史记录
- 告警处理：告警确认和处理

### 8. 系统管理
- Redis缓存：使用Redis作为缓存层，提升系统性能
- 系统配置：车辆位置更新间隔、自动任务分配开关、地图服务提供商选择
- 权限设置：角色权限配置
- 系统日志：操作日志查看和管理

## 快速开始

### 1. 配置数据库

编辑 `backend/src/main/resources/application.yml`，修改数据库配置，或使用 `.env` 文件配置（推荐）。

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

在 `backend` 目录下创建 `.env` 文件：

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

# 百度地图配置（可选）
BAIDU_MAP_AK=your-baidu-map-ak
```

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

前端默认运行在：http://localhost:5173

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
| 实时监控 | 是 | 是 | 是 | 是 | 是 | 否 | 否 |
| 地图监控 | 是 | 是 | 是 | 是 | 是 | 否 | 否 |
| 统计分析 | 是 | 是 | 是 | 是 | 是 | 否 | 否 |
| 告警管理 | 是 | 否 | 否 | 是 | 否 | 否 | 否 |
| 系统配置 | 是 | 否 | 否 | 否 | 否 | 否 | 否 |

## 主要特性

1. **完整的RBAC权限控制**: 前后端双重验证，细粒度权限管理
2. **WebSocket实时定位**: 车辆位置实时推送，支持订阅/取消订阅
3. **Redis缓存** (可选): 提升系统性能，缓存热点数据，自动降级机制
4. **实时数据监控**: 车辆状态、任务进度实时更新
5. **智能任务调度**: 优先级排序、自动分配建议
6. **多地图支持**: 支持百度、高德、腾讯三种地图供应商
7. **响应式设计**: 适配不同屏幕尺寸
8. **多设备MQTT数据流转**: 支持手机定位、车辆定位器、网页监控端数据流转

## 文档

### 核心文档
- [架构设计文档](docs/architecture.md) - 系统架构设计和技术选型
- [部署指南](docs/deployment-guide.md) - 详细的部署步骤和配置说明

### 功能文档
- [环境变量配置指南](docs/environment-configuration.md) - 环境变量配置说明
- [MQTT + 华为云IoT集成指南](docs/mqtt-huawei-iot-integration-guide.md) - MQTT定位数据接入和地图显示
- [多设备MQTT数据流转指南](docs/multi-device-mqtt-integration-guide.md) - 多设备数据流转配置
- [设备激活指南](docs/device-activation-guide.md) - 华为云IoT设备激活
- [MQTTX使用指南](docs/mqttx-usage-guide.md) - MQTTX客户端工具使用说明
- [百度地图配置指南](docs/baidu-map-server-proxy-guide.md) - 百度地图服务端代理配置
- [百度地图浏览器AK指南](docs/create-browser-ak-guide.md) - 创建浏览器端AK
- [邮件配置指南](docs/email-configuration.md) - 邮件发送功能配置

### 测试与安全
- [Git安全配置指南](docs/git-security-guide.md) - Git提交前的安全检查
- [MQTT故障排除](docs/troubleshooting-mqtt-connection.md) - MQTT连接问题排查

## 作者

Corkedmzx

## 许可证

MIT License
