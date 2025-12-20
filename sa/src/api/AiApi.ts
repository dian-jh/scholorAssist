import { get, del } from './index'
import type { ApiResponse } from './index'
import type { MessageResponse, HistoryListResponse } from '@/types/ai'

/**
 * 获取某文档的历史会话列表
 * @param documentId 文献ID
 */
export async function getDocumentChatHistory(documentId: string): Promise<ApiResponse<HistoryListResponse>> {
  return get<HistoryListResponse>(`/ai/history/${documentId}`)
}

/**
 * 获取指定会话的详细消息
 * @param documentId 文献ID
 * @param chatId 会话ID
 */
export async function getChatDetails(documentId: string, chatId: string): Promise<ApiResponse<MessageResponse[]>> {
  return get<MessageResponse[]>(`/ai/history/${documentId}/${chatId}`)
}

/**
 * 删除会话
 * @param documentId 文献ID
 * @param chatId 会话ID
 */
export async function deleteChat(documentId: string, chatId: string): Promise<ApiResponse<void>> {
  return del<void>(`/ai/history/${documentId}/${chatId}`)
}

// 注意：/api/ai/chat 是流式接口，不在 Axios 中处理，而是在 AiStreamingService 中使用 fetch 处理
