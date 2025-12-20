---
alwaysApply: true
---
# 🔄 前端统一响应处理指南

## 📋 概述

本文档详细说明了前端如何处理后端API的统一响应格式，包括成功响应、错误处理、数据解析等最佳实践。

## 🎯 统一响应格式

### 响应结构

所有API接口均返回以下统一格式：

```typescript
interface ApiResponse<T = any> {
  code: number    // 状态码（200表示成功，非200表示错误）
  msg: string     // 状态信息（成功时为"success"，错误时为具体错误描述）
  data: T         // 实际返回的业务数据（可为null）
}
```

### 成功响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "doc_1",
    "title": "文档标题",
    "author": "作者"
  }
}
```

### 错误响应示例

```json
{
  "code": 400,
  "msg": "参数错误：标题不能为空",
  "data": null
}
```

## 🛠️ 前端处理规范

### 1. API调用标准写法

```typescript
// ✅ 推荐写法
async function fetchDocuments() {
  try {
    const response = await getDocumentList({ page: 1, pageSize: 10 })
    
    // 响应拦截器已经处理了code判断，这里直接使用data
    const documents = response.data
    
    // 处理业务逻辑
    console.log('获取文档成功:', documents)
    return documents
    
  } catch (error) {
    // 错误已经在拦截器中处理并显示消息
    console.error('获取文档失败:', error)
    throw error
  }
}
```

### 2. 错误处理最佳实践

#### 全局错误处理（已在axios拦截器中实现）

```typescript
// src/api/index.ts - 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response
    
    if (data.code === 200) {
      return data  // 返回完整的ApiResponse对象
    } else {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg || '请求失败'))
    }
  },
  (error) => {
    // 网络错误处理
    let message = '网络错误'
    
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录'
          // 清除登录状态，跳转到登录页
          localStorage.removeItem('token')
          localStorage.removeItem('isLoggedIn')
          window.location.href = '/login'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址出错'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `连接错误${error.response.status}`
      }
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)
```

#### 组件中的错误处理

```typescript
// ✅ 推荐：简洁的错误处理
async function handleSubmit() {
  loading.value = true
  
  try {
    const response = await createDocument(formData)
    ElMessage.success('创建成功')
    
    // 使用response.data获取业务数据
    const newDocument = response.data
    documents.value.push(newDocument)
    
  } catch (error) {
    // 错误消息已在拦截器中显示，这里只需要处理业务逻辑
    console.error('创建失败:', error)
  } finally {
    loading.value = false
  }
}

// ❌ 不推荐：重复的错误处理
async function handleSubmitBad() {
  try {
    const response = await createDocument(formData)
    
    if (response.code === 200) {
      ElMessage.success('创建成功')  // 重复处理
    } else {
      ElMessage.error(response.msg)  // 重复处理
    }
  } catch (error) {
    ElMessage.error('请求失败')  // 重复处理
  }
}
```

### 3. 数据解析规范

#### 基础数据解析

```typescript
// ✅ 正确的数据解析
async function loadUserProfile() {
  try {
    const response = await getUserProfile()
    
    // 直接使用response.data，类型安全
    const userInfo: UserInfo = response.data
    
    // 更新状态
    userStore.setUserInfo(userInfo)
    
  } catch (error) {
    // 处理加载失败的情况
    console.error('加载用户信息失败:', error)
  }
}
```

#### 列表数据解析

```typescript
// ✅ 列表数据处理
async function loadDocumentList() {
  try {
    const response = await getDocumentList({ page: 1, pageSize: 20 })
    
    // response.data 是 MockDocument[] 类型
    const documentList: MockDocument[] = response.data
    
    // 安全的数据处理
    if (Array.isArray(documentList)) {
      documents.value = documentList
      documentCount.value = documentList.length
    } else {
      console.warn('返回的数据格式不正确')
      documents.value = []
    }
    
  } catch (error) {
    // 加载失败时的处理
    documents.value = []
    documentCount.value = 0
  }
}
```

#### 分页数据处理

```typescript
// ✅ 分页数据处理示例
interface PaginatedResponse<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

async function loadPaginatedData(page: number, pageSize: number) {
  try {
    const response = await getDocumentList({ page, pageSize })
    
    // 如果后端返回分页信息
    if (response.data && typeof response.data === 'object') {
      const paginatedData = response.data as PaginatedResponse<MockDocument>
      
      documents.value = paginatedData.list
      totalCount.value = paginatedData.total
      currentPage.value = paginatedData.page
      
    } else if (Array.isArray(response.data)) {
      // 如果后端直接返回数组
      documents.value = response.data
      totalCount.value = response.data.length
    }
    
  } catch (error) {
    console.error('加载分页数据失败:', error)
  }
}
```

### 4. 状态码处理规范

```typescript
// 状态码常量定义
export const HTTP_STATUS = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  INTERNAL_SERVER_ERROR: 500
} as const

// 特殊状态码处理
async function handleSpecialCases() {
  try {
    const response = await someApiCall()
    
    // 大部分情况下不需要手动检查code，拦截器已处理
    const data = response.data
    
  } catch (error: any) {
    // 根据具体错误类型进行特殊处理
    if (error.response?.status === 409) {
      // 资源冲突的特殊处理
      ElMessageBox.confirm(
        '资源已存在，是否覆盖？',
        '提示',
        {
          confirmButtonText: '覆盖',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        // 执行覆盖操作
      })
    }
  }
}
```

### 5. TypeScript类型安全

```typescript
// ✅ 推荐：使用 import type 导入DTO
import type { UserDTO } from '@/types/user'
// ❌ 避免：直接导入可能导致循环依赖
// import { UserDTO } from '@/types/user'

// ✅ 类型安全的API调用
interface CreateDocumentParams {
  title: string
  content: string
  categoryId?: string
}

interface DocumentResponse {
  id: string
  title: string
  createdAt: string
}

async function createDocumentSafely(params: CreateDocumentParams) {
  try {
    // 明确指定返回类型
    const response = await createDocument(params) as ApiResponse<DocumentResponse>
    
    // TypeScript会提供类型检查和智能提示
    const newDocument = response.data
    console.log('新文档ID:', newDocument.id)
    console.log('创建时间:', newDocument.createdAt)
    
    return newDocument
    
  } catch (error) {
    console.error('创建文档失败:', error)
    throw error
  }
}
```

## 🚨 常见错误和解决方案

### 1. 重复错误处理

```typescript
// ❌ 错误：重复处理错误消息
async function badExample() {
  try {
    const response = await getDocuments()
    if (response.code !== 200) {
      ElMessage.error(response.msg)  // 拦截器已经处理了
    }
  } catch (error) {
    ElMessage.error('请求失败')  // 又处理了一次
  }
}

// ✅ 正确：信任拦截器的处理
async function goodExample() {
  try {
    const response = await getDocuments()
    // 直接使用数据，错误已在拦截器中处理
    return response.data
  } catch (error) {
    // 只处理业务逻辑，不重复显示错误消息
    console.error('获取文档失败:', error)
    return []
  }
}
```

### 2. 数据类型假设错误

```typescript
// ❌ 错误：假设数据结构
async function badDataHandling() {
  const response = await getDocuments()
  // 没有检查数据类型就直接使用
  response.data.forEach(doc => console.log(doc.title))  // 可能报错
}

// ✅ 正确：安全的数据处理
async function safeDataHandling() {
  try {
    const response = await getDocuments()
    
    if (Array.isArray(response.data)) {
      response.data.forEach(doc => {
        if (doc && typeof doc.title === 'string') {
          console.log(doc.title)
        }
      })
    } else {
      console.warn('返回数据格式不正确:', response.data)
    }
  } catch (error) {
    console.error('处理数据失败:', error)
  }
}
```

### 3. 忽略loading状态

```typescript
// ✅ 完整的loading状态管理
const loading = ref(false)
const error = ref<string | null>(null)

async function loadDataWithStates() {
  loading.value = true
  error.value = null
  
  try {
    const response = await getDocuments()
    documents.value = response.data
  } catch (err: any) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}
```

## 📝 最佳实践总结

1. **信任拦截器**：不要重复处理已在拦截器中处理的错误
2. **类型安全**：使用TypeScript类型定义确保数据安全
3. **数据验证**：对返回的数据进行必要的类型和结构检查
4. **状态管理**：正确管理loading、error等状态
5. **错误边界**：在组件层面设置错误边界，优雅处理异常
6. **用户体验**：提供清晰的错误提示和加载状态
7. **日志记录**：在开发环境记录详细的错误信息用于调试

## 🔍 调试技巧

### 开发环境调试

```typescript
// 开发环境下的详细日志
if (import.meta.env.DEV) {
  console.group('API调用详情')
  console.log('请求参数:', params)
  console.log('响应数据:', response)
  console.log('处理结果:', processedData)
  console.groupEnd()
}
```

### 错误追踪

```typescript
// 错误信息收集
function trackError(error: any, context: string) {
  const errorInfo = {
    message: error.message,
    stack: error.stack,
    context,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent
  }
  
  if (import.meta.env.DEV) {
    console.error('错误详情:', errorInfo)
  } else {
    // 生产环境发送到错误监控服务
    // sendToErrorTracking(errorInfo)
  }
}
```