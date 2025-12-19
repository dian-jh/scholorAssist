/**
 * 认证事件处理器
 * 统一处理token过期、登出等认证相关事件
 */

import { useUserStore } from '@/store/modules/user'
import router from '@/router'
import { ElMessage } from 'element-plus'

export interface IAuthEventHandler {
  initialize(): void
  destroy(): void
}

export class AuthEventHandler implements IAuthEventHandler {
  private isInitialized = false

  /**
   * 初始化事件监听器
   */
  initialize(): void {
    if (this.isInitialized) {
      console.warn('认证事件处理器已经初始化')
      return
    }

    // 监听token过期事件
    window.addEventListener('token-expired', this.handleTokenExpiration)
    
    // 监听存储变化（多标签页同步）
    window.addEventListener('storage', this.handleStorageChange)
    
    // 监听页面可见性变化（检查token状态）
    document.addEventListener('visibilitychange', this.handleVisibilityChange)

    this.isInitialized = true
    console.log('认证事件处理器已初始化')
  }

  /**
   * 销毁事件监听器
   */
  destroy(): void {
    if (!this.isInitialized) {
      return
    }

    window.removeEventListener('token-expired', this.handleTokenExpiration)
    window.removeEventListener('storage', this.handleStorageChange)
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)

    this.isInitialized = false
    console.log('认证事件处理器已销毁')
  }

  /**
   * 处理token过期事件
   */
  private handleTokenExpiration = (): void => {
    console.warn('收到token过期事件')
    
    const userStore = useUserStore()
    
    // 清除用户数据
    userStore.clearUserData()
    
    // 显示提示消息
    ElMessage.warning('登录已过期，请重新登录')
    
    // 跳转到登录页面
    this.redirectToLogin()
  }

  /**
   * 处理存储变化事件（多标签页同步）
   */
  private handleStorageChange = (event: StorageEvent): void => {
    // 监听关键认证信息的变化
    if (event.key === 'isLoggedIn' || event.key === 'token' || event.key === 'userInfo') {
      console.log(`存储变化: ${event.key}`, { oldValue: event.oldValue, newValue: event.newValue })
      
      const userStore = useUserStore()
      
      // 如果在其他标签页登出，同步当前标签页状态
      if (event.key === 'isLoggedIn' && event.newValue === null) {
        console.log('检测到其他标签页登出，同步状态')
        userStore.clearUserData()
        this.redirectToLogin()
      }
      
      // 如果在其他标签页登录，重新初始化认证状态
      if (event.key === 'isLoggedIn' && event.newValue === 'true' && !userStore.getIsLoggedIn) {
        console.log('检测到其他标签页登录，重新初始化')
        userStore.initializeAuth()
      }
    }
  }

  /**
   * 处理页面可见性变化
   */
  private handleVisibilityChange = (): void => {
    // 当页面重新变为可见时，检查认证状态
    if (!document.hidden) {
      const userStore = useUserStore()
      
      // 检查认证状态是否仍然有效
      if (userStore.getIsLoggedIn && !userStore.checkAuthStatus()) {
        console.log('页面重新可见时发现认证状态无效')
        ElMessage.warning('登录状态已失效，请重新登录')
        this.redirectToLogin()
      }
    }
  }

  /**
   * 跳转到登录页面
   */
  private redirectToLogin(): void {
    const currentPath = window.location.pathname
    
    // 避免在登录页面重复跳转
    if (currentPath === '/login') {
      return
    }
    
    // 保存当前路径，登录后可以跳转回来
    const redirectPath = currentPath !== '/' ? currentPath : undefined
    
    if (router && router.currentRoute) {
      router.push({
        path: '/login',
        query: redirectPath ? { redirect: redirectPath } : undefined
      }).catch(error => {
        console.error('路由跳转失败:', error)
        // 如果路由跳转失败，直接修改location
        window.location.href = redirectPath ? `/login?redirect=${encodeURIComponent(redirectPath)}` : '/login'
      })
    } else {
      // 如果router不可用，直接修改location
      window.location.href = redirectPath ? `/login?redirect=${encodeURIComponent(redirectPath)}` : '/login'
    }
  }
}

// 创建单例实例
export const authEventHandler: IAuthEventHandler = new AuthEventHandler()

// 导出便捷方法
export const initAuthEventHandler = (): void => {
  authEventHandler.initialize()
}

export const destroyAuthEventHandler = (): void => {
  authEventHandler.destroy()
}

export default authEventHandler