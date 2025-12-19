import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { AiRequestWrapper, validateAiChatRequest, sendAiChat, getAiModels } from '../AiApi'
import type { AiChatRequest } from '../AiApi'

// Mock axios
vi.mock('../index', () => ({
  get: vi.fn(),
  post: vi.fn(),
  ApiResponse: {}
}))

import { get, post } from '../index'

describe('AiApi', () => {
  let aiWrapper: AiRequestWrapper

  beforeEach(() => {
    aiWrapper = AiRequestWrapper.getInstance()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('validateAiChatRequest', () => {
    it('应该通过有效的请求参数验证', () => {
      const validRequest: AiChatRequest = {
        message: '这是一个有效的消息',
        model: 'gpt-3.5-turbo',
        temperature: 0.7,
        max_tokens: 1024
      }

      const errors = validateAiChatRequest(validRequest)
      expect(errors).toHaveLength(0)
    })

    it('应该拒绝空消息', () => {
      const invalidRequest: AiChatRequest = {
        message: ''
      }

      const errors = validateAiChatRequest(invalidRequest)
      expect(errors).toContain('消息内容不能为空白字符')
    })

    it('应该拒绝非字符串消息', () => {
      const invalidRequest = {
        message: 123
      } as any

      const errors = validateAiChatRequest(invalidRequest)
      expect(errors).toContain('消息内容不能为空且必须为字符串')
    })

    it('应该拒绝过长的消息', () => {
      const invalidRequest: AiChatRequest = {
        message: 'a'.repeat(4001)
      }

      const errors = validateAiChatRequest(invalidRequest)
      expect(errors).toContain('消息内容不能超过4000个字符')
    })

    it('应该拒绝无效的temperature值', () => {
      const invalidRequest: AiChatRequest = {
        message: '测试消息',
        temperature: 3
      }

      const errors = validateAiChatRequest(invalidRequest)
      expect(errors).toContain('temperature参数必须是0-2之间的数字')
    })

    it('应该拒绝无效的max_tokens值', () => {
      const invalidRequest: AiChatRequest = {
        message: '测试消息',
        max_tokens: 5000
      }

      const errors = validateAiChatRequest(invalidRequest)
      expect(errors).toContain('max_tokens参数必须是1-4096之间的整数')
    })

    it('应该拒绝不支持的模型', () => {
      const invalidRequest: AiChatRequest = {
        message: '测试消息',
        model: 'unsupported-model'
      }

      const errors = validateAiChatRequest(invalidRequest)
      expect(errors).toContain('model参数必须是以下值之一: gpt-3.5-turbo, gpt-4, gpt-4-turbo, claude-3-sonnet')
    })

    it('应该返回多个验证错误', () => {
      const invalidRequest: AiChatRequest = {
        message: '',
        temperature: 5,
        max_tokens: -1
      }

      const errors = validateAiChatRequest(invalidRequest)
      expect(errors.length).toBeGreaterThan(1)
    })
  })

  describe('AiRequestWrapper', () => {
    it('应该是单例模式', () => {
      const instance1 = AiRequestWrapper.getInstance()
      const instance2 = AiRequestWrapper.getInstance()
      expect(instance1).toBe(instance2)
    })

    it('应该能设置和获取默认配置', () => {
      const config = {
        model: 'gpt-4',
        temperature: 0.8
      }

      aiWrapper.setDefaultConfig(config)
      const retrievedConfig = aiWrapper.getDefaultConfig()
      
      expect(retrievedConfig.model).toBe('gpt-4')
      expect(retrievedConfig.temperature).toBe(0.8)
    })

    it('应该合并默认配置和请求参数', async () => {
      const mockResponse = {
        code: 200,
        msg: 'success',
        data: {
          message: 'AI回复',
          conversation_id: 'conv_123',
          model: 'gpt-4',
          usage: { prompt_tokens: 10, completion_tokens: 20, total_tokens: 30 }
        }
      }

      vi.mocked(post).mockResolvedValue(mockResponse)

      aiWrapper.setDefaultConfig({
        model: 'gpt-4',
        temperature: 0.8
      })

      await aiWrapper.sendChatRequest({
        message: '测试消息'
      })

      expect(post).toHaveBeenCalledWith('/ai/chat', expect.objectContaining({
        message: '测试消息',
        model: 'gpt-4',
        temperature: 0.8
      }))
    })

    it('应该在参数验证失败时抛出错误', async () => {
      await expect(aiWrapper.sendChatRequest({
        message: ''
      })).rejects.toThrow('参数校验失败')
    })

    it('应该处理API请求错误', async () => {
      const error = new Error('网络错误')
      vi.mocked(post).mockRejectedValue(error)

      await expect(aiWrapper.sendChatRequest({
        message: '测试消息'
      })).rejects.toThrow('网络错误')
    })
  })

  describe('getAvailableModels', () => {
    it('应该成功获取模型列表', async () => {
      const mockResponse = {
        code: 200,
        msg: 'success',
        data: ['gpt-3.5-turbo', 'gpt-4']
      }

      vi.mocked(get).mockResolvedValue(mockResponse)

      const result = await aiWrapper.getAvailableModels()
      
      expect(get).toHaveBeenCalledWith('/ai/models')
      expect(result).toEqual(mockResponse)
    })

    it('应该处理获取模型列表的错误', async () => {
      const error = new Error('服务器错误')
      vi.mocked(get).mockRejectedValue(error)

      await expect(aiWrapper.getAvailableModels()).rejects.toThrow('服务器错误')
    })
  })

  describe('getConversationHistory', () => {
    it('应该成功获取对话历史', async () => {
      const mockResponse = {
        code: 200,
        msg: 'success',
        data: {
          messages: [],
          total: 0
        }
      }

      vi.mocked(get).mockResolvedValue(mockResponse)

      const result = await aiWrapper.getConversationHistory('conv_123', 1, 20)
      
      expect(get).toHaveBeenCalledWith('/ai/conversations/conv_123/history', {
        page: 1,
        page_size: 20
      })
      expect(result).toEqual(mockResponse)
    })

    it('应该使用默认分页参数', async () => {
      const mockResponse = {
        code: 200,
        msg: 'success',
        data: { messages: [] }
      }

      vi.mocked(get).mockResolvedValue(mockResponse)

      await aiWrapper.getConversationHistory('conv_123')
      
      expect(get).toHaveBeenCalledWith('/ai/conversations/conv_123/history', {
        page: 1,
        page_size: 20
      })
    })

    it('应该验证conversationId参数', async () => {
      await expect(aiWrapper.getConversationHistory('')).rejects.toThrow('对话ID不能为空且必须为字符串')
      await expect(aiWrapper.getConversationHistory(null as any)).rejects.toThrow('对话ID不能为空且必须为字符串')
    })

    it('应该验证分页参数', async () => {
      await expect(aiWrapper.getConversationHistory('conv_123', 0)).rejects.toThrow('页码必须大于0，每页数量必须在1-100之间')
      await expect(aiWrapper.getConversationHistory('conv_123', 1, 101)).rejects.toThrow('页码必须大于0，每页数量必须在1-100之间')
    })
  })

  describe('deleteConversation', () => {
    it('应该成功删除对话', async () => {
      const mockResponse = {
        code: 200,
        msg: 'success',
        data: null
      }

      vi.mocked(post).mockResolvedValue(mockResponse)

      const result = await aiWrapper.deleteConversation('conv_123')
      
      expect(post).toHaveBeenCalledWith('/ai/conversations/conv_123/delete')
      expect(result).toEqual(mockResponse)
    })

    it('应该验证conversationId参数', async () => {
      await expect(aiWrapper.deleteConversation('')).rejects.toThrow('对话ID不能为空且必须为字符串')
      await expect(aiWrapper.deleteConversation(null as any)).rejects.toThrow('对话ID不能为空且必须为字符串')
    })

    it('应该处理删除对话的错误', async () => {
      const error = new Error('删除失败')
      vi.mocked(post).mockRejectedValue(error)

      await expect(aiWrapper.deleteConversation('conv_123')).rejects.toThrow('删除失败')
    })
  })

  describe('导出的便捷函数', () => {
    it('sendAiChat应该调用AiRequestWrapper实例', async () => {
      const mockResponse = {
        code: 200,
        msg: 'success',
        data: {
          message: 'AI回复',
          conversation_id: 'conv_123',
          model: 'gpt-3.5-turbo',
          usage: { prompt_tokens: 10, completion_tokens: 20, total_tokens: 30 }
        }
      }

      vi.mocked(post).mockResolvedValue(mockResponse)

      const result = await sendAiChat({
        message: '测试消息'
      })

      expect(result).toEqual(mockResponse)
    })

    it('getAiModels应该调用AiRequestWrapper实例', async () => {
      const mockResponse = {
        code: 200,
        msg: 'success',
        data: ['gpt-3.5-turbo', 'gpt-4']
      }

      vi.mocked(get).mockResolvedValue(mockResponse)

      const result = await getAiModels()

      expect(result).toEqual(mockResponse)
    })
  })

  describe('边界条件测试', () => {
    it('应该处理最大长度的有效消息', () => {
      const validRequest: AiChatRequest = {
        message: 'a'.repeat(4000)
      }

      const errors = validateAiChatRequest(validRequest)
      expect(errors).toHaveLength(0)
    })

    it('应该处理边界值的temperature', () => {
      const validRequest1: AiChatRequest = {
        message: '测试消息',
        temperature: 0
      }

      const validRequest2: AiChatRequest = {
        message: '测试消息',
        temperature: 2
      }

      expect(validateAiChatRequest(validRequest1)).toHaveLength(0)
      expect(validateAiChatRequest(validRequest2)).toHaveLength(0)
    })

    it('应该处理边界值的max_tokens', () => {
      const validRequest1: AiChatRequest = {
        message: '测试消息',
        max_tokens: 1
      }

      const validRequest2: AiChatRequest = {
        message: '测试消息',
        max_tokens: 4096
      }

      expect(validateAiChatRequest(validRequest1)).toHaveLength(0)
      expect(validateAiChatRequest(validRequest2)).toHaveLength(0)
    })
  })

  describe('错误处理测试', () => {
    it('应该正确处理网络超时错误', async () => {
      const timeoutError = new Error('timeout')
      timeoutError.name = 'TimeoutError'
      vi.mocked(post).mockRejectedValue(timeoutError)

      await expect(aiWrapper.sendChatRequest({
        message: '测试消息'
      })).rejects.toThrow('timeout')
    })

    it('应该正确处理服务器5xx错误', async () => {
      const serverError = {
        response: {
          status: 500,
          data: {
            code: 500,
            msg: '服务器内部错误'
          }
        }
      }
      vi.mocked(post).mockRejectedValue(serverError)

      await expect(aiWrapper.sendChatRequest({
        message: '测试消息'
      })).rejects.toThrow()
    })
  })
})