import { get, post, put, del } from './index'
import { getMockHandler } from './mockManager'
import { ConfigManager } from '@/config'

// 笔记数据类型定义
export interface Note {
  id: string
  document_id: string
  title: string
  content: string
  page_number: number
  created_at: string
  updated_at: string
  tags: string[]
  highlight_data?: {
    text: string
    start_offset: number
    end_offset: number
    color: string
    position: {
      x: number
      y: number
      width: number
      height: number
    }
  }
}

/**
 * 接口名称：获取笔记列表
 * 功能描述：获取用户的笔记列表，支持按文档筛选
 * 入参：{ document_id?: string, page?: number, pageSize?: number }
 * 返回参数：{ code, msg, data: Note[] }
 * URL：/api/notes
 * 请求方式：GET
 */
export function getNotesList(params?: {
  document_id?: string
  page?: number
  pageSize?: number
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/notes')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return get<Note[]>('/notes', params)
}

/**
 * 接口名称：获取笔记详情
 * 功能描述：根据笔记ID获取笔记详细信息
 * 入参：{ id: string }
 * 返回参数：{ code, msg, data: Note }
 * URL：/api/notes/:id
 * 请求方式：GET
 */
export function getNoteDetail(id: string) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/notes/:id')
    if (mockHandler) {
      return mockHandler({ id })
    }
  }
  
  return get<Note>(`/notes/${id}`)
}

/**
 * 接口名称：创建笔记
 * 功能描述：创建新的笔记
 * 入参：{ document_id: string, title: string, content: string, page_number: number, tags?: string[], highlight_data?: object }
 * 返回参数：{ code, msg, data: Note }
 * URL：/api/notes
 * 请求方式：POST
 */
export function createNote(params: {
  document_id: string
  title: string
  content: string
  page_number: number
  tags?: string[]
  highlight_data?: {
    text: string
    start_offset: number
    end_offset: number
    color: string
    position: {
      x: number
      y: number
      width: number
      height: number
    }
  }
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/notes')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return post<Note>('/notes', params)
}

/**
 * 接口名称：更新笔记
 * 功能描述：更新现有笔记的信息
 * 入参：{ id: string, title?: string, content?: string, tags?: string[], highlight_data?: object }
 * 返回参数：{ code, msg, data: Note }
 * URL：/api/notes/:id
 * 请求方式：PUT
 */
export function updateNote(id: string, params: {
  title?: string
  content?: string
  tags?: string[]
  highlight_data?: {
    text: string
    start_offset: number
    end_offset: number
    color: string
    position: {
      x: number
      y: number
      width: number
      height: number
    }
  }
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('PUT', '/api/notes/:id')
    if (mockHandler) {
      return mockHandler({ id, ...params })
    }
  }
  
  return put<Note>(`/notes/${id}`, params)
}

/**
 * 接口名称：删除笔记
 * 功能描述：删除指定的笔记
 * 入参：{ id: string }
 * 返回参数：{ code, msg, data: null }
 * URL：/api/notes/:id
 * 请求方式：DELETE
 */
export function deleteNote(id: string) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('DELETE', '/api/notes/:id')
    if (mockHandler) {
      return mockHandler({ id })
    }
  }
  
  return del<null>(`/notes/${id}`)
}

/**
 * 接口名称：批量删除笔记
 * 功能描述：批量删除多个笔记
 * 入参：{ ids: string[] }
 * 返回参数：{ code, msg, data: { deleted_count: number } }
 * URL：/api/notes/batch-delete
 * 请求方式：POST
 */
export function batchDeleteNotes(ids: string[]) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/notes/batch-delete')
    if (mockHandler) {
      return mockHandler({ ids })
    }
  }
  
  return post<{ deleted_count: number }>('/notes/batch-delete', { ids })
}

/**
 * 接口名称：搜索笔记
 * 功能描述：根据关键词搜索笔记
 * 入参：{ keyword: string, document_id?: string, page?: number, pageSize?: number }
 * 返回参数：{ code, msg, data: { notes: Note[], total: number } }
 * URL：/api/notes/search
 * 请求方式：GET
 */
export function searchNotes(params: {
  keyword: string
  document_id?: string
  page?: number
  pageSize?: number
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/notes/search')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return get<{
    notes: Note[]
    total: number
  }>('/notes/search', params)
}