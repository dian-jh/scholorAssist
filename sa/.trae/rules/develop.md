---
alwaysApply: true
---
# 🧭 Scholar Assist 前端开发规范 v4.0 (全量终极版)

> **适用阶段**：前后端联调期 **核心目标**：统一全模块数据交互标准，消除类型隐患，确保 RAG 系统核心流程（上传->分类->阅读->笔记->AI）顺畅跑通。

## 🛠 一、技术栈与工程约定

- **核心框架**：Vue 3 (Script Setup) + TypeScript (严格模式)
- **构建工具**：Vite
- **UI 组件**：Element Plus (按需引入)
- **状态管理**：Pinia (用户状态、全局分类树需持久化)
- **HTTP 客户端**：
  - **通用业务**：Axios (单例，全局拦截)
  - **AI 流式**：原生 `fetch` (处理 SSE)
- **核心依赖**：
  - `pdfjs-dist`: PDF 渲染与文本选取
  - `dayjs`: 时间格式化
  - `lodash-es`: 防抖、深拷贝 (树形数据处理必备)

## 🔌 二、全局数据交互规范 (CRITICAL)

### 1. 统一响应结构 (Result<T>)

后端 `sc-common` 的 `Result.java` 是唯一真理。

```
// src/types/api.d.ts

/**
 * 通用响应包装
 */
export interface ApiResponse<T = any> {
  code: number;      // 200 表示成功
  message: string;   // 提示信息
  data: T;           // 业务数据
  requestId?: string;// 链路追踪 ID
}

/**
 * 分页响应包装
 */
export interface PageResult<T> {
  records: T[];    // 列表数据
  total: number;   // 总数
  size: number;    // 页大小
  current: number; // 当前页
}
```

### 2. ❄️ ID 精度与类型安全

**规则**：虽然 `categories.md` 等文档里示例 ID 可能是 `"cat_1"` 这种短字符串，但后端底层可能混用了 Snowflake ID (Long)。 **强制执行**：前端所有 ID 字段（`id`, `user_id`, `category_id` 等）统一定义为 **`string`** 类型。

## 📂 三、各模块详细开发规范

### 1. 用户模块 (Users) 

- **接口文件**：`src/api/user.ts`

- **鉴权逻辑**：

  - 登录成功后，将 `token` 存入 `localStorage`。
  - 必须定义 `UserStore` (Pinia) 存储用户信息。

- **复杂对象映射**： 后端返回的 User 对象包含嵌套的 `restrictions`（限制配额）。

  ```
  // src/types/user.d.ts
  export interface UserRestrictions {
    max_documents: number;
    storage_limit: number; // 字节单位，前端展示时需转为 MB/GB
  }
  
  export interface UserDTO {
    id: string;
    username: string;
    email: string;
    role: 'user' | 'admin'; 
    status: string;
    restrictions: UserRestrictions; // 嵌套对象
  }
  ```

### 2. 分类模块 (Categories) 

- **接口文件**：`src/api/category.ts`

- **数据结构**：**树形递归结构**。

- **组件实现**：使用 Element Plus 的 `<el-tree>` 或递归组件。

- **类型定义**：

  ```
  // src/types/category.d.ts
  export interface CategoryDTO {
    id: string;
    name: string;
    parent_id: string | null; // null 表示根节点
    children?: CategoryDTO[]; // ⚠️ 递归定义：子节点类型即自身
    document_count: number;
  }
  ```

- **特殊逻辑**：

  - 在移动分类时，前端需校验“不能将分类设为自己的子分类”，虽然后端也会校验，但前端拦截体验更好。

### 3. 文献模块 (Documents)

- **接口文件**：`src/api/document.ts`
- **文件上传**：使用 `FormData`，参数名 `file`。
- **文件流 (Stream)**：
  - PDF 预览不走 Axios。
  - 使用 PDF.js 的 `getDocument({ url, httpHeaders: { Authorization: ... } })` 加载。
  - **重要**：URL 需拼接 `/api/documents/{id}/file`，不要硬编码 `/uploads/` 静态路径。

### 4. 笔记模块 (Notes)

- **接口文件**：`src/api/note.ts`
- **查询**：使用 `POST` 方法查询列表（因为要传 `tags` 数组）。
- **坐标 (Coord)**：
  - 后端给的是 JSON 字符串，前端收到后在 Service 层统一 `JSON.parse` 转为对象再传给组件。
  - 提交时统一 `JSON.stringify`。

### 5. AI 模块 (AI)

- **接口文件**：`src/api/ai.ts`
- **流式读取**：
  - 必须使用 `fetch`。
  - 需处理 Markdown 渲染（推荐 `markdown-it`）。
  - 需防抖处理（不要每收到一个字符就更新 DOM，建议每接收一小段或每 50ms 更新一次 UI）。

## 🏗 四、目录结构规范 (Standardized)（建议）

```
src/
├─ api/ # 所有接口文件（*.ts）
│ ├─ mockManager.ts # Mock接口注册中心
│ └─ index.ts # 统一导出API
├─ components/ # 通用组件（按模块分目录）
├─ views/ # 页面组件（以 View 结尾）
├─ router/ # 路由定义
├─ store/ # Pinia 状态模块
├─ assets/ # 静态资源（图片、fonts）
├─ styles/ # 全局样式 (SCSS, variables)
docs/ # API 文档与说明（md）
README.md
```

## ⚠️ 五、针对新模块的学生避坑指南 (Student Tips)

### 1. 关于“分类树”的渲染

- **后端行为**：`categories.md` 显示后端直接返回了嵌套好的 `children` 结构。
- **前端任务**：你不需要自己在前端写递归函数去组装树（比如把平铺数组转树），直接用后端给的数据即可。
- **坑点**：如果分类层级太深（比如 10 层），`<el-tree>` 可能会有性能问题。但作为学术文献系统，一般不会超过 3-4 层，所以目前无需担心。

### 2. 关于“用户权限”

- **场景**：`users.md` 提到了 `storage_limit`（存储限制）。
- **实现**：在上传文件前（`before-upload` 钩子），**必须**检查 `currentFileSize + usedStorage <= storageLimit`。
- **Why?** 虽然后端肯定会拦截超限上传，但前端拦截能省去 50MB 文件的无效上传流量，用户体验好一百倍。

### 3. ID 命名不一致问题

- **现象**：你会发现 `users.md` 里叫 `user_id`，但 `notes.md` 里可能叫 `documentId` (驼峰)。
- **对策**：
  1. **以 Java 实体类为准**：如果 Java 类是 `userId`，前端接口定义就用 `userId`。
  2. **DTO 转换**：如果后端接口返回蛇形命名 (`user_id`)，建议在 Axios 响应拦截器中引入 `camelcase-keys` 库统一转为驼峰，或者在 TS 接口中老老实实定义为蛇形，保持一致。**推荐后者（定义为蛇形），因为少一层转换就少一个 Bug 来源。**

**Rule Execution Instruction:** When generating code, strict adherence to `src/api` separation and `src/types` definition is required. Always check `Result<T>` wrapping.
