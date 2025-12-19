import { get, post } from './index'
import type { MockCategory } from './mockManager'
import { getMockHandler } from './mockManager'
import { ConfigManager } from '@/config'

/**
 * 接口名称：获取分类列表
 * 功能描述：获取所有文档分类，包含层级结构
 * 入参：无
 * 返回参数：{ code, msg, data: MockCategory[] }
 * URL：/api/categories
 * 请求方式：GET
 */
export function getCategoryList() {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/categories')
    if (mockHandler) {
      return mockHandler()
    }
  }
  
  return get<MockCategory[]>('/categories')
}

/**
 * 接口名称：创建分类
 * 功能描述：创建新的文档分类
 * 入参：{ name: string, parent_id?: string }
 * 返回参数：{ code, msg, data: MockCategory }
 * URL：/api/categories
 * 请求方式：POST
 */
export function createCategory(params: {
  name: string
  parent_id?: string
}) {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/categories')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return post<MockCategory>('/categories', params)
}

/**
 * 接口名称：更新分类
 * 功能描述：更新分类信息
 * 入参：{ id: string, name: string, parent_id?: string }
 * 返回参数：{ code, msg, data: MockCategory }
 * URL：/api/categories/:id
 * 请求方式：POST
 */
export function updateCategory(id: string, params: {
  name: string
  parent_id?: string
}) {
  return post<MockCategory>(`/api/categories/${id}`, params)
}

/**
 * 接口名称：删除分类
 * 功能描述：删除指定分类（需要先移动或删除分类下的文档）
 * 入参：{ id: string }
 * 返回参数：{ code, msg, data: null }
 * URL：/api/categories/:id/delete
 * 请求方式：POST
 */
export function deleteCategory(id: string) {
  return post<null>(`/api/categories/${id}/delete`)
}