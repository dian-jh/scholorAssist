import { get, post } from './index'
import type { ApiResponse } from './index'

// AI模型枚举
export enum AiModel {
  GPT_3_5_TURBO = 'gpt-3.5-turbo',
  GPT_4 = 'gpt-4',
  GPT_4_TURBO = 'gpt-4-turbo',
  CLAUDE_3_SONNET = 'claude-3-sonnet'
}

// AI请求参数接口定义
export interface AiChatRequest {
  message: string
  conversation_id?: string
  model?: AiModel | string
  temperature?: number
  max_tokens?: number
  stream?: boolean
}

export interface AiChatResponse {
  message: string
  conversation_id: string
  model: string
  usage: {
    prompt_tokens: number
    completion_tokens: number
    total_tokens: number
  }
}

export interface ConversationInfo {
  id: string;
  title: string;
  created_at: string;
  last_message_at: string;
}

export interface ConversationListResponse {
  total: number;
  page: number;
  page_size: number;
  conversations: ConversationInfo[];
}

export interface AiStreamChunk {
  id: string
  object: string
  created: number
  model: string
  choices: Array<{
    index: number
    delta: {
      content?: string
      role?: string
    }
    finish_reason?: string
  }>
}

// 参数校验函数
export function validateAiChatRequest(params: AiChatRequest): string[] {
  const errors: string[] = []
  
  if (!params.message || typeof params.message !== 'string') {
    errors.push('消息内容不能为空且必须为字符串')
  }
  
  if (params.message && params.message.trim().length === 0) {
    errors.push('消息内容不能为空白字符')
  }
  
  if (params.message && params.message.length > 4000) {
    errors.push('消息内容不能超过4000个字符')
  }
  
  if (params.temperature !== undefined) {
    if (typeof params.temperature !== 'number' || params.temperature < 0 || params.temperature > 2) {
      errors.push('temperature参数必须是0-2之间的数字')
    }
  }
  
  if (params.max_tokens !== undefined) {
    if (typeof params.max_tokens !== 'number' || params.max_tokens < 1 || params.max_tokens > 4096) {
      errors.push('max_tokens参数必须是1-4096之间的整数')
    }
  }
  
  if (params.model !== undefined) {
    const allowedModels = Object.values(AiModel)
    if (!allowedModels.includes(params.model as AiModel)) {
      errors.push(`model参数必须是以下值之一: ${allowedModels.join(', ')}`)
    }
  }
  
  return errors
}

// AI请求封装类
export class AiRequestWrapper {
  private static instance: AiRequestWrapper
  private defaultConfig: Partial<AiChatRequest> = {
    model: AiModel.GPT_3_5_TURBO,
    temperature: 0.7,
    max_tokens: 2048,
    stream: false
  }

  private constructor() {}

  public static getInstance(): AiRequestWrapper {
    if (!AiRequestWrapper.instance) {
      AiRequestWrapper.instance = new AiRequestWrapper()
    }
    return AiRequestWrapper.instance
  }

  /**
   * 设置默认配置
   * @param config 默认配置参数
   */
  public setDefaultConfig(config: Partial<AiChatRequest>): void {
    this.defaultConfig = { ...this.defaultConfig, ...config }
  }

  /**
   * 获取当前默认配置
   */
  public getDefaultConfig(): Partial<AiChatRequest> {
    return { ...this.defaultConfig }
  }

  /**
   * 发送AI聊天请求
   * @param params 请求参数
   * @returns Promise<ApiResponse<AiChatResponse>>
   */
  public async sendChatRequest(params: AiChatRequest): Promise<ApiResponse<AiChatResponse>> {
    // 合并默认配置
    const requestParams = { ...this.defaultConfig, ...params }
    
    // 参数校验
    const validationErrors = validateAiChatRequest(requestParams)
    if (validationErrors.length > 0) {
      throw new Error(`参数校验失败: ${validationErrors.join(', ')}`)
    }

    try {
      const response = await post<AiChatResponse>('/ai/chat', requestParams)
      return response
    } catch (error) {
      console.error('AI聊天请求失败:', error)
      throw error
    }
  }

  /**
   * 获取AI模型列表
   * @returns Promise<ApiResponse<string[]>>
   */
  public async getAvailableModels(): Promise<ApiResponse<string[]>> {
    try {
      const response = await get<string[]>('/ai/models')
      return response
    } catch (error) {
      console.error('获取AI模型列表失败:', error)
      throw error
    }
  }

  /**
   * 获取对话历史
   * @param conversationId 对话ID
   * @param page 页码
   * @param pageSize 每页数量
   * @returns Promise<ApiResponse<any>>
   */
  public async getConversationHistory(
    conversationId: string,
    page: number = 1,
    pageSize: number = 20
  ): Promise<ApiResponse<any>> {
    if (!conversationId || typeof conversationId !== 'string') {
      throw new Error('对话ID不能为空且必须为字符串')
    }

    if (page < 1 || pageSize < 1 || pageSize > 100) {
      throw new Error('页码必须大于0，每页数量必须在1-100之间')
    }

    try {
      const response = await get(`/ai/conversations/${conversationId}/history`, {
        page,
        page_size: pageSize
      })
      return response
    } catch (error) {
      console.error('获取对话历史失败:', error)
      throw error
    }
  }

  /**
   * 删除对话
   * @param conversationId 对话ID
   * @returns Promise<ApiResponse<null>>
   */
  public async deleteConversation(conversationId: string): Promise<ApiResponse<null>> {
    if (!conversationId || typeof conversationId !== 'string') {
      throw new Error('对话ID不能为空且必须为字符串')
    }

    try {
      const response = await post<null>(`/ai/conversations/${conversationId}/delete`)
      return response
    } catch (error) {
      console.error('删除对话失败:', error)
      throw error
    }
  }

  /**
   * 获取对话列表
   * @param page 页码
   * @param pageSize 每页数量
   * @returns Promise<ApiResponse<ConversationListResponse>>
   */
  public async getConversationList(
    page: number = 1,
    pageSize: number = 20
  ): Promise<ApiResponse<ConversationListResponse>> {
    if (page < 1 || pageSize < 1 || pageSize > 100) {
      throw new Error('页码必须大于0，每页数量必须在1-100之间');
    }

    try {
      const response = await get<ConversationListResponse>('/ai/conversations', {
        page,
        page_size: pageSize,
      });
      return response;
    } catch (error) {
      console.error('获取对话列表失败:', error);
      throw error;
    }
  }
}

/**
 * AI聊天请求
 * 功能描述：发送消息到AI模型并获取回复
 * 入参：AiChatRequest对象，包含消息内容、对话ID等
 * 返回参数：AI回复消息和使用统计
 * url地址：/ai/chat
 * 请求方式：POST
 */
export function sendAiChat(params: AiChatRequest): Promise<ApiResponse<AiChatResponse>> {
  return AiRequestWrapper.getInstance().sendChatRequest(params)
}

/**
 * 获取AI模型列表
 * 功能描述：获取当前可用的AI模型列表
 * 入参：无
 * 返回参数：可用模型名称数组
 * url地址：/ai/models
 * 请求方式：GET
 */
export function getAiModels(): Promise<ApiResponse<string[]>> {
  return AiRequestWrapper.getInstance().getAvailableModels()
}

/**
 * 获取可用AI模型（别名导出）
 * 说明：为兼容现有组件中对 `getAvailableModels` 的引用，
 * 提供同名导出并委托到包装器实现。
 */
export function getAvailableModels(): Promise<ApiResponse<string[]>> {
  return AiRequestWrapper.getInstance().getAvailableModels()
}

/**
 * 获取对话历史
 * 功能描述：获取指定对话的历史消息记录
 * 入参：conversationId(对话ID), page(页码), pageSize(每页数量)
 * 返回参数：分页的历史消息列表
 * url地址：/ai/conversations/{conversationId}/history
 * 请求方式：GET
 */
export function getConversationHistory(
  conversationId: string,
  page?: number,
  pageSize?: number
): Promise<ApiResponse<any>> {
  return AiRequestWrapper.getInstance().getConversationHistory(conversationId, page, pageSize)
}

/**
 * 删除对话
 * 功能描述：删除指定的对话及其所有历史记录
 * 入参：conversationId(对话ID)
 * 返回参数：删除结果
 * url地址：/ai/conversations/{conversationId}/delete
 * 请求方式：POST
 */
export function deleteConversation(conversationId: string): Promise<ApiResponse<null>> {
  return AiRequestWrapper.getInstance().deleteConversation(conversationId)
}

/**
 * 获取对话列表
 * 功能描述：获取用户的对话列表
 * 入参：page(页码), pageSize(每页数量)
 * 返回参数：分页的对话列表
 * url地址：/ai/conversations
 * 请求方式：GET
 */
export function getConversationList(
  page?: number,
  pageSize?: number
): Promise<ApiResponse<ConversationListResponse>> {
  return AiRequestWrapper.getInstance().getConversationList(page, pageSize);
}

// 导出AI请求包装器实例
export const aiApi = AiRequestWrapper.getInstance()