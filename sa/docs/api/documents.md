# 文献管理模块 API 文档

## 📖 概述

本模块负责处理文献（PDF）的生命周期管理。包括文件的上传存储、元数据管理（标题、分类）、列表查询以及文件二进制流的读取。

**设计理念说明：** 我们将“元数据管理”与“文件流服务”合并在本文档中。虽然在后端代码中它们可能分属 `DocumentController` 和 `FileController`，但对前端而言，它们都是对 **Document (文献)** 这一资源的操作。

## 🔗 基础信息

- **基础 URL：** `/api/documents`
- **服务端口：** `10150` (参考 `application.yaml`)
- **认证方式：** Header 中需携带 Token (参考 `UserContextUtil`)
- **数据格式：**
  - 请求体：`multipart/form-data` (上传) 或 `application/json` (其他)
  - 响应体：标准 `Result<T>` 封装

## 📋 接口列表

### 1. 上传文献

**接口名称：** 上传并创建文献

**接口地址：** `/api/documents/upload`

**请求方式：** `POST`

**Content-Type：** `multipart/form-data`

#### ⚙️ 功能说明

用户上传 PDF 文件。

- **后端逻辑**：后端会接收文件 -> 计算 MD5 (防重复) -> 存储到本地/OSS -> 提取文件名作为默认标题 -> 写入数据库 -> 触发异步解析任务（如有）。
- **状态流转**：上传成功后，文档的解析状态 (`parseStatus`) 默认为 `PENDING` 或 `PROCESSING`。

#### 📥 请求参数 (Form-Data)

| 参数名         | 类型 | 必填 | 说明                              |
| -------------- | ---- | ---- | --------------------------------- |
| **file**       | File | ✅ 是 | PDF 文件二进制流 (限制 50MB)      |
| **categoryId** | Long | ❌ 否 | 分类 ID。如果不传，归为默认分类。 |

#### 📤 响应参数 (`Result<DocumentUploadResponse>`)

```
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "1865230658941235200",
    "filename": "Attention_Is_All_You_Need.pdf",
    "status": 1,
    "uploadTime": "2025-12-19 10:00:00"
  }
}
```

### 2. 获取文献列表 (分页)

**接口名称：** 文献列表查询

**接口地址：** `/api/documents`

**请求方式：** `GET`

#### ⚙️ 功能说明

用于在“我的文献”页面展示列表。支持按分类筛选和关键词搜索。

#### 📥 请求参数 (Query)

| 参数名         | 类型    | 必填 | 说明                           | 示例          |
| -------------- | ------- | ---- | ------------------------------ | ------------- |
| **page**       | Integer | ❌ 否 | 页码，默认 1                   | `1`           |
| **pageSize**   | Integer | ❌ 否 | 每页条数，默认 10              | `10`          |
| **categoryId** | Long    | ❌ 否 | 筛选特定分类下的文献           | `1865230...`  |
| **keyword**    | String  | ❌ 否 | 搜索关键词（匹配标题或文件名） | `Transformer` |

#### 📤 响应参数 (`Result<PageResult<DocumentListResponse>>`)

> **注意**：`id` 字段在 JSON 中应为 String 类型，防止 JavaScript 处理 Long 类型雪花 ID 时的精度丢失。

```
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 52,
    "records": [
      {
        "id": "1865230658941235200",
        "title": "Attention Is All You Need",
        "filename": "attention.pdf",
        "size": 2048576,
        "categoryName": "深度学习",
        "parseStatus": "SUCCESS", 
        "createTime": "2025-12-01 12:00:00"
      }
    ]
  }
}
```

- `parseStatus` 说明：
  - `WAITING`: 等待解析
  - `PROCESSING`: 解析中 (前端可显示 Loading 动画)
  - `SUCCESS`: 解析完成 (可点击阅读)
  - `FAIL`: 解析失败

### 3. 获取文献详情

**接口名称：** 获取文献详情

**接口地址：** `/api/documents/{id}`

**请求方式：** `GET`

#### ⚙️ 功能说明

在点击列表项进入“阅读页”或“编辑页”时调用。包含比列表更详细的信息（如摘要、解析进度详情等）。

#### 📥 请求参数 (Path)

| 参数名 | 类型 | 必填 | 说明    |
| ------ | ---- | ---- | ------- |
| **id** | Long | ✅ 是 | 文献 ID |

#### 📤 响应参数 (`Result<DocumentDetailResponse>`)

```
{
  "code": 200,
  "data": {
    "id": "1865230658941235200",
    "title": "Attention Is All You Need",
    "description": "这是Transformer模型的开山之作...",
    "pageCount": 15,
    "categoryId": "1865230...",
    "fileUrl": "/api/documents/1865230658941235200/file", 
    "parseStatus": "SUCCESS"
  }
}
```

- `fileUrl`: 前端 PDF 阅读器可以直接使用这个 URL 加载文件流。

### 4. 获取文献文件流 (预览/下载)

**接口名称：** 获取 PDF 文件流

**接口地址：** `/api/documents/{id}/file`

**请求方式：** `GET`

#### ⚙️ 功能说明

这是专门为 **PDF.js** 或浏览器原生预览器设计的接口。

- 它支持 HTTP `Range` 头，允许断点续传和分片加载（对于大体积 PDF 非常重要，能极大提升首屏加载速度）。
- **权限控制**：不同于直接访问静态资源服务器（Nginx），该接口经过了 Spring Security 拦截链，确保只有拥有权限的用户才能下载/预览该文件。

#### 📥 请求参数 (Path & Header)

| 参数名            | 位置   | 必填 | 说明                                                    |
| ----------------- | ------ | ---- | ------------------------------------------------------- |
| **id**            | Path   | ✅ 是 | 文献 ID                                                 |
| **Range**         | Header | ❌ 否 | 字节范围 (例如 `bytes=0-1023`)，通常由 PDF 插件自动发送 |
| **Authorization** | Header | ✅ 是 | Bearer Token                                            |

#### 📤 响应内容

- **Content-Type**: `application/pdf`
- **Body**: 二进制文件流

### 5. 修改文献信息

**接口名称：** 更新文献元数据

**接口地址：** `/api/documents/{id}`

**请求方式：** `PUT`

#### ⚙️ 功能说明

用户修改标题、移动分类或添加备注描述。

#### 📥 请求参数 (Body)

```
{
  "title": "新标题",
  "categoryId": "1865230...",
  "description": "这是我修改后的备注"
}
```

#### 📤 响应参数

```
{ "code": 200, "message": "操作成功", "data": true }
```

### 6. 删除文献

**接口名称：** 删除文献

**接口地址：** `/api/documents/{id}`

**请求方式：** `DELETE`

#### ⚙️ 功能说明

逻辑删除或物理删除文献记录。

- **级联操作**：删除文献通常意味着需要级联删除其产生的 向量数据(Vector)、笔记(Notes) 和 历史对话(ChatSession)。后端代码中应处理这些逻辑。

#### 📥 请求参数 (Path)

| 参数名 | 类型 | 必填 | 说明    |
| ------ | ---- | ---- | ------- |
| **id** | Long | ✅ 是 | 文献 ID |

#### 📤 响应参数

```
{ "code": 200, "message": "操作成功", "data": true }
```

## 💡 重点补充说明（Wait, Why?）

### 1. 为什么文件流接口 (`/file`) 不直接用 Nginx 静态代理？

你可能会问，直接把文件扔到 Nginx 目录下让前端访问 `http://xxx/uploads/a.pdf` 不是更快吗？ **理由**：

- **安全性 (Security)**：如果用 Nginx 静态代理，只要有人知道 URL 就能下载，无法验证用户是否登录，也无法验证该用户是否有权查看该文档（比如私有文档）。
- **统一鉴权**：通过 Java 接口 `/api/documents/{id}/file`，我们可以复用现有的 JWT 认证体系。`FileController` 代码中显式调用了文件读取服务，确保了控制权在业务逻辑手中。

### 2. 关于 ID 的精度问题

后端数据库使用了 `Bigint` (Snowflake ID)，Java 中对应 `Long`。 前端 JavaScript 的 `Number` 类型最大安全整数是 $2^{53}-1$。Snowflake ID 通常会超过这个范围。 **解决方案**：后端在序列化 JSON 时，**必须**将 Long 类型的 ID 转换为 String 类型。

- 检查代码：你的 `Document` 实体类中 ID 字段是否加了 `@JsonSerialize(using = ToStringSerializer.class)` 注解？如果没有，前端获取到的 ID 后面几位会变成 0，导致请求 404。请务必确认这一点。

### 3. 解析状态的作用

上传 PDF 不仅仅是“保存文件”。为了实现 AI 问答，后端需要对 PDF 进行：

1. 文本提取 (OCR/Text Extraction)
2. 分块 (Chunking)
3. 向量化 (Embedding) **这个过程是耗时的。** 所以上传接口返回成功，只代表“文件存下来了”，不代表“AI 准备好了”。前端必须根据 `parseStatus` 来提示用户“AI 正在学习该文档，请稍候...”。