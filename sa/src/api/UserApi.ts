import { get, post } from './index'
import type { ApiResponse } from './index'
import { ConfigManager } from '@/config'
import { getMockHandler } from './mockManager'

// 用户信息接口
export interface UserInfo {
  user_id: string
  username: string
  email: string
  real_name?: string
  avatar?: string
  role: 'user' | 'admin' | 'super_admin'
  status: 'active' | 'suspended' | 'pending_verification'
  created_at: string
  last_login_at?: string
  email_verified: boolean
}

// 登录请求参数
export interface LoginRequest {
  login: string // 用户名或邮箱
  password: string
  remember_me?: boolean
}

// 登录响应数据
export interface LoginResponse {
  user_id: string
  username: string
  email: string
  real_name?: string
  role: 'user' | 'admin' | 'super_admin'
  status: 'active' | 'suspended' | 'pending_verification'
  token: string
  refresh_token?: string
  expires_at: string
  expires_in?: number
  last_login_at: string
}

// 注册请求参数
export interface RegisterRequest {
  username: string
  email: string
  password: string
  confirm_password: string
  real_name?: string
}

// 注册响应数据
export interface RegisterResponse {
  user_id: string
  username: string
  email: string
  real_name?: string
  status: 'pending_verification' | 'active' | 'suspended'
  role: 'user' | 'admin' | 'super_admin'
  created_at: string
}

// 忘记密码请求参数
export interface ForgotPasswordRequest {
  email: string
}

// 修改密码请求参数
export interface ChangePasswordRequest {
  current_password: string
  new_password: string
  confirm_password: string
}

/**
 * 输入验证工具类
 */
export class UserInputValidator {
  /**
   * 验证邮箱格式
   */
  static validateEmail(email: string): { valid: boolean; message?: string } {
    if (!email) {
      return { valid: false, message: '邮箱地址不能为空' }
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      return { valid: false, message: '请输入有效的邮箱地址' }
    }
    
    if (email.length > 100) {
      return { valid: false, message: '邮箱地址长度不能超过100个字符' }
    }
    
    return { valid: true }
  }

  /**
   * 验证用户名格式
   */
  static validateUsername(username: string): { valid: boolean; message?: string } {
    if (!username) {
      return { valid: false, message: '用户名不能为空' }
    }
    
    if (username.length < 3 || username.length > 20) {
      return { valid: false, message: '用户名长度必须在3-20个字符之间' }
    }
    
    const usernameRegex = /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/
    if (!usernameRegex.test(username)) {
      return { valid: false, message: '用户名只能包含字母、数字、下划线和中文字符' }
    }
    
    return { valid: true }
  }

  /**
   * 验证密码强度
   */
  static validatePassword(password: string): { valid: boolean; message?: string } {
    if (!password) {
      return { valid: false, message: '密码不能为空' }
    }
    
    if (password.length < 8) {
      return { valid: false, message: '密码长度不能少于8位' }
    }
    
    if (password.length > 32) {
      return { valid: false, message: '密码长度不能超过32位' }
    }
    
    // 检查密码复杂度：至少包含大写字母、小写字母、数字中的两种
    const hasLowerCase = /[a-z]/.test(password)
    const hasUpperCase = /[A-Z]/.test(password)
    const hasNumbers = /\d/.test(password)
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password)
    
    const complexityCount = [hasLowerCase, hasUpperCase, hasNumbers, hasSpecialChar].filter(Boolean).length
    
    if (complexityCount < 2) {
      return { 
        valid: false, 
        message: '密码必须包含大写字母、小写字母、数字、特殊字符中的至少两种' 
      }
    }
    
    return { valid: true }
  }

  /**
   * 验证登录输入
   */
  static validateLoginInput(data: LoginRequest): { valid: boolean; message?: string } {
    if (!data.login) {
      return { valid: false, message: '请输入用户名或邮箱' }
    }
    
    if (!data.password) {
      return { valid: false, message: '请输入密码' }
    }
    
    // 判断是邮箱还是用户名
    if (data.login.includes('@')) {
      const emailValidation = this.validateEmail(data.login)
      if (!emailValidation.valid) {
        return emailValidation
      }
    } else {
      const usernameValidation = this.validateUsername(data.login)
      if (!usernameValidation.valid) {
        return usernameValidation
      }
    }
    
    return { valid: true }
  }

  /**
   * 验证注册输入
   */
  static validateRegisterInput(data: RegisterRequest): { valid: boolean; message?: string } {
    // 验证用户名
    const usernameValidation = this.validateUsername(data.username)
    if (!usernameValidation.valid) {
      return usernameValidation
    }
    
    // 验证邮箱
    const emailValidation = this.validateEmail(data.email)
    if (!emailValidation.valid) {
      return emailValidation
    }
    
    // 验证密码
    const passwordValidation = this.validatePassword(data.password)
    if (!passwordValidation.valid) {
      return passwordValidation
    }
    
    // 验证确认密码
    if (data.password !== data.confirm_password) {
      return { valid: false, message: '两次输入的密码不一致' }
    }
    
    // 验证真实姓名（可选）
    if (data.real_name && (data.real_name.length < 2 || data.real_name.length > 50)) {
      return { valid: false, message: '真实姓名长度必须在2-50个字符之间' }
    }
    
    return { valid: true }
  }
}

/**
 * 用户登录
 * 功能描述：用户通过用户名/邮箱和密码登录系统
 * 入参：{ login: string, password: string, remember_me?: boolean }
 * 返回参数：{ code, msg, data: LoginResponse }
 * URL：/api/users/login
 * 请求方式：POST
 */
export function userLogin(params: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  // 严格的输入验证
  const validation = UserInputValidator.validateLoginInput(params)
  if (!validation.valid) {
    return Promise.reject(new Error(validation.message))
  }
  
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/users/login')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return post<LoginResponse>('/users/login', {
    login: params.login.trim(),
    password: params.password,
    remember_me: params.remember_me || false
  })
}

/**
 * 用户注册
 * 功能描述：新用户注册账号，创建用户基本信息
 * 入参：{ username: string, email: string, password: string, confirm_password: string, real_name?: string }
 * 返回参数：{ code, msg, data: RegisterResponse }
 * URL：/api/users/register
 * 请求方式：POST
 */
export function userRegister(params: RegisterRequest): Promise<ApiResponse<RegisterResponse>> {
  // 严格的输入验证
  const validation = UserInputValidator.validateRegisterInput(params)
  if (!validation.valid) {
    return Promise.reject(new Error(validation.message))
  }
  
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/users/register')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return post<RegisterResponse>('/users/register', {
    username: params.username.trim(),
    email: params.email.trim().toLowerCase(),
    password: params.password,
    confirm_password: params.confirm_password,
    real_name: params.real_name?.trim()
  })
}

/**
 * 用户登出
 * 功能描述：用户退出登录，使当前Token失效
 * 入参：无
 * 返回参数：{ code, msg, data: null }
 * URL：/api/users/logout
 * 请求方式：POST
 */
export function userLogout(): Promise<ApiResponse<null>> {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/users/logout')
    if (mockHandler) {
      return mockHandler()
    }
  }
  
  return post<null>('/users/logout')
}

/**
 * 获取用户信息
 * 功能描述：获取当前登录用户的详细信息
 * 入参：无
 * 返回参数：{ code, msg, data: UserInfo }
 * URL：/api/users/profile
 * 请求方式：GET
 */
export function getUserProfile(): Promise<ApiResponse<UserInfo>> {
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('GET', '/api/users/profile')
    if (mockHandler) {
      return mockHandler()
    }
  }
  
  return get<UserInfo>('/users/profile')
}

/**
 * 忘记密码
 * 功能描述：发送密码重置邮件
 * 入参：{ email: string }
 * 返回参数：{ code, msg, data: { message: string } }
 * URL：/api/users/forgot-password
 * 请求方式：POST
 */
export function forgotPassword(params: ForgotPasswordRequest): Promise<ApiResponse<{ message: string }>> {
  // 验证邮箱格式
  const emailValidation = UserInputValidator.validateEmail(params.email)
  if (!emailValidation.valid) {
    return Promise.reject(new Error(emailValidation.message))
  }
  
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/users/forgot-password')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return post<{ message: string }>('/users/forgot-password', {
    email: params.email.trim().toLowerCase()
  })
}

/**
 * 修改密码
 * 功能描述：用户修改登录密码
 * 入参：{ current_password: string, new_password: string, confirm_password: string }
 * 返回参数：{ code, msg, data: { message: string, updated_at: string } }
 * URL：/api/users/change-password
 * 请求方式：POST
 */
export function changePassword(params: ChangePasswordRequest): Promise<ApiResponse<{ message: string; updated_at: string }>> {
  // 验证当前密码
  if (!params.current_password) {
    return Promise.reject(new Error('请输入当前密码'))
  }
  
  // 验证新密码
  const passwordValidation = UserInputValidator.validatePassword(params.new_password)
  if (!passwordValidation.valid) {
    return Promise.reject(new Error(passwordValidation.message))
  }
  
  // 验证确认密码
  if (params.new_password !== params.confirm_password) {
    return Promise.reject(new Error('两次输入的新密码不一致'))
  }
  
  // 检查新密码不能与当前密码相同
  if (params.current_password === params.new_password) {
    return Promise.reject(new Error('新密码不能与当前密码相同'))
  }
  
  // 根据配置系统决定是否使用Mock数据
  if (ConfigManager.useMock()) {
    const mockHandler = getMockHandler('POST', '/api/users/change-password')
    if (mockHandler) {
      return mockHandler(params)
    }
  }
  
  return post<{ message: string; updated_at: string }>('/users/change-password', params)
}