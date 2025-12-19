### 一、项目核心定位

你的项目本质上是一个：

> “基于 Spring AI 的智能文献管理与检索系统”。

也就是——
 普通的文献管理系统负责**存储、分类、检索、标注**等操作；
 而你通过 RAG（Retrieval-Augmented Generation，检索增强生成）+ Spring AI，让系统具备**智能摘要、语义搜索、智能问答**等高级功能。

这在课程设计里属于**“信息系统 + 人工智能应用”混合型项目**，非常有亮点。

------

### 二、功能模块划分（合理又可实现）

#### （1）基础模块（占60%工作量）

> 这些是必须的 CRUD 功能，保证系统能运行。

- **文献管理**：上传（PDF/文本）、分类、标注、编辑、删除、预览。
- **用户管理**（可简化为单用户或登录注册）。
- **文献分类管理**：支持层级分类与关键词标签。
- **全文检索**：基于 ElasticSearch / PostgreSQL full-text search / 向量检索。

#### （2）AI增强模块（高分关键）

> 这些部分展示你对 Spring AI、RAG、LLM 的理解。

- **智能摘要**：用户上传文献后，自动调用 Spring AI + OpenAI 接口生成摘要。
- **语义检索（RAG）**：将文献向量化（例如使用 pgvector 或 Pinecone），实现语义级搜索。
- **AI 辅助阅读**：用户选中文献后，可向 AI 提问（如“这篇论文的主要创新点是什么？”），系统基于检索到的上下文内容进行回答。
- **推荐系统（可选）**：AI 根据用户已读文献，推荐相似研究。

#### （3）扩展模块（锦上添花）

> 若时间有余，增加这些展示综合能力。

- **微服务拆分**：文献服务、AI服务、用户服务、网关服务。
- **文档解析微服务**：负责 PDF → 文本 → 向量化处理。
- **日志与监控**：Spring Boot Admin / Zipkin 追踪链路。
- **前端展示（简单 Vue/React）**：AI 聊天阅读界面。

------

### 三、技术栈建议（你现有技能完全能撑起来）

| 模块           | 技术选型                                |
| -------------- | --------------------------------------- |
| 主体框架       | Spring Boot + Spring Cloud              |
| AI集成         | Spring AI + OpenAI 或 Ollama（本地）    |
| 向量存储       | PostgreSQL + pgvector 或 Milvus         |
| 文献存储       | MySQL / PostgreSQL                      |
| 文档解析       | Apache PDFBox / Tika                    |
| 检索增强       | RAG pipeline：检索 + Prompt 构造 + 生成 |
| 网关与注册中心 | Spring Cloud Gateway + Nacos / Eureka   |
| 前端           | Vue3 + Element Plus（简洁展示即可）     |

------

### 四、可行性分析

| 维度         | 分析                                                       |
| ------------ | ---------------------------------------------------------- |
| **技术难度** | 中等偏上。Spring AI + RAG 有新颖性，但资料充足。           |
| **实现周期** | 一周能完成后端（见下文计划）。                             |
| **创新度**   | 明显高于“书店”“论坛”等常规题。                             |
| **演示性**   | 可展示智能问答、语义检索等炫酷功能，非常加分。             |
| **风险**     | 向量数据库与 AI 接口调试稍耗时；要提前选好模型与存储方案。 |

------

### 五、一周实现计划（针对后端）

| 天数  | 任务内容                                 | 输出成果           |
| ----- | ---------------------------------------- | ------------------ |
| 第1天 | 项目搭建（Spring Boot + Cloud 模块划分） | 架构基础、依赖配置 |
| 第2天 | 文献与分类模块（增删查改 + MyBatis）     | REST 接口          |
| 第3天 | 文档上传解析（PDF→文本）、数据库入库     | PDF 提取服务       |
| 第4天 | 向量化与语义检索（pgvector + Embedding） | RAG pipeline       |
| 第5天 | Spring AI 接入（摘要生成、问答接口）     | AI 模块可交互      |
| 第6天 | 集成调试、接口联调                       | 系统通路跑通       |
| 第7天 | 文档撰写 + 前端展示界面                  | 演示版上线         |

------

### 六、评分潜力分析

| 评价维度         | 你的系统                   | 优势               |
| ---------------- | -------------------------- | ------------------ |
| **功能完整度**   | ✅ CRUD + 搜索 + AI 阅读    | 覆盖全面           |
| **技术先进性**   | ✅ Spring AI、RAG、pgvector | 明显优于常规项目   |
| **系统复杂度**   | ✅ 微服务可选加分           | 展示架构思维       |
| **可演示性**     | ✅ AI 聊天式阅读            | 演示直观、有“哇”感 |
| **工作量合理性** | ✅ 一周后端可完成           | 成果感强           |

------

### 七、扩展建议（可为答辩时准备）

1. **展示RAG架构图**：从用户提问 → 向量检索 → 拼接上下文 → 模型回答。
2. **展示AI提示词设计（Prompt Engineering）**：说明如何保证答案围绕文献内容生成。
3. **展示对比**：普通检索 vs 语义检索效果对比。
4. **展示日志与微服务架构**：体现工程化能力。

------

总结一句话：

> 这个项目选题“既有CRUD的工程厚度，又有AI的创新亮点”，
>  一周时间全力推进后端完全可行，只要结构清晰、功能完整、展示自然，你的分数不会低。









# 文献管理系统 — 需求文档

## 1. 项目概述

构建一个基于 Spring（微服务可选）与 PostgreSQL + pgvector 的文献管理系统，支持：

- 类目（文件夹/类别）管理（增删改查）；
- 论文（PDF）上传、存储、查询与删除（不支持修改内容）；
- 基于 RAG 的 AI 辅助阅读：对论文进行语义检索并以对话/问答形式提供摘要与问答服务；
- 用户登录后可查看自己历史上传记录并继续交互。

目标：功能完整、工程可演示、有 AI 创新点，后端一周可完成 MVP。

------

## 2. 参与者（Actors）

- 普通用户（登录/匿名视情况决定）：上传/删除/查询文献，创建/修改/删除类目，AI 提问交互。
- 后端服务：文件解析服务（PDF→文本）、向量化服务（Embedding）、AI 服务（调用 LLM）、数据库（Postgres + pgvector）。

------

## 3. 总体功能（要点）

1. 用户可创建类目（Category）：
   - 名称可创建/修改/删除（删除类目时必须空）。
2. 在类目中上传论文（PDF）：
   - 上传后解析为纯文本，切分成 chunk，生成 embedding 并入库。
3. 类目下查询论文列表（分页）。
4. 论文详情页：
   - 显示元信息（标题、作者、上传时间、摘要（AI 自动生成或原文摘要））。
   - 可发起对话式问答（AI 辅助阅读），例如“请总结中心思想”。
   - 支持删除论文；不支持修改论文内容（元信息允许编辑或根据需求决定）。
5. 权限规则：
   - 普通用户只能管理自己上传的类目/文献（除非设计为共享空间）。
6. 日志与错误处理：上传/解析/向量化失败需要有回滚与失败告警。
7. 数据一致性：文献元信息与 embedding 必须保持事务一致性。

------

## 4. 详细功能描述（含边界条件）

### 4.1 类目（Category）

- 创建：必须包含名称；名称长度限制（例如 1–100 字符）；用户可自定义。
- 修改：可以修改名称和描述。
- 删除：仅当该类目下没有任何文献（文档表无记录）时允许删除；否则返回 400/409 错误并提示原因。
- 查询：列出用户所有类目（支持分页与排序）。

### 4.2 论文（Document）

- 新增（上传）：
  - 上传 PDF 文件（支持大小限制，例如 ≤ 50MB；可根据环境调整）。
  - 服务器解析 PDF → 提取文本（建议用 Apache PDFBox/Tika）。
  - 文本清洗（去空行、合并页眉页脚等简单规则）。
  - 切分为 chunk（建议每 chunk 300–1000 字或 ~500 tokens，允许重叠 50–100 字以保留上下文）。
  - 对每个 chunk 生成 embedding（固定维度，例如 1536；视使用模型而定）。
  - 将元数据（title、author、category_id、uploader_id、upload_time、原始文件路径或对象存储路径）与 chunks（含 embedding）存入 Postgres（embedding 列使用 pgvector）。
  - 事务：若任何一步失败需回滚并删除已入库的中间数据与文件。
- 删除：
  - 用户可删除自己上传的论文；删除动作应删除 document 记录、对应 chunks 和向量、以及存储中的原始文件。
- 查询列表：
  - 支持按类目列出、按关键词（title、author、tags）模糊搜索、分页排序。
- 详情：
  - 返回元信息 + AI 自动摘要（可缓存摘要） + 可触发 AI 问答会话。
- 修改：
  - 题主要求：**不支持**修改论文内容（可支持修改 metadata，如标题/标签），在需求文档中明确。

### 4.3 AI 辅助阅读（RAG 闭环）

- 流程：
  1. 用户提问 → 服务端将问题转成 embedding；
  2. 在 chunks 表上做向量相似度检索（ORDER BY embedding <-> query_embedding LIMIT k）；
  3. 将 top-k 相关 chunk 拼接（注意长度限制）并构造 prompt（包含 system instruction、context、question）；
  4. 调用 LLM（通过 Spring AI / OpenAI / 其它模型）生成回答；
  5. 返回回答给前端；可同时返回用于说明的来源 chunk ID 列表（用于可追溯性/展示“证据”）。
- Prompt 设计要点（文档内须保存模板、并可在需要时改进）：
  - System: “你是一个基于给定文献段落的学术助理，回答要基于提供的段落，不要编造事实，如果无法回答请说明并返回最相关段落。”
  - User/question: 包含用户自然语言问题。
- 缓存：对常见问题/摘要进行缓存，减少重复调用模型与费用。

------

## 5. 数据库设计（建议 ER & 核心表）

### 5.1 核心表（建议）

```mysql
-- 启用 pgvector 扩展（一次性）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE categories (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  name TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE documents (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  category_id BIGINT REFERENCES categories(id),
  title TEXT,
  authors TEXT,
  original_filename TEXT,
  storage_path TEXT, -- 本地路径或对象存储路径
  summary TEXT,      -- AI 自动摘要（可为空）
  uploaded_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chunks (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT REFERENCES documents(id) ON DELETE CASCADE,
  chunk_index INT NOT NULL,
  text TEXT NOT NULL,
  embedding VECTOR(1536), -- 根据 embedding 模型维度设置
  created_at TIMESTAMP DEFAULT NOW()
);
```

### 5.2 索引与检索

- 在 `chunks` 上创建向量索引（pgvector 支持 ivfflat/flat）

```
-- 伪代码，实际需根据 pgvector 版本及配置调整
CREATE INDEX ON chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

- 在 `documents` 的 title/authors 建模全文索引（GIN）或简单 LIKE 索引以支持元数据搜索。

------

## 6. API 设计建议（概要，供你写 API 文档用）

说明：下面为建议的 REST 风格接口，返回 JSON，错误用 standard codes。

### 6.1 类目（Category）

- `POST /api/categories`
   请求体：`{ "name": "...", "description": "..." }`
- `GET /api/categories`
   支持分页 `?page=1&size=20`
- `PUT /api/categories/{id}`
   修改名称/描述
- `DELETE /api/categories/{id}`
   删除（如果类目下有文献返回 409）

### 6.2 文献（Document）

- `POST /api/categories/{categoryId}/documents`
   表单上传 `multipart/form-data`：file=pdf，metadata 字段可选（title, authors）
   返回：document id + 状态（解析中/完成/失败）
- `GET /api/categories/{categoryId}/documents`
   列表，分页与模糊查询 `?q=关键词&page=...`
- `GET /api/documents/{documentId}`
   详情：metadata + summary + basic preview（如文本片段）
- `DELETE /api/documents/{documentId}`
   删除文献（权限检查）
- `PATCH /api/documents/{documentId}/metadata`
   修改可改字段（例如 title、tags），但不修改文件内容

### 6.3 AI 问答（对话）

- `POST /api/documents/{documentId}/qa`
   请求体：`{ "question": "请帮我总结..." }`
   返回：`{ "answer": "...", "sources": [{chunk_id, score, text_snippet}], "prompt_used": "..." }`
- `POST /api/qa` （全局文献集合搜索+问答）
   支持跨文献检索（例如在用户所有文献中检索）

### 6.4 状态与任务（异步任务）

- `GET /api/tasks/{taskId}` 查看长耗时任务（解析/向量化）状态。

------

## 7. 上传 & 解析流程（序列）

1. 前端上传文件到后端 `POST /documents`。
2. 后端保存原始文件（本地或对象存储），创建 `documents` 记录（状态 = processing）。
3. 后端异步调用文档解析服务（或线程）：
   - 解析 PDF → 得到纯文本；
   - 切分为 chunks；
   - 批量调用 Embedding 接口获取向量；
   - 插入 `chunks` 表（embedding 字段）；
4. 若成功：更新 `documents.summary`（可触发一次自动摘要生成）、status=ready；若失败则回滚并记录错误。

注意：即便是异步，也要保持事务语义：若解析阶段失败，删除刚创建的 documents 记录或标记为 failed 并提供任务日志。

------

## 8. 非功能需求（NFR）

- **并发**：支持并发上传（限制并发数，防止嵌入 API 被滥用）。
- **性能**：小规模数据（课程设计）下 pgvector 足够；建议初始化 ivf 参数以加速检索。
- **安全**：用户认证（JWT）、文件扫描与大小限制、上传权限校验。
- **可靠性**：失败回滚、任务重试机制、错误日志与告警。
- **可维护性**：日志记录、可配置 embedding 模型与维度、可调整 chunk 大小。
- **成本控制**：AI 调用应限速与预算监控（避免测试期间产生高额费用）。

------

## 9. 验收标准（Acceptance Criteria / 测试用例）

- 类目能被创建/修改/查询；尝试删除含文档类目返回错误。
- 上传 PDF 能在后台被解析并在 1~5 分钟内完成（取决于模型与文件大小），生成 chunks 与 embedding 并可通过 `GET /documents/{id}` 查看。
- AI 问答接口能返回连贯答案并列出来源 chunk id（top-k），且在明显无法回答时返回“无法在提供文献中找到答案”的提示。
- 删除文档后相关 chunks 被级联删除，文件从存储中移除。
- 权限测试：不同用户不能删除他人文献/类目。
- 错误场景测试：上传损坏PDF、embedding 服务故障、数据库写入失败，应有合理错误码与日志。

------

## 10. 扩展建议（可选加分项）

- **多用户共享类目/权限管理**（团队协作模式）。
- **标签与推荐系统**：基于向量相似度推荐相关文章。
- **版本控制**：文献的注释与高亮保存，并支持版本回滚。
- **全文检索混合（向量+全文）**：先用关键词过滤再用向量精排。
- **前端**：实现“聊天式阅读器”界面，显示 AI 回答并高亮来源段落。
- **监控**：Prometheus + Grafana 跟踪调用次数/延迟/失败率。

------

## 11. 实施注意事项（工程细节）

- 建议先做单体后拆微服务：一周内完成后端单体版（包含 AI、解析、pgvector）；之后根据需要拆出 document-service、ai-service、parser-service。
- Embedding 调用应采用批量方式（节省时间/请求数）。
- Chunk 切分策略对效果影响较大：实验并选择合适长度与重叠比例。
- Prompt 里务必加入“只使用提供的上下文回答”与“返回来源段落 id”：这样可避免 hallucination（模型虚构）。

------

## 12. 示例 Prompt 模板（基础版）

**System**:

```
你是一个学术助理，只能基于下列文献片段回答问题。不要编造事实。如无法回答，请说明并返回最相关片段编号。
```

**User**（拼接 context + question）:

```
Context:
[1] 段落文本A
[2] 段落文本B
...
Question: 请基于以上内容回答："请帮我总结这篇论文的中心思想"
```

返回格式（建议 JSON）：

```
{
  "answer": "...",
  "sources": [
    {"chunk_id": 123, "score": 0.12, "snippet": "...."},
    ...
  ]
}
```

------

## 13. 最终一句话建议（答辩用）

> 把 PostgreSQL + pgvector 作为单一数据库来存储元数据与向量，是平衡工程复杂度与功能深度的最佳选择。系统的关键亮点是 RAG 流水线（PDF→chunk→embedding→检索→生成），答辩时画出这条流水线，能让评审一眼看懂你的工程深度与技术栈选型逻辑。





# 接口列表（详细）

------

### 1. 注册

`POST /api/auth/register`
 Auth: 无

请求（JSON）：

```
{
  "username": "alice",
  "password": "Password123"
}
```

响应（成功）：

```
{
  "code": 0,
  "msg": "ok",
  "data": {
    "userId": 123,
    "username": "alice"
  }
}
```

错误示例（用户名已存在）：

```
{ "code": 4001, "msg": "username already exists", "data": {} }
```

------

### 2. 登录

`POST /api/auth/login`
 Auth: 无

请求：

```
{
  "username": "alice",
  "password": "Password123"
}
```

响应（成功）：

```
{
  "code": 0,
  "msg": "ok",
  "data": {
    "userId": 123,
    "username": "alice",
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 3600
  }
}
```

错误（凭证错误）：

```
{ "code": 1002, "msg": "invalid username or password", "data": {} }
```

------

### 3. 创建类目（Category）

`POST /api/categories`
 Auth required

请求（JSON）：

```
{
  "name": "计算机视觉",
  "description": "CV 相关论文"
}
```

响应：

```
{
  "code": 0,
  "msg": "ok",
  "data": {
    "categoryId": 45,
    "name": "计算机视觉",
    "description": "CV 相关论文",
    "createdAt": "2025-10-29T10:00:00Z"
  }
}
```

校验：

- `name` 必填，长度例如 1–100。

------

### 4. 查询当前用户所有类目（分页）

`GET /api/categories?page=1&size=20`
 Auth required

响应：

```
{
  "code":0,
  "msg":"ok",
  "data":{
    "page":1,"size":20,"total":3,
    "items":[
      {"categoryId":45,"name":"计算机视觉","description":"...","createdAt":"..."},
      ...
    ]
  }
}
```

------

### 5. 获取类目详情

`GET /api/categories/{categoryId}`
 Auth required

成功：

```
{ "code":0, "msg":"ok", "data": { "categoryId":45, "name":"计算机视觉", "description":"...", "createdAt":"..." } }
```

若不存在返回：

```
{ "code":3001, "msg":"category not found", "data":{} }
```

------

### 6. 修改类目

`PUT /api/categories/{categoryId}`
 Auth required（仅类目所有者）

请求：

```
{ "name": "CV 与图像处理", "description": "更新描述" }
```

成功：

```
{ "code":0, "msg":"ok", "data": { "categoryId":45, "name":"CV 与图像处理", ... } }
```

错误（无权限）：

```
{ "code":1001, "msg":"unauthorized", "data":{} }
```

------

### 7. 删除类目（仅当类目空时允许）

`DELETE /api/categories/{categoryId}`
 Auth required（仅类目所有者）

成功：

```
{ "code":0, "msg":"deleted", "data": {} }
```

若类目下存在文献：

```
{ "code":4001, "msg":"category not empty; cannot delete", "data": {} }
```

注意：后端需在删除前查询该 category 下是否存在文档；因无外键，删除操作务必在事务或代码中保证一致性。

------

### 8. 在类目下上传论文（PDF）

```
POST /api/categories/{categoryId}/documents`
 Auth required
 Content-Type: `multipart/form-data
```

表单字段：

- `file`：PDF 必填
- `title`：可选（若空，后端从 PDF metadata 或文本中尝试抽取）
- `authors`：可选
- `tags`：可选，逗号分隔

响应（立即返回任务 id）：

```
{
  "code":0,
  "msg":"upload accepted",
  "data":{
    "documentId": 234,        // documents 表的记录 id（可先创建）
    "taskId": "task-uuid-xxx",// 后台解析/向量化任务 id（用于轮询）
    "status": "processing"    // processing / ready / failed
  }
}
```

失败（文件格式/大小）：

```
{ "code":2001, "msg":"invalid file type or too large", "data":{} }
```

实现提示：

- 后端接收并保存原始文件 -> 创建 `documents` 记录（status=processing）-> 异步解析 PDF -> 切分 chunk -> 批量请求 embedding -> 插入 `chunks` 表 -> 更新 `documents` status=ready / summary。
- 因你要避免外键，确保 `chunks.document_id` 为 documents.id 的引用，由代码维护一致性与 CASCADE 删除逻辑。

------

### 9. 获取类目下论文列表（分页+过滤）

`GET /api/categories/{categoryId}/documents?q=关键词&page=1&size=20`
 Auth required

响应：

```
{
  "code":0,
  "msg":"ok",
  "data":{
    "page":1,"size":20,"total":2,
    "items":[
      {"documentId":234,"title":"A Great Paper","authors":"X","summary":"...","uploadedAt":"..."},
      ...
    ]
  }
}
```

实现提示：支持按 title/authors/tags 关键词过滤（可用 ILIKE 或全文索引）。

------

### 10. 全局文献查询（用户所有文献）

`GET /api/documents?q=关键词&page=1&size=20`
 Auth required

返回结构同上。

------

### 11. 获取论文详情

`GET /api/documents/{documentId}`
 Auth required（仅所有者或共享者）

响应：

```
{
  "code":0,
  "msg":"ok",
  "data":{
    "documentId":234,
    "title":"A Great Paper",
    "authors":"X",
    "originalFilename":"paper.pdf",
    "storagePath":"/files/2025/10/..",
    "summary":"自动生成摘要（可能为空，代表未生成）",
    "status":"ready",
    "uploadedAt":"2025-10-29T10:30:00Z",
    "chunksCount": 12
  }
}
```

不存在：

```
{ "code":3001, "msg":"document not found", "data":{} }
```

------

### 12. 修改论文元信息（不改文件内容）

`PATCH /api/documents/{documentId}/metadata`
 Auth required

请求：

```
{ "title": "New Title", "authors": "A,B", "tags": "nlp,transformer" }
```

成功：

```
{ "code":0, "msg":"ok", "data": { "documentId":234, "title":"New Title", ... } }
```

------

### 13. 删除论文

`DELETE /api/documents/{documentId}`
 Auth required（仅所有者）

成功：

```
{ "code":0, "msg":"deleted", "data": {} }
```

注意实现：

- 删除操作需在事务/业务逻辑中完成三件事：删除 `chunks`（按 documentId）、删除 `documents` 记录、删除存储的原始文件。由于没有外键，必须代码保证不留孤儿数据。

------

### 14. 文档级 AI 问答（对话式问答）

`POST /api/documents/{documentId}/qa`
 Auth required

请求：

```
{
  "question": "请帮我总结一下这篇论文的中心思想",
  "topK": 5,          // 可选，默认 5
  "maxTokens": 512    // 可选
}
```

响应（示例）：

```
{
  "code":0,
  "msg":"ok",
  "data":{
    "answer":"这篇论文的中心思想是……",
    "sources":[
      {"chunkId": 1001, "score": 0.12, "snippet":"..."},
      {"chunkId": 1003, "score": 0.15, "snippet":"..."}
    ],
    "model":"gpt-4o",
    "rawCost": {"tokens": 320, "estimatedUsd": 0.01}
  }
}
```

实现细节：

- 后端将 question embedding -> 在 `chunks` 上做向量检索（ORDER BY embedding <-> q_emb LIMIT topK）-> 拼接 context -> 调用 LLM -> 返回 answer + 列出来源 chunk id。
- 返回 `sources` 用于前端高亮或说明依据，降低 hallucination 风险。

错误（document 未 ready）：

```
{ "code":4001, "msg":"document not ready for QA (parsing/embedding not finished)", "data":{} }
```

------

### 15. 全库问答（在用户所有文献中检索）

`POST /api/qa`
 Auth required

请求：

```
{ "question":"帮我找与Transformer微调相关的论文要点", "topK": 10 }
```

响应与文档级 QA 相似，但 sources 可能来自不同 documentId。

------

### 16. 查询任务状态（用于轮询上传/解析任务）

`GET /api/tasks/{taskId}`
 Auth required

响应示例：

```
{
  "code":0,
  "msg":"ok",
  "data":{
    "taskId":"task-uuid-xxx",
    "type":"document_parse",
    "status":"processing|ready|failed",
    "progress": 60,
    "startedAt":"2025-10-29T10:01:00Z",
    "finishedAt": null,
    "errorMessage": null,
    "relatedDocumentId":234
  }
}
```

------

# 数据库建表脚本（PostgreSQL，无外键）

> 假设 embedding 维度为 1536（可按实际模型调整）。请在数据库中安装 `pgvector` 扩展。

```
-- 1) 启用 pgvector 扩展（仅需执行一次）
CREATE EXTENSION IF NOT EXISTS vector;

-- 2) users 表
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  user_type TEXT NOT NULL DEFAULT 'user', -- 'user' | 'admin'（保留）
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- 3) categories 表（不使用外键）
CREATE TABLE categories (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL, -- 由代码保证引用 users.id
  name TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_categories_user ON categories (user_id);

-- 4) documents 表（元数据）
CREATE TABLE documents (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,    -- uploader id
  category_id BIGINT NOT NULL,-- 所属类目
  title TEXT,
  authors TEXT,
  tags TEXT,                  -- 逗号分隔或者 JSON 格式（按需）
  original_filename TEXT,
  storage_path TEXT,          -- 本地路径或 OSS 路径
  status TEXT NOT NULL DEFAULT 'processing', -- processing | ready | failed
  summary TEXT,               -- AI 自动摘要，可能为空
  uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_documents_user ON documents (user_id);
CREATE INDEX idx_documents_category ON documents (category_id);
CREATE INDEX idx_documents_title ON documents USING gin (to_tsvector('simple', coalesce(title, '')));

-- 5) chunks 表（每篇文档的分片，含 embedding)
CREATE TABLE chunks (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL, -- 由代码保证引用 documents.id
  chunk_index INT NOT NULL,
  text TEXT NOT NULL,
  embedding vector(1536),      -- 向量维度请与模型一致
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_chunks_document ON chunks (document_id);
-- 向量索引（ivfflat），lists 参数可根据数据量调整
CREATE INDEX chunks_embedding_idx ON chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 6) tasks 表（记录异步任务状态）
CREATE TABLE tasks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type TEXT NOT NULL,         -- e.g. document_parse, embedding_batch
  status TEXT NOT NULL DEFAULT 'processing', -- processing | ready | failed
  progress INT DEFAULT 0,     -- 0..100
  started_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  finished_at TIMESTAMP WITH TIME ZONE,
  error_message TEXT,
  meta JSONB                  -- 可存放 relatedDocumentId, batch info 等
);
CREATE INDEX idx_tasks_status ON tasks (status);
```

**注意**：没有外键的原因你已说明（性能/教学需求）。但请在应用层实现逻辑约束：

- 插入 `chunks` 前保证 `documents` 已创建，并在失败时回滚或清理。
- 删除 `documents` 时，同时删除 `chunks` 与清理存储文件（事务式或补偿式删除）。

------

# 实现建议与约束（工程提示）

1. **事务与补偿**：上传流程建议两阶段：先创建 `documents`（status=processing），返回 `taskId`；解析与向量化完成后更新 `status=ready`。若中途失败，将 `status=failed` 并记录 `tasks.error_message`；由后台定时 cleanup 未完成或失败的记录。
2. **Chunk 切分策略**：300–800 字/块，重叠 50–150 字可提高检索上下文连贯性。
3. **批量 Embedding**：对 chunk 批量请求 embedding，减少网络/模型调用次数。
4. **向量检索性能**：小数据量用 ivfflat 即可；若数据量增长到百万级，考虑 Milvus / Pinecone 或 PG 调优。
5. **安全**：对上传文件做类型检查与大小限制（例如 <= 50MB），并校验文件名与存储路径防止路径遍历。密码存储用 bcrypt/argon2。
6. **权限**：所有写操作校验 `user_id` 为当前 JWT 用户 id；读取操作如需共享功能后再做扩展。
7. **日志与审计**：记录上传/删除/QA 请求以便答辩时展示调用统计与费用。

------

# 示例工作流（简短）

1. 前端登录 -> 得到 token。
2. 创建类目：`POST /api/categories` -> 得到 `categoryId`。
3. 上传 PDF 到 `POST /api/categories/{categoryId}/documents` -> 得到 `documentId` + `taskId`。
4. 前端轮询 `GET /api/tasks/{taskId}`，等待 `status=ready`。
5. `GET /api/documents/{documentId}` 查看详情与摘要。
6. `POST /api/documents/{documentId}/qa` 进行 AI 问答。