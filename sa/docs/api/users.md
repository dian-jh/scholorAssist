# 用户管理接口

## 用户注册

**接口名称：** 用户注册  
**功能描述：** 新用户注册账号，创建用户基本信息  
**接口地址：** `/api/users/register`  
**请求方式：** POST

### 功能说明

用户通过邮箱和密码注册新账号。系统会验证邮箱格式、密码强度，确保用户名唯一性。注册成功后自动分配基础权限。

```mermaid
sequenceDiagram
    participant Client
    participant Server
    participant Database
    participant EmailService
    Client->>Server: POST /api/users/register
    Server->>Server: 验证参数格式
    Server->>Database: 检查用户名/邮箱唯一性
    Database-->>Server: 返回检查结果
    Server->>Database: 创建用户记录
    Database-->>Server: 返回用户信息
    Server->>EmailService: 发送验证邮件
    Server-->>Client: 返回注册结果
```

### 请求参数

**请求体参数：**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "confirm_password": "SecurePass123!",
  "real_name": "张三"
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| username | string | 是 | 用户名，3-20字符，只能包含字母、数字、下划线 | "john_doe" |
| email | string | 是 | 邮箱地址，必须是有效的邮箱格式 | "john@example.com" |
| password | string | 是 | 密码，8-32字符，必须包含大小写字母、数字和特殊字符 | "SecurePass123!" |
| confirm_password | string | 是 | 确认密码，必须与password一致 | "SecurePass123!" |
| real_name | string | 否 | 真实姓名，1-50字符 | "张三" |

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "user_id": "user_123456789",
    "username": "john_doe",
    "email": "john@example.com",
    "real_name": "张三",
    "status": "pending_verification",
    "role": "user",
    "created_at": "2024-01-21T10:30:00Z"
  }
}
```

**错误响应示例：**
```json
{
  "code": 409,
  "msg": "用户名已存在",
  "data": null
}
```

**响应字段说明：**

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| code | int | 是 | 状态码 | 200 |
| msg | string | 是 | 状态信息 | "success" |
| data | object | 是 | 用户信息 | {} |
| data.user_id | string | 是 | 用户唯一标识 | "user_123456789" |
| data.username | string | 是 | 用户名 | "john_doe" |
| data.email | string | 是 | 邮箱地址 | "john@example.com" |
| data.real_name | string | 否 | 真实姓名 | "张三" |
| data.status | string | 是 | 用户状态：pending_verification/active/suspended | "pending_verification" |
| data.role | string | 是 | 用户角色：user/admin/super_admin | "user" |
| data.created_at | string | 是 | 创建时间（ISO格式） | "2024-01-21T10:30:00Z" |

### 接口权限要求
- 无需登录
- 公开接口

### 接口调用频率限制
- 每IP每小时最多10次注册请求

---

## 用户登录

**接口名称：** 用户登录  
**功能描述：** 用户通过用户名/邮箱和密码登录系统  
**接口地址：** `/api/users/login`  
**请求方式：** POST

### 功能说明

用户使用用户名或邮箱配合密码进行身份验证。登录成功后返回访问令牌(JWT Token)，用于后续API调用的身份认证。

```mermaid
sequenceDiagram
    participant Client
    participant Server
    participant Database
    participant TokenService
    Client->>Server: POST /api/users/login
    Server->>Database: 验证用户凭据
    Database-->>Server: 返回用户信息
    Server->>TokenService: 生成JWT Token
    TokenService-->>Server: 返回Token
    Server-->>Client: 返回登录结果和Token
```

### 请求参数

**请求体参数：**
```json
{
  "login": "john_doe",
  "password": "SecurePass123!",
  "remember_me": true
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| login | string | 是 | 用户名或邮箱地址 | "john_doe" |
| password | string | 是 | 用户密码 | "SecurePass123!" |
| remember_me | boolean | 否 | 是否记住登录状态，默认false | true |

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "user_id": "user_123456789",
    "username": "john_doe",
    "email": "john@example.com",
    "real_name": "张三",
    "role": "user",
    "status": "active",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_at": "2024-01-22T10:30:00Z",
    "last_login_at": "2024-01-21T10:30:00Z"
  }
}
```

**错误响应示例：**
```json
{
  "code": 401,
  "msg": "用户名或密码错误",
  "data": null
}
```

**响应字段说明：**

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| code | int | 是 | 状态码 | 200 |
| msg | string | 是 | 状态信息 | "success" |
| data | object | 是 | 登录信息 | {} |
| data.user_id | string | 是 | 用户唯一标识 | "user_123456789" |
| data.username | string | 是 | 用户名 | "john_doe" |
| data.email | string | 是 | 邮箱地址 | "john@example.com" |
| data.real_name | string | 否 | 真实姓名 | "张三" |
| data.role | string | 是 | 用户角色 | "user" |
| data.status | string | 是 | 用户状态 | "active" |
| data.token | string | 是 | JWT访问令牌 | "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." |
| data.expires_at | string | 是 | Token过期时间 | "2024-01-22T10:30:00Z" |
| data.last_login_at | string | 是 | 上次登录时间 | "2024-01-21T10:30:00Z" |

### 接口权限要求
- 无需登录
- 公开接口

### 接口调用频率限制
- 每IP每分钟最多5次登录尝试

---

## 用户登出

**接口名称：** 用户登出  
**功能描述：** 用户退出登录，使当前Token失效  
**接口地址：** `/api/users/logout`  
**请求方式：** POST

### 功能说明

用户主动退出登录，服务器将当前Token加入黑名单，确保Token立即失效。

### 请求参数

**请求头参数：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

无请求体参数

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

**错误响应示例：**
```json
{
  "code": 401,
  "msg": "Token无效或已过期",
  "data": null
}
```

### 接口权限要求
- 需要用户登录
- 需要有效的JWT Token

### 接口调用频率限制
- 无限制

---

## 获取用户信息

**接口名称：** 获取用户信息  
**功能描述：** 获取当前登录用户的详细信息  
**接口地址：** `/api/users/profile`  
**请求方式：** GET

### 功能说明

获取当前登录用户的完整个人信息，包括基本信息、权限信息、统计数据等。

### 请求参数

**请求头参数：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

无其他参数

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "user_id": "user_123456789",
    "username": "john_doe",
    "email": "john@example.com",
    "real_name": "张三",
    "avatar": "https://example.com/avatars/user_123456789.jpg",
    "role": "user",
    "status": "active",
    "created_at": "2024-01-15T10:30:00Z",
    "last_login_at": "2024-01-21T10:30:00Z",
    "email_verified": true,
    "preferences": {
      "language": "zh-CN",
      "theme": "light",
      "notifications": {
        "email": true,
        "push": false
      }
    },
    "statistics": {
      "documents_count": 25,
      "notes_count": 48,
      "reading_time": 12600
    }
  }
}
```

**错误响应示例：**
```json
{
  "code": 401,
  "msg": "Token无效或已过期",
  "data": null
}
```

**响应字段说明：**

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| code | int | 是 | 状态码 | 200 |
| msg | string | 是 | 状态信息 | "success" |
| data | object | 是 | 用户信息 | {} |
| data.user_id | string | 是 | 用户唯一标识 | "user_123456789" |
| data.username | string | 是 | 用户名 | "john_doe" |
| data.email | string | 是 | 邮箱地址 | "john@example.com" |
| data.real_name | string | 否 | 真实姓名 | "张三" |
| data.avatar | string | 否 | 头像URL | "https://example.com/avatars/user_123456789.jpg" |
| data.role | string | 是 | 用户角色 | "user" |
| data.status | string | 是 | 用户状态 | "active" |
| data.created_at | string | 是 | 创建时间 | "2024-01-15T10:30:00Z" |
| data.last_login_at | string | 是 | 上次登录时间 | "2024-01-21T10:30:00Z" |
| data.email_verified | boolean | 是 | 邮箱是否已验证 | true |
| data.preferences | object | 是 | 用户偏好设置 | {} |
| data.preferences.language | string | 是 | 界面语言 | "zh-CN" |
| data.preferences.theme | string | 是 | 界面主题 | "light" |
| data.preferences.notifications | object | 是 | 通知设置 | {} |
| data.preferences.notifications.email | boolean | 是 | 邮件通知开关 | true |
| data.preferences.notifications.push | boolean | 是 | 推送通知开关 | false |
| data.statistics | object | 是 | 用户统计数据 | {} |
| data.statistics.documents_count | int | 是 | 文档数量 | 25 |
| data.statistics.notes_count | int | 是 | 笔记数量 | 48 |
| data.statistics.reading_time | int | 是 | 总阅读时间（秒） | 12600 |

### 接口权限要求
- 需要用户登录
- 需要有效的JWT Token

### 接口调用频率限制
- 每分钟最多30次请求

---

## 更新用户信息

**接口名称：** 更新用户信息  
**功能描述：** 更新当前登录用户的个人信息  
**接口地址：** `/api/users/profile`  
**请求方式：** POST

### 功能说明

用户可以更新自己的基本信息，包括真实姓名、头像、偏好设置等。敏感信息如邮箱、密码需要通过专门的接口进行修改。

### 请求参数

**请求头参数：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**请求体参数：**
```json
{
  "real_name": "李四",
  "avatar": "https://example.com/avatars/new_avatar.jpg",
  "preferences": {
    "language": "en-US",
    "theme": "dark",
    "notifications": {
      "email": false,
      "push": true
    }
  }
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| real_name | string | 否 | 真实姓名，1-50字符 | "李四" |
| avatar | string | 否 | 头像URL，必须是有效的图片链接 | "https://example.com/avatars/new_avatar.jpg" |
| preferences | object | 否 | 用户偏好设置 | {} |
| preferences.language | string | 否 | 界面语言：zh-CN/en-US | "en-US" |
| preferences.theme | string | 否 | 界面主题：light/dark | "dark" |
| preferences.notifications | object | 否 | 通知设置 | {} |
| preferences.notifications.email | boolean | 否 | 邮件通知开关 | false |
| preferences.notifications.push | boolean | 否 | 推送通知开关 | true |

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "user_id": "user_123456789",
    "username": "john_doe",
    "email": "john@example.com",
    "real_name": "李四",
    "avatar": "https://example.com/avatars/new_avatar.jpg",
    "role": "user",
    "status": "active",
    "preferences": {
      "language": "en-US",
      "theme": "dark",
      "notifications": {
        "email": false,
        "push": true
      }
    },
    "updated_at": "2024-01-21T10:30:00Z"
  }
}
```

**错误响应示例：**
```json
{
  "code": 400,
  "msg": "头像URL格式不正确",
  "data": null
}
```

**响应字段说明：**

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| code | int | 是 | 状态码 | 200 |
| msg | string | 是 | 状态信息 | "success" |
| data | object | 是 | 更新后的用户信息 | {} |
| data.user_id | string | 是 | 用户唯一标识 | "user_123456789" |
| data.username | string | 是 | 用户名 | "john_doe" |
| data.email | string | 是 | 邮箱地址 | "john@example.com" |
| data.real_name | string | 否 | 真实姓名 | "李四" |
| data.avatar | string | 否 | 头像URL | "https://example.com/avatars/new_avatar.jpg" |
| data.role | string | 是 | 用户角色 | "user" |
| data.status | string | 是 | 用户状态 | "active" |
| data.preferences | object | 是 | 用户偏好设置 | {} |
| data.updated_at | string | 是 | 更新时间 | "2024-01-21T10:30:00Z" |

### 接口权限要求
- 需要用户登录
- 需要有效的JWT Token
- 只能修改自己的信息

### 接口调用频率限制
- 每分钟最多10次请求

---

## 修改密码

**接口名称：** 修改密码  
**功能描述：** 用户修改登录密码  
**接口地址：** `/api/users/change-password`  
**请求方式：** POST

### 功能说明

用户通过提供当前密码和新密码来修改登录密码。系统会验证当前密码的正确性，确保新密码符合安全要求。

### 请求参数

**请求头参数：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**请求体参数：**
```json
{
  "current_password": "OldPass123!",
  "new_password": "NewSecurePass456!",
  "confirm_password": "NewSecurePass456!"
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| current_password | string | 是 | 当前密码 | "OldPass123!" |
| new_password | string | 是 | 新密码，8-32字符，必须包含大小写字母、数字和特殊字符 | "NewSecurePass456!" |
| confirm_password | string | 是 | 确认新密码，必须与new_password一致 | "NewSecurePass456!" |

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "message": "密码修改成功",
    "updated_at": "2024-01-21T10:30:00Z"
  }
}
```

**错误响应示例：**
```json
{
  "code": 400,
  "msg": "当前密码不正确",
  "data": null
}
```

### 接口权限要求
- 需要用户登录
- 需要有效的JWT Token

### 接口调用频率限制
- 每小时最多5次请求

---

## 获取用户权限

**接口名称：** 获取用户权限  
**功能描述：** 获取当前用户的权限列表和角色信息  
**接口地址：** `/api/users/permissions`  
**请求方式：** GET

### 功能说明

获取当前登录用户的详细权限信息，包括角色权限、功能权限等，用于前端权限控制和功能展示。

### 请求参数

**请求头参数：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

无其他参数

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "user_id": "user_123456789",
    "role": "user",
    "role_name": "普通用户",
    "permissions": [
      {
        "module": "documents",
        "actions": ["read", "create", "update", "delete"]
      },
      {
        "module": "notes",
        "actions": ["read", "create", "update", "delete"]
      },
      {
        "module": "categories",
        "actions": ["read", "create"]
      },
      {
        "module": "profile",
        "actions": ["read", "update"]
      }
    ],
    "restrictions": {
      "max_documents": 100,
      "max_notes": 500,
      "max_categories": 20,
      "storage_limit": 1073741824
    }
  }
}
```

**错误响应示例：**
```json
{
  "code": 401,
  "msg": "Token无效或已过期",
  "data": null
}
```

**响应字段说明：**

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| code | int | 是 | 状态码 | 200 |
| msg | string | 是 | 状态信息 | "success" |
| data | object | 是 | 权限信息 | {} |
| data.user_id | string | 是 | 用户唯一标识 | "user_123456789" |
| data.role | string | 是 | 用户角色代码 | "user" |
| data.role_name | string | 是 | 用户角色名称 | "普通用户" |
| data.permissions | array | 是 | 权限列表 | [] |
| data.permissions[].module | string | 是 | 功能模块名称 | "documents" |
| data.permissions[].actions | array | 是 | 允许的操作列表 | ["read", "create", "update", "delete"] |
| data.restrictions | object | 是 | 使用限制 | {} |
| data.restrictions.max_documents | int | 是 | 最大文档数量 | 100 |
| data.restrictions.max_notes | int | 是 | 最大笔记数量 | 500 |
| data.restrictions.max_categories | int | 是 | 最大分类数量 | 20 |
| data.restrictions.storage_limit | int | 是 | 存储空间限制（字节） | 1073741824 |

### 接口权限要求
- 需要用户登录
- 需要有效的JWT Token

### 接口调用频率限制
- 每分钟最多20次请求

---

## 管理员获取用户列表

**接口名称：** 获取用户列表  
**功能描述：** 管理员获取系统中所有用户的列表信息  
**接口地址：** `/api/users`  
**请求方式：** GET

### 功能说明

管理员可以获取系统中所有用户的列表，支持分页、搜索、筛选等功能。用于用户管理、权限分配等管理功能。

### 请求参数

**请求头参数：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**查询参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| page | int | 否 | 页码，默认1 | 2 |
| page_size | int | 否 | 每页数量，默认20，最大100 | 50 |
| search | string | 否 | 搜索关键词，支持用户名、邮箱、真实姓名 | "john" |
| role | string | 否 | 角色筛选：user/admin/super_admin | "user" |
| status | string | 否 | 状态筛选：active/suspended/pending_verification | "active" |
| sort_by | string | 否 | 排序字段：created_at/last_login_at/username | "created_at" |
| sort_order | string | 否 | 排序方向：asc/desc，默认desc | "desc" |

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "users": [
      {
        "user_id": "user_123456789",
        "username": "john_doe",
        "email": "john@example.com",
        "real_name": "张三",
        "role": "user",
        "status": "active",
        "created_at": "2024-01-15T10:30:00Z",
        "last_login_at": "2024-01-21T10:30:00Z",
        "email_verified": true,
        "documents_count": 25,
        "notes_count": 48
      }
    ],
    "pagination": {
      "current_page": 1,
      "page_size": 20,
      "total_count": 156,
      "total_pages": 8,
      "has_next": true,
      "has_prev": false
    }
  }
}
```

**错误响应示例：**
```json
{
  "code": 403,
  "msg": "权限不足",
  "data": null
}
```

### 接口权限要求
- 需要用户登录
- 需要管理员权限（admin或super_admin角色）

### 接口调用频率限制
- 每分钟最多30次请求

---

## 管理员更新用户权限

**接口名称：** 更新用户权限  
**功能描述：** 管理员修改指定用户的角色和权限  
**接口地址：** `/api/users/{user_id}/permissions`  
**请求方式：** POST

### 功能说明

超级管理员可以修改其他用户的角色和权限设置。普通管理员只能修改普通用户的权限，不能修改其他管理员的权限。

### 请求参数

**请求头参数：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| user_id | string | 是 | 目标用户ID | "user_123456789" |

**请求体参数：**
```json
{
  "role": "admin",
  "status": "active",
  "restrictions": {
    "max_documents": 500,
    "max_notes": 2000,
    "max_categories": 50,
    "storage_limit": 5368709120
  }
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|-------|------|-----|------|--------|
| role | string | 否 | 用户角色：user/admin/super_admin | "admin" |
| status | string | 否 | 用户状态：active/suspended/pending_verification | "active" |
| restrictions | object | 否 | 使用限制设置 | {} |
| restrictions.max_documents | int | 否 | 最大文档数量 | 500 |
| restrictions.max_notes | int | 否 | 最大笔记数量 | 2000 |
| restrictions.max_categories | int | 否 | 最大分类数量 | 50 |
| restrictions.storage_limit | int | 否 | 存储空间限制（字节） | 5368709120 |

### 响应参数

**成功响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "user_id": "user_123456789",
    "username": "john_doe",
    "role": "admin",
    "status": "active",
    "restrictions": {
      "max_documents": 500,
      "max_notes": 2000,
      "max_categories": 50,
      "storage_limit": 5368709120
    },
    "updated_at": "2024-01-21T10:30:00Z",
    "updated_by": "admin_987654321"
  }
}
```

**错误响应示例：**
```json
{
  "code": 403,
  "msg": "权限不足，无法修改该用户权限",
  "data": null
}
```

### 接口权限要求
- 需要用户登录
- 需要管理员权限（admin或super_admin角色）
- super_admin可以修改所有用户权限
- admin只能修改普通用户权限

### 接口调用频率限制
- 每分钟最多10次请求