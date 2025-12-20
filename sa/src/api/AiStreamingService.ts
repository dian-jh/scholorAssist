import { TokenManager } from '@/utils/tokenManager'
import type { ChatRequest } from '@/types/ai'

export interface StreamOptions {
  onMessage: (chunk: string) => void
  onError: (error: Error) => void
  onComplete: () => void
  signal?: AbortSignal
}

/**
 * AI 流式对话服务
 * 使用 fetch + ReadableStream 处理 SSE 响应
 */
export class AiStreamingService {
  private static readonly API_URL = '/api/ai/chat'

  /**
   * 发送流式对话请求
   * @param request 请求参数
   * @param options 回调选项
   */
  static async sendMessage(request: ChatRequest, options: StreamOptions): Promise<void> {
    const token = TokenManager.getToken()
    
    try {
      const response = await fetch(this.API_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        body: JSON.stringify(request),
        signal: options.signal
      })

      if (!response.ok) {
        // 尝试读取错误信息
        const errorText = await response.text().catch(() => 'Unknown error')
        throw new Error(`请求失败 (${response.status}): ${errorText}`)
      }

      if (!response.body) {
        throw new Error('响应体为空')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          break
        }

        // 解码数据块
        const textChunk = decoder.decode(value, { stream: true })
        
        // 解析 SSE 格式 (data: ...)
        // 将数据块按行分割
        const lines = textChunk.split('\n')
        
        for (const line of lines) {
          const trimmedLine = line.trim()
          if (!trimmedLine) continue
          
          // 检查是否以 data: 开头
          if (trimmedLine.startsWith('data:')) {
            // 提取内容 (去掉 data: 前缀)
            let content = trimmedLine.slice(5)
            // 如果是以空格开头，去掉第一个空格（有些 SSE 实现会加空格）
            if (content.startsWith(' ')) {
              content = content.slice(1)
            }
            
            if (content) {
              options.onMessage(content)
            }
          } else {
             // 兼容非 SSE 格式的纯文本流 (以防万一)
             // options.onMessage(trimmedLine)
             // 暂时忽略非 data: 开头的行，避免输出乱码或心跳包
          }
        }
      }

      options.onComplete()
      
    } catch (error: any) {
      if (error.name === 'AbortError') {
        // 用户主动停止，不算错误
        options.onComplete()
        return
      }
      console.error('AI Stream Error:', error)
      options.onError(error instanceof Error ? error : new Error(String(error)))
    }
  }
}
