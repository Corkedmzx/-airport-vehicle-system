# 快速启动指南

## 📦 系统要求

在开始之前，请确保您的系统已安装：

- ✅ **Java 17+** (JDK)
- ✅ **MySQL 8.0+** 或 **MariaDB 10.11+**
- ✅ **Maven 3.8+** (后端构建)
- ✅ **Node.js 16+** 和 **npm 8+** (前端构建)
- ⚠️ **Redis** (可选，未安装时系统会自动降级到内存缓存)

## 🚀 快速启动步骤

### 第一步：配置数据库

#### 1.1 启动MySQL服务

**Windows:**
- 打开"服务"管理器，找到MySQL服务并启动
- 或在命令行执行：`net start MySQL`

**Linux/macOS:**
```bash
sudo systemctl start mysql
# 或
sudo service mysql start
```

#### 1.2 创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 执行以下SQL命令
CREATE DATABASE airport_vehicle_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

#### 1.3 导入初始数据

```bash
# 在项目根目录执行
mysql -u root -p airport_vehicle_system < database/init.sql
```

### 第二步：配置应用

#### 2.1 配置后端数据库连接

**方式一：直接修改配置文件（推荐）**

1. 打开 `backend/src/main/resources/application.yml`
2. 找到以下配置项并修改为您的数据库信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/airport_vehicle_system?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
    username: root          # 修改为您的MySQL用户名
    password: your_password # 修改为您的MySQL密码
```

**方式二：使用环境变量（高级）**

1. 创建 `.env` 文件（在 `backend` 目录下）：
```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=airport_vehicle_system
DB_USERNAME=root
DB_PASSWORD=your_password
```

2. 使用启动脚本（脚本会自动读取环境变量）

#### 2.2 配置前端API地址（可选）

如果后端运行在不同端口或地址，需要修改：

1. 打开 `frontend/.env.development`（开发环境）或 `frontend/.env.production`（生产环境）
2. 修改 `VITE_API_BASE_URL` 为您的后端地址

### 第三步：启动应用

#### 方式一：使用启动脚本（推荐）

**Windows:**
```bash
# 双击运行或在命令行执行
start-backend.bat
start-frontend.bat
```

**Linux/macOS:**
```bash
chmod +x start-backend.sh start-frontend.sh
./start-backend.sh
./start-frontend.sh
```

#### 方式二：手动启动

**启动后端:**
```bash
cd backend
mvn clean package
java -jar target/airport-vehicle-system-1.0.0.jar
```

**启动前端:**
```bash
cd frontend
npm install
npm run dev
```

### 第四步：访问系统

- **前端地址**: http://localhost:5173
- **后端API**: http://localhost:8080/api
- **API文档**: http://localhost:8080/api/doc.html

**默认登录账号:**
- 用户名: `admin`
- 密码: `admin123`

## ⚠️ 常见问题

### 问题1: 数据库连接失败

**错误信息**: `Communications link failure` 或 `Access denied`

**解决方案:**
1. 检查MySQL服务是否启动
2. 检查数据库用户名和密码是否正确
3. 检查数据库是否已创建
4. 检查防火墙是否阻止了3306端口

### 问题2: 端口被占用

**错误信息**: `Port 8080 is already in use`

**解决方案:**
1. 修改 `application.yml` 中的 `server.port` 为其他端口（如8081）
2. 或关闭占用端口的程序

### 问题3: Redis连接失败

**错误信息**: `Unable to connect to Redis server`

**解决方案:**
- Redis是可选的，如果未安装Redis，系统会自动使用内存缓存
- 如需使用Redis，请安装并启动Redis服务

### 问题4: 前端无法连接后端

**解决方案:**
1. 检查后端是否正常启动
2. 检查 `frontend/.env.development` 中的API地址配置
3. 检查浏览器控制台的错误信息

## 📝 配置说明

### 数据库配置参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `url` | 数据库连接URL | `jdbc:mysql://localhost:3306/airport_vehicle_system...` |
| `username` | 数据库用户名 | `root` |
| `password` | 数据库密码 | 需用户配置 |

### 环境变量配置（可选）

如果使用环境变量，可以在启动脚本中设置：

```bash
# Windows (start-backend.bat)
set DB_USERNAME=root
set DB_PASSWORD=your_password
java -jar target/airport-vehicle-system-1.0.0.jar

# Linux/macOS (start-backend.sh)
export DB_USERNAME=root
export DB_PASSWORD=your_password
java -jar target/airport-vehicle-system-1.0.0.jar
```

## 🔧 生产环境部署

生产环境部署请参考 `docs/deployment-guide.md` 文档。

## 📞 获取帮助

如遇到问题，请：
1. 查看 `docs/deployment-guide.md` 详细部署文档
2. 查看 `docs/system-integrity-check.md` 系统检查报告
3. 检查日志文件：`backend/logs/airport-vehicle-system.log`

---

**祝您使用愉快！** 🎉

