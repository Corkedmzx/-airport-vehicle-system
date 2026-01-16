# 创建百度地图浏览器端AK指南

## 问题说明

如果当前使用的服务端AK无法在百度平台查看或修改，或者JavaScript API服务无法启用，建议创建一个新的**浏览器端AK**（JavaScript API类型）。

## 创建步骤

### 步骤1：登录百度地图开放平台

1. 访问：https://lbsyun.baidu.com/
2. 使用你的百度账号登录

### 步骤2：创建新应用

1. 登录后，点击右上角的"控制台"
2. 在左侧菜单中找到"应用管理"
3. 点击"创建应用"按钮

### 步骤3：填写应用信息

1. **应用名称**：填写一个描述性的名称，如"机场车辆管理系统-前端"
2. **应用类型**：选择 **"浏览器端"** 或 **"Web端"**（**重要：不要选择服务端**）
3. **应用描述**：可选，填写应用用途说明

### 步骤4：配置Referer白名单

在"Referer白名单"配置中，添加以下内容：

**开发环境配置：**
```
http://localhost:*
http://127.0.0.1:*
```

**或者更具体的配置：**
```
http://localhost:3000
http://localhost:5173
http://127.0.0.1:3000
http://127.0.0.1:5173
```

**生产环境配置（根据实际域名）：**
```
https://yourdomain.com/*
http://yourdomain.com/*
```

**允许所有来源（仅用于测试，不推荐用于生产）：**
```
*
```

### 步骤5：启用JavaScript API服务

1. 创建应用后，进入应用详情页
2. 找到"服务管理"或"已开通服务"
3. 确保"JavaScript API"服务已启用
4. 如果未启用，点击"启用"或"开通"

### 步骤6：复制AK

1. 在应用详情页找到"访问应用（AK）"
2. 复制完整的AK字符串
3. 格式类似：`your-baidu-map-ak-here`

### 步骤7：更新配置

1. 打开 `backend/.env` 文件
2. 更新 `BAIDU_MAP_AK` 的值：
   ```
   BAIDU_MAP_AK=你的新AK
   ```
3. 保存文件

### 步骤8：重启后端服务

```bash
# 停止当前服务（Ctrl+C）
cd backend
mvn spring-boot:run
```

### 步骤9：测试

1. 刷新前端页面
2. 查看浏览器控制台，应该能看到：
   ```
   正在获取百度地图API配置...
   后端响应状态: 200 OK
   正在通过后端代理获取第一个脚本内容...
   第一个脚本内容获取成功
   提取到getscript URL: ...
   getscript脚本加载成功
   百度地图API加载成功，BMap对象已可用
   地图初始化成功
   ```

## 浏览器端AK vs 服务端AK

| 特性 | 浏览器端AK | 服务端AK |
|------|-----------|---------|
| **用途** | 前端JavaScript API | 后端API调用 |
| **白名单类型** | Referer白名单（域名） | IP白名单 |
| **支持端口** | 支持（如 `localhost:3000`） | 不支持端口 |
| **JavaScript API** | 原生支持 | 需要单独启用服务 |
| **安全性** | AK会暴露在前端 | AK不暴露，更安全 |

## 注意事项

1. **AK安全**：
   - 浏览器端AK会暴露在前端代码中
   - 建议配置严格的Referer白名单
   - 生产环境不要使用 `*` 通配符

2. **服务启用**：
   - 确保JavaScript API服务已启用
   - 检查服务配额是否充足

3. **Referer白名单**：
   - 必须包含协议（`http://` 或 `https://`）
   - 必须包含端口号（如果使用非标准端口）
   - HTTP和HTTPS需要分别配置

4. **测试环境**：
   - 开发环境可以使用 `localhost:*` 通配符
   - 生产环境应该配置具体的域名

## 故障排查

### 问题1：仍然显示"APP服务被禁用了"

**解决方案**：
- 检查JavaScript API服务是否已启用
- 等待1-2分钟让配置生效
- 清除浏览器缓存后重试

### 问题2：Referer白名单配置无效

**解决方案**：
- 确认Referer值包含协议和端口
- 检查浏览器Network标签页中的Referer请求头
- 确认没有设置 `referrer: no-referrer` 策略

### 问题3：AK配置后仍然无法使用

**解决方案**：
- 确认 `backend/.env` 文件中的AK值正确
- 确认已重启后端服务
- 查看后端日志，确认AK已正确加载

## 参考链接

- [百度地图开放平台](https://lbsyun.baidu.com/)
- [应用管理](https://lbsyun.baidu.com/apiconsole/key)
- [JavaScript API文档](https://lbsyun.baidu.com/index.php?title=jspopularGL)
- [Referer白名单配置说明](https://lbsyun.baidu.com/index.php?title=faq/q/44)
