import { defineStore } from 'pinia'
import { 
  userLogin, 
  userRegister, 
  userLogout, 
  getUserProfile,
  forgotPassword,
  changePassword,
  type UserInfo, 
  type LoginRequest, 
  type RegisterRequest,
  type ForgotPasswordRequest,
  type ChangePasswordRequest,
  UserInputValidator
} from '@/api/UserApi'
import { TokenManager } from '@/utils/tokenManager'
import { tokenRefreshService } from '@/services/tokenRefreshService'

interface UserState {
  userInfo: UserInfo | null
  isLoggedIn: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: null,
    isLoggedIn: false
  }),

  getters: {
    getUserInfo: (state) => state.userInfo,
    getIsLoggedIn: (state) => state.isLoggedIn,
    getToken: () => TokenManager.getToken(),
    getRefreshToken: () => TokenManager.getRefreshToken(),
    isTokenValid: () => TokenManager.isTokenValid(),
    shouldRefreshToken: () => TokenManager.shouldRefreshToken(),
    getTokenInfo: () => TokenManager.getTokenInfo()
  },

  actions: {
    /**
     * 设置用户信息
     */
    setUserInfo(userInfo: UserInfo) {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    /**
     * 设置登录状态
     */
    setLoginStatus(status: boolean) {
      this.isLoggedIn = status
      localStorage.setItem('isLoggedIn', String(status))
    },

    /**
     * 用户登录
     */
    async login(loginData: LoginRequest): Promise<{ success: boolean; message: string; data?: any }> {
      try {
        // 前端输入验证
        const validation = UserInputValidator.validateLoginInput(loginData)
        if (!validation.valid) {
          return { success: false, message: validation.message! }
        }

        // 调用后端API
        const response = await userLogin(loginData)
        
        if (response.code === 200) {
          const loginResponse = response.data
          const lr: any = loginResponse
          
          // 构建用户信息对象
          const userInfo: UserInfo = {
            user_id: loginResponse.user_id,
            username: loginResponse.username,
            email: loginResponse.email,
            real_name: loginResponse.real_name,
            role: loginResponse.role,
            status: loginResponse.status,
            created_at: new Date().toISOString(),
            last_login_at: loginResponse.last_login_at || '',
            email_verified: true // 默认值
          }
          
          // 解析并保存token信息，兼容多种后端字段命名
          const accessToken: string = lr.accessToken ?? lr.token
          const refreshToken: string | undefined = lr.refreshToken ?? lr.refresh_token
          const tokenType: string = lr.tokenType ?? 'Bearer'
          const expiresIn: number = (
            lr.expiresIn ?? lr.expires_in ?? (
              lr.expires_at ? Math.max(60, Math.floor((new Date(lr.expires_at).getTime() - Date.now()) / 1000)) : 86400
            )
          )

          TokenManager.setTokenInfo(
            accessToken,
            refreshToken,
            expiresIn,
            tokenType
          )
          
          // 保存用户信息和登录状态
          this.setUserInfo(userInfo)
          this.setLoginStatus(true)
          
          // 启动token刷新服务
          tokenRefreshService.start()
          
          console.log('登录成功，用户信息已保存:', userInfo)
          console.log('Token信息已保存:', { tokenType, expiresIn })
          
          return { 
            success: true, 
            message: response.msg,
            data: response.data
          }
        } else {
          return { 
            success: false, 
            message: response.msg 
          }
        }
      } catch (error: any) {
        console.error('登录失败:', error)
        return { 
          success: false, 
          message: error.message || '登录失败，请重试' 
        }
      }
    },

    /**
     * 用户注册
     */
    async register(registerData: RegisterRequest): Promise<{ success: boolean; message: string; data?: any }> {
      try {
        // 前端输入验证
        const validation = UserInputValidator.validateRegisterInput(registerData)
        if (!validation.valid) {
          return { success: false, message: validation.message! }
        }

        // 调用后端API
        const response = await userRegister(registerData)
        
        if (response.code === 200) {
          return { 
            success: true, 
            message: response.msg,
            data: response.data
          }
        } else {
          return { 
            success: false, 
            message: response.msg 
          }
        }
      } catch (error: any) {
        console.error('注册失败:', error)
        return { 
          success: false, 
          message: error.message || '注册失败，请重试' 
        }
      }
    },

    /**
     * 用户登出
     */
    async logout(): Promise<{ success: boolean; message: string }> {
      try {
        // 调用后端API
        await userLogout()
        
        // 清除本地数据
        this.clearUserData()
        
        return { success: true, message: '退出登录成功' }
      } catch (error: any) {
        console.error('登出失败:', error)
        // 即使后端调用失败，也要清除本地数据
        this.clearUserData()
        return { success: true, message: '退出登录成功' }
      }
    },

    /**
     * 忘记密码
     */
    async requestPasswordReset(email: string): Promise<{ success: boolean; message: string }> {
      try {
        // 验证邮箱格式
        const emailValidation = UserInputValidator.validateEmail(email)
        if (!emailValidation.valid) {
          return { success: false, message: emailValidation.message! }
        }

        const response = await forgotPassword({ email })
        
        if (response.code === 200) {
          return { success: true, message: response.msg }
        } else {
          return { success: false, message: response.msg }
        }
      } catch (error: any) {
        console.error('发送重置邮件失败:', error)
        return { 
          success: false, 
          message: error.message || '发送失败，请重试' 
        }
      }
    },

    /**
     * 修改密码
     */
    async updatePassword(passwordData: ChangePasswordRequest): Promise<{ success: boolean; message: string }> {
      try {
        const response = await changePassword(passwordData)
        
        if (response.code === 200) {
          return { success: true, message: response.msg }
        } else {
          return { success: false, message: response.msg }
        }
      } catch (error: any) {
        console.error('修改密码失败:', error)
        return { 
          success: false, 
          message: error.message || '修改密码失败，请重试' 
        }
      }
    },

    /**
     * 获取用户信息
     */
    async fetchUserProfile(): Promise<{ success: boolean; message: string; data?: UserInfo }> {
      try {
        const response = await getUserProfile()
        
        if (response.code === 200) {
          this.setUserInfo(response.data)
          return { 
            success: true, 
            message: response.msg,
            data: response.data
          }
        } else {
          return { success: false, message: response.msg }
        }
      } catch (error: any) {
        console.error('获取用户信息失败:', error)
        return { 
          success: false, 
          message: error.message || '获取用户信息失败' 
        }
      }
    },

    /**
     * 清除用户数据
     */
    clearUserData() {
      this.userInfo = null
      this.isLoggedIn = false
      
      // 使用TokenManager清除token信息
      TokenManager.clearTokenInfo()
      
      // 停止token刷新服务
      tokenRefreshService.stop()
      
      // 清除其他用户相关数据
      localStorage.removeItem('userInfo')
      localStorage.removeItem('isLoggedIn')
    },

    /**
     * 初始化认证状态
     */
    initializeAuth() {
      try {
        const savedLoginStatus = localStorage.getItem('isLoggedIn')
        const savedUserInfo = localStorage.getItem('userInfo')
        
        // 检查是否有有效的token
        const hasValidToken = TokenManager.isTokenValid()
        
        if (savedLoginStatus === 'true' && savedUserInfo && hasValidToken) {
          this.userInfo = JSON.parse(savedUserInfo)
          this.isLoggedIn = true
          
          // 启动token刷新服务
          tokenRefreshService.start()
          
          console.log('认证状态初始化成功')
        } else {
          // 如果认证信息不完整或token无效，清除所有数据
          this.clearUserData()
          console.log('认证信息无效，已清除')
        }
      } catch (error) {
        console.error('初始化认证状态失败:', error)
        this.clearUserData()
      }
    },

    /**
     * 检查认证状态
     */
    async checkAuthStatus(): Promise<boolean> {
      try {
        // 检查token是否有效
        const hasValidToken = TokenManager.isTokenValid()
        
        // 检查用户是否已登录
        const isUserLoggedIn = this.isLoggedIn
        
        console.log('检查认证状态:', {
          hasValidToken,
          isUserLoggedIn,
          userInfo: this.userInfo
        })
        
        // 如果token无效但用户状态显示已登录，清理状态
        if (!hasValidToken && isUserLoggedIn) {
          console.log('Token已失效，清理用户状态')
          this.clearUserData()
          return false
        }
        
        // 如果token有效但用户状态显示未登录，尝试恢复状态
        if (hasValidToken && !isUserLoggedIn) {
          console.log('Token有效但用户状态未登录，尝试恢复状态')
          await this.initializeAuth()
        }
        
        // 返回最终的认证状态
        const finalStatus = hasValidToken && this.isLoggedIn
        console.log('最终认证状态:', finalStatus)
        
        return finalStatus
      } catch (error) {
        console.error('检查认证状态时出错:', error)
        this.clearUserData()
        return false
      }
    }
  }
})