/**
 * Token刷新服务
 * 处理token的自动刷新、重试机制和并发控制
 */

import { TokenManager, type RefreshTokenResponse } from '@/utils/tokenManager'
import { post } from '@/api/index'
import type { ApiResponse } from '@/api/index'

export interface TokenRefreshOptions {
  maxRetries?: number // 最大重试次数
  retryDelay?: number // 重试延迟（毫秒）
  enableAutoRefresh?: boolean // 是否启用自动刷新
}

export class TokenRefreshService {
  private static instance: TokenRefreshService
  private refreshPromise: Promise<boolean> | null = null
  private refreshTimer: NodeJS.Timeout | null = null
  private options: Required<TokenRefreshOptions>

  // 默认配置
  private static readonly DEFAULT_OPTIONS: Required<TokenRefreshOptions> = {
    maxRetries: 3,
    retryDelay: 1000,
    enableAutoRefresh: true
  }

  private constructor(options: TokenRefreshOptions = {}) {
    this.options = { ...TokenRefreshService.DEFAULT_OPTIONS, ...options }
    this.initializeAutoRefresh()
  }

  /**
   * 获取单例实例
   */
  static getInstance(options?: TokenRefreshOptions): TokenRefreshService {
    if (!TokenRefreshService.instance) {
      TokenRefreshService.instance = new TokenRefreshService(options)
    }
    return TokenRefreshService.instance
  }

  /**
   * 刷新token
   * @param force 是否强制刷新
   * @returns Promise<boolean> 刷新是否成功
   */
  async refreshToken(force: boolean = false): Promise<boolean> {
    // 如果已经有刷新请求在进行中，返回该Promise
    if (this.refreshPromise && !force) {
      return this.refreshPromise
    }

    // 检查是否需要刷新
    if (!force && !TokenManager.shouldRefreshToken()) {
      return true
    }

    const refreshToken = TokenManager.getRefreshToken()
    if (!refreshToken) {
      console.warn('没有可用的refresh token')
      return false
    }

    // 创建刷新Promise
    this.refreshPromise = this.performRefresh(refreshToken)
    
    try {
      const result = await this.refreshPromise
      return result
    } finally {
      this.refreshPromise = null
    }
  }

  /**
   * 执行token刷新
   */
  private async performRefresh(refreshToken: string): Promise<boolean> {
    let lastError: Error | null = null

    for (let attempt = 1; attempt <= this.options.maxRetries; attempt++) {
      try {
        console.log(`尝试刷新token (${attempt}/${this.options.maxRetries})`)

        const response = await this.callRefreshAPI(refreshToken)
        
        if (response.code === 200) {
          const { token, refreshToken: newRefreshToken, expiresIn } = response.data
          
          // 更新token信息
          TokenManager.updateToken(token, newRefreshToken, expiresIn)
          
          console.log('Token刷新成功')
          this.scheduleNextRefresh()
          return true
        } else {
          throw new Error(response.msg || 'Token刷新失败')
        }
      } catch (error: any) {
        lastError = error
        console.error(`Token刷新失败 (尝试 ${attempt}/${this.options.maxRetries}):`, error.message)

        // 如果是401错误，说明refresh token也过期了，不再重试
        if (error.response?.status === 401) {
          console.warn('Refresh token已过期，需要重新登录')
          this.handleRefreshFailure()
          return false
        }

        // 如果不是最后一次尝试，等待后重试
        if (attempt < this.options.maxRetries) {
          await this.delay(this.options.retryDelay * attempt)
        }
      }
    }

    // 所有重试都失败了
    console.error('Token刷新最终失败:', lastError?.message)
    this.handleRefreshFailure()
    return false
  }

  /**
   * 调用刷新token的API
   */
  private async callRefreshAPI(refreshToken: string): Promise<ApiResponse<RefreshTokenResponse>> {
    // 这里调用实际的刷新token API
    // 根据后端API设计调整请求参数和URL
    return post<RefreshTokenResponse>('/auth/refresh', {
      refresh_token: refreshToken
    })
  }

  /**
   * 处理刷新失败的情况
   */
  private handleRefreshFailure(): void {
    // 清除所有认证信息
    TokenManager.clearTokenInfo()
    
    // 停止自动刷新
    this.stopAutoRefresh()
    
    // 触发登出事件（可以通过事件总线或状态管理器通知其他组件）
    this.notifyTokenExpired()
  }

  /**
   * 通知token过期
   */
  private notifyTokenExpired(): void {
    // 可以通过自定义事件或状态管理器通知应用
    window.dispatchEvent(new CustomEvent('token-expired'))
    
    // 如果在浏览器环境中，跳转到登录页
    if (typeof window !== 'undefined' && window.location) {
      const currentPath = window.location.pathname
      if (currentPath !== '/login') {
        window.location.href = '/login'
      }
    }
  }

  /**
   * 初始化自动刷新
   */
  private initializeAutoRefresh(): void {
    if (!this.options.enableAutoRefresh) return

    // 检查当前token状态
    if (TokenManager.isTokenValid()) {
      this.scheduleNextRefresh()
    }
  }

  /**
   * 安排下次刷新
   */
  private scheduleNextRefresh(): void {
    if (!this.options.enableAutoRefresh) return

    // 清除之前的定时器
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer)
    }

    const remainingTime = TokenManager.getTokenRemainingTime()
    const refreshTime = Math.max(remainingTime - TokenManager['REFRESH_THRESHOLD'], 60000) // 至少1分钟后刷新

    console.log(`安排在 ${Math.floor(refreshTime / 1000)} 秒后刷新token`)

    this.refreshTimer = setTimeout(() => {
      this.refreshToken()
    }, refreshTime)
  }

  /**
   * 停止自动刷新
   */
  private stopAutoRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer)
      this.refreshTimer = null
    }
  }

  /**
   * 延迟函数
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  /**
   * 启动服务
   */
  start(): void {
    console.log('Token刷新服务已启动')
    this.initializeAutoRefresh()
  }

  /**
   * 停止服务
   */
  stop(): void {
    console.log('Token刷新服务已停止')
    this.stopAutoRefresh()
    this.refreshPromise = null
  }

  /**
   * 获取服务状态
   */
  getStatus(): {
    isRunning: boolean
    hasValidToken: boolean
    shouldRefresh: boolean
    nextRefreshTime?: Date
  } {
    const tokenInfo = TokenManager.getTokenInfo()
    const remainingTime = TokenManager.getTokenRemainingTime()
    
    return {
      isRunning: this.options.enableAutoRefresh && !!this.refreshTimer,
      hasValidToken: TokenManager.isTokenValid(),
      shouldRefresh: TokenManager.shouldRefreshToken(),
      nextRefreshTime: tokenInfo ? new Date(Date.now() + remainingTime - TokenManager['REFRESH_THRESHOLD']) : undefined
    }
  }
}

// 导出单例实例
export const tokenRefreshService = TokenRefreshService.getInstance()
export default tokenRefreshService