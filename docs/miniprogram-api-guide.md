# 小程序接口使用指南

本文档说明机场车辆监控与调度系统为微信小程序提供的API接口和使用方法。

## 概述

系统为微信小程序提供了完整的后端API支持，允许司机和维修员通过移动端进行任务管理、消息查看等操作。

## 支持的平台

- **微信小程序**: 支持司机和维修员角色
- **API基础路径**: `http://your-domain.com/api` 或 `https://your-domain.com/api`

## 认证方式

所有接口都需要JWT Token认证。

### 获取Token

通过登录接口获取Token：

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "password"
}
```

### 使用Token

在请求头中携带Token：

```
Authorization: Bearer <JWT_TOKEN>
```

## 核心接口

### 1. 获取我的任务列表

**接口**: `GET /api/tasks?action=my-tasks`

**说明**: 获取分配给当前用户的任务列表（只返回未完成的任务）

**请求参数**:
- 无（通过JWT Token获取当前用户ID）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "taskNo": "TASK202601230001",
      "taskName": "机场接送任务",
      "taskType": "常规调度",
      "priority": 3,
      "status": 2,
      "assignedVehicleId": 10,
      "assignedDriverId": 5,
      "assignedUserId": null,
      "startLocation": "航站楼A",
      "endLocation": "航站楼B",
      "startTime": "2026-01-23T10:00:00",
      "progress": 0.00
    },
    {
      "id": 2,
      "taskNo": "TASK202601230002",
      "taskName": "车辆维修任务",
      "taskType": "维护调度",
      "priority": 4,
      "status": 2,
      "assignedVehicleId": 15,
      "assignedDriverId": null,
      "assignedUserId": 8,
      "startLocation": "维修车间",
      "endLocation": "维修车间",
      "startTime": "2026-01-23T14:00:00",
      "progress": 0.00
    }
  ]
}
```

**过滤逻辑**:
- 只返回 `assignedDriverId == currentUserId` 或 `assignedUserId == currentUserId` 的任务
- 只返回未完成的任务（`status != 4 AND status != 5`）
- 按开始时间降序排序

**小程序调用示例**:
```javascript
// 获取我的任务列表
wx.request({
  url: 'https://your-domain.com/api/tasks?action=my-tasks',
  method: 'GET',
  header: {
    'Authorization': 'Bearer ' + wx.getStorageSync('token')
  },
  success: function(res) {
    if (res.data.code === 200) {
      const tasks = res.data.data;
      console.log('我的任务:', tasks);
    }
  }
});
```

---

### 2. 确认任务完成

**接口**: `POST /api/tasks/confirm-complete`

**说明**: 确认任务完成，支持传递完成说明等信息

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | Long | 是 | 任务ID |
| taskName | String | 否 | 任务名称（可选） |
| content | String | 否 | 完成说明 |
| vehicleId | Long | 否 | 车辆ID（可选，如果未提供则从任务对象获取） |
| taskType | String | 否 | 任务类型（可选） |

**请求格式**: `application/x-www-form-urlencoded`

**响应示例**:
```json
{
  "code": 200,
  "message": "任务完成确认成功",
  "data": {
    "id": 1,
    "taskNo": "TASK202601230001",
    "taskName": "机场接送任务",
    "status": 4,
    "actualEndTime": "2026-01-23T15:30:00",
    "progress": 100.00,
    "remark": "完成说明：任务已顺利完成"
  }
}
```

**功能说明**:
1. 更新任务状态为已完成（4）
2. 设置实际结束时间和进度
3. 保存完成说明到任务备注
4. 自动更新车辆状态（维护调度任务完成后，车辆状态更新为正常）
5. 发送站内信通知给调度员

**小程序调用示例**:
```javascript
// 确认任务完成
wx.request({
  url: 'https://your-domain.com/api/tasks/confirm-complete',
  method: 'POST',
  header: {
    'Authorization': 'Bearer ' + wx.getStorageSync('token'),
    'Content-Type': 'application/x-www-form-urlencoded'
  },
  data: {
    taskId: 1,
    taskName: '机场接送任务',
    content: '任务已顺利完成',
    vehicleId: 10,
    taskType: '常规调度'
  },
  success: function(res) {
    if (res.data.code === 200) {
      wx.showToast({
        title: '任务完成确认成功',
        icon: 'success'
      });
    }
  }
});
```

---

### 3. 创建站内信消息

**接口**: `POST /api/messages/create`

**说明**: 创建新的站内信消息（小程序使用）

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| title | String | 是 | 消息标题 |
| content | String | 是 | 消息内容 |
| messageType | String | 否 | 消息类型（默认：system） |
| targetUserIds | List<Long> | 否 | 接收用户ID列表（为空则发送给管理员） |
| targetRoleCodes | List<String> | 否 | 接收角色代码列表（为空则发送给管理员） |
| vehicleId | Long | 否 | 关联车辆ID |
| priority | String | 否 | 优先级（默认：normal） |

**请求格式**: `application/x-www-form-urlencoded`

**响应示例**:
```json
{
  "code": 200,
  "message": "消息创建成功",
  "data": {
    "id": 100,
    "title": "车辆故障报告",
    "content": "车辆编号V001出现故障，需要维修",
    "messageType": "feedback",
    "createTime": "2026-01-23T15:30:00"
  }
}
```

**小程序调用示例**:
```javascript
// 创建站内信（问题反馈）
wx.request({
  url: 'https://your-domain.com/api/messages/create',
  method: 'POST',
  header: {
    'Authorization': 'Bearer ' + wx.getStorageSync('token'),
    'Content-Type': 'application/x-www-form-urlencoded'
  },
  data: {
    title: '车辆故障报告',
    content: '车辆编号V001出现故障，需要维修',
    messageType: 'feedback',
    vehicleId: 10,
    priority: 'high'
  },
  success: function(res) {
    if (res.data.code === 200) {
      wx.showToast({
        title: '反馈已提交',
        icon: 'success'
      });
    }
  }
});
```

---

### 4. 获取消息列表

**接口**: `GET /api/messages`

**说明**: 获取当前用户的消息列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码（默认：0） |
| size | Integer | 否 | 每页数量（默认：20） |
| read | Boolean | 否 | 是否已读（null表示全部） |
| messageType | String | 否 | 消息类型（null表示全部） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "任务分配通知",
        "content": "您已被分配了一个新的驾驶任务",
        "messageType": "task_assignment",
        "read": false,
        "createTime": "2026-01-23T10:00:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

### 5. 清空已读消息

**接口**: `POST /api/messages/operations/clear-read`

**说明**: 清空当前用户的所有已读消息

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "已清空 5 条已读消息",
  "data": 5
}
```

**小程序调用示例**:
```javascript
// 清空已读消息
wx.request({
  url: 'https://your-domain.com/api/messages/operations/clear-read',
  method: 'POST',
  header: {
    'Authorization': 'Bearer ' + wx.getStorageSync('token')
  },
  success: function(res) {
    if (res.data.code === 200) {
      wx.showToast({
        title: res.data.message,
        icon: 'success'
      });
    }
  }
});
```

---

### 6. 上传用户位置

**接口**: `POST /api/vehicles/upload-location`

**说明**: 上传小程序用户位置信息。用户在线即可上传，无需关联车辆或任务。支持全局位置上传（小程序打开期间持续上传，60秒间隔）

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| longitude | Double | 是 | 经度（GCJ02坐标系） |
| latitude | Double | 是 | 纬度（GCJ02坐标系） |
| address | String | 否 | 位置地址 |
| speed | Double | 否 | 速度(km/h) |
| direction | Double | 否 | 方向角(度) |
| accuracy | Double | 否 | 精度(m) |

**功能说明**:
1. **自动获取用户ID**：系统从JWT Token中自动获取当前登录用户的ID，无需传递userId参数
2. **不关联车辆或任务**：用户位置上传独立于车辆和任务，只要用户在线即可上传
3. **实时地图显示**：通过WebSocket实时推送到前端地图，小程序位置以不同颜色标记显示
4. **坐标转换**：小程序提供GCJ02坐标，前端自动转换为BD09坐标系（百度地图标准）
5. **全局位置上传**：小程序打开期间持续上传位置（60秒间隔），直到小程序关闭

**注意**: 
- ✅ 用户位置上传**不需要关联车辆或任务**，只要用户在线即可上传
- ✅ 系统自动使用当前登录用户的ID（从JWT Token中获取），**无需传递userId参数**
- ✅ 车辆位置由车辆定位器单独处理，与用户位置分离
- ✅ 小程序位置和PC位置在地图上使用不同颜色和样式标记，便于区分
- ⚠️ 定位精度：在微信开发者工具中可能使用基站定位（精度较低），建议在真实手机上运行以获取GPS定位（精度通常10-50米）

**请求格式**: `application/x-www-form-urlencoded`

**响应示例**:
```json
{
  "code": 200,
  "message": "位置上传成功",
  "data": {
    "userId": 2,
    "userName": "司机",
    "longitude": 113.58646,
    "latitude": 23.54835,
    "accuracy": 65
  }
}
```

**小程序调用示例**:
```javascript
// 上传位置（系统自动使用当前登录用户的ID）
wx.getLocation({
  type: 'gcj02',  // 必须使用gcj02坐标系
  success: function(res) {
    wx.request({
      url: 'https://your-domain.com/api/vehicles/upload-location',
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token'),
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      data: {
        longitude: res.longitude,
        latitude: res.latitude,
        accuracy: res.accuracy,
        speed: res.speed,
        direction: res.direction
      },
      success: function(res) {
        if (res.data.code === 200) {
          console.log('位置上传成功');
        }
      },
      fail: function(err) {
        console.error('位置上传失败', err);
      }
    });
  },
  fail: function(err) {
    console.error('获取位置失败', err);
  }
});
```

**位置数据流转**:
```
小程序获取位置（GCJ02）
    ↓
调用上传接口（POST /api/vehicles/upload-location）
    ↓
后端接收，从JWT Token获取userId
    ↓
通过WebSocket实时推送到前端（source: "miniprogram"）
    ↓
前端接收，转换坐标（GCJ02 → BD09）
    ↓
在地图上显示小程序位置标记（不同颜色）
```

**坐标系统说明**:
- **小程序位置**：使用GCJ02坐标系（微信小程序标准），前端自动转换为BD09坐标系显示
- **PC位置**：使用WGS84坐标系（浏览器标准），前端自动转换为BD09坐标系显示
- **地图显示**：统一使用BD09坐标系（百度地图标准）
- 不需要为每个用户重复订阅，vehicle_001已订阅mobile_001的主题
- 多个用户的位置数据都通过同一个mobile_001设备发布，vehicle_001统一接收并转发

---

### 7. 获取未读消息数量

**接口**: `GET /api/messages/unread-count`

**说明**: 获取当前用户的未读消息数量

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": 3
}
```

---

## 任务分配逻辑

### 任务分配字段说明

系统使用以下字段标识任务分配：

1. **assignedVehicleId**: 分配车辆ID（所有任务都有）
2. **assignedDriverId**: 分配司机ID（常规调度、紧急调度任务使用）
3. **assignedUserId**: 分配用户ID（维护调度任务使用，分配给维修员）

### 任务类型与分配字段

| 任务类型 | 分配字段 | 说明 |
|---------|---------|------|
| 常规调度 | `assignedDriverId` | 分配给司机 |
| 紧急调度 | `assignedDriverId` | 分配给司机 |
| 维护调度 | `assignedUserId` | 分配给维修员 |

### 查询我的任务

小程序调用 `GET /api/tasks?action=my-tasks` 时，后端会自动：
- 检查当前用户ID
- 查询 `assignedDriverId == userId` 或 `assignedUserId == userId` 的任务
- 过滤掉已完成（4）和已取消（5）的任务
- 按开始时间降序返回

## 任务状态说明

| 状态值 | 状态名称 | 说明 |
|--------|---------|------|
| 1 | 待分配 | 任务已创建，尚未分配 |
| 2 | 已分配 | 任务已分配给车辆和司机/维修员 |
| 3 | 执行中 | 任务正在执行 |
| 4 | 已完成 | 任务已完成 |
| 5 | 已取消 | 任务已取消 |
| 6 | 异常 | 任务执行异常 |

## 权限说明

### task:complete 权限

- **权限代码**: `task:complete`
- **权限名称**: 完成任务
- **权限类型**: 按钮权限
- **已分配角色**: 
  - DRIVER（司机）
  - MAINTENANCE（维修员）
  - ADMIN（管理员）
  - DISPATCHER（调度员）

### 权限验证

所有需要权限的接口都会自动验证：
- 如果用户没有相应权限，返回 403 错误
- 错误信息：`"无权限完成任务，需要task:complete权限"`

## 错误处理

### 常见错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|---------|
| 200 | 成功 | 正常处理 |
| 401 | 未认证 | 重新登录获取Token |
| 403 | 无权限 | 检查用户角色和权限配置 |
| 404 | 资源不存在 | 检查请求的资源ID是否正确 |
| 500 | 服务器错误 | 查看服务器日志 |

### 错误响应格式

```json
{
  "code": 403,
  "message": "无权限完成任务，需要task:complete权限",
  "data": null
}
```

## 小程序集成示例

### 1. 配置API基础路径

```javascript
// config.js
const config = {
  apiBaseUrl: 'https://your-domain.com/api'
};

module.exports = config;
```

### 2. 封装请求方法

```javascript
// utils/request.js
const config = require('./config');

function request(url, method, data, header = {}) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    
    wx.request({
      url: config.apiBaseUrl + url,
      method: method,
      data: data,
      header: {
        'Authorization': token ? 'Bearer ' + token : '',
        'Content-Type': 'application/x-www-form-urlencoded',
        ...header
      },
      success: function(res) {
        if (res.data.code === 200) {
          resolve(res.data);
        } else if (res.data.code === 401) {
          // Token过期，跳转到登录页
          wx.removeStorageSync('token');
          wx.reLaunch({
            url: '/pages/login/login'
          });
          reject(res.data);
        } else {
          reject(res.data);
        }
      },
      fail: function(err) {
        reject(err);
      }
    });
  });
}

module.exports = {
  request
};
```

### 3. 使用示例

```javascript
// pages/tasks/tasks.js
const { request } = require('../../utils/request');

Page({
  data: {
    tasks: []
  },
  
  onLoad() {
    this.loadMyTasks();
  },
  
  // 加载我的任务
  loadMyTasks() {
    request('/tasks?action=my-tasks', 'GET')
      .then(res => {
        this.setData({
          tasks: res.data
        });
      })
      .catch(err => {
        wx.showToast({
          title: '加载失败',
          icon: 'error'
        });
      });
  },
  
  // 确认任务完成
  confirmComplete(e) {
    const taskId = e.currentTarget.dataset.id;
    const task = this.data.tasks.find(t => t.id === taskId);
    
    wx.showModal({
      title: '确认完成',
      content: '确定要完成此任务吗？',
      success: (res) => {
        if (res.confirm) {
          request('/tasks/confirm-complete', 'POST', {
            taskId: taskId,
            taskName: task.taskName,
            content: '任务已完成',
            vehicleId: task.assignedVehicleId,
            taskType: task.taskType
          })
          .then(res => {
            wx.showToast({
              title: '任务完成确认成功',
              icon: 'success'
            });
            this.loadMyTasks(); // 重新加载任务列表
          })
          .catch(err => {
            wx.showToast({
              title: err.message || '操作失败',
              icon: 'error'
            });
          });
        }
      }
    });
  }
});
```

## 最佳实践

### 1. Token管理

- 登录成功后，将Token存储到本地存储
- 每次请求自动携带Token
- Token过期时，自动跳转到登录页

### 2. 错误处理

- 统一处理401错误（未认证）
- 统一处理403错误（无权限）
- 显示友好的错误提示

### 3. 数据刷新

- 完成任务后，重新加载任务列表
- 定期刷新未读消息数量
- 使用下拉刷新功能

### 4. 用户体验

- 操作前显示确认对话框
- 操作成功显示成功提示
- 操作失败显示错误提示
- 加载数据时显示加载状态

## 测试建议

### 1. 接口测试

使用Postman或curl测试接口：

```bash
# 获取我的任务
curl -X GET "http://localhost:8080/api/tasks?action=my-tasks" \
  -H "Authorization: Bearer <JWT_TOKEN>"

# 确认任务完成
curl -X POST "http://localhost:8080/api/tasks/confirm-complete" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "taskId=1&content=任务已完成"
```

### 2. 权限测试

- 使用司机账号测试任务完成功能
- 使用维修员账号测试任务完成功能
- 使用没有权限的账号测试，确认返回403错误

### 3. 数据测试

- 测试获取我的任务，确认只返回分配给当前用户的任务
- 测试任务完成，确认任务状态正确更新
- 测试车辆状态更新，确认维护调度任务完成后车辆状态更新为正常

## 位置上传说明

### 位置数据流转流程

```
小程序获取位置（GCJ02坐标系）
    ↓
调用 POST /api/vehicles/upload-location
    ↓
后端从JWT Token获取userId
    ↓
通过WebSocket实时推送到前端（source: "miniprogram"）
    ↓
前端接收，转换坐标（GCJ02 → BD09）
    ↓
在地图上显示小程序位置标记（不同颜色）
```

### 关键特性

1. **独立于车辆和任务**：用户位置上传不关联车辆或任务，只要用户在线即可上传
2. **自动用户识别**：系统从JWT Token中自动获取用户ID，无需传递userId参数
3. **实时地图显示**：通过WebSocket实时推送，前端自动显示在地图上
4. **坐标自动转换**：小程序提供GCJ02坐标，前端自动转换为BD09坐标系（百度地图标准）
5. **全局位置上传**：小程序打开期间持续上传位置（60秒间隔），直到小程序关闭

### 地图显示

位置上传后，会通过WebSocket实时推送到前端：
- 前端地图页面会自动接收位置更新
- 小程序位置以不同颜色的标记显示，与PC位置和车辆位置区分
- 支持多个客户端同时查看
- 信息窗口显示用户名称、坐标、精度等信息

### 定时上传位置

小程序支持全局位置上传（小程序打开期间持续上传）：

```javascript
// 全局位置上传（小程序打开期间持续上传）
let locationTimer = null;

function startGlobalLocationUpload() {
  // 立即上传一次
  uploadLocation();
  
  // 每60秒上传一次
  locationTimer = setInterval(() => {
    uploadLocation();
  }, 60000);
}

function stopGlobalLocationUpload() {
  if (locationTimer) {
    clearInterval(locationTimer);
    locationTimer = null;
  }
}

function uploadLocation() {
  wx.getLocation({
    type: 'gcj02',  // 必须使用gcj02坐标系
    success: function(res) {
      wx.request({
        url: 'https://your-domain.com/api/vehicles/upload-location',
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + wx.getStorageSync('token'),
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        data: {
          longitude: res.longitude,
          latitude: res.latitude,
          accuracy: res.accuracy,
          speed: res.speed,
          direction: res.direction
        },
        success: function(res) {
          console.log('位置上传成功');
        },
        fail: function(err) {
          console.error('位置上传失败', err);
        }
      });
    },
    fail: function(err) {
      console.error('获取位置失败', err);
    }
  });
}

// 小程序启动时开始上传
App({
  onLaunch() {
    startGlobalLocationUpload();
  },
  onHide() {
    stopGlobalLocationUpload();
  }
});
```

## 常见问题

### Q1: 为什么获取不到我的任务？

**A**: 检查以下几点：
1. Token是否正确传递
2. 任务是否已分配给当前用户（检查 `assignedDriverId` 或 `assignedUserId`）
3. 任务状态是否为已完成或已取消（这些任务会被过滤掉）

### Q2: 为什么无法完成任务？

**A**: 检查以下几点：
1. 用户是否有 `task:complete` 权限
2. 任务状态是否为已分配（2）或执行中（3）
3. 任务是否存在

### Q3: 任务完成后，车辆状态没有更新？

**A**: 
- 维护调度任务完成后，车辆状态会自动更新为正常（1）
- 常规/紧急调度任务完成后，车辆状态保持为正常（1），不需要更新
- 如果车辆状态没有更新，检查任务类型是否为"维护调度"

### Q4: 如何判断任务分配给谁？

**A**: 
- 如果 `assignedDriverId` 不为 `null`，说明任务分配给司机
- 如果 `assignedUserId` 不为 `null`，说明任务分配给维修员
- 如果两个都是 `null`，说明任务未分配（状态为1-待分配）

### Q5: 为什么位置上传后地图上没有显示？

**A**: 检查以下几点：
1. 位置是否成功上传（查看接口返回，确认返回code为200）
2. WebSocket连接是否正常（前端需要连接WebSocket）
3. 前端地图页面是否订阅了位置更新事件
4. 坐标转换是否正确（小程序使用GCJ02，前端自动转换为BD09）
5. 浏览器控制台是否有错误信息

### Q6: 小程序位置和PC位置有什么区别？

**A**: 
- **小程序位置**：使用GCJ02坐标系（微信小程序标准），前端自动转换为BD09显示，以不同颜色的标记显示
- **PC位置**：使用WGS84坐标系（浏览器标准），前端自动转换为BD09显示，以蓝色标记显示
- **坐标转换**：所有位置统一转换为BD09坐标系（百度地图标准）显示
- **定位精度**：PC定位精度较低（通常100-500米），小程序在真实手机上GPS定位精度较高（通常10-50米）

### Q7: 为什么小程序位置和PC位置差距很大？

**A**: 
- **定位方式不同**：PC使用IP/WiFi/基站定位，小程序在开发者工具中可能使用基站定位，真实手机使用GPS定位
- **精度差异**：PC定位精度通常100-500米，小程序在开发者工具中精度可能较低（500-1000米），真实手机GPS精度通常10-50米
- **建议**：在真实手机上运行小程序以获取GPS定位，连接WiFi可提高PC定位精度

## 相关文档

- [功能更新文档](feature-updates.md) - 最新功能更新和改进说明
- [架构设计文档](architecture.md) - 系统架构设计和技术选型

---

**文档版本**: 1.1.0  
**最后更新**: 2026-01-24  
**维护者**: Corkedmzx
