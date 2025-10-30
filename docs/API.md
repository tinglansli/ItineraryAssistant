# Itinerary Assistant API 文档

## 基本信息

- **API 基础 URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token (JWT)
- **请求/响应格式**: JSON
- **字符编码**: UTF-8

## 响应格式

所有 API 响应都遵循统一的格式：

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 响应状态码

| Code | HTTP Status | 说明 |
|------|-------------|------|
| 200 | 200 | 操作成功 |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未授权/Token 无效 |
| 403 | 403 | 禁止访问/无权限 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |

---

## 用户模块 (User API)

### 1. 用户注册

**请求**

```http
POST /users/register HTTP/1.1
Content-Type: application/json

{
  "username": "user123",
  "password": "password123"
}
```

**响应 (201 Created)**

```json
{
  "success": true,
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": "user-001",
    "username": "user123"
  }
}
```

**错误情况**

- `400`: 用户名已存在
- `400`: 参数校验失败

---

### 2. 用户登录

**请求**

```http
POST /users/login HTTP/1.1
Content-Type: application/json

{
  "username": "user123",
  "password": "password123"
}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**错误情况**

- `401`: 用户名或密码错误

---

### 3. 获取用户信息

**请求**

```http
GET /users/{userId} HTTP/1.1
Authorization: Bearer {token}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": "user-001",
    "username": "user123",
    "createdAt": "2025-10-30T10:00:00Z"
  }
}
```

**错误情况**

- `401`: Token 无效或过期
- `404`: 用户不存在

---

### 4. 获取用户偏好

**请求**

```http
GET /users/preferences HTTP/1.1
Authorization: Bearer {token}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": "喜欢自然风光;预算较高"
}
```

**错误情况**

- `401`: 未授权

---

### 5. 更新用户偏好

**请求**

```http
PUT /users/preferences HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "preferences": "喜欢自然风光;预算较高"
}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "偏好更新成功",
  "data": null
}
```

**错误情况**

- `401`: 未授权
- `404`: 用户不存在

---

## 行程模块 (Trip API)

### 1. 从文本创建行程

**请求**

```http
POST /trips/from-text HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "text": "我想去北京玩3天，预算5000块"
}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "行程创建成功",
  "data": {
    "tripId": "trip-001",
    "destination": "北京",
    "duration": 3,
    "budget": 5000,
    "days": [
      {
        "dayIndex": 1,
        "date": "2025-10-30",
        "activities": []
      }
    ]
  }
}
```

**错误情况**

- `400`: 文本内容无效
- `401`: 未授权

---

### 2. 确认行程

**请求**

```http
POST /trips/{tripId}/confirm HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "行程确认成功",
  "data": {
    "tripId": "trip-001",
    "status": "confirmed"
  }
}
```

**错误情况**

- `403`: 无权操作该资源
- `404`: 行程不存在
- `401`: 未授权

---

### 3. 获取已确认行程列表

**请求**

```http
GET /trips/confirmed HTTP/1.1
Authorization: Bearer {token}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "tripId": "trip-001",
      "destination": "北京",
      "duration": 3,
      "budget": 5000,
      "status": "confirmed"
    }
  ]
}
```

**错误情况**

- `401`: 未授权
- `404`: 用户不存在

---

### 4. 获取行程详情与行程单

**请求**

```http
GET /trips/{tripId}/itinerary HTTP/1.1
Authorization: Bearer {token}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    "tripId": "trip-001",
    "destination": "北京",
    "duration": 3,
    "budget": 5000,
    "days": [
      {
        "dayIndex": 1,
        "date": "2025-10-30",
        "activities": [
          {
            "activityId": "act-001",
            "name": "故宫参观",
            "time": "09:00",
            "cost": 60
          }
        ]
      }
    ]
  }
}
```

**错误情况**

- `404`: 行程不存在
- `401`: 未授权

---

## 开销模块 (Expense API)

### 1. 从文本创建开销

**请求**

```http
POST /expenses/from-text HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "tripId": "trip-001",
  "text": "早上买了门票60块"
}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "开销创建成功",
  "data": {
    "expenseId": "exp-001",
    "tripId": "trip-001",
    "description": "门票",
    "amount": 60,
    "category": "景点门票",
    "timestamp": "2025-10-30T10:00:00Z"
  }
}
```

**错误情况**

- `400`: 文本内容无效或为空
- `404`: 行程不存在
- `401`: 未授权

---

### 2. 获取开销列表

**请求**

```http
GET /expenses?tripId={tripId} HTTP/1.1
Authorization: Bearer {token}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "expenseId": "exp-001",
      "tripId": "trip-001",
      "description": "门票",
      "amount": 60,
      "category": "景点门票",
      "timestamp": "2025-10-30T10:00:00Z"
    }
  ]
}
```

**错误情况**

- `404`: 行程不存在
- `401`: 未授权

---

## 预算模块 (Budget API)

### 1. 获取预算信息

**请求**

```http
GET /budgets/{tripId} HTTP/1.1
Authorization: Bearer {token}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    "tripId": "trip-001",
    "totalBudget": 5000,
    "categoryBudgets": {
      "景点门票": 1000,
      "餐饮": 1500,
      "住宿": 2000
    }
  }
}
```

**错误情况**

- `404`: 行程不存在
- `401`: 未授权

---

### 2. 获取预算分析

**请求**

```http
GET /budgets/{tripId}/summary HTTP/1.1
Authorization: Bearer {token}
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    "tripId": "trip-001",
    "totalBudget": 5000,
    "totalExpense": 1500,
    "remaining": 3500,
    "remainingPercentage": 70,
    "categoryAnalysis": {
      "景点门票": {
        "budget": 1000,
        "expense": 120,
        "remaining": 880
      },
      "餐饮": {
        "budget": 1500,
        "expense": 1380,
        "remaining": 120
      }
    },
    "isOverBudget": false
  }
}
```

**错误情况**

- `404`: 行程不存在
- `401`: 未授权

---

## 语音模块 (Speech API)

### 1. 语音转文字

**请求**

```http
POST /speech/transcribe HTTP/1.1
Authorization: Bearer {token}
Content-Type: multipart/form-data

[上传 .wav/.mp3 音频文件作为 "audio" 字段]
```

**响应**

```json
{
  "success": true,
  "code": 200,
  "message": "语音识别成功",
  "data": "我想去北京玩三天，预算5000块"
}
```

**错误情况**

- `400`: 文件为空
- `400`: 不支持的音频格式
- `400`: 语音识别失败
- `500`: 服务器错误
- `401`: 未授权

---

## 认证说明

### 获取 Token

1. 调用 `/users/register` 注册新用户
2. 调用 `/users/login` 获取 JWT Token
3. 在所有需要认证的请求头中添加：

```
Authorization: Bearer {token}
```

### Token 过期

- Token 有效期为 24 小时
- 过期后需要重新登录获取新 Token
- 过期会返回 `401` 错误

---

## 错误处理

所有错误响应格式如下：

```json
{
  "success": false,
  "code": 400,
  "message": "请求参数错误",
  "data": null
}
```

### 常见错误码

| Code | Message | 说明 |
|------|---------|------|
| 400 | 请求参数错误 | 参数验证失败 |
| 401 | 未授权 | Token 缺失或无效 |
| 403 | 无权访问 | 用户无权操作该资源 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 500 | 服务器内部错误 | 服务器异常 |

---

## Postman 测试步骤

1. **导入 API 文档**
   - 在 Postman 中创建新的 Collection
   - 按照上述 API 端点添加请求

2. **设置环境变量**
   - 创建名为 `base_url` 的变量，值为 `http://localhost:8080/api`
   - 创建名为 `token` 的变量，用于存储登录返回的 Token

3. **执行流程**
   - 先调用 `/users/register` 注册账户
   - 再调用 `/users/login` 获取 Token
   - 将 Token 设置到 `token` 环境变量
   - 在所有需要认证的请求中使用 `Authorization: Bearer {{token}}` 头

4. **测试用例**
   - 注册 → 登录 → 创建行程 → 添加开销 → 查看预算分析
   - 语音上传 → 文本解析 → 行程生成

---

## 注意事项

1. **时区**: 所有时间均为 UTC+8 (北京时间)
2. **金额**: 金额字段均为整数，单位为人民币元
3. **文件上传**: 语音文件大小限制为 10MB，支持格式 .wav、.mp3
4. **并发限制**: 单个用户 Token 同时只支持一个活跃会话
5. **速率限制**: 暂无速率限制，生产环境建议添加

---

## 示例流程

### 完整的行程创建到预算分析流程

```
1. POST /users/register
   → 获取 userId

2. POST /users/login
   → 获取 token

3. POST /trips/from-text
   - 使用 token 认证
   - 文本: "我想去北京玩3天，预算5000块"
   → 获取 tripId

4. POST /trips/{tripId}/confirm
   - 使用 token 认证
   → 确认行程

5. POST /expenses/from-text
   - 使用 token 认证
   - 文本: "早上买了门票60块"
   → 创建开销

6. GET /budgets/{tripId}/summary
   - 使用 token 认证
   → 获取预算分析结果
```

---

**API 文档最后更新**: 2025-10-30  
**API 版本**: v1.0.0
