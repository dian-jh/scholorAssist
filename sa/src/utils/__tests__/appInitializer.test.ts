/**
 * AppInitializer 单元测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { AppInitializer } from '../appInitializer'
import { AuthEventHandler } from '../authEventHandler'
import { TokenManager } from '../tokenManager'
import { tokenRefreshService } from '@/services/tokenRefreshService'
import { useUserStore } from '@/store/modules/user'

// Mock dependencies
vi.mock('../authEventHandler')
vi.mock('../tokenManager')
vi.mock('@/services/tokenRefreshService')
vi.mock('@/store/modules/user')

const mockAuthEventHandler = {
  initialize: vi.fn(),
  destroy: vi.fn()
}

const mockTokenRefreshService = {
  start: vi.fn(),
  stop: vi.fn()
}

const mockUserStore = {
  initializeAuth: vi.fn()
}

// 修复mock实现
vi.mocked(AuthEventHandler).mockImplementation(() => mockAuthEventHandler as any)
vi.mocked(tokenRefreshService, true).mockReturnValue(mockTokenRefreshService as any)
vi.mocked(useUserStore).mockReturnValue(mockUserStore as any)

// Mock console methods
const mockConsoleLog = vi.spyOn(console, 'log').mockImplementation(() => {})
const mockConsoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

// Mock window.addEventListener
const mockAddEventListener = vi.fn()
const mockRemoveEventListener = vi.fn()

Object.defineProperty(window, 'addEventListener', {
  value: mockAddEventListener
})

Object.defineProperty(window, 'removeEventListener', {
  value: mockRemoveEventListener
})

describe('AppInitializer', () => {
  let appInitializer: AppInitializer

  beforeEach(() => {
    vi.clearAllMocks()
    appInitializer = new AppInitializer()
  })

  afterEach(() => {
    appInitializer.destroy()
    mockConsoleLog.mockClear()
    mockConsoleError.mockClear()
  })

  describe('initialize', () => {
    it('应该成功初始化应用', async () => {
      vi.mocked(TokenManager.isTokenValid).mockReturnValue(true)

      await appInitializer.initialize()

      expect(mockAuthEventHandler.initialize).toHaveBeenCalled()
      expect(mockUserStore.initializeAuth).toHaveBeenCalled()
      expect(mockTokenRefreshService.start).toHaveBeenCalled()
      expect(mockAddEventListener).toHaveBeenCalledWith(
        'error',
        expect.any(Function)
      )
      expect(mockAddEventListener).toHaveBeenCalledWith(
        'unhandledrejection',
        expect.any(Function)
      )
      expect(mockConsoleLog).toHaveBeenCalledWith('应用初始化完成')
    })

    it('应该在没有有效token时不启动刷新服务', async () => {
      vi.mocked(TokenManager.isTokenValid).mockReturnValue(false)

      await appInitializer.initialize()

      expect(mockAuthEventHandler.initialize).toHaveBeenCalled()
      expect(mockUserStore.initializeAuth).toHaveBeenCalled()
      expect(mockTokenRefreshService.start).not.toHaveBeenCalled()
      expect(mockConsoleLog).toHaveBeenCalledWith('应用初始化完成')
    })

    it('应该启用性能监控', async () => {
      vi.mocked(TokenManager.isTokenValid).mockReturnValue(true)

      await appInitializer.initialize({ enablePerformanceMonitoring: true })

      expect(mockConsoleLog).toHaveBeenCalledWith('性能监控已启用')
      expect(mockConsoleLog).toHaveBeenCalledWith('应用初始化完成')
    })

    it('应该处理初始化错误', async () => {
      const error = new Error('初始化失败')
      mockAuthEventHandler.initialize.mockImplementation(() => {
        throw error
      })

      await expect(appInitializer.initialize()).rejects.toThrow('初始化失败')
      expect(mockConsoleError).toHaveBeenCalledWith('应用初始化失败:', error)
    })
  })

  describe('destroy', () => {
    it('应该正确销毁应用', () => {
      appInitializer.destroy()

      expect(mockTokenRefreshService.stop).toHaveBeenCalled()
      expect(mockAuthEventHandler.destroy).toHaveBeenCalled()
      expect(mockRemoveEventListener).toHaveBeenCalledWith(
        'error',
        expect.any(Function)
      )
      expect(mockRemoveEventListener).toHaveBeenCalledWith(
        'unhandledrejection',
        expect.any(Function)
      )
      expect(mockConsoleLog).toHaveBeenCalledWith('应用已销毁')
    })

    it('应该处理销毁过程中的错误', () => {
      const error = new Error('销毁失败')
      mockTokenRefreshService.stop.mockImplementation(() => {
        throw error
      })

      appInitializer.destroy()

      expect(mockConsoleError).toHaveBeenCalledWith('应用销毁过程中出现错误:', error)
    })
  })

  describe('全局错误处理', () => {
    it('应该处理全局错误', async () => {
      await appInitializer.initialize()

      // 获取绑定的错误处理函数
      const errorHandler = mockAddEventListener.mock.calls.find(
        call => call[0] === 'error'
      )?.[1]

      expect(errorHandler).toBeDefined()

      const errorEvent = new ErrorEvent('error', {
        message: '测试错误',
        filename: 'test.js',
        lineno: 10,
        colno: 5,
        error: new Error('测试错误')
      })

      errorHandler(errorEvent)

      expect(mockConsoleError).toHaveBeenCalledWith('全局错误:', {
        message: '测试错误',
        filename: 'test.js',
        lineno: 10,
        colno: 5,
        error: expect.any(Error)
      })
    })

    it('应该处理未捕获的Promise拒绝', async () => {
      await appInitializer.initialize()

      // 获取绑定的Promise拒绝处理函数
      const rejectionHandler = mockAddEventListener.mock.calls.find(
        call => call[0] === 'unhandledrejection'
      )?.[1]

      expect(rejectionHandler).toBeDefined()

      const rejectionEvent = new PromiseRejectionEvent('unhandledrejection', {
        promise: Promise.reject('测试拒绝'),
        reason: '测试拒绝'
      })

      rejectionHandler(rejectionEvent)

      expect(mockConsoleError).toHaveBeenCalledWith('未捕获的Promise拒绝:', '测试拒绝')
    })
  })

  describe('性能监控', () => {
    it('应该记录性能指标', async () => {
      // Mock performance API
      const mockPerformance = {
        getEntriesByType: vi.fn().mockReturnValue([
          { name: 'navigation', duration: 1000 }
        ]),
        mark: vi.fn(),
        measure: vi.fn()
      }

      Object.defineProperty(window, 'performance', {
        value: mockPerformance,
        configurable: true
      })

      await appInitializer.initialize({ enablePerformanceMonitoring: true })

      expect(mockConsoleLog).toHaveBeenCalledWith('性能监控已启用')
    })
  })

  describe('重复初始化和销毁', () => {
    it('应该防止重复初始化', async () => {
      vi.mocked(TokenManager.isTokenValid).mockReturnValue(true)

      await appInitializer.initialize()
      await appInitializer.initialize()

      // 应该只初始化一次
      expect(mockAuthEventHandler.initialize).toHaveBeenCalledTimes(1)
      expect(mockUserStore.initializeAuth).toHaveBeenCalledTimes(1)
    })

    it('应该允许重新初始化已销毁的实例', async () => {
      vi.mocked(TokenManager.isTokenValid).mockReturnValue(true)

      await appInitializer.initialize()
      appInitializer.destroy()
      await appInitializer.initialize()

      expect(mockAuthEventHandler.initialize).toHaveBeenCalledTimes(2)
      expect(mockUserStore.initializeAuth).toHaveBeenCalledTimes(2)
    })
  })
})