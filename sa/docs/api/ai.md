# AI 模块 API 文档 (RAG 专用版)

## 📖 概述

AI 模块专门用于**文献辅助阅读系统**。它不再是一个通用的 LLM 代理，而是深度集成了 RAG（检索增强生成）流程。 后端会自动根据 `documentId` 检索向量数据库中的论文片段，构建 Prompt 并流式返回答案。

## 🔗 基础信息

- **基础 URL：** `/api/ai`
- **服务端口：** `10160` (参考 `application.yaml`)
- **认证方式：** Header 中需携带 Token (参考 `UserContextUtil`)
- **内容类型：** `application/json`

## 📋 接口列表

### 1. 发送 AI 对话 (RAG 问答)

**接口名称：** 文献智能问答

**接口地址：** `/api/ai/chat`

**请求方式：** `POST`

**响应格式：** `text/event-stream` (SSE 流式)

#### ⚙️ 功能说明

用户针对某篇文献或特定会话发送问题。后端执行以下逻辑：

1. **上下文管理**：自动根据 `chatId` 保存/读取历史记录。
2. **RAG 检索**：
   - 先检索 `anchor` (摘要/总结) 以获取宏观背景。
   - 再检索 `content` (正文切片) 以获取具体细节。
3. **Prompt 组装**：将检索到的上下文填入模板。
4. **流式响应**：通过 SSE 逐字返回 AI 的回答。

#### 📥 请求参数 (Body)

对应的 Java 类：`com.zd.scaiservice.model.dto.request.ChatRequest`

```
{
  "chatId": "可为空，但强烈建议前端生成 UUID",
  "documentId": "123456",
  "prompt": "这篇文章主要讲了什么创新点？"
}
```

|

| **参数名** | **类型** | **必填** | **说明** | | **prompt** | string | ✅ 是 | 用户的提问内容。 | | **documentId** | string | ✅ 建议 | 当前正在阅读的文献 ID。用于限定 AI 的回答范围（RAG 检索上下文）。 | | **chatId** | string | ❌ 否 | 会话 ID。  1. **推荐**：前端生成 UUID 传入，以便前端能维护会话状态。  2. **不推荐**：传空，由后端生成（但目前后端流式响应未返回该 ID，会导致前端丢失上下文）。 |

#### 📤 响应说明

响应是一个 **Text Event Stream (SSE)**。 不同于标准的 SSE (`data: ...`)，Spring AI 的 `Flux<String>` 默认可能会直接返回纯文本流。 **前端接收到的将是 AI 生成的文本片段（Chunk）。**

**流式数据示例：**

```
这
篇
文章
的
核心
创新点
是
...
```

### 2. 获取某文档的历史会话列表

**接口名称：** 获取文档会话列表

**接口地址：** `/api/ai/history/{documentId}`

**请求方式：** `GET`

#### ⚙️ 功能说明

获取当前用户在阅读某篇文档时产生的历史会话 ID 列表（例如：用户可能针对同一篇论文开启了多次不同的问答）。

#### 📥 请求参数 (Path)

| **参数名** | **类型** | **必填** | **说明** | | **documentId** | string | ✅ 是 | 文献 ID |

#### 📤 响应参数

返回一个字符串列表，包含所有的 `chatId`。

```
[
  "chat_uuid_1",
  "chat_uuid_2",
  "chat_uuid_3"
]
```

### 3. 获取指定会话的详细消息

**接口名称：** 获取会话详情

**接口地址：** `/api/ai/history/{documentId}/{chatId}`

**请求方式：** `GET`

#### ⚙️ 功能说明

加载某个特定会话（`chatId`）中的所有历史聊天记录（用户提问 + AI 回答），通常用于页面初始化时回显历史。

#### 📥 请求参数 (Path)

| **参数名** | **类型** | **必填** | **说明** | | **documentId** | string | ✅ 是 | 文献 ID | | **chatId** | string | ✅ 是 | 会话 ID |

#### 📤 响应参数

对应的 Java 类：`List<MessageResponse>`

```
[
  {
    "role": "user",
    "content": "这篇文章的作者是谁？"
  },
  {
    "role": "assistant",
    "content": "根据文档信息，作者是 Zhang San 和 Li Si..."
  }
]
```

| **字段** | **类型** | **说明** | | **role** | string | 角色。`user` (用户) 或 `assistant` (AI)。 | | **content** | string | 消息的具体文本内容。 |

### 4. 删除会话

**接口名称：** 删除会话

**接口地址：** `/api/ai/history/{documentId}/{chatId}`

**请求方式：** `DELETE`

#### ⚙️ 功能说明

删除指定的会话记录。

#### 📥 请求参数 (Path)

| **参数名** | **类型** | **必填** | **说明** | | **documentId** | string | ✅ 是 | 文献 ID | | **chatId** | string | ✅ 是 | 会话 ID |

#### 📤 响应参数

无响应体（HTTP 200 OK）。

## 💻 前端对接指南 (Vue/React 参考)

由于后端接口是 `POST` 请求但返回流式数据 (`text/event-stream`)，标准的 `EventSource` (浏览器原生 API) **不支持 POST 请求**。

你需要使用 `fetch` 配合 `ReadableStream`。

### 推荐实现方案 (Fetch API)

```
// 生成 UUID 的工具函数 (前端必须自己生成 chatId)
import { v4 as uuidv4 } from 'uuid';

async function sendMessageToAI(prompt: string, documentId: string, currentChatId?: string) {
  // 1. 如果没有会话ID，生成一个新的
  const chatId = currentChatId || uuidv4();
  
  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + localStorage.getItem('token') // 假设 Token 存在 localStorage
      },
      body: JSON.stringify({
        prompt: prompt,
        documentId: documentId,
        chatId: chatId // 关键：前端生成并传递 ID
      })
    });

    if (!response.ok) {
      throw new Error('Network error');
    }

    if (!response.body) {
      throw new Error('No response body');
    }

    // 2. 获取 reader 读取流
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    
    // 3. 循环读取数据块
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      // 解码数据块 (Chunk)
      const textChunk = decoder.decode(value, { stream: true });
      
      // 4. 更新 UI (append 到当前对话框)
      console.log('Stream chunk:', textChunk);
      // updateLastMessage(textChunk); 
    }
    
    // 5. 返回当前使用的 chatId，以便组件状态更新
    return chatId;

  } catch (error) {
    console.error('Chat error:', error);
  }
}
```

### 常见问题排查 (Troubleshooting)

1. **收不到流式效果，而是一次性返回？**
   - **原因**：Nginx 或网关开启了缓冲 (Buffering)。
   - **解决**：检查 Nginx 配置，确保 `proxy_buffering off;`，或者检查后端返回头是否包含 `X-Accel-Buffering: no`。
2. **`chatId` 为空时历史记录丢失？**
   - **原因**：后端生成了 ID 但没返回给前端。
   - **解决**：务必按照上述指南，**在前端生成 `chatId`** 并传给后端，不要依赖后端生成。