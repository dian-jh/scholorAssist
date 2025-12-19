import type { ApiResponse } from './index'
import type { AiChatResponse, AiStreamChunk } from './AiApi'

// 响应数据校验接口
export interface ResponseValidator<T> {
  validate(data: any): ValidationResult<T>
}

export interface ValidationResult<T> {
  isValid: boolean
  data?: T
  errors: string[]
}

// AI响应数据校验器
export class AiChatResponseValidator implements ResponseValidator<AiChatResponse> {
  validate(data: any): ValidationResult<AiChatResponse> {
    const errors: string[] = []
    
    if (!data || typeof data !== 'object') {
      errors.push('响应数据必须是对象类型')
      return { isValid: false, errors }
    }
    
    if (!data.message || typeof data.message !== 'string') {
      errors.push('message字段必须是非空字符串')
    }
    
    if (!data.conversation_id || typeof data.conversation_id !== 'string') {
      errors.push('conversation_id字段必须是非空字符串')
    }
    
    if (!data.model || typeof data.model !== 'string') {
      errors.push('model字段必须是非空字符串')
    }
    
    if (!data.usage || typeof data.usage !== 'object') {
      errors.push('usage字段必须是对象类型')
    } else {
      const { usage } = data
      if (typeof usage.prompt_tokens !== 'number' || usage.prompt_tokens < 0) {
        errors.push('usage.prompt_tokens必须是非负数')
      }
      if (typeof usage.completion_tokens !== 'number' || usage.completion_tokens < 0) {
        errors.push('usage.completion_tokens必须是非负数')
      }
      if (typeof usage.total_tokens !== 'number' || usage.total_tokens < 0) {
        errors.push('usage.total_tokens必须是非负数')
      }
    }
    
    if (errors.length > 0) {
      return { isValid: false, errors }
    }
    
    return {
      isValid: true,
      data: data as AiChatResponse,
      errors: []
    }
  }
}

// 流式响应数据校验器
export class AiStreamChunkValidator implements ResponseValidator<AiStreamChunk> {
  validate(data: any): ValidationResult<AiStreamChunk> {
    const errors: string[] = []
    
    if (!data || typeof data !== 'object') {
      errors.push('流式响应数据必须是对象类型')
      return { isValid: false, errors }
    }
    
    if (!data.id || typeof data.id !== 'string') {
      errors.push('id字段必须是非空字符串')
    }
    
    if (!data.object || typeof data.object !== 'string') {
      errors.push('object字段必须是非空字符串')
    }
    
    if (typeof data.created !== 'number') {
      errors.push('created字段必须是数字类型')
    }
    
    if (!data.model || typeof data.model !== 'string') {
      errors.push('model字段必须是非空字符串')
    }
    
    if (!Array.isArray(data.choices)) {
      errors.push('choices字段必须是数组类型')
    } else {
      data.choices.forEach((choice: any, index: number) => {
        if (typeof choice.index !== 'number') {
          errors.push(`choices[${index}].index必须是数字类型`)
        }
        if (!choice.delta || typeof choice.delta !== 'object') {
          errors.push(`choices[${index}].delta必须是对象类型`)
        }
      })
    }
    
    if (errors.length > 0) {
      return { isValid: false, errors }
    }
    
    return {
      isValid: true,
      data: data as AiStreamChunk,
      errors: []
    }
  }
}

// AI响应处理器
export class AiResponseHandler {
  private static instance: AiResponseHandler
  private chatValidator = new AiChatResponseValidator()
  private streamValidator = new AiStreamChunkValidator()

  private constructor() {}

  public static getInstance(): AiResponseHandler {
    if (!AiResponseHandler.instance) {
      AiResponseHandler.instance = new AiResponseHandler()
    }
    return AiResponseHandler.instance
  }

  /**
   * 处理AI聊天响应
   * @param response API响应对象
   * @returns 处理后的响应数据
   */
  public handleChatResponse(response: ApiResponse<any>): ApiResponse<AiChatResponse> {
    try {
      // 基础响应格式校验
      if (!this.isValidApiResponse(response)) {
        throw new Error('响应格式不符合API规范')
      }

      // AI聊天响应数据校验
      const validation = this.chatValidator.validate(response.data)
      if (!validation.isValid) {
        throw new Error(`AI响应数据校验失败: ${validation.errors.join(', ')}`)
      }

      return {
        code: response.code,
        msg: response.msg,
        data: validation.data!
      }
    } catch (error) {
      console.error('AI聊天响应处理失败:', error)
      throw error
    }
  }

  /**
   * 处理流式响应数据块
   * @param chunk 流式数据块
   * @returns 处理后的数据块
   */
  public handleStreamChunk(chunk: string): AiStreamChunk | null {
    try {
      // 解析JSON数据
      let parsedData: any
      try {
        parsedData = JSON.parse(chunk)
      } catch (parseError) {
        console.warn('流式数据解析失败:', chunk)
        return null
      }

      // 数据校验
      const validation = this.streamValidator.validate(parsedData)
      if (!validation.isValid) {
        console.warn('流式数据校验失败:', validation.errors.join(', '))
        return null
      }

      return validation.data!
    } catch (error) {
      console.error('流式响应处理失败:', error)
      return null
    }
  }

  /**
   * 处理错误响应
   * @param error 错误对象
   * @returns 标准化的错误信息
   */
  public handleErrorResponse(error: any): {
    code: number
    message: string
    details?: any
  } {
    // 如果是API响应错误
    if (error.response && error.response.data) {
      const { data } = error.response
      return {
        code: data.code || error.response.status,
        message: data.msg || data.message || '请求失败',
        details: data
      }
    }

    // 如果是网络错误
    if (error.code === 'ECONNABORTED') {
      return {
        code: 408,
        message: '请求超时，请检查网络连接'
      }
    }

    // 如果是其他类型错误
    return {
      code: 500,
      message: error.message || '未知错误',
      details: error
    }
  }

  /**
   * 批量处理响应数据
   * @param responses 响应数组
   * @returns 处理结果
   */
  public handleBatchResponses<T>(
    responses: ApiResponse<any>[],
    validator: ResponseValidator<T>
  ): {
    successful: ApiResponse<T>[]
    failed: { index: number; error: string }[]
  } {
    const successful: ApiResponse<T>[] = []
    const failed: { index: number; error: string }[] = []

    responses.forEach((response, index) => {
      try {
        if (!this.isValidApiResponse(response)) {
          failed.push({ index, error: '响应格式不符合API规范' })
          return
        }

        const validation = validator.validate(response.data)
        if (!validation.isValid) {
          failed.push({ 
            index, 
            error: `数据校验失败: ${validation.errors.join(', ')}` 
          })
          return
        }

        successful.push({
          code: response.code,
          msg: response.msg,
          data: validation.data!
        })
      } catch (error) {
        failed.push({ 
          index, 
          error: error instanceof Error ? error.message : '处理失败' 
        })
      }
    })

    return { successful, failed }
  }

  /**
   * 校验API响应基础格式
   * @param response 响应对象
   * @returns 是否符合规范
   */
  private isValidApiResponse(response: any): boolean {
    return (
      response &&
      typeof response === 'object' &&
      typeof response.code === 'number' &&
      typeof response.msg === 'string' &&
      response.hasOwnProperty('data')
    )
  }

  /**
   * 格式化响应数据用于显示
   * @param response AI响应数据
   * @returns 格式化后的显示文本
   */
  public formatResponseForDisplay(response: AiChatResponse): string {
    const { message, model, usage } = response
    
    return `${message}\n\n---\n模型: ${model} | Token使用: ${usage.total_tokens} (输入: ${usage.prompt_tokens}, 输出: ${usage.completion_tokens})`
  }

  /**
   * 提取流式响应中的文本内容
   * @param chunk 流式数据块
   * @returns 文本内容
   */
  public extractStreamContent(chunk: AiStreamChunk): string {
    if (!chunk.choices || chunk.choices.length === 0) {
      return ''
    }

    const choice = chunk.choices[0]
    return choice.delta?.content || ''
  }

  /**
   * 检查流式响应是否结束
   * @param chunk 流式数据块
   * @returns 是否结束
   */
  public isStreamFinished(chunk: AiStreamChunk): boolean {
    if (!chunk.choices || chunk.choices.length === 0) {
      return false
    }

    const choice = chunk.choices[0]
    return choice.finish_reason === 'stop' || choice.finish_reason === 'length'
  }
}

// 导出响应处理器实例和工具函数
export const aiResponseHandler = AiResponseHandler.getInstance()

/**
 * 快速处理AI聊天响应
 * @param response API响应
 * @returns 处理后的响应
 */
export function handleAiChatResponse(response: ApiResponse<any>): ApiResponse<AiChatResponse> {
  return aiResponseHandler.handleChatResponse(response)
}

/**
 * 快速处理流式响应块
 * @param chunk 数据块字符串
 * @returns 处理后的数据块
 */
export function handleStreamChunk(chunk: string): AiStreamChunk | null {
  return aiResponseHandler.handleStreamChunk(chunk)
}

/**
 * 快速处理错误响应
 * @param error 错误对象
 * @returns 标准化错误信息
 */
export function handleAiError(error: any) {
  return aiResponseHandler.handleErrorResponse(error)
}