# 邮件发送配置指南

## 概述

系统支持从数据库读取用户邮箱地址和授权码，使用各自的邮箱账号发送邮件。用户邮箱和授权码存储在 `sys_user` 表的 `email` 和 `email_auth_code` 字段中。

## 数据库配置

### 1. 数据库表结构

系统已自动在 `sys_user` 表中添加了 `email_auth_code` 字段：

```sql
-- 查看表结构
DESC sys_user;

-- 查看所有用户的邮箱和授权码配置
SELECT id, username, email, 
       CASE WHEN email_auth_code IS NOT NULL THEN '已配置' ELSE '未配置' END AS auth_code_status
FROM sys_user 
WHERE email IS NOT NULL;
```

### 2. 更新用户邮箱和授权码

```sql
-- 更新用户邮箱和授权码
UPDATE sys_user 
SET email = 'user@163.com', 
    email_auth_code = 'your-auth-code' 
WHERE id = 1;
```

## 前端配置

### 1. 用户管理页面

管理员可以在"用户管理"页面为每个用户配置：
- **邮箱地址**：用户的邮箱地址
- **邮箱授权码**：用于SMTP发送邮件的授权码

### 2. 个人资料页面

用户可以在"个人资料"页面配置自己的：
- **邮箱地址**：用于接收邮件通知
- **邮箱授权码**：用于发送邮件（163/QQ邮箱需要使用授权码）

## 获取邮箱授权码

### 163邮箱授权码获取

1. 登录163邮箱
2. 点击"设置" -> "POP3/SMTP/IMAP"
3. 开启"POP3/SMTP服务"
4. 点击"生成授权码"
5. 将生成的授权码填写到系统中

### QQ邮箱授权码获取

1. 登录QQ邮箱
2. 点击"设置" -> "账户"
3. 找到"POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务"
4. 开启"POP3/SMTP服务"或"IMAP/SMTP服务"
5. 点击"生成授权码"
6. 将生成的授权码填写到系统中

## 工作原理

1. **系统从数据库读取**：发送邮件时，系统会根据收件人邮箱地址从数据库查找对应的用户信息
2. **使用用户自己的邮箱**：系统使用该用户的邮箱地址和授权码创建SMTP连接
3. **自动选择SMTP服务器**：系统会根据邮箱域名自动选择对应的SMTP服务器（如163.com -> smtp.163.com）
4. **发送邮件**：使用用户的邮箱账号发送邮件给该用户

## 邮件测试功能

系统提供了邮件测试功能，可以在系统设置页面测试邮件发送是否正常。

### 使用步骤

1. **进入系统设置**
   - 登录系统后，点击左侧菜单"系统设置"
   - 选择"邮件配置"标签页

2. **查看已配置邮箱的用户**
   - 系统会显示所有已填写邮箱的用户列表
   - 包括用户ID、用户名、真实姓名、邮箱地址等信息

3. **发送测试邮件**
   - 在用户列表的操作列，点击"测试发送"按钮
   - 确认后，系统会向该用户的邮箱发送一封测试邮件
   - 测试邮件包含系统信息和邮件配置验证说明

### API接口

系统提供了邮件测试API接口：

- **接口地址**: `POST /api/email/test`
- **请求参数**:
  ```json
  {
    "email": "user@example.com",
    "subject": "测试邮件 - 机场车辆监控与调度系统"
  }
  ```
- **权限要求**: 需要用户认证（JWT Token）

## 使用示例

### 后端代码

系统已经实现了邮件发送功能，会自动从数据库读取用户邮箱和授权码：

```java
@Autowired
private EmailService emailService;

// 发送任务分配邮件
// 系统会自动从数据库读取userEmail对应的用户信息（包括授权码）
emailService.sendDriverTaskAssignmentEmail(
    userEmail,  // 从数据库sys_user表的email字段读取
    taskNo, taskName, taskType, priority,
    startLocation, endLocation, startTime,
    vehicleNo, vehicleBrand, vehicleModel
);
```

### 前端代码

前端无需特殊配置，邮件发送由后端自动处理。用户只需在个人资料或管理员在用户管理中配置邮箱和授权码即可。

### 邮件测试代码示例

```typescript
// 前端调用邮件测试接口
import { sendTestEmailApi } from '@/api/email'

const testEmail = async (email: string) => {
  try {
    const response = await sendTestEmailApi(email, '测试邮件')
    if (response.data.code === 200) {
      console.log('测试邮件发送成功')
    }
  } catch (error) {
    console.error('测试邮件发送失败:', error)
  }
}
```

## 常见问题

### 1. 邮件发送失败：收件人邮箱不存在于数据库中

**原因**：收件人邮箱地址在数据库中不存在

**解决**：
- 确认用户已在个人资料中填写邮箱地址
- 或管理员在用户管理中为用户配置邮箱地址

### 2. 邮件发送失败：收件人邮箱未配置授权码

**原因**：用户邮箱未配置授权码

**解决**：
- 用户在个人资料中填写邮箱授权码
- 或管理员在用户管理中为用户配置授权码

### 3. 邮件发送失败：Authentication failed

**原因**：授权码错误或已过期

**解决**：
- 确认使用的是授权码，不是登录密码
- 确认邮箱已开启SMTP服务
- 重新生成授权码并更新到系统中

### 4. 邮件发送失败：Connection timeout

**原因**：SMTP服务器地址或端口错误

**解决**：
- 系统会自动根据邮箱域名选择SMTP服务器
- 如果自动选择失败，检查网络连接

## 安全说明

**重要**：
- 邮箱授权码存储在数据库中，请确保数据库安全
- 授权码不会返回给前端，仅用于后端发送邮件
- 建议定期更换授权码
- 建议对数据库进行加密存储

## 支持的邮箱类型

系统支持以下邮箱类型，并会自动选择对应的SMTP服务器：

- **163邮箱**：smtp.163.com:465
- **QQ邮箱**：smtp.qq.com:465
- **Gmail**：smtp.gmail.com:465
- **126邮箱**：smtp.126.com:465
- **Sina邮箱**：smtp.sina.com:465
- **Outlook/Hotmail**：smtp-mail.outlook.com:587

其他邮箱类型会使用默认配置的SMTP服务器。
