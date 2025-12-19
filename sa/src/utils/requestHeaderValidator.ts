/**
 * 请求头验证工具类
 * 用于管理API请求的请求头配置和验证逻辑
 */

// 允许的请求头列表（网关允许的请求头）
export const ALLOWED_HEADERS = [
  'Authorization',
  'Content-Type', 
  'Accept',
  'Origin',
  'X-Requested-With'
] as const

// 不需要Authorization头的路径列表
export const PUBLIC_PATHS = [
  '/api/users/register',
  '/api/users/login', 
  '/api/users/check-username',
  '/api/users/check-email',
  '/actuator',
  '/swagger-ui',
  '/v3/api-docs',
  '/favicon.ico',
  '/error'
] as const

/**
 * 请求头验证器类
 */
export class RequestHeaderValidator {
  /**
   * 检查路径是否需要Authorization头
   * @param url 请求URL
   * @returns 是否需要Authorization头
   */
  static requiresAuthorization(url: string): boolean {
    // 提取路径部分（去除查询参数和hash）
    const path = new URL(url, 'http://localhost').pathname
    
    // 检查是否为公开路径
    return !PUBLIC_PATHS.some(publicPath => {
      // 精确匹配或前缀匹配
      return path === publicPath || path.startsWith(publicPath + '/')
    })
  }

  /**
   * 验证请求头是否符合网关要求
   * @param headers 请求头对象
   * @returns 过滤后的合法请求头
   */
  static validateHeaders(headers: Record<string, any>): Record<string, any> {
    const validHeaders: Record<string, any> = {}
    
    // 只保留允许的请求头
    Object.keys(headers).forEach(key => {
      const normalizedKey = key.toLowerCase()
      const allowedKey = ALLOWED_HEADERS.find(allowed => 
        allowed.toLowerCase() === normalizedKey
      )
      
      if (allowedKey && headers[key] !== undefined && headers[key] !== null) {
        validHeaders[allowedKey] = headers[key]
      }
    })
    
    return validHeaders
  }

  /**
   * 为请求配置合适的请求头
   * @param url 请求URL
   * @param headers 原始请求头
   * @param token 认证token（可选）
   * @param data 请求数据（用于检测FormData）
   * @returns 配置好的请求头
   */
  static configureHeaders(
    url: string, 
    headers: Record<string, any> = {}, 
    token?: string,
    data?: any
  ): Record<string, any> {
    // 先验证并过滤请求头
    const validHeaders = this.validateHeaders(headers)
    
    // 检查是否为FormData请求
    const isFormData = data instanceof FormData
    
    // 只有在非FormData请求时才设置默认的Content-Type
    // FormData请求需要浏览器自动设置Content-Type（包含boundary）
    if (!validHeaders['Content-Type'] && !isFormData) {
      validHeaders['Content-Type'] = 'application/json'
    }
    
    // 设置默认的Accept（如果没有设置）
    if (!validHeaders['Accept']) {
      validHeaders['Accept'] = 'application/json, text/plain, */*'
    }
    
    // 根据路径决定是否添加Authorization头
    if (this.requiresAuthorization(url) && token) {
      validHeaders['Authorization'] = `Bearer ${token}`
    }
    
    return validHeaders
  }

  /**
   * 检查请求头是否包含不允许的自定义头
   * @param headers 请求头对象
   * @returns 不允许的请求头列表
   */
  static getDisallowedHeaders(headers: Record<string, any>): string[] {
    const disallowed: string[] = []
    
    Object.keys(headers).forEach(key => {
      const normalizedKey = key.toLowerCase()
      const isAllowed = ALLOWED_HEADERS.some(allowed => 
        allowed.toLowerCase() === normalizedKey
      )
      
      if (!isAllowed) {
        disallowed.push(key)
      }
    })
    
    return disallowed
  }

  /**
   * 记录请求头配置信息（用于调试）
   * @param url 请求URL
   * @param originalHeaders 原始请求头
   * @param finalHeaders 最终请求头
   */
  static logHeaderConfiguration(
    url: string,
    originalHeaders: Record<string, any>,
    finalHeaders: Record<string, any>
  ): void {
    if (process.env.NODE_ENV === 'development') {
      const disallowed = this.getDisallowedHeaders(originalHeaders)
      
      console.group(`🔧 请求头配置 [${url}]`)
      console.log('需要Authorization:', this.requiresAuthorization(url))
      console.log('原始请求头:', originalHeaders)
      console.log('最终请求头:', finalHeaders)
      
      if (disallowed.length > 0) {
        console.warn('已移除的不允许请求头:', disallowed)
      }
      
      console.groupEnd()
    }
  }
}

/**
 * CORS配置常量
 */
export const CORS_CONFIG = {
  credentials: true,
  maxAge: 3600
} as const

export default RequestHeaderValidator