# 机场车辆监控与调度系统

## 项目概述

基于Java+SpringBoot的机场车辆监控与调度系统，实现对机场内各类车辆的实时监管与智能调度，提升车辆利用效率，保障运行安全。系统采用前后端分离架构，提供完整的RBAC权限控制、实时监控、智能调度和数据分析功能。

## 技术栈

- **后端**: Java 17 + SpringBoot 3.2 + Spring Security + Spring Data JPA + JWT
- **数据库**: MySQL 8.0
- **前端**: Vue.js 3 + TypeScript + Element Plus + Vite + Pinia + ECharts
- **构建工具**: Maven (后端) + npm (前端)
- **API文档**: Swagger/OpenAPI 3 (Knife4j)
- **其他**: BCrypt密码加密、响应式设计

## 项目结构

```
airport-vehicle-system/
├── backend/                 # SpringBoot后端
│   ├── src/main/java/
│   │   └── com/airport/
│   │       ├── controller/  # REST API控制器
│   │       │   ├── AuthController.java          # 认证控制器
│   │       │   ├── UserController.java          # 用户管理
│   │       │   ├── VehicleController.java       # 车辆管理
│   │       │   ├── DispatchTaskController.java  # 任务调度
│   │       │   ├── StatisticsController.java    # 数据统计
│   │       │   ├── AlertController.java         # 告警管理
│   │       │   ├── SystemConfigController.java  # 系统配置
│   │       │   └── ...
│   │       ├── service/     # 业务逻辑层
│   │       ├── repository/  # 数据访问层
│   │       ├── entity/      # 实体类
│   │       ├── dto/         # 数据传输对象
│   │       ├── config/      # 配置类
│   │       └── utils/       # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml  # 应用配置
│   │   └── static/         # 静态资源
│   └── pom.xml             # Maven配置
├── frontend/               # Vue.js前端
│   ├── src/
│   │   ├── views/         # 页面视图
│   │   │   ├── Dashboard/      # 仪表盘
│   │   │   ├── Vehicles/       # 车辆管理
│   │   │   ├── Tasks/          # 任务管理
│   │   │   ├── Dispatch/       # 调度中心
│   │   │   ├── Monitoring/     # 实时监控
│   │   │   ├── Map/            # 地图监控
│   │   │   ├── Statistics/     # 统计分析
│   │   │   ├── Alerts/         # 告警管理
│   │   │   ├── Users/          # 用户管理
│   │   │   ├── Settings/       # 系统设置
│   │   │   ├── Profile/        # 个人资料
│   │   │   └── Logs/          # 系统日志
│   │   ├── api/           # API接口
│   │   ├── router/        # 路由配置
│   │   ├── store/         # 状态管理
│   │   ├── utils/         # 工具函数
│   │   └── layout/        # 布局组件
│   └── package.json
├── database/              # 数据库脚本
│   └── init.sql          # 初始化脚本（包含表结构、初始数据、权限配置）
├── docs/                 # 文档
│   ├── architecture.md        # 架构设计文档
│   ├── deployment-guide.md    # 部署指南
│   ├── system-integrity-check.md  # 系统完整性检查
│   └── project-delivery-summary.md # 项目交付总结
└── README.md             # 项目说明
```

## 核心功能模块

### 1. 用户认证与权限管理模块
- ✅ **用户注册与登录**: JWT Token认证机制
- ✅ **密码管理**: BCrypt加密存储，支持修改密码（需验证旧密码）
- ✅ **角色权限控制(RBAC)**: 
  - 7种角色：系统管理员(ADMIN)、调度员(DISPATCHER)、操作员(OPERATOR)、监控员(MONITOR)、查看者(VIEWER)、司机(DRIVER)、维修员(MAINTENANCE)
  - 细粒度权限控制：user:view/create/update/delete、vehicle:view/create/update/delete、task:view/create/update/delete/assign等
  - 前后端双重权限验证
- ✅ **个人资料管理**: 查看和修改个人信息、头像、密码
- ✅ **用户管理**: 用户CRUD、角色分配、状态管理（仅管理员）

### 2. 车辆管理模块
- ✅ **车辆信息管理**: 车辆CRUD操作
- ✅ **车辆状态监控**: 
  - 正常运行、维护中、故障、离线、执行中、已分配、空闲
  - 状态优先级显示（维护/故障/离线 > 任务相关状态）
- ✅ **车辆类型管理**: 行李车、摆渡车、货运车、清洁车、维修车、巡逻车
- ✅ **车辆详情**: 查看车辆详细信息、历史记录
- ✅ **权限控制**: 基于权限的按钮显示和操作控制

### 3. 任务调度模块
- ✅ **任务创建**: 创建调度任务，设置优先级
- ✅ **任务分配**: 
  - 手动分配任务到车辆
  - 取消分配功能
  - 权限验证（task:assign）
- ✅ **任务状态管理**: 
  - 待分配(1) → 已分配(2) → 执行中(3) → 已完成(4)
  - 任务取消、重新发送
- ✅ **任务排序**: 
  - 待分配任务按优先级降序
  - 已分配和执行中任务按优先级降序
  - 已完成任务按优先级降序
  - 整体顺序：待分配 > (已分配,执行中) > 已完成
- ✅ **调度中心**: 实时调度概览、可用车辆展示、任务分配界面

### 4. 实时监控模块
- ✅ **车辆实时监控**: 实时显示车辆位置、状态、速度等信息
- ✅ **监控统计**: 在线车辆数、运行中任务数、实时数据刷新
- ✅ **车辆筛选**: 按状态、任务筛选车辆
- ✅ **数据可视化**: 实时数据图表展示

### 5. 地图监控模块
- ✅ **多地图供应商支持**: 
  - 百度地图（BD-09坐标系）
  - 高德地图（GCJ-02坐标系）
  - 腾讯地图（GCJ-02坐标系）
- ✅ **地图切换**: 系统配置中切换地图供应商，地图监控页面自动适配
- ✅ **车辆定位**: 在地图上查看车辆位置
- ✅ **地图样式**: 支持街道、卫星、混合模式

### 6. 统计分析模块
- ✅ **仪表盘统计**: 
  - 车辆总数、活跃车辆数
  - 任务总数、今日任务、进行中任务、已完成任务
  - 任务完成率
  - 最近任务列表（按优先级排序）
- ✅ **车辆使用情况排行**: 车辆使用率统计和排名
- ✅ **任务效率统计**: 任务完成效率分析
- ✅ **数据可视化**: ECharts图表展示
- ✅ **实时数据**: 数据实时刷新

### 7. 告警管理模块
- ✅ **告警规则配置**: 创建和管理告警规则
- ✅ **告警记录**: 查看告警历史记录
- ✅ **告警处理**: 告警确认和处理

### 8. 系统管理模块
- ✅ **系统配置**: 
  - 车辆位置更新间隔
  - 自动任务分配开关
  - 维护提醒提前天数
  - 地图服务提供商选择（百度/高德/腾讯）
  - 位置追踪开关
- ✅ **权限设置**: 
  - 角色权限配置（弹窗式勾选）
  - 权限查看（显示格式：√权限名称）
  - 仅管理员可操作
- ✅ **系统日志**: 操作日志查看和管理
- ✅ **用户管理**: 用户列表、角色分配、状态管理

### 9. 响应式设计
- ✅ **自适应布局**: 支持1920x1080及以下分辨率
- ✅ **响应式组件**: 表格、卡片、表单自适应
- ✅ **移动端适配**: 基础移动端支持

## 开发进度

- [x] 项目架构设计
- [x] 后端API开发
- [x] 前端界面开发
- [x] 权限控制系统
- [x] 地图监控功能
- [x] 个人资料管理
- [x] 系统配置管理
- [x] 响应式设计
- [x] 系统集成测试
- [x] 文档完善

## 运行要求

### 后端要求
- Java 17+
- Maven 3.6+
- MySQL 8.0+ 或 MariaDB 10.11+

### 前端要求
- Node.js 16+
- npm 8.0+ 或 yarn

## 快速开始

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE airport_vehicle_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入初始化脚本
mysql -u root -p airport_vehicle_system < database/init.sql
```

### 2. 后端启动

```bash
cd backend
# 修改 application.yml 中的数据库配置
mvn clean package
java -jar target/airport-vehicle-system-1.0.0.jar
```

后端默认运行在：http://localhost:8080/api

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：http://localhost:3000

### 4. 默认账号

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
| 用户管理 | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| 车辆管理 | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ |
| 任务创建 | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 任务分配 | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 实时监控 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 地图监控 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 统计分析 | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| 告警管理 | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| 系统配置 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

## 主要特性

1. **完整的RBAC权限控制**: 前后端双重验证，细粒度权限管理
2. **实时数据监控**: 车辆状态、任务进度实时更新
3. **智能任务调度**: 优先级排序、自动分配建议
4. **多地图支持**: 支持百度、高德、腾讯三种地图供应商
5. **响应式设计**: 适配不同屏幕尺寸
6. **完善的用户管理**: 个人资料、密码修改、权限查看
7. **系统配置管理**: 灵活的配置项管理
8. **数据统计分析**: 多维度数据统计和可视化

## 技术亮点

- **前后端分离**: RESTful API设计，职责清晰
- **JWT认证**: 无状态认证，支持分布式部署
- **权限控制**: 基于角色的访问控制，前后端双重验证
- **响应式设计**: 使用CSS变量、clamp()、媒体查询实现自适应
- **代码质量**: TypeScript类型检查、统一代码规范
- **安全性**: BCrypt密码加密、SQL注入防护、XSS防护

## 文档

- [架构设计文档](docs/architecture.md)
- [部署指南](docs/deployment-guide.md)
- [系统完整性检查](docs/system-integrity-check.md)
- [项目交付总结](docs/project-delivery-summary.md)

## 作者

Corkedmzx

## 许可证

MIT License
