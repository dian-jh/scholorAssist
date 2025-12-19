/**
 * TokenRefreshService 单元测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TokenRefreshService } from '../tokenRefreshService'
import { TokenManager } from '@/utils/tokenManager'

// Mock dependencies
vi.mock('@/utils/tokenManager')
vi.mock('@/api/index')

const mockTokenManager = vi.mocked(TokenManager)
const mockPost = vi.fn()

vi.mock('@/api/index', () => ({
  post: mockPost
}))

describe('TokenRefreshService', () => {
  let service: TokenRefreshService

  beforeEach(() => {
    vi.clearAllMocks()
    service = TokenRefreshService.getInstance()
  })

  afterEach(() => {
    service.stop()
  })

  describe('getInstance', () => {
    it('应该返回单例实例', () => {
      const instance1 = TokenRefreshService.getInstance()
      const instance2 = TokenRefreshService.getInstance()
      
      expect(instance1).toBe(instance2)
    })
  })

  describe('refreshToken', () => {
    it('应该在不需要刷新时返回true', async () => {
      mockTokenManager.shouldRefreshToken.mockReturnValue(false)

      const result = await service.refreshToken()
      
      expect(result).toBe(true)
      expect(mockPost).not.toHaveBeenCalled()
    })

    it('应该在没有refreshToken时返回false', async () => {
      mockTokenManager.shouldRefreshToken.mockReturnValue(true)
      mockTokenManager.getRefreshToken.mockReturnValue(null)

      const result = await service.refreshToken()
      
      expect(result).toBe(false)
      expect(mockPost).not.toHaveBeenCalled()
    })

    it('应该成功刷新token', async () => {
      const refreshToken = 'test-refresh-token'
      const newToken = 'new-token'
      const newRefreshToken = 'new-refresh-token'
      const expiresIn = 3600

      mockTokenManager.shouldRefreshToken.mockReturnValue(true)
      mockTokenManager.getRefreshToken.mockReturnValue(refreshToken)
      
      mockPost.mockResolvedValue({
        code: 200,
        data: {
          token: newToken,
          refreshToken: newRefreshToken,
          expiresIn
        }
      })

      const result = await service.refreshToken()
      
      expect(result).toBe(true)
      expect(mockPost).toHaveBeenCalledWith('/auth/refresh', {
        refresh_token: refreshToken
      })
      expect(mockTokenManager.updateToken).toHaveBeenCalledWith(
        newToken,
        newRefreshToken,
        expiresIn
      )
    })

    it('应该处理刷新失败的情况', async () => {
      const refreshToken = 'test-refresh-token'

      mockTokenManager.shouldRefreshToken.mockReturnValue(true)
      mockTokenManager.getRefreshToken.mockReturnValue(refreshToken)
      
      mockPost.mockResolvedValue({
        code: 400,
        msg: '刷新失败'
      })

      const result = await service.refreshToken()
      
      expect(result).toBe(false)
      expect(mockTokenManager.clearTokenInfo).toHaveBeenCalled()
    })

    it('应该处理401错误（refresh token过期）', async () => {
      const refreshToken = 'test-refresh-token'

      mockTokenManager.shouldRefreshToken.mockReturnValue(true)
      mockTokenManager.getRefreshToken.mockReturnValue(refreshToken)
      
      const error = new Error('Unauthorized')
      error.response = { status: 401 }
      mockPost.mockRejectedValue(error)

      const result = await service.refreshToken()
      
      expect(result).toBe(false)
      expect(mockTokenManager.clearTokenInfo).toHaveBeenCalled()
    })

    it('应该重试失败的请求', async () => {
      const refreshToken = 'test-refresh-token'

      mockTokenManager.shouldRefreshToken.mockReturnValue(true)
      mockTokenManager.getRefreshToken.mockReturnValue(refreshToken)
      
      // 第一次失败，第二次成功
      mockPost
        .mockRejectedValueOnce(new Error('Network error'))
        .mockResolvedValueOnce({
          code: 200,
          data: {
            token: 'new-token',
            refreshToken: 'new-refresh-token',
            expiresIn: 3600
          }
        })

      const result = await service.refreshToken()
      
      expect(result).toBe(true)
      expect(mockPost).toHaveBeenCalledTimes(2)
    })

    it('应该在达到最大重试次数后失败', async () => {
      const refreshToken = 'test-refresh-token'

      mockTokenManager.shouldRefreshToken.mockReturnValue(true)
      mockTokenManager.getRefreshToken.mockReturnValue(refreshToken)
      
      mockPost.mockRejectedValue(new Error('Network error'))

      const result = await service.refreshToken()
      
      expect(result).toBe(false)
      expect(mockPost).toHaveBeenCalledTimes(3) // 默认最大重试3次
      expect(mockTokenManager.clearTokenInfo).toHaveBeenCalled()
    })
  })

  describe('start and stop', () => {
    it('应该启动和停止服务', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {})

      service.start()
      expect(consoleSpy).toHaveBeenCalledWith('Token刷新服务已启动')

      service.stop()
      expect(consoleSpy).toHaveBeenCalledWith('Token刷新服务已停止')

      consoleSpy.mockRestore()
    })
  })

  describe('getStatus', () => {
    it('应该返回服务状态', () => {
      mockTokenManager.getTokenInfo.mockReturnValue({
        token: 'test-token',
        refreshToken: 'test-refresh-token',
        expiresAt: Date.now() + 3600000,
        issuedAt: Date.now()
      })
      mockTokenManager.isTokenValid.mockReturnValue(true)
      mockTokenManager.shouldRefreshToken.mockReturnValue(false)
      mockTokenManager.getTokenRemainingTime.mockReturnValue(3600000)

      const status = service.getStatus()
      
      expect(status).toEqual({
        isRunning: expect.any(Boolean),
        hasValidToken: true,
        shouldRefresh: false,
        nextRefreshTime: expect.any(Date)
      })
    })

    it('应该处理没有token的情况', () => {
      mockTokenManager.getTokenInfo.mockReturnValue(null)
      mockTokenManager.isTokenValid.mockReturnValue(false)
      mockTokenManager.shouldRefreshToken.mockReturnValue(false)
      mockTokenManager.getTokenRemainingTime.mockReturnValue(0)

      const status = service.getStatus()
      
      expect(status).toEqual({
        isRunning: expect.any(Boolean),
        hasValidToken: false,
        shouldRefresh: false,
        nextRefreshTime: undefined
      })
    })
  })
})