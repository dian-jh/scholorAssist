/**
 * AuthEventHandler 单元测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { AuthEventHandler } from '../authEventHandler'
import { TokenManager } from '../tokenManager'
import { useUserStore } from '@/store/modules/user'

// Mock dependencies
vi.mock('../tokenManager')
vi.mock('@/store/modules/user')
vi.mock('@/router', () => ({
  default: {
    push: vi.fn()
  }
}))

const mockTokenManager = vi.mocked(TokenManager)
const mockUserStore = {
  clearUserData: vi.fn(),
  checkAuthStatus: vi.fn()
}

vi.mocked(useUserStore).mockReturnValue(mockUserStore as any)

// Mock DOM events
const mockAddEventListener = vi.fn()
const mockRemoveEventListener = vi.fn()

Object.defineProperty(window, 'addEventListener', {
  value: mockAddEventListener
})

Object.defineProperty(window, 'removeEventListener', {
  value: mockRemoveEventListener
})

Object.defineProperty(document, 'addEventListener', {
  value: mockAddEventListener
})

Object.defineProperty(document, 'removeEventListener', {
  value: mockRemoveEventListener
})

describe('AuthEventHandler', () => {
  let authEventHandler: AuthEventHandler

  beforeEach(() => {
    vi.clearAllMocks()
    authEventHandler = new AuthEventHandler()
  })

  afterEach(() => {
    authEventHandler.destroy()
  })

  describe('initialize', () => {
    it('应该初始化所有事件监听器', () => {
      authEventHandler.initialize()

      expect(mockAddEventListener).toHaveBeenCalledWith(
        'storage',
        expect.any(Function)
      )
      expect(mockAddEventListener).toHaveBeenCalledWith(
        'visibilitychange',
        expect.any(Function)
      )
      expect(mockAddEventListener).toHaveBeenCalledWith(
        'focus',
        expect.any(Function)
      )
    })
  })

  describe('destroy', () => {
    it('应该移除所有事件监听器', () => {
      authEventHandler.initialize()
      authEventHandler.destroy()

      expect(mockRemoveEventListener).toHaveBeenCalledWith(
        'storage',
        expect.any(Function)
      )
      expect(mockRemoveEventListener).toHaveBeenCalledWith(
        'visibilitychange',
        expect.any(Function)
      )
      expect(mockRemoveEventListener).toHaveBeenCalledWith(
        'focus',
        expect.any(Function)
      )
    })
  })

  describe('handleTokenExpiration', () => {
    it('应该处理token过期', async () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

      await authEventHandler.handleTokenExpiration()

      expect(mockUserStore.clearUserData).toHaveBeenCalled()
      expect(consoleSpy).toHaveBeenCalledWith('Token已过期，用户已登出')

      consoleSpy.mockRestore()
    })
  })

  describe('handleStorageChange', () => {
    it('应该处理token相关的存储变化', () => {
      const event = new StorageEvent('storage', {
        key: 'auth_token',
        newValue: null,
        oldValue: 'old-token'
      })

      authEventHandler.handleStorageChange(event)

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })

    it('应该处理refreshToken相关的存储变化', () => {
      const event = new StorageEvent('storage', {
        key: 'refresh_token',
        newValue: 'new-refresh-token',
        oldValue: 'old-refresh-token'
      })

      authEventHandler.handleStorageChange(event)

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })

    it('应该处理isLoggedIn相关的存储变化', () => {
      const event = new StorageEvent('storage', {
        key: 'isLoggedIn',
        newValue: 'false',
        oldValue: 'true'
      })

      authEventHandler.handleStorageChange(event)

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })

    it('应该忽略非认证相关的存储变化', () => {
      const event = new StorageEvent('storage', {
        key: 'someOtherKey',
        newValue: 'value',
        oldValue: 'oldValue'
      })

      authEventHandler.handleStorageChange(event)

      expect(mockUserStore.checkAuthStatus).not.toHaveBeenCalled()
    })

    it('应该处理null key的情况', () => {
      const event = new StorageEvent('storage', {
        key: null,
        newValue: 'value',
        oldValue: 'oldValue'
      })

      authEventHandler.handleStorageChange(event)

      expect(mockUserStore.checkAuthStatus).not.toHaveBeenCalled()
    })
  })

  describe('handleVisibilityChange', () => {
    it('应该在页面变为可见时检查认证状态', () => {
      Object.defineProperty(document, 'visibilityState', {
        value: 'visible',
        configurable: true
      })

      authEventHandler.handleVisibilityChange()

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })

    it('应该在页面隐藏时不执行任何操作', () => {
      Object.defineProperty(document, 'visibilityState', {
        value: 'hidden',
        configurable: true
      })

      authEventHandler.handleVisibilityChange()

      expect(mockUserStore.checkAuthStatus).not.toHaveBeenCalled()
    })
  })

  describe('handleWindowFocus', () => {
    it('应该在窗口获得焦点时检查认证状态', () => {
      authEventHandler.handleWindowFocus()

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })
  })

  describe('事件监听器绑定', () => {
    it('应该正确绑定storage事件', () => {
      authEventHandler.initialize()

      // 获取绑定的事件处理函数
      const storageHandler = mockAddEventListener.mock.calls.find(
        call => call[0] === 'storage'
      )?.[1]

      expect(storageHandler).toBeDefined()

      // 模拟storage事件
      const event = new StorageEvent('storage', {
        key: 'auth_token',
        newValue: null
      })

      storageHandler(event)

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })

    it('应该正确绑定visibilitychange事件', () => {
      authEventHandler.initialize()

      // 获取绑定的事件处理函数
      const visibilityHandler = mockAddEventListener.mock.calls.find(
        call => call[0] === 'visibilitychange'
      )?.[1]

      expect(visibilityHandler).toBeDefined()

      Object.defineProperty(document, 'visibilityState', {
        value: 'visible',
        configurable: true
      })

      visibilityHandler()

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })

    it('应该正确绑定focus事件', () => {
      authEventHandler.initialize()

      // 获取绑定的事件处理函数
      const focusHandler = mockAddEventListener.mock.calls.find(
        call => call[0] === 'focus'
      )?.[1]

      expect(focusHandler).toBeDefined()

      focusHandler()

      expect(mockUserStore.checkAuthStatus).toHaveBeenCalled()
    })
  })
})