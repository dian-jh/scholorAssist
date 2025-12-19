import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'

describe('User Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const userStore = useUserStore()
    
    expect(userStore.userInfo).toBeNull()
    expect(userStore.token).toBeNull()
    expect(userStore.isLoggedIn).toBe(false)
  })

  it('should set user info correctly', () => {
    const userStore = useUserStore()
    const mockUserInfo = {
      id: '1',
      name: 'Test User',
      email: 'test@example.com'
    }
    
    userStore.setUserInfo(mockUserInfo)
    
    expect(userStore.userInfo).toEqual(mockUserInfo)
  })

  it('should set token correctly', () => {
    const userStore = useUserStore()
    const mockToken = 'mock-token-123'
    
    userStore.setToken(mockToken)
    
    expect(userStore.token).toBe(mockToken)
  })

  it('should set login status correctly', () => {
    const userStore = useUserStore()
    
    userStore.setLoginStatus(true)
    
    expect(userStore.isLoggedIn).toBe(true)
  })

  it('should handle login successfully', async () => {
    const userStore = useUserStore()
    
    const result = await userStore.login('test@example.com', 'password123')
    
    expect(result.success).toBe(true)
    expect(result.message).toBe('登录成功')
    expect(userStore.isLoggedIn).toBe(true)
    expect(userStore.userInfo).toBeTruthy()
    expect(userStore.token).toBeTruthy()
  })

  it('should handle logout correctly', () => {
    const userStore = useUserStore()
    
    // 先设置登录状态
    userStore.setUserInfo({
      id: '1',
      name: 'Test User',
      email: 'test@example.com'
    })
    userStore.setToken('mock-token')
    userStore.setLoginStatus(true)
    
    // 执行登出
    userStore.logout()
    
    expect(userStore.userInfo).toBeNull()
    expect(userStore.token).toBeNull()
    expect(userStore.isLoggedIn).toBe(false)
  })
})