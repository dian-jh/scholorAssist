import axios, { type AxiosResponse, type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { TokenManager } from '@/utils/tokenManager'
import { tokenRefreshService } from '@/services/tokenRefreshService'
import { RequestHeaderValidator, CORS_CONFIG } from '@/utils/requestHeaderValidator'

// API响应接口
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
  requestId?: string
}

// 请求队列接口
interface PendingRequest {
  resolve: (value: any) => void
  reject: (reason: any) => void
  config: InternalAxiosRequestConfig
}

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    // 移除默认的Content-Type设置，让请求拦截器根据请求类型动态设置
    // 这样FormData请求可以让浏览器自动设置正确的multipart/form-data Content-Type
    'Accept': 'application/json, text/plain, */*'
  },
  // CORS配置
  withCredentials: CORS_CONFIG.credentials
})

// 请求队列，用于处理token刷新期间的请求
let isRefreshing = false
let pendingRequests: PendingRequest[] = []

/**
 * 添加请求到队列
 */
function addRequestToQueue(config: InternalAxiosRequestConfig): Promise<any> {
  return new Promise((resolve, reject) => {
    pendingRequests.push({ resolve, reject, config })
  })
}

/**
 * 处理队列中的请求
 */
function processQueue(error: any = null): void {
  pendingRequests.forEach(({ resolve, reject, config }) => {
    if (error) {
      reject(error)
    } else {
      resolve(api(config))
    }
  })
  pendingRequests = []
  isRefreshing = false
}

/**
 * 生成请求ID
 */
function generateRequestId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 请求拦截器
api.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    // 如果正在刷新token，将请求加入队列
    if (isRefreshing) {
      return addRequestToQueue(config)
    }

    // 检查是否需要刷新token
    if (TokenManager.shouldRefreshToken()) {
      isRefreshing = true
      
      try {
        const refreshSuccess = await tokenRefreshService.refreshToken()
        if (!refreshSuccess) {
          // 刷新失败，清除队列并跳转登录
          processQueue(new Error('Token刷新失败'))
          return Promise.reject(new Error('Token刷新失败'))
        }
        processQueue()
      } catch (error) {
        processQueue(error)
        return Promise.reject(error)
      }
    }

    // 获取当前token与认证头
    const token = TokenManager.getToken()
    const authHeader = TokenManager.getAuthorizationHeader()
    
    // 构建完整的请求URL用于路径验证
    const fullUrl = config.baseURL ? `${config.baseURL}${config.url}` : config.url || ''
    
    // 使用请求头验证器配置请求头
    const configuredHeaders = RequestHeaderValidator.configureHeaders(
      fullUrl,
      config.headers as Record<string, any>,
      token || undefined,
      config.data // 传递请求数据以检测FormData
    )
    
    // 记录请求头配置过程（开发环境）
    if (process.env.NODE_ENV === 'development') {
      RequestHeaderValidator.logHeaderConfiguration(
        fullUrl,
        config.headers as Record<string, any>,
        configuredHeaders
      )
    }
    
    // 应用配置好的请求头
    Object.keys(configuredHeaders).forEach(key => {
      config.headers[key] = configuredHeaders[key]
    })
    // 如可用，基于tokenType设置Authorization头
    if (authHeader && RequestHeaderValidator.requiresAuthorization(fullUrl)) {
      config.headers['Authorization'] = authHeader
    }

    return config
  },
  (error) => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 如果是二进制响应（arraybuffer / application/octet-stream / application/pdf），直接透传
    const contentType = (response.headers && (response.headers['content-type'] || (response.headers as any)['Content-Type'])) || ''
    const isBinary = ((response.request as any)?.responseType === 'arraybuffer')
      || contentType.includes('application/octet-stream')
      || contentType.includes('application/pdf')

    if (isBinary) {
      return response
    }

    const { code, msg } = response.data as ApiResponse

    // 记录响应日志
    console.log(`API响应 [${response.config.method?.toUpperCase()}] ${response.config.url}:`, {
      code,
      msg
    })

    // 处理业务状态码
    switch (code) {
      case 200:
        return response
      case 401:
        // 401错误统一处理
        handleUnauthorized()
        return Promise.reject(new Error(msg || '用户未登录'))
      case 403:
        ElMessage.error(msg || '权限不足')
        return Promise.reject(new Error(msg || '权限不足'))
      case 404:
        ElMessage.error(msg || '请求的资源不存在')
        return Promise.reject(new Error(msg || '请求的资源不存在'))
      case 500:
        ElMessage.error(msg || '服务器内部错误')
        return Promise.reject(new Error(msg || '服务器内部错误'))
      default:
        ElMessage.error(msg || '请求失败')
        return Promise.reject(new Error(msg || '请求失败'))
    }
  },
  async (error: AxiosError) => {
    console.error('API请求错误:', error)

    // 网络错误处理
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络设置')
      return Promise.reject(new Error('网络连接失败'))
    }

    const { status, data } = error.response as AxiosResponse<ApiResponse>

    // HTTP状态码处理
    switch (status) {
      case 401:
        // HTTP 401错误，可能是token过期
        if (data?.code === 401) {
          handleUnauthorized()
        } else {
          ElMessage.error('认证失败，请重新登录')
          redirectToLogin()
        }
        break
      case 403:
        ElMessage.error('权限不足，无法访问该资源')
        break
      case 404:
        ElMessage.error('请求的接口不存在')
        break
      case 429:
        ElMessage.error('请求过于频繁，请稍后再试')
        break
      case 500:
        ElMessage.error('服务器内部错误')
        break
      case 502:
      case 503:
      case 504:
        ElMessage.error('服务暂时不可用，请稍后再试')
        break
      default:
        ElMessage.error(data?.msg || `请求失败 (${status})`)
    }

    return Promise.reject(error)
  }
)

/**
 * 处理401未授权错误
 */
function handleUnauthorized(): void {
  console.warn('收到401错误，清除认证信息并跳转登录')
  
  // 清除认证信息
  TokenManager.clearTokenInfo()
  
  // 停止token刷新服务
  tokenRefreshService.stop()
  
  // 跳转到登录页面
  redirectToLogin()
}

/**
 * 跳转到登录页面
 */
function redirectToLogin(): void {
  const currentPath = window.location.pathname
  
  // 避免在登录页面重复跳转
  if (currentPath === '/login') {
    return
  }
  
  // 保存当前路径，登录后可以跳转回来
  const redirectPath = currentPath !== '/' ? currentPath : undefined
  
  if (router) {
    router.push({
      path: '/login',
      query: redirectPath ? { redirect: redirectPath } : undefined
    })
  } else {
    // 如果router不可用，直接修改location
    window.location.href = redirectPath ? `/login?redirect=${encodeURIComponent(redirectPath)}` : '/login'
  }
}

// 封装请求方法
export const get = <T = any>(url: string, params?: any): Promise<ApiResponse<T>> => {
  return api.get(url, { params }).then(response => response.data)
}

// 获取二进制数组（ArrayBuffer）专用方法
export const getArrayBuffer = (url: string, params?: any): Promise<ArrayBuffer> => {
  return api.get(url, {
    params,
    responseType: 'arraybuffer',
    headers: {
      // 显式声明接受二进制，避免某些下载管理器基于Accept误判
      'Accept': 'application/octet-stream'
    }
  }).then(response => {
    try {
      const ct = (response.headers && (response.headers['content-type'] || (response.headers as any)['Content-Type'])) || ''
      console.log('字节流响应头:', {
        'content-type': ct,
        'content-length': response.headers?.['content-length'],
        'content-disposition': response.headers?.['content-disposition']
      })
    } catch {}
    return response.data as unknown as ArrayBuffer
  })
}

export const post = <T = any>(url: string, data?: any): Promise<ApiResponse<T>> => {
  return api.post(url, data).then(response => response.data)
}

export const put = <T = any>(url: string, data?: any): Promise<ApiResponse<T>> => {
  return api.put(url, data).then(response => response.data)
}

export const del = <T = any>(url: string): Promise<ApiResponse<T>> => {
  return api.delete(url).then(response => response.data)
}

export default api