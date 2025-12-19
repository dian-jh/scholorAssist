import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  AiResponseHandler,
  AiChatResponseValidator,
  AiStreamChunkValidator,
  handleAiChatResponse,
  handleStreamChunk,
  handleAiError
} from '../AiResponseHandler'
import type { ApiResponse } from '../index'
import type { AiChatResponse, AiStreamChunk } from '../AiApi'

describe('AiResponseHandler', () => {
  let handler: AiResponseHandler

  beforeEach(() => {
    handler = AiResponseHandler.getInstance()
  })

  describe('AiChatResponseValidator', () => {
    let validator: AiChatResponseValidator

    beforeEach(() => {
      validator = new AiChatResponseValidator()
    })

    it('应该验证有效的AI聊天响应', () => {
      const validData: AiChatResponse = {
        message: '这是AI的回复',
        conversation_id: 'conv_123',
        model: 'gpt-3.5-turbo',
        usage: {
          prompt_tokens: 10,
          completion_tokens: 20,
          total_tokens: 30
        }
      }

      const result = validator.validate(validData)
      expect(result.isValid).toBe(true)
      expect(result.data).toEqual(validData)
      expect(result.errors).toHaveLength(0)
    })

    it('应该拒绝非对象类型的数据', () => {
      const result = validator.validate('invalid data')
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('响应数据必须是对象类型')
    })

    it('应该拒绝缺少message字段的数据', () => {
      const invalidData = {
        conversation_id: 'conv_123',
        model: 'gpt-3.5-turbo',
        usage: { prompt_tokens: 10, completion_tokens: 20, total_tokens: 30 }
      }

      const result = validator.validate(invalidData)
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('message字段必须是非空字符串')
    })

    it('应该拒绝无效的usage字段', () => {
      const invalidData = {
        message: '回复',
        conversation_id: 'conv_123',
        model: 'gpt-3.5-turbo',
        usage: {
          prompt_tokens: -1,
          completion_tokens: 'invalid',
          total_tokens: 30
        }
      }

      const result = validator.validate(invalidData)
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('usage.prompt_tokens必须是非负数')
      expect(result.errors).toContain('usage.completion_tokens必须是非负数')
    })

    it('应该拒绝缺少usage字段的数据', () => {
      const invalidData = {
        message: '回复',
        conversation_id: 'conv_123',
        model: 'gpt-3.5-turbo'
      }

      const result = validator.validate(invalidData)
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('usage字段必须是对象类型')
    })
  })

  describe('AiStreamChunkValidator', () => {
    let validator: AiStreamChunkValidator

    beforeEach(() => {
      validator = new AiStreamChunkValidator()
    })

    it('应该验证有效的流式响应块', () => {
      const validChunk: AiStreamChunk = {
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 0,
            delta: {
              content: '这是流式内容'
            },
            finish_reason: null
          }
        ]
      }

      const result = validator.validate(validChunk)
      expect(result.isValid).toBe(true)
      expect(result.data).toEqual(validChunk)
      expect(result.errors).toHaveLength(0)
    })

    it('应该拒绝非对象类型的数据', () => {
      const result = validator.validate('invalid chunk')
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('流式响应数据必须是对象类型')
    })

    it('应该拒绝缺少必要字段的数据', () => {
      const invalidChunk = {
        object: 'chat.completion.chunk',
        created: 1699123456
      }

      const result = validator.validate(invalidChunk)
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('id字段必须是非空字符串')
      expect(result.errors).toContain('model字段必须是非空字符串')
    })

    it('应该拒绝无效的choices字段', () => {
      const invalidChunk = {
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: 'invalid'
      }

      const result = validator.validate(invalidChunk)
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('choices字段必须是数组类型')
    })

    it('应该验证choices数组中的元素', () => {
      const invalidChunk = {
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 'invalid',
            delta: 'invalid'
          }
        ]
      }

      const result = validator.validate(invalidChunk)
      expect(result.isValid).toBe(false)
      expect(result.errors).toContain('choices[0].index必须是数字类型')
      expect(result.errors).toContain('choices[0].delta必须是对象类型')
    })
  })

  describe('AiResponseHandler', () => {
    it('应该是单例模式', () => {
      const instance1 = AiResponseHandler.getInstance()
      const instance2 = AiResponseHandler.getInstance()
      expect(instance1).toBe(instance2)
    })

    it('应该成功处理有效的AI聊天响应', () => {
      const validResponse: ApiResponse<AiChatResponse> = {
        code: 200,
        msg: 'success',
        data: {
          message: 'AI回复',
          conversation_id: 'conv_123',
          model: 'gpt-3.5-turbo',
          usage: {
            prompt_tokens: 10,
            completion_tokens: 20,
            total_tokens: 30
          }
        }
      }

      const result = handler.handleChatResponse(validResponse)
      expect(result.code).toBe(200)
      expect(result.msg).toBe('success')
      expect(result.data).toEqual(validResponse.data)
    })

    it('应该拒绝无效的API响应格式', () => {
      const invalidResponse = {
        status: 200,
        message: 'success'
      } as any

      expect(() => handler.handleChatResponse(invalidResponse))
        .toThrow('响应格式不符合API规范')
    })

    it('应该拒绝无效的AI响应数据', () => {
      const invalidResponse: ApiResponse<any> = {
        code: 200,
        msg: 'success',
        data: {
          message: '',
          conversation_id: 'conv_123'
        }
      }

      expect(() => handler.handleChatResponse(invalidResponse))
        .toThrow('AI响应数据校验失败')
    })

    it('应该成功处理有效的流式响应块', () => {
      const validChunkString = JSON.stringify({
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 0,
            delta: {
              content: '流式内容'
            }
          }
        ]
      })

      const result = handler.handleStreamChunk(validChunkString)
      expect(result).not.toBeNull()
      expect(result?.id).toBe('chunk_001')
      expect(result?.choices[0].delta.content).toBe('流式内容')
    })

    it('应该处理无效的JSON字符串', () => {
      const invalidJson = 'invalid json'
      const result = handler.handleStreamChunk(invalidJson)
      expect(result).toBeNull()
    })

    it('应该处理校验失败的流式数据', () => {
      const invalidChunkString = JSON.stringify({
        id: 'chunk_001'
      })

      const result = handler.handleStreamChunk(invalidChunkString)
      expect(result).toBeNull()
    })

    it('应该正确处理API响应错误', () => {
      const apiError = {
        response: {
          status: 400,
          data: {
            code: 400,
            msg: '参数错误'
          }
        }
      }

      const result = handler.handleErrorResponse(apiError)
      expect(result.code).toBe(400)
      expect(result.message).toBe('参数错误')
      expect(result.details).toEqual(apiError.response.data)
    })

    it('应该处理网络超时错误', () => {
      const timeoutError = {
        code: 'ECONNABORTED',
        message: '请求超时'
      }

      const result = handler.handleErrorResponse(timeoutError)
      expect(result.code).toBe(408)
      expect(result.message).toBe('请求超时，请检查网络连接')
    })

    it('应该处理未知错误', () => {
      const unknownError = new Error('未知错误')

      const result = handler.handleErrorResponse(unknownError)
      expect(result.code).toBe(500)
      expect(result.message).toBe('未知错误')
    })

    it('应该批量处理响应数据', () => {
      const responses: ApiResponse<any>[] = [
        {
          code: 200,
          msg: 'success',
          data: {
            message: '回复1',
            conversation_id: 'conv_1',
            model: 'gpt-3.5-turbo',
            usage: { prompt_tokens: 10, completion_tokens: 20, total_tokens: 30 }
          }
        },
        {
          code: 200,
          msg: 'success',
          data: {
            message: '',
            conversation_id: 'conv_2'
          }
        }
      ]

      const validator = new AiChatResponseValidator()
      const result = handler.handleBatchResponses(responses, validator)

      expect(result.successful).toHaveLength(1)
      expect(result.failed).toHaveLength(1)
      expect(result.failed[0].index).toBe(1)
    })

    it('应该格式化响应数据用于显示', () => {
      const response: AiChatResponse = {
        message: 'AI回复内容',
        conversation_id: 'conv_123',
        model: 'gpt-4',
        usage: {
          prompt_tokens: 15,
          completion_tokens: 25,
          total_tokens: 40
        }
      }

      const formatted = handler.formatResponseForDisplay(response)
      expect(formatted).toContain('AI回复内容')
      expect(formatted).toContain('gpt-4')
      expect(formatted).toContain('40')
      expect(formatted).toContain('15')
      expect(formatted).toContain('25')
    })

    it('应该提取流式响应中的文本内容', () => {
      const chunk: AiStreamChunk = {
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 0,
            delta: {
              content: '提取的内容'
            }
          }
        ]
      }

      const content = handler.extractStreamContent(chunk)
      expect(content).toBe('提取的内容')
    })

    it('应该处理空的choices数组', () => {
      const chunk: AiStreamChunk = {
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: []
      }

      const content = handler.extractStreamContent(chunk)
      expect(content).toBe('')
    })

    it('应该检查流式响应是否结束', () => {
      const finishedChunk: AiStreamChunk = {
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 0,
            delta: {},
            finish_reason: 'stop'
          }
        ]
      }

      const continuingChunk: AiStreamChunk = {
        id: 'chunk_002',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 0,
            delta: {
              content: '继续中'
            }
          }
        ]
      }

      expect(handler.isStreamFinished(finishedChunk)).toBe(true)
      expect(handler.isStreamFinished(continuingChunk)).toBe(false)
    })
  })

  describe('导出的便捷函数', () => {
    it('handleAiChatResponse应该调用handler实例', () => {
      const validResponse: ApiResponse<AiChatResponse> = {
        code: 200,
        msg: 'success',
        data: {
          message: 'AI回复',
          conversation_id: 'conv_123',
          model: 'gpt-3.5-turbo',
          usage: {
            prompt_tokens: 10,
            completion_tokens: 20,
            total_tokens: 30
          }
        }
      }

      const result = handleAiChatResponse(validResponse)
      expect(result.code).toBe(200)
      expect(result.data.message).toBe('AI回复')
    })

    it('handleStreamChunk应该调用handler实例', () => {
      const validChunkString = JSON.stringify({
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 0,
            delta: {
              content: '流式内容'
            }
          }
        ]
      })

      const result = handleStreamChunk(validChunkString)
      expect(result).not.toBeNull()
      expect(result?.id).toBe('chunk_001')
    })

    it('handleAiError应该调用handler实例', () => {
      const error = new Error('测试错误')
      const result = handleAiError(error)
      expect(result.code).toBe(500)
      expect(result.message).toBe('测试错误')
    })
  })

  describe('边界条件和错误处理', () => {
    it('应该处理null和undefined数据', () => {
      const validator = new AiChatResponseValidator()
      
      expect(validator.validate(null).isValid).toBe(false)
      expect(validator.validate(undefined).isValid).toBe(false)
    })

    it('应该处理空对象', () => {
      const validator = new AiChatResponseValidator()
      const result = validator.validate({})
      
      expect(result.isValid).toBe(false)
      expect(result.errors.length).toBeGreaterThan(0)
    })

    it('应该处理包含额外字段的有效数据', () => {
      const validator = new AiChatResponseValidator()
      const dataWithExtraFields = {
        message: 'AI回复',
        conversation_id: 'conv_123',
        model: 'gpt-3.5-turbo',
        usage: {
          prompt_tokens: 10,
          completion_tokens: 20,
          total_tokens: 30
        },
        extraField: 'extra'
      }

      const result = validator.validate(dataWithExtraFields)
      expect(result.isValid).toBe(true)
    })
  })
})