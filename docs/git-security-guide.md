# Git安全配置指南

## 重要安全提醒

**在将项目上传到Git之前，请务必检查并处理以下敏感信息！**

## 1. 敏感信息检查清单

### 已安全处理的项目

1. **邮箱账号和授权码**
   - 存储在数据库中（`sys_user`表的`email`和`email_auth_code`字段）
   - 不在配置文件中硬编码
   - 不会提交到Git

2. **数据库密码**
   - 已改为使用环境变量 `${DB_PASSWORD}`
   - 默认值为占位符 `your-database-password`

3. **JWT密钥**
   - 已改为使用环境变量 `${JWT_SECRET}`
   - 默认值为占位符 `your-jwt-secret-key-change-this-in-production`

4. **Druid监控密码**
   - 已改为使用环境变量 `${DRUID_PASSWORD}`
   - 默认值为占位符 `your-druid-password`

### 需要检查的文件

1. **`backend/src/main/resources/application.yml`**
   - 确保所有敏感信息都使用环境变量
   - 不要包含真实的密码、密钥、API Key

2. **`database/init.sql`**
   - 只包含测试数据（如 `admin@airport.com`）
   - 不包含真实的邮箱和授权码

3. **日志文件**
   - 已添加到 `.gitignore`
   - 确保不会提交日志文件（可能包含敏感信息）

## 2. 环境变量配置

### 创建 `.env` 文件（不提交到Git）

在项目根目录创建 `.env` 文件：

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=airport_vehicle_system
DB_USERNAME=app_user
DB_PASSWORD=your-actual-database-password

# Druid监控
DRUID_USERNAME=admin
DRUID_PASSWORD=your-actual-druid-password

# JWT密钥（生产环境必须使用强随机密钥）
JWT_SECRET=your-strong-random-jwt-secret-key-min-32-characters

# Redis配置（如果设置了密码）
REDIS_PASSWORD=your-redis-password

# 地图API密钥
BAIDU_MAP_AK=your-baidu-map-api-key
GAODE_MAP_KEY=your-gaode-map-api-key
TENCENT_MAP_KEY=your-tencent-map-api-key
```

### 使用环境变量

#### Windows PowerShell
```powershell
# 设置环境变量
$env:DB_PASSWORD="your-actual-password"
$env:JWT_SECRET="your-jwt-secret"

# 启动应用
cd backend
mvn spring-boot:run
```

#### Linux/macOS
```bash
# 设置环境变量
export DB_PASSWORD="your-actual-password"
export JWT_SECRET="your-jwt-secret"

# 启动应用
cd backend
mvn spring-boot:run
```

#### 使用 .env 文件（推荐）

安装 `dotenv-cli`：
```bash
npm install -g dotenv-cli
```

启动应用：
```bash
cd backend
dotenv -e ../.env mvn spring-boot:run
```

## 3. .gitignore 配置

项目已包含 `.gitignore` 文件，确保以下内容不会被提交：

- `backend/target/` - 编译输出
- `backend/logs/` - 日志文件
- `*.log` - 所有日志文件
- `.env` - 环境变量文件
- `application-local.yml` - 本地配置
- `application-prod.yml` - 生产配置
- `frontend/node_modules/` - 前端依赖
- `frontend/dist/` - 前端构建输出

## 4. 上传前检查步骤

### 步骤1：检查敏感信息

```bash
# 检查配置文件中是否包含真实密码
grep -r "password.*=" backend/src/main/resources/application.yml
grep -r "secret.*=" backend/src/main/resources/application.yml
grep -r "@163.com\|@qq.com\|@gmail.com" backend/src/main/resources/
```

### 步骤2：检查Git状态

```bash
# 查看将要提交的文件
git status

# 查看文件内容，确认没有敏感信息
git diff
```

### 步骤3：检查.gitignore

```bash
# 确认.gitignore已正确配置
cat .gitignore
```

### 步骤4：测试构建

```bash
# 使用环境变量测试构建
cd backend
mvn clean compile -DskipTests
```

## 5. 数据库安全

### 邮箱和授权码存储

- **邮箱地址**：存储在 `sys_user.email` 字段
- **授权码**：存储在 `sys_user.email_auth_code` 字段
- **加密建议**：生产环境建议对授权码进行加密存储

### 数据库备份安全

```bash
# 备份时排除敏感数据
mysqldump -u user -p airport_vehicle_system \
  --ignore-table=airport_vehicle_system.sys_user \
  > backup_without_users.sql

# 单独备份用户表（加密）
mysqldump -u user -p airport_vehicle_system sys_user \
  | openssl enc -aes-256-cbc -salt -out users_encrypted.sql.enc
```

## 6. 生产环境安全建议

### 1. 使用配置中心

- 使用 Spring Cloud Config
- 使用 Nacos、Apollo 等配置中心
- 敏感信息存储在配置中心，不放在代码仓库

### 2. 数据库加密

- 对 `email_auth_code` 字段进行加密存储
- 使用 AES 加密或数据库加密功能

### 3. 密钥管理

- 使用密钥管理服务（如 AWS KMS、阿里云KMS）
- JWT密钥定期轮换
- 使用强随机密钥（至少32字符）

### 4. 访问控制

- 限制数据库访问IP
- 使用最小权限原则
- 定期审计访问日志

### 5. 代码审查

- 提交前进行代码审查
- 使用 Git Hooks 检查敏感信息
- 使用工具扫描敏感信息（如 git-secrets）

## 7. 如果已经提交了敏感信息

### 立即处理

1. **更改所有泄露的密码和密钥**
   - 数据库密码
   - JWT密钥
   - 邮箱授权码
   - API密钥

2. **从Git历史中删除**

```bash
# 使用 git-filter-branch 删除历史记录中的敏感信息
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch backend/src/main/resources/application.yml" \
  --prune-empty --tag-name-filter cat -- --all

# 强制推送（危险操作，会重写历史）
git push origin --force --all
```

3. **使用 BFG Repo-Cleaner（推荐）**

```bash
# 安装BFG
# 下载：https://rtyley.github.io/bfg-repo-cleaner/

# 删除包含敏感信息的文件
java -jar bfg.jar --delete-files application.yml

# 清理Git历史
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

## 8. 安全检查工具

### 使用 git-secrets

```bash
# 安装
git clone https://github.com/awslabs/git-secrets.git
cd git-secrets
sudo make install

# 配置
cd /path/to/your/repo
git secrets --install
git secrets --register-aws

# 添加自定义模式
git secrets --add 'password.*=.*[^your-]'
git secrets --add '@163\.com|@qq\.com|@gmail\.com'
```

### 使用 truffleHog

```bash
# 扫描敏感信息
trufflehog git file://. --json
```

## 总结

### 当前项目安全状态

1. **邮箱和授权码**：安全（存储在数据库，不提交到Git）
2. **配置文件**：已改为使用环境变量
3. **.gitignore**：已配置，排除敏感文件
4. **文档**：只包含示例，无真实敏感信息

### 上传前必做

1. 确认 `application.yml` 中所有敏感信息都使用环境变量
2. 确认 `.gitignore` 已正确配置
3. 确认没有真实的邮箱和授权码在代码中
4. 确认数据库密码已改为环境变量
5. 确认JWT密钥已改为环境变量

### 推荐做法

1. 使用 `application-example.yml` 作为模板
2. 创建 `.env` 文件存储真实配置（不提交）
3. 使用环境变量或配置中心管理敏感信息
4. 定期审查代码和配置
5. 使用工具自动检查敏感信息
