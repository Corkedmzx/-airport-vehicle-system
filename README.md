# 机场车辆监控与调度系统

## 项目概述

基于Java+SpringBoot的机场车辆监控与调度系统，实现对机场内各类车辆的实时监管与智能调度，提升车辆利用效率，保障运行安全。

## 技术栈

- **后端**: Java 17 + SpringBoot 3.2 + Spring Security + Spring Data JPA
- **数据库**: MySQL 8.0
- **前端**: Vue.js 3 + Element Plus + Axios
- **构建工具**: Maven
- **其他**: Redis (缓存) + Swagger (API文档)

## 项目结构

```
airport-vehicle-system/
├── backend/                 # SpringBoot后端
│   ├── src/main/java/
│   │   └── com/airport/
│   │       ├── controller/  # 控制器层
│   │       ├── service/     # 服务层
│   │       ├── repository/  # 数据访问层
│   │       ├── entity/      # 实体类
│   │       ├── dto/         # 数据传输对象
│   │       ├── config/      # 配置类
│   │       └── security/    # 安全配置
│   ├── src/main/resources/
│   │   ├── application.yml  # 应用配置
│   │   └── static/         # 静态资源
│   └── pom.xml             # Maven配置
├── frontend/               # Vue.js前端
│   ├── src/
│   │   ├── components/     # 组件
│   │   ├── views/         # 页面
│   │   ├── router/        # 路由
│   │   ├── api/           # API接口
│   │   └── utils/         # 工具类
│   └── package.json
├── database/              # 数据库脚本
│   ├── init.sql          # 初始化脚本
│   └── migrations/       # 数据迁移
├── docs/                 # 文档
│   ├── api/              # API文档
│   ├── deployment/       # 部署文档
│   └── user-guide/       # 用户手册
└── scripts/              # 脚本文件
```

## 核心功能模块

### 1. 用户管理模块
- 用户注册与登录
- 权限控制
- 角色管理

### 2. 车辆监控模块
- 车辆实时位置监控
- 车辆状态监测
- 轨迹回放

### 3. 调度管理模块
- 调度任务创建
- 任务分配与执行
- 调度策略优化

### 4. 车辆管理模块
- 车辆信息维护
- 车辆档案管理
- 维修记录

### 5. 统计分析模块
- 运行数据统计
- 效率分析报告
- 趋势预测

## 开发进度

- [x] 项目架构设计
- [ ] 后端API开发
- [ ] 前端界面开发
- [ ] 系统集成测试
- [ ] 部署上线

## 运行要求

- Java 17+
- MySQL 8.0+
- Node.js 16+
- Maven 3.6+

## 作者

Corkedmzx

## 许可证

MIT License