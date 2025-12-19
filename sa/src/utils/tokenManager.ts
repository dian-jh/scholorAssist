/**
 * Token管理工具类
 * 提供token的安全存储、获取、更新和清除功能
 * 支持token过期检查和自动刷新机制
 */

export interface TokenInfo {
  token: string
  tokenType?: string
  refreshToken?: string
  expiresAt: number // 过期时间戳
  issuedAt: number // 签发时间戳
  expiresIn?: number // 有效期（秒）
}

export interface RefreshTokenResponse {
  token: string
  refreshToken?: string
  expiresIn: number // 有效期（秒）
}

export class TokenManager {
  private static readonly TOKEN_KEY = 'auth_token'
  private static readonly REFRESH_TOKEN_KEY = 'refresh_token'
  private static readonly TOKEN_INFO_KEY = 'token_info'
  // 统一与应用状态键命名保持一致，便于多标签页同步
  private static readonly USER_INFO_KEY = 'userInfo'
  private static readonly LOGIN_STATUS_KEY = 'isLoggedIn'

  // Token刷新的提前时间（5分钟）
  private static readonly REFRESH_THRESHOLD = 5 * 60 * 1000

  /**
   * 存储token信息
   * @param token 访问令牌
   * @param refreshToken 刷新令牌
   * @param expiresIn 有效期（秒）
   * @param tokenType token类型（如Bearer）
   */
  static setTokenInfo(token: string, refreshToken?: string, expiresIn: number = 3600, tokenType: string = 'Bearer'): void {
    try {
      const now = Date.now()
      const tokenInfo: TokenInfo = {
        token,
        tokenType,
        refreshToken,
        expiresAt: now + (expiresIn * 1000),
        issuedAt: now,
        expiresIn
      }

      // 存储到localStorage
      localStorage.setItem(this.TOKEN_KEY, token)
      if (refreshToken) {
        localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken)
      }
      localStorage.setItem(this.TOKEN_INFO_KEY, JSON.stringify(tokenInfo))
      localStorage.setItem(this.LOGIN_STATUS_KEY, 'true')

      console.log('Token信息已保存', { expiresAt: new Date(tokenInfo.expiresAt) })
    } catch (error) {
      console.error('保存token信息失败:', error)
      throw new Error('Token存储失败')
    }
  }

  /**
   * 获取当前token
   * @returns token字符串或null
   */
  static getToken(): string | null {
    try {
      return localStorage.getItem(this.TOKEN_KEY)
    } catch (error) {
      console.error('获取token失败:', error)
      return null
    }
  }

  /**
   * 获取刷新token
   * @returns refreshToken字符串或null
   */
  static getRefreshToken(): string | null {
    try {
      return localStorage.getItem(this.REFRESH_TOKEN_KEY)
    } catch (error) {
      console.error('获取refreshToken失败:', error)
      return null
    }
  }

  /**
   * 获取完整的token信息
   * @returns TokenInfo对象或null
   */
  static getTokenInfo(): TokenInfo | null {
    try {
      const tokenInfoStr = localStorage.getItem(this.TOKEN_INFO_KEY)
      if (!tokenInfoStr) return null
      
      return JSON.parse(tokenInfoStr) as TokenInfo
    } catch (error) {
      console.error('获取token信息失败:', error)
      return null
    }
  }

  /**
   * 检查token是否存在且有效
   * @returns boolean
   */
  static isTokenValid(): boolean {
    const tokenInfo = this.getTokenInfo()
    if (!tokenInfo || !tokenInfo.token) {
      return false
    }

    // 检查是否过期
    return Date.now() < tokenInfo.expiresAt
  }

  /**
   * 检查token是否即将过期（需要刷新）
   * @returns boolean
   */
  static shouldRefreshToken(): boolean {
    const tokenInfo = this.getTokenInfo()
    if (!tokenInfo) return false

    // 如果没有refreshToken，则不能刷新
    if (!tokenInfo.refreshToken) return false

    // 如果距离过期时间小于阈值，则需要刷新
    return (tokenInfo.expiresAt - Date.now()) < this.REFRESH_THRESHOLD
  }

  /**
   * 获取token剩余有效时间（毫秒）
   * @returns number
   */
  static getTokenRemainingTime(): number {
    const tokenInfo = this.getTokenInfo()
    if (!tokenInfo) return 0

    const remaining = tokenInfo.expiresAt - Date.now()
    return Math.max(0, remaining)
  }

  /**
   * 清除所有认证信息
   */
  static clearTokenInfo(): void {
    try {
      localStorage.removeItem(this.TOKEN_KEY)
      localStorage.removeItem(this.REFRESH_TOKEN_KEY)
      localStorage.removeItem(this.TOKEN_INFO_KEY)
      localStorage.removeItem(this.USER_INFO_KEY)
      localStorage.removeItem(this.LOGIN_STATUS_KEY)
      
      console.log('认证信息已清除')
    } catch (error) {
      console.error('清除认证信息失败:', error)
    }
  }

  /**
   * 更新token（用于刷新token后）
   * @param newToken 新的访问令牌
   * @param newRefreshToken 新的刷新令牌（可选）
   * @param expiresIn 有效期（秒）
   */
  static updateToken(newToken: string, newRefreshToken?: string, expiresIn: number = 3600): void {
    this.setTokenInfo(newToken, newRefreshToken || this.getRefreshToken() || undefined, expiresIn)
  }

  /**
   * 获取Authorization头的值
   * @returns string | null
   */
  static getAuthorizationHeader(): string | null {
    const info = this.getTokenInfo()
    const token = info?.token || this.getToken()
    const type = info?.tokenType || 'Bearer'
    return token ? `${type} ${token}` : null
  }

  /**
   * 检查登录状态
   * @returns boolean
   */
  static isLoggedIn(): boolean {
    try {
      const loginStatus = localStorage.getItem(this.LOGIN_STATUS_KEY)
      return loginStatus === 'true' && this.isTokenValid()
    } catch (error) {
      console.error('检查登录状态失败:', error)
      return false
    }
  }

  /**
   * 设置用户信息
   * @param userInfo 用户信息对象
   */
  static setUserInfo(userInfo: any): void {
    try {
      localStorage.setItem(this.USER_INFO_KEY, JSON.stringify(userInfo))
    } catch (error) {
      console.error('保存用户信息失败:', error)
    }
  }

  /**
   * 获取用户信息
   * @returns 用户信息对象或null
   */
  static getUserInfo(): any | null {
    try {
      const userInfoStr = localStorage.getItem(this.USER_INFO_KEY)
      return userInfoStr ? JSON.parse(userInfoStr) : null
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  /**
   * 初始化认证状态（应用启动时调用）
   * @returns boolean 是否有有效的认证状态
   */
  static initializeAuth(): boolean {
    try {
      const isValid = this.isTokenValid()
      if (!isValid) {
        this.clearTokenInfo()
        return false
      }
      return true
    } catch (error) {
      console.error('初始化认证状态失败:', error)
      this.clearTokenInfo()
      return false
    }
  }

  /**
   * 获取token调试信息（仅用于开发环境）
   */
  static getDebugInfo(): any {
    if (process.env.NODE_ENV !== 'development') {
      return null
    }

    const tokenInfo = this.getTokenInfo()
    if (!tokenInfo) return null

    return {
      hasToken: !!tokenInfo.token,
      hasRefreshToken: !!tokenInfo.refreshToken,
      isValid: this.isTokenValid(),
      shouldRefresh: this.shouldRefreshToken(),
      expiresAt: new Date(tokenInfo.expiresAt).toLocaleString(),
      issuedAt: new Date(tokenInfo.issuedAt).toLocaleString(),
      remainingTime: Math.floor(this.getTokenRemainingTime() / 1000) + '秒'
    }
  }
}

export default TokenManager