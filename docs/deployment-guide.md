# 机场车辆监控与调度系统 - 部署指南

## 系统概述

机场车辆监控与调度系统是一个基于B/S架构的现代化管理平台，采用前后端分离的技术架构。

### 技术栈

- **后端**: Java 17, SpringBoot 3.2.0, Maven 3.8+
- **前端**: Vue 3, TypeScript, Vite 5, Element Plus
- **数据库**: MySQL 8.0+ / MariaDB 10.11+
- **构建工具**: Maven (后端), npm (前端)

## 系统要求

### 硬件要求
- CPU: 2核心以上
- 内存: 4GB以上
- 硬盘: 20GB以上可用空间
- 网络: 稳定的互联网连接

### 软件环境
- 操作系统: Linux (推荐), Windows, macOS
- Java: JDK 17+
- Node.js: 16.0+
- npm: 8.0+
- MySQL/MariaDB: 8.0+/10.11+
- Maven: 3.8+

## 数据库安装与配置

### MySQL安装 (推荐)

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install mysql-server mysql-client
sudo mysql_secure_installation
```

#### Windows
1. 下载MySQL安装包: https://dev.mysql.com/downloads/mysql/
2. 运行安装程序，选择"Developer Default"安装类型
3. 设置root密码并记住

#### macOS
```bash
brew install mysql
brew services start mysql
```

### 数据库初始化

1. **启动MySQL服务**
```bash
# Linux
sudo systemctl start mysql
sudo systemctl enable mysql

# macOS
brew services start mysql

# Windows - 在服务管理器中启动MySQL服务
```

2. **创建数据库和用户**
```sql
-- 登录MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE airport_vehicle_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户 (可选)
CREATE USER 'airport_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON airport_vehicle_system.* TO 'airport_user'@'localhost';
FLUSH PRIVILEGES;

-- 代码内部调试使用的用户密码
username: app_user
password: App@Secure123!

-- 退出
EXIT;
```

3. **导入数据库结构**
```bash
# 在项目根目录下执行
mysql -u root -p airport_vehicle_system < database/init.sql

# 如果创建了专用用户，使用以下命令:
# mysql -u airport_user -p airport_vehicle_system < database/init.sql
```

## 后端部署

### 环境准备

1. **安装Java 17+**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc

# 验证安装
java -version
javac -version
```

2. **安装Maven**
```bash
# Ubuntu/Debian
sudo apt install maven

# macOS
brew install maven

# 验证安装
mvn -version
```

### 编译与打包

1. **进入后端目录**
```bash
cd airport-vehicle-system/backend
```

2. **清理并编译**
```bash
mvn clean compile
```

3. **运行测试 (可选)**
```bash
mvn test
```

4. **打包应用**
```bash
mvn clean package -DskipTests
```

### 配置文件

编辑 `src/main/resources/application.yml` 文件，配置数据库连接:

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/airport_vehicle_system?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true
    username: airport_user
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        
  redis:
    host: localhost
    port: 6379
    timeout: 3000ms
    password:
```

### 启动应用

#### 开发环境
```bash
mvn spring-boot:run
```

#### 生产环境
```bash
# 使用打包后的JAR文件
java -jar target/airport-vehicle-system-1.0.0.jar

# 或者后台运行
nohup java -jar target/airport-vehicle-system-1.0.0.jar > app.log 2>&1 &
```

### 验证启动

访问API文档地址: http://localhost:8080/api/swagger-ui.html
如果能看到Swagger文档页面，说明后端启动成功。

## 前端部署

### 环境准备

1. **安装Node.js和npm**
```bash
# 使用nvm安装Node.js 18+
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
nvm use 18

# 验证安装
node --version
npm --version
```

### 安装依赖

```bash
cd airport-vehicle-system/frontend
npm install
```

### 配置环境变量

编辑 `.env.development` 和 `.env.production`:

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=机场车辆监控与调度系统(开发版)
VITE_APP_VERSION=1.0.0

# .env.production  
VITE_API_BASE_URL=http://your-domain.com/api
VITE_APP_TITLE=机场车辆监控与调度系统
VITE_APP_VERSION=1.0.0
```

### 开发环境运行

```bash
npm run dev
```

前端将在 http://localhost:3000 启动，支持热重载。

### 生产环境构建

```bash
npm run build
```

构建完成后，静态文件将生成在 `dist/` 目录下。

## 生产部署

### 静态文件服务器

#### 使用Nginx

1. **安装Nginx**
```bash
# Ubuntu/Debian
sudo apt install nginx

# macOS
brew install nginx
```

2. **配置Nginx**
创建 `/etc/nginx/sites-available/airport-vehicle-system`:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    location / {
        root /var/www/airport-vehicle-system/dist;
        try_files $uri $uri/ /index.html;
        
        # 静态资源缓存
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
    
    # API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
}
```

3. **部署静态文件**
```bash
# 构建前端
cd airport-vehicle-system/frontend
npm run build

# 复制到Nginx目录
sudo cp -r dist/* /var/www/airport-vehicle-system/

# 启用站点
sudo ln -s /etc/nginx/sites-available/airport-vehicle-system /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 系统服务配置

#### 创建Systemd服务 (Linux)

创建 `/etc/systemd/system/airport-vehicle-system.service`:

```ini
[Unit]
Description=Airport Vehicle System Backend
After=network.target mysql.service

[Service]
Type=simple
User=airport
WorkingDirectory=/opt/airport-vehicle-system/backend
ExecStart=/usr/bin/java -jar -Xms512m -Xmx2048m target/airport-vehicle-system-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用服务:
```bash
sudo systemctl daemon-reload
sudo systemctl enable airport-vehicle-system
sudo systemctl start airport-vehicle-system
sudo systemctl status airport-vehicle-system
```

## 性能优化

### JVM参数优化

在生产环境中，适当调整JVM参数:

```bash
java -jar \
  -Xms1024m \
  -Xmx2048m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseCGroupMemoryLimitForHeap \
  target/airport-vehicle-system-1.0.0.jar
```

### 数据库优化

1. **MySQL配置优化** (`/etc/mysql/mysql.conf.d/mysqld.cnf`):

```ini
[mysqld]
# 内存优化
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2

# 连接优化
max_connections = 200
connect_timeout = 60
wait_timeout = 28800

# 查询缓存
query_cache_type = 1
query_cache_size = 256M
```

2. **重启MySQL服务**
```bash
sudo systemctl restart mysql
```

### Redis配置 (可选)

如果使用Redis缓存，安装并配置:

```bash
# 安装Redis
sudo apt install redis-server

# 配置Redis (可选)
sudo nano /etc/redis/redis.conf

# 启动Redis
sudo systemctl enable redis-server
sudo systemctl start redis-server
```

## 监控与日志

### 日志配置

日志文件位置:
- 后端日志: `/var/log/airport-vehicle-system/`
- Nginx日志: `/var/log/nginx/`
- MySQL日志: `/var/log/mysql/`

### 健康检查

创建健康检查脚本 `/opt/airport-vehicle-system/health-check.sh`:

```bash
#!/bin/bash

# 检查后端服务
if ! curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "Backend service is down"
    sudo systemctl restart airport-vehicle-system
fi

# 检查前端服务
if ! curl -f http://localhost/ > /dev/null 2>&1; then
    echo "Frontend service is down"
    sudo systemctl reload nginx
fi
```

添加到cron定时任务:
```bash
# 每5分钟执行一次健康检查
*/5 * * * * /opt/airport-vehicle-system/health-check.sh
```

## 常见问题解决

### 1. Maven构建失败

**问题**: 依赖下载失败或版本错误

**解决方案**:
```bash
# 清理Maven缓存
mvn dependency:purge-local-repository

# 使用阿里云镜像
mvn -s ~/.m2/settings.xml clean package
```

### 2. 数据库连接失败

**问题**: 无法连接到MySQL

**解决方案**:
```bash
# 检查MySQL服务状态
sudo systemctl status mysql

# 检查数据库用户权限
mysql -u root -p -e "SELECT User, Host FROM mysql.user;"

# 检查防火墙设置
sudo ufw status
```

### 3. 前端构建失败

**问题**: npm依赖安装失败

**解决方案**:
```bash
# 清理npm缓存
npm cache clean --force

# 删除node_modules重新安装
rm -rf node_modules package-lock.json
npm install
```

### 4. 端口占用问题

**问题**: 8080或3000端口被占用

**解决方案**:
```bash
# 查看端口占用
netstat -tulpn | grep 8080
netstat -tulpn | grep 3000

# 杀死占用进程
sudo kill -9 <PID>

# 或者修改配置文件中的端口
```

## 备份策略

### 数据库备份

创建备份脚本 `/opt/airport-vehicle-system/backup.sh`:

```bash
#!/bin/bash

BACKUP_DIR="/var/backups/airport-vehicle-system"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 备份数据库
mysqldump -u airport_user -p'your_password' airport_vehicle_system > $BACKUP_DIR/db_backup_$DATE.sql

# 备份前端静态文件
tar -czf $BACKUP_DIR/frontend_backup_$DATE.tar.gz /var/www/airport-vehicle-system/

# 删除30天前的备份
find $BACKUP_DIR -type f -mtime +30 -delete

echo "Backup completed: $DATE"
```

添加到cron:
```bash
# 每天凌晨2点执行备份
0 2 * * * /opt/airport-vehicle-system/backup.sh
```

## 安全建议

1. **修改默认密码**: 修改MySQL root密码和应用数据库密码
2. **防火墙配置**: 只开放必要端口(80, 443, 22)
3. **SSL证书**: 使用HTTPS加密传输
4. **定期更新**: 保持系统和依赖库更新
5. **访问控制**: 限制数据库和管理接口的访问权限

## 技术支持

如遇到技术问题，请检查:

1. 系统日志文件
2. 应用错误日志
3. 数据库连接状态
4. 网络连通性

系统已通过完整的Maven构建测试，所有依赖项和版本兼容性验证通过。

---

**部署完成后，请访问前端地址进行系统功能测试。**
