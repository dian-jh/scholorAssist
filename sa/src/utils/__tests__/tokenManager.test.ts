/**
 * TokenManager 单元测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TokenManager } from '../tokenManager'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

describe('TokenManager', () => {
  beforeEach(() => {
    // 清除所有mock调用记录
    vi.clearAllMocks()
    
    // 重置localStorage mock
    localStorageMock.getItem.mockReturnValue(null)
  })

  afterEach(() => {
    // 清除token信息
    TokenManager.clearTokenInfo()
  })

  describe('setTokenInfo', () => {
    it('应该正确设置token信息', () => {
      const token = 'test-token'
      const refreshToken = 'test-refresh-token'
      const expiresIn = 3600

      TokenManager.setTokenInfo(token, refreshToken, expiresIn)

      expect(localStorageMock.setItem).toHaveBeenCalledWith('auth_token', token)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refresh_token', refreshToken)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('token_info', expect.any(String))
    })

    it('应该处理空的refreshToken', () => {
      const token = 'test-token'
      const expiresIn = 3600

      TokenManager.setTokenInfo(token, '', expiresIn)

      expect(localStorageMock.setItem).toHaveBeenCalledWith('auth_token', token)
    })
  })

  describe('getToken', () => {
    it('应该返回存储的token', () => {
      const token = 'test-token'
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'auth_token') return token
        return null
      })

      const result = TokenManager.getToken()
      expect(result).toBe(token)
    })

    it('应该在没有token时返回null', () => {
      const result = TokenManager.getToken()
      expect(result).toBeNull()
    })
  })

  describe('getRefreshToken', () => {
    it('应该返回存储的refreshToken', () => {
      const refreshToken = 'test-refresh-token'
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'refresh_token') return refreshToken
        return null
      })

      const result = TokenManager.getRefreshToken()
      expect(result).toBe(refreshToken)
    })

    it('应该在没有refreshToken时返回null', () => {
      const result = TokenManager.getRefreshToken()
      expect(result).toBeNull()
    })
  })

  describe('isTokenValid', () => {
    it('应该在token有效时返回true', () => {
      const futureTime = Date.now() + 3600000 // 1小时后
      const tokenInfo = {
        token: 'test-token',
        expiresAt: futureTime,
        issuedAt: Date.now()
      }
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'auth_token') return 'test-token'
        if (key === 'token_info') return JSON.stringify(tokenInfo)
        return null
      })

      const result = TokenManager.isTokenValid()
      expect(result).toBe(true)
    })

    it('应该在token过期时返回false', () => {
      const pastTime = Date.now() - 3600000 // 1小时前
      const tokenInfo = {
        token: 'test-token',
        expiresAt: pastTime,
        issuedAt: Date.now() - 7200000
      }
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'auth_token') return 'test-token'
        if (key === 'token_info') return JSON.stringify(tokenInfo)
        return null
      })

      const result = TokenManager.isTokenValid()
      expect(result).toBe(false)
    })

    it('应该在没有token时返回false', () => {
      const result = TokenManager.isTokenValid()
      expect(result).toBe(false)
    })

    it('应该在没有过期时间时返回false', () => {
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'auth_token') return 'test-token'
        return null
      })

      const result = TokenManager.isTokenValid()
      expect(result).toBe(false)
    })
  })

  describe('shouldRefreshToken', () => {
    it('应该在需要刷新时返回true', () => {
      const nearFutureTime = Date.now() + 240000 // 4分钟后（小于5分钟阈值）
      const tokenInfo = {
        token: 'test-token',
        refreshToken: 'test-refresh-token',
        expiresAt: nearFutureTime,
        issuedAt: Date.now()
      }
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'auth_token') return 'test-token'
        if (key === 'refresh_token') return 'test-refresh-token'
        if (key === 'token_info') return JSON.stringify(tokenInfo)
        return null
      })

      const result = TokenManager.shouldRefreshToken()
      expect(result).toBe(true)
    })

    it('应该在不需要刷新时返回false', () => {
      const farFutureTime = Date.now() + 3600000 // 1小时后
      const tokenInfo = {
        token: 'test-token',
        refreshToken: 'test-refresh-token',
        expiresAt: farFutureTime,
        issuedAt: Date.now()
      }
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'auth_token') return 'test-token'
        if (key === 'refresh_token') return 'test-refresh-token'
        if (key === 'token_info') return JSON.stringify(tokenInfo)
        return null
      })

      const result = TokenManager.shouldRefreshToken()
      expect(result).toBe(false)
    })

    it('应该在没有refreshToken时返回false', () => {
      const nearFutureTime = Date.now() + 240000
      const tokenInfo = {
        token: 'test-token',
        expiresAt: nearFutureTime,
        issuedAt: Date.now()
      }
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'auth_token') return 'test-token'
        if (key === 'token_info') return JSON.stringify(tokenInfo)
        return null
      })

      const result = TokenManager.shouldRefreshToken()
      expect(result).toBe(false)
    })
  })

  describe('getTokenRemainingTime', () => {
    it('应该返回正确的剩余时间', () => {
      const futureTime = Date.now() + 3600000 // 1小时后
      const tokenInfo = {
        token: 'test-token',
        expiresAt: futureTime,
        issuedAt: Date.now()
      }
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'token_info') return JSON.stringify(tokenInfo)
        return null
      })

      const result = TokenManager.getTokenRemainingTime()
      expect(result).toBeGreaterThan(3590000) // 约59分钟
      expect(result).toBeLessThanOrEqual(3600000) // 不超过1小时
    })

    it('应该在token过期时返回0', () => {
      const pastTime = Date.now() - 3600000 // 1小时前
      const tokenInfo = {
        token: 'test-token',
        expiresAt: pastTime,
        issuedAt: Date.now() - 7200000
      }
      localStorageMock.getItem.mockImplementation((key) => {
        if (key === 'token_info') return JSON.stringify(tokenInfo)
        return null
      })

      const result = TokenManager.getTokenRemainingTime()
      expect(result).toBe(0)
    })

    it('应该在没有过期时间时返回0', () => {
      const result = TokenManager.getTokenRemainingTime()
      expect(result).toBe(0)
    })
  })

  describe('updateToken', () => {
    it('应该更新token信息', () => {
      const newToken = 'new-token'
      const newRefreshToken = 'new-refresh-token'
      const expiresIn = 7200

      TokenManager.updateToken(newToken, newRefreshToken, expiresIn)

      expect(localStorageMock.setItem).toHaveBeenCalledWith('auth_token', newToken)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refresh_token', newRefreshToken)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('token_info', expect.any(String))
    })
  })

  describe('clearTokenInfo', () => {
    it('应该清除所有token信息', () => {
      TokenManager.clearTokenInfo()

      expect(localStorageMock.removeItem).toHaveBeenCalledWith('auth_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('refresh_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('token_info')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('user_info')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('is_logged_in')
    })
  })

  describe('getTokenInfo', () => {
    it('应该返回完整的token信息', () => {
      const token = 'test-token'
      const refreshToken = 'test-refresh-token'
      const expiresAt = Date.now() + 3600000
      const issuedAt = Date.now()

      const tokenInfo = {
        token,
        refreshToken,
        expiresAt,
        issuedAt
      }

      localStorageMock.getItem.mockImplementation((key) => {
        switch (key) {
          case 'auth_token': return token
          case 'refresh_token': return refreshToken
          case 'token_info': return JSON.stringify(tokenInfo)
          default: return null
        }
      })

      const result = TokenManager.getTokenInfo()
      expect(result).toEqual({
        token,
        refreshToken,
        expiresAt,
        issuedAt
      })
    })

    it('应该在没有token时返回null', () => {
      const result = TokenManager.getTokenInfo()
      expect(result).toBeNull()
    })
  })
})