import { describe, it, expect, vi, beforeEach, afterEach, beforeAll, afterAll } from 'vitest'
import { sendAiChat, getAiModels, getConversationHistory, deleteConversation } from '../../AiApi'
import { createWebSocketStream, createSSEStream } from '../../AiStreamingService'
import { aiResponseHandler } from '../../AiResponseHandler'
import type { AiChatRequest } from '../../AiApi'

// 集成测试配置
const TEST_CONFIG = {
  baseURL: 'http://localhost:3001/api',
  wsURL: 'ws://localhost:3001/api/ai/chat/stream',
  sseURL: 'http://localhost:3001/api/ai/chat/stream',
  timeout: 10000
}

// Mock服务器响应
const mockResponses = {
  chatSuccess: {
    code: 200,
    msg: 'success',
    data: {
      message: '这是AI的回复内容，用于测试集成功能。',
      conversation_id: 'conv_integration_test_001',
      model: 'gpt-3.5-turbo',
      usage: {
        prompt_tokens: 15,
        completion_tokens: 25,
        total_tokens: 40
      }
    }
  },
  modelsSuccess: {
    code: 200,
    msg: 'success',
    data: ['gpt-3.5-turbo', 'gpt-4', 'gpt-4-turbo', 'claude-3-sonnet']
  },
  historySuccess: {
    code: 200,
    msg: 'success',
    data: {
      conversation_id: 'conv_integration_test_001',
      total: 4,
      page: 1,
      page_size: 20,
      messages: [
        {
          id: 'msg_001',
          role: 'user',
          content: '你好',
          timestamp: '2024-01-15T10:30:00Z'
        },
        {
          id: 'msg_002',
          role: 'assistant',
          content: '您好！有什么可以帮助您的吗？',
          timestamp: '2024-01-15T10:30:05Z',
          model: 'gpt-3.5-turbo',
          usage: {
            prompt_tokens: 5,
            completion_tokens: 12,
            total_tokens: 17
          }
        }
      ]
    }
  },
  deleteSuccess: {
    code: 200,
    msg: 'success',
    data: null
  }
}

// Mock fetch for HTTP requests
const mockFetch = vi.fn()
global.fetch = mockFetch

// Mock WebSocket for streaming tests
class MockWebSocket {
  static CONNECTING = 0
  static OPEN = 1
  static CLOSING = 2
  static CLOSED = 3

  readyState = MockWebSocket.CONNECTING
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null

  constructor(public url: string) {
    setTimeout(() => {
      this.readyState = MockWebSocket.OPEN
      this.onopen?.(new Event('open'))
    }, 100)
  }

  send(data: string) {
    // 模拟服务器响应
    setTimeout(() => {
      const chunks = [
        {
          type: 'ai_chat_chunk',
          data: {
            id: 'chunk_001',
            object: 'chat.completion.chunk',
            created: Date.now(),
            model: 'gpt-3.5-turbo',
            choices: [{
              index: 0,
              delta: { content: '这是' }
            }]
          }
        },
        {
          type: 'ai_chat_chunk',
          data: {
            id: 'chunk_002',
            object: 'chat.completion.chunk',
            created: Date.now(),
            model: 'gpt-3.5-turbo',
            choices: [{
              index: 0,
              delta: { content: '流式' }
            }]
          }
        },
        {
          type: 'ai_chat_chunk',
          data: {
            id: 'chunk_003',
            object: 'chat.completion.chunk',
            created: Date.now(),
            model: 'gpt-3.5-turbo',
            choices: [{
              index: 0,
              delta: { content: '回复' },
              finish_reason: 'stop'
            }]
          }
        }
      ]

      chunks.forEach((chunk, index) => {
        setTimeout(() => {
          this.onmessage?.(new MessageEvent('message', {
            data: JSON.stringify(chunk)
          }))
        }, index * 100)
      })
    }, 200)
  }

  close(code?: number, reason?: string) {
    this.readyState = MockWebSocket.CLOSED
    this.onclose?.(new CloseEvent('close', { code: code || 1000, reason }))
  }
}

global.WebSocket = MockWebSocket as any

describe('AI模块集成测试', () => {
  beforeAll(() => {
    // 设置测试环境
    process.env.VITE_API_BASE_URL = TEST_CONFIG.baseURL
  })

  beforeEach(() => {
    vi.clearAllMocks()
    // 设置默认的成功响应
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(mockResponses.chatSuccess)
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('完整的AI聊天流程', () => {
    it('应该完成从发送消息到接收回复的完整流程', async () => {
      // 1. 获取可用模型
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockResponses.modelsSuccess)
      })

      const modelsResponse = await getAiModels()
      expect(modelsResponse.code).toBe(200)
      expect(modelsResponse.data).toContain('gpt-3.5-turbo')

      // 2. 发送AI聊天请求
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockResponses.chatSuccess)
      })

      const chatRequest: AiChatRequest = {
        message: '请介绍一下人工智能的发展历程',
        model: 'gpt-3.5-turbo',
        temperature: 0.7,
        max_tokens: 2048
      }

      const chatResponse = await sendAiChat(chatRequest)
      expect(chatResponse.code).toBe(200)
      expect(chatResponse.data.message).toBeTruthy()
      expect(chatResponse.data.conversation_id).toBeTruthy()

      // 3. 获取对话历史
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockResponses.historySuccess)
      })

      const historyResponse = await getConversationHistory(
        chatResponse.data.conversation_id,
        1,
        20
      )
      expect(historyResponse.code).toBe(200)
      expect(historyResponse.data.messages).toBeInstanceOf(Array)

      // 4. 删除对话
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockResponses.deleteSuccess)
      })

      const deleteResponse = await deleteConversation(chatResponse.data.conversation_id)
      expect(deleteResponse.code).toBe(200)
    }, TEST_CONFIG.timeout)

    it('应该处理API错误并正确恢复', async () => {
      // 模拟服务器错误
      mockFetch.mockRejectedValueOnce({
        response: {
          status: 500,
          data: {
            code: 500,
            msg: '服务器内部错误'
          }
        }
      })

      const chatRequest: AiChatRequest = {
        message: '测试错误处理'
      }

      await expect(sendAiChat(chatRequest)).rejects.toThrow()

      // 恢复正常服务
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockResponses.chatSuccess)
      })

      const retryResponse = await sendAiChat(chatRequest)
      expect(retryResponse.code).toBe(200)
    })
  })

  describe('流式传输集成测试', () => {
    it('应该完成WebSocket流式聊天的完整流程', async () => {
      return new Promise<void>((resolve, reject) => {
        const receivedChunks: string[] = []
        let connectionEstablished = false

        const wsStream = createWebSocketStream(
          { url: TEST_CONFIG.wsURL },
          {
            onOpen: () => {
              connectionEstablished = true
              console.log('WebSocket连接已建立')
            },
            onMessage: (chunk) => {
              const content = aiResponseHandler.extractStreamContent(chunk)
              if (content) {
                receivedChunks.push(content)
                console.log('接收到流式内容:', content)
              }

              if (aiResponseHandler.isStreamFinished(chunk)) {
                console.log('流式传输完成')
                
                // 验证结果
                expect(connectionEstablished).toBe(true)
                expect(receivedChunks.length).toBeGreaterThan(0)
                expect(receivedChunks.join('')).toBe('这是流式回复')
                
                wsStream.disconnect()
                resolve()
              }
            },
            onError: (error) => {
              console.error('WebSocket错误:', error)
              reject(error)
            },
            onClose: () => {
              console.log('WebSocket连接已关闭')
            }
          }
        )

        // 建立连接并发送消息
        wsStream.connect().then(() => {
          return wsStream.sendStreamRequest({
            message: '请进行流式回复测试',
            stream: true
          })
        }).catch(reject)

        // 设置超时
        setTimeout(() => {
          reject(new Error('流式传输测试超时'))
        }, TEST_CONFIG.timeout)
      })
    }, TEST_CONFIG.timeout)

    it('应该处理流式传输中的连接中断和重连', async () => {
      return new Promise<void>((resolve, reject) => {
        let reconnectCount = 0
        let connectionEstablished = false

        const wsStream = createWebSocketStream(
          { 
            url: TEST_CONFIG.wsURL,
            reconnectAttempts: 2,
            reconnectDelay: 500
          },
          {
            onOpen: () => {
              connectionEstablished = true
            },
            onMessage: (chunk) => {
              // 模拟连接中断
              if (reconnectCount === 0) {
                const ws = (wsStream as any).ws
                ws.close(1006, '模拟连接中断')
              }
            },
            onReconnect: (attempt) => {
              reconnectCount = attempt
              console.log(`第${attempt}次重连尝试`)
              
              if (attempt === 1) {
                // 验证重连机制
                expect(connectionEstablished).toBe(true)
                wsStream.disconnect()
                resolve()
              }
            },
            onError: (error) => {
              console.error('重连测试错误:', error)
            }
          }
        )

        wsStream.connect().then(() => {
          return wsStream.sendStreamRequest({
            message: '测试重连机制',
            stream: true
          })
        }).catch(reject)

        setTimeout(() => {
          reject(new Error('重连测试超时'))
        }, TEST_CONFIG.timeout)
      })
    }, TEST_CONFIG.timeout)
  })

  describe('响应处理集成测试', () => {
    it('应该正确处理各种响应格式', async () => {
      // 测试正常响应
      const normalResponse = aiResponseHandler.handleChatResponse(mockResponses.chatSuccess)
      expect(normalResponse.code).toBe(200)
      expect(normalResponse.data.message).toBeTruthy()

      // 测试流式响应块
      const streamChunk = JSON.stringify({
        id: 'chunk_test',
        object: 'chat.completion.chunk',
        created: Date.now(),
        model: 'gpt-3.5-turbo',
        choices: [{
          index: 0,
          delta: { content: '测试内容' }
        }]
      })

      const parsedChunk = aiResponseHandler.handleStreamChunk(streamChunk)
      expect(parsedChunk).not.toBeNull()
      expect(parsedChunk?.choices[0].delta.content).toBe('测试内容')

      // 测试错误响应
      const error = {
        response: {
          status: 400,
          data: {
            code: 400,
            msg: '参数错误'
          }
        }
      }

      const errorResult = aiResponseHandler.handleErrorResponse(error)
      expect(errorResult.code).toBe(400)
      expect(errorResult.message).toBe('参数错误')
    })

    it('应该正确格式化响应数据', () => {
      const response = mockResponses.chatSuccess.data
      const formatted = aiResponseHandler.formatResponseForDisplay(response)
      
      expect(formatted).toContain(response.message)
      expect(formatted).toContain(response.model)
      expect(formatted).toContain(response.usage.total_tokens.toString())
    })
  })

  describe('并发请求处理', () => {
    it('应该能够处理多个并发的AI请求', async () => {
      const requests: AiChatRequest[] = [
        { message: '请求1：什么是机器学习？' },
        { message: '请求2：什么是深度学习？' },
        { message: '请求3：什么是神经网络？' }
      ]

      // 为每个请求设置不同的响应
      requests.forEach((_, index) => {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: () => Promise.resolve({
            ...mockResponses.chatSuccess,
            data: {
              ...mockResponses.chatSuccess.data,
              message: `这是对请求${index + 1}的回复`,
              conversation_id: `conv_concurrent_${index + 1}`
            }
          })
        })
      })

      const promises = requests.map(request => sendAiChat(request))
      const responses = await Promise.all(promises)

      expect(responses).toHaveLength(3)
      responses.forEach((response, index) => {
        expect(response.code).toBe(200)
        expect(response.data.message).toContain(`请求${index + 1}`)
      })
    })

    it('应该能够处理部分请求失败的情况', async () => {
      const requests: AiChatRequest[] = [
        { message: '成功请求1' },
        { message: '失败请求' },
        { message: '成功请求2' }
      ]

      // 设置混合响应（成功-失败-成功）
      mockFetch
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: () => Promise.resolve(mockResponses.chatSuccess)
        })
        .mockRejectedValueOnce(new Error('模拟请求失败'))
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: () => Promise.resolve(mockResponses.chatSuccess)
        })

      const results = await Promise.allSettled(
        requests.map(request => sendAiChat(request))
      )

      expect(results[0].status).toBe('fulfilled')
      expect(results[1].status).toBe('rejected')
      expect(results[2].status).toBe('fulfilled')
    })
  })

  describe('性能和压力测试', () => {
    it('应该在合理时间内处理大量请求', async () => {
      const startTime = Date.now()
      const requestCount = 10
      
      // 设置所有请求的响应
      for (let i = 0; i < requestCount; i++) {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: () => Promise.resolve(mockResponses.chatSuccess)
        })
      }

      const requests = Array.from({ length: requestCount }, (_, i) => 
        sendAiChat({ message: `性能测试请求 ${i + 1}` })
      )

      const responses = await Promise.all(requests)
      const endTime = Date.now()
      const duration = endTime - startTime

      expect(responses).toHaveLength(requestCount)
      expect(duration).toBeLessThan(5000) // 应该在5秒内完成
      
      responses.forEach(response => {
        expect(response.code).toBe(200)
      })
    })
  })

  describe('数据一致性测试', () => {
    it('应该保持对话上下文的一致性', async () => {
      const conversationId = 'conv_consistency_test'
      
      // 第一条消息
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({
          ...mockResponses.chatSuccess,
          data: {
            ...mockResponses.chatSuccess.data,
            conversation_id: conversationId
          }
        })
      })

      const firstResponse = await sendAiChat({
        message: '我的名字是张三',
        conversation_id: conversationId
      })

      expect(firstResponse.data.conversation_id).toBe(conversationId)

      // 第二条消息（引用上下文）
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({
          ...mockResponses.chatSuccess,
          data: {
            ...mockResponses.chatSuccess.data,
            message: '你好，张三！',
            conversation_id: conversationId
          }
        })
      })

      const secondResponse = await sendAiChat({
        message: '你还记得我的名字吗？',
        conversation_id: conversationId
      })

      expect(secondResponse.data.conversation_id).toBe(conversationId)
      expect(secondResponse.data.message).toContain('张三')
    })
  })
})