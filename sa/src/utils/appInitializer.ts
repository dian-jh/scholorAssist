/**
 * 应用初始化器
 * 统一管理应用启动时的初始化逻辑
 */

import { useUserStore } from '@/store/modules/user'
import { TokenManager } from '@/utils/tokenManager'
import { tokenRefreshService } from '@/services/tokenRefreshService'
import { initAuthEventHandler, destroyAuthEventHandler } from '@/utils/authEventHandler'

export interface IAppInitializer {
  initialize(options?: { enablePerformanceMonitoring?: boolean }): Promise<void>
  destroy(): void
}

export class AppInitializer implements IAppInitializer {
  private isInitialized = false

  /**
   * 初始化应用
   */
  async initialize(options: { enablePerformanceMonitoring?: boolean } = {}): Promise<void> {
    if (this.isInitialized) {
      console.warn('应用已经初始化')
      return
    }

    console.log('开始初始化应用...')

    try {
      // 1. 初始化认证事件处理器
      initAuthEventHandler()

      // 2. 初始化用户状态
      const userStore = useUserStore()
      userStore.initializeAuth()

      // 3. 检查token状态并启动刷新服务
      if (TokenManager.isTokenValid()) {
        console.log('发现有效token，启动刷新服务')
        tokenRefreshService.start()
      } else {
        console.log('未发现有效token')
      }

      // 4. 设置全局错误处理
      this.setupGlobalErrorHandling()

      // 5. 设置性能监控（可选）
      if (options.enablePerformanceMonitoring) {
        this.setupPerformanceMonitoring()
      }

      this.isInitialized = true
      console.log('应用初始化完成')

    } catch (error) {
      console.error('应用初始化失败:', error)
      throw error
    }
  }

  /**
   * 销毁应用
   */
  destroy(): void {
    if (!this.isInitialized) {
      return
    }

    console.log('开始销毁应用...')

    try {
      // 停止token刷新服务
      tokenRefreshService.stop()

      // 销毁认证事件处理器
      destroyAuthEventHandler()

      this.isInitialized = false
      console.log('应用销毁完成')

    } catch (error) {
      console.error('应用销毁失败:', error)
    }
  }

  /**
   * 设置全局错误处理
   */
  private setupGlobalErrorHandling(): void {
    // 捕获未处理的Promise错误
    window.addEventListener('unhandledrejection', (event) => {
      console.error('未处理的Promise错误:', event.reason)
      
      // 检查是否是认证相关错误
      if (this.isAuthError(event.reason)) {
        console.warn('检测到认证错误，触发登出流程')
        const userStore = useUserStore()
        userStore.clearUserData()
      }
    })

    // 捕获全局JavaScript错误
    window.addEventListener('error', (event) => {
      console.error('全局JavaScript错误:', event.error)
    })
  }

  /**
   * 检查是否是认证相关错误
   */
  private isAuthError(error: any): boolean {
    if (!error) return false

    const errorMessage = error.message || error.toString()
    const authErrorKeywords = [
      'token',
      'unauthorized',
      '401',
      'authentication',
      'login'
    ]

    return authErrorKeywords.some(keyword => 
      errorMessage.toLowerCase().includes(keyword.toLowerCase())
    )
  }

  /**
   * 设置性能监控
   */
  private setupPerformanceMonitoring(): void {
    // 监控API请求性能
    if ('performance' in window && 'PerformanceObserver' in window) {
      try {
        const observer = new PerformanceObserver((list) => {
          list.getEntries().forEach((entry) => {
            if (entry.entryType === 'navigation') {
              const navEntry = entry as PerformanceNavigationTiming
              console.log('页面加载性能:', {
                loadTime: navEntry.loadEventEnd - navEntry.loadEventStart,
                domContentLoaded: navEntry.domContentLoadedEventEnd - navEntry.domContentLoadedEventStart,
                totalTime: navEntry.loadEventEnd - navEntry.fetchStart
              })
            }
          })
        })

        observer.observe({ entryTypes: ['navigation'] })
      } catch (error) {
        console.warn('性能监控初始化失败:', error)
      }
    }
  }

  /**
   * 获取初始化状态
   */
  getInitializationStatus(): {
    isInitialized: boolean
    tokenServiceStatus: any
    authEventHandlerStatus: boolean
  } {
    return {
      isInitialized: this.isInitialized,
      tokenServiceStatus: tokenRefreshService.getStatus(),
      authEventHandlerStatus: this.isInitialized // 简化状态检查
    }
  }
}

// 创建单例实例
export const appInitializer: IAppInitializer = new AppInitializer()

// 导出便捷方法
export const initializeApp = async (options?: { enablePerformanceMonitoring?: boolean }): Promise<void> => {
  await appInitializer.initialize(options)
}

export const destroyApp = (): void => {
  appInitializer.destroy()
}

export default appInitializer