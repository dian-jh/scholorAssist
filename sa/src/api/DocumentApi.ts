import { get, post, getArrayBuffer } from './index'
import type { MockDocument } from './mockManager'
import { getMockHandler } from './mockManager'
import { ConfigManager } from '@/config'

/**
 * 接口名称：获取文档列表
 * 功能描述：分页获取文档列表，支持分类筛选和搜索
 * 入参：{ category_id?: string, search?: string, page?: number, pageSize?: number }
 * 返回参数：{ code, msg, data: MockDocument[] }
 * URL：/api/documents
 * 请求方式：GET
 */
export function getDocumentList(params: {
  category_id?: string
  search?: string
  page?: number
  pageSize?: number
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/documents')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return get<MockDocument[]>('/documents', params)
}

/**
 * 接口名称：获取文档详情
 * 功能描述：根据文档ID获取文档详细信息
 * 入参：{ id: string }
 * 返回参数：{ code, msg, data: MockDocument }
 * URL：/api/documents/:id
 * 请求方式：GET
 */
export function getDocumentDetail(id: string) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/documents/:id')
    if (mockHandler) {
      return mockHandler({ id })
    }
  }
  
  return get<MockDocument>(`/documents/${id}`)
}

/**
 * 接口名称：上传文档
 * 功能描述：上传PDF文档并进行解析处理
 * 入参：{ file: File, title?: string, category_id?: string }
 * 返回参数：{ code, msg, data: MockDocument }
 * URL：/api/documents/upload
 * 请求方式：POST
 */
export function uploadDocument(params: {
  file: File
  title?: string
  category_id?: string
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/documents/upload')
    if (mockHandler) {
      return mockHandler({
        filename: params.file.name,
        title: params.title,
        category_id: params.category_id,
        file_size: `${(params.file.size / 1024 / 1024).toFixed(1)} MB`,
        pages: Math.floor(Math.random() * 50) + 10
      })
    }
  }
  
  const formData = new FormData()
  formData.append('file', params.file)
  if (params.title) formData.append('title', params.title)
  if (params.category_id) formData.append('category_id', params.category_id)
  
  return post<MockDocument>('/documents/upload', formData)
}

/**
 * 接口名称：更新阅读进度
 * 功能描述：更新文档的阅读进度
 * 入参：{ id: string, progress: number }
 * 返回参数：{ code, msg, data: { progress: number } }
 * URL：/api/documents/:id/progress
 * 请求方式：POST
 */
export function updateReadingProgress(id: string, progress: number) {
  // 修复重复的 /api 前缀，统一遵循 axios baseURL: /api
  return post<{ progress: number }>(`/documents/${id}/progress`, { progress })
}

/**
 * 接口名称：删除文档
 * 功能描述：删除指定的文档
 * 入参：{ id: string }
 * 返回参数：{ code, msg, data: null }
 * URL：/api/documents/:id
 * 请求方式：DELETE
 */
export function deleteDocument(id: string) {
  // 后端约定使用POST到删除地址；避免在post中传入第三个参数
  return post<null>(`/documents/${id}/delete`)
}

/**
 * 接口名称：获取PDF内容
 * 功能描述：获取PDF文档的页面内容，支持分页获取
 * 入参：{ id: string, page?: number, pageSize?: number }
 * 返回参数：{ code, msg, data: { pages: PDFPage[], total_pages: number } }
 * URL：/api/documents/:id/content
 * 请求方式：GET
 */
export function getPdfContent(id: string, params?: {
  page?: number
  pageSize?: number
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/documents/:id/content')
    if (mockHandler) {
      return mockHandler({ id, ...params })
    }
  }
  
  return get<{
    pages: Array<{
      page_number: number
      content: string
      width: number
      height: number
      text_blocks: Array<{
        id: string
        text: string
        x: number
        y: number
        width: number
        height: number
        font_size: number
      }>
    }>
    total_pages: number
  }>(`/documents/${id}/content`, params)
}

/**
 * 接口名称：提取文档文本
 * 功能描述：提取PDF文档的纯文本内容
 * 入参：{ id: string, page_start?: number, page_end?: number }
 * 返回参数：{ code, msg, data: { text: string, pages: number } }
 * URL：/api/documents/:id/extract-text
 * 请求方式：GET
 */
export function extractDocumentText(id: string, params?: {
  page_start?: number
  page_end?: number
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/documents/:id/extract-text')
    if (mockHandler) {
      return mockHandler({ id, ...params })
    }
  }
  
  return get<{
    text: string
    pages: number
  }>(`/documents/${id}/extract-text`, params)
}

/**
 * 接口名称：获取文档文件URL（前端拼接）
 * 功能描述：返回标准文件流接口的URL，供iframe或下载使用
 * 说明：后端需实现 GET /api/documents/:id/file
 */
export function getDocumentFileUrl(id: string): string {
  const baseApi = ConfigManager.getApiBaseUrl()
  return `${baseApi}/documents/${id}/file`
}

/**
 * 接口名称：获取文档字节流
 * 功能描述：以 ArrayBuffer 形式获取 PDF 内容，避免浏览器或下载管理器拦截
 * URL：/api/files/documents/:id/bytes
 * 请求方式：GET
 */
export function getDocumentBytes(id: string): Promise<ArrayBuffer> {
  // 修正：网关路由前缀为 /files，完整路径为 /api/files/documents/{id}/bytes
  return getArrayBuffer(`/files/documents/${id}/bytes`)
}

/**
 * 接口名称：搜索文档内容
 * 功能描述：在文档内容中搜索关键词
 * 入参：{ id: string, keyword: string, page?: number, pageSize?: number }
 * 返回参数：{ code, msg, data: { results: SearchResult[], total: number } }
 * URL：/api/documents/:id/search
 * 请求方式：GET
 */
export function searchDocumentContent(id: string, params: {
  keyword: string
  page?: number
  pageSize?: number
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/documents/:id/search')
    if (mockHandler) {
      return mockHandler({ id, ...params })
    }
  }
  
  return get<{
    results: Array<{
      page_number: number
      text: string
      highlight_start: number
      highlight_end: number
      context: string
    }>
    total: number
  }>(`/documents/${id}/search`, params)
}