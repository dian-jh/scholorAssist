import { get, post, put, del } from './index'
import type { NoteDTO, NoteCreateRequest, NoteQueryRequest, NoteUpdateRequest } from '@/types/note'
import type { PageResult, ApiResponse } from '@/types/api'

// 基础路径
const BASE_URL = '/notes'

/**
 * 创建笔记/标注
 * @param data 创建请求参数
 */
export function createNote(data: NoteCreateRequest): Promise<ApiResponse<string>> {
  return post<string>(BASE_URL, data)
}

/**
 * 查询笔记列表 (搜索)
 * @param data 查询请求参数
 */
export function searchNotes(data: NoteQueryRequest): Promise<ApiResponse<PageResult<NoteDTO>>> {
  // 修正：根据用户反馈和API文档摘要，查询笔记应使用 GET /api/notes 接口
  return get<PageResult<NoteDTO>>(BASE_URL, data)
}

export const getNotesList = searchNotes

/**
 * 更新笔记信息
 * @param data 更新请求参数
 */
export function updateNote(data: NoteUpdateRequest): Promise<ApiResponse<boolean>> {
  return put<boolean>(BASE_URL, data)
}

/**
 * 删除笔记
 * @param id 笔记ID
 */
export function deleteNote(id: string): Promise<ApiResponse<boolean>> {
  return del<boolean>(`${BASE_URL}/${id}`)
}
