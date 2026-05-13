# 环境变量配置指南

## 功能说明

系统支持自动从 `.env` 文件加载环境变量，直接运行 `mvn spring-boot:run` 即可自动连接数据库和华为云IoT，无需手动设置环境变量。

## 使用方法

### 步骤1：创建 .env 文件

在 `backend` 目录下创建 `.env` 文件：

```powershell
cd backend
# 如果文件不存在，复制示例文件
Copy-Item .env.example .env
```

### 步骤2：编辑 .env 文件

打开 `backend/.env` 文件，填写实际配置值：

```env
# MySQL数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=airport_vehicle_system
DB_USERNAME=app_user
DB_PASSWORD=your-password

# JWT配置
JWT_SECRET=your-jwt-secret-key

# 华为云IoT MQTT配置
HUAWEI_IOT_MQTT_ENABLED=true
HUAWEI_IOT_MQTT_BROKER=ssl://your-broker:8883
HUAWEI_IOT_MQTT_INSTANCE_ID=your-instance-id

# 百度地图配置
BAIDU_MAP_AK=your-baidu-map-ak
```

### 步骤3：启动服务

直接运行 Maven 命令：

```powershell
cd backend
mvn spring-boot:run
```

系统会自动读取 `.env` 文件并设置所有环境变量。

## 工作原理

系统在启动时会自动执行 `DotEnvConfig` 配置类，在 Spring Boot 启动的早期阶段加载 `.env` 文件，将变量设置到系统属性中。

配置优先级：
1. 系统环境变量（最高优先级）
2. 系统属性（从 .env 文件加载）
3. 配置文件默认值（application.yml 中的默认值）

## .env 文件格式

`.env` 文件使用简单的 `KEY=VALUE` 格式：

```env
# 注释以 # 开头
DB_HOST=localhost
DB_PASSWORD=your-password
```

注意事项：
- 每行一个环境变量
- 格式：`KEY=VALUE`（等号两边可以有空格）
- 空行和以 `#` 开头的行会被忽略

## 环境变量列表

### 必需的环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_PASSWORD` | 数据库密码 | `your-database-password` |
| `JWT_SECRET` | JWT密钥 | `your-secret-key` |

### 可选的环境变量

#### MySQL数据库配置
- `DB_HOST` - 数据库主机（默认：`localhost`）
- `DB_PORT` - 数据库端口（默认：`3306`）
- `DB_NAME` - 数据库名称（默认：`airport_vehicle_system`）
- `DB_USERNAME` - 数据库用户名（默认：`app_user`）

#### 华为云IoT MQTT配置
- `HUAWEI_IOT_MQTT_ENABLED` - 是否启用MQTT（默认：`false`）
- `HUAWEI_IOT_MQTT_BROKER` - MQTT Broker地址
- `HUAWEI_IOT_MQTT_INSTANCE_ID` - 实例ID
- `HUAWEI_IOT_MQTT_DEVICE_KEY_PATH` - 设备密钥文件路径

#### 华为「数据转发」HTTP 推送（可选）
- `HUAWEI_IOT_FORWARD_WEBHOOK_SECRET` - 与控制台「转发目标」Token **完全一致**；用于校验华为推送的 `signature`（或调试用 `X-IoT-Webhook-Token`）。勿提交到 Git，仅写在 `backend/.env` 或部署环境变量中。

#### 百度地图配置
- `BAIDU_MAP_AK` - 百度地图API密钥

## 安全性

1. **`backend/.env`** 与仓库根目录 **`.env`** 已加入 `.gitignore`，不会被提交。
2. **`backend/.env.example`** 为无敏感值的模板，**可以且应当**提交；新成员复制为 `.env` 后本地填写。
3. **`backend/src/main/resources/application.yml`** 默认被忽略（每人本地一份）；团队共享请使用 **`application-example.yml`** + `${环境变量}`，勿在可提交 YAML 中写生产密码或华为 Token。
4. **`backend/logs/`**、`*.log` 已忽略；日志可能含 URL、设备 ID 等，发布前勿打包进仓库。

## 故障排除

### 问题1：找不到 .env 文件

**解决方法**：
1. 确认 `.env` 文件存在于 `backend` 目录
2. 如果不存在，复制 `.env.example` 为 `.env`

### 问题2：环境变量未生效

**可能原因**：
- `.env` 文件格式错误
- 系统环境变量已存在（优先级更高）

**解决方法**：
1. 检查 `.env` 文件格式是否正确
2. 查看启动日志，确认加载的变量数量

### 问题3：数据库连接失败

**解决方法**：
1. 检查 `.env` 文件中的 `DB_PASSWORD` 是否正确
2. 检查数据库用户是否存在和权限是否足够

---

**最后更新**：2026-05-13
