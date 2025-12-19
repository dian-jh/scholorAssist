<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo和标题 -->
      <div class="login-header">
        <div class="logo">
          <el-icon size="48" color="#1763EA"><Reading /></el-icon>
        </div>
        <h1 class="title">文献辅助阅读系统</h1>
        <p class="subtitle">智能PDF阅读与AI问答平台</p>
      </div>

      <!-- 登录表单 -->
      <div v-show="currentForm === 'login'" class="form-container">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="email">
            <el-input
              v-model="loginForm.email"
              type="email"
              placeholder="请输入邮箱地址"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
            >
              <template #suffix>
                <el-icon 
                  class="password-toggle" 
                  @click="showPassword = !showPassword"
                >
                  <View v-if="showPassword" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="loginForm.remember">
              记住密码
            </el-checkbox>
            <el-button type="primary" text @click="currentForm = 'forgot'">
              忘记密码？
            </el-button>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              <el-icon><User /></el-icon>
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <el-button type="primary" text @click="currentForm = 'register'">
            立即注册
          </el-button>
        </div>
      </div>

      <!-- 注册表单 -->
      <div v-show="currentForm === 'register'" class="form-container">
        <div class="form-header">
          <el-button :icon="ArrowLeft" text @click="currentForm = 'login'">
            返回登录
          </el-button>
          <h2>创建新账号</h2>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
        >
          <el-form-item prop="name">
            <el-input
              v-model="registerForm.name"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="realName">
            <el-input
              v-model="registerForm.realName"
              placeholder="请输入真实姓名（可选）"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              type="email"
              placeholder="请输入邮箱地址"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（至少8位）"
              size="large"
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-btn"
              :loading="loading"
              @click="handleRegister"
            >
              <el-icon><UserFilled /></el-icon>
              注册账号
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 忘记密码表单 -->
      <div v-show="currentForm === 'forgot'" class="form-container">
        <div class="form-header">
          <el-button :icon="ArrowLeft" text @click="currentForm = 'login'">
            返回登录
          </el-button>
          <h2>重置密码</h2>
        </div>

        <el-form
          ref="forgotFormRef"
          :model="forgotForm"
          :rules="forgotRules"
          class="forgot-form"
        >
          <el-form-item prop="email">
            <el-input
              v-model="forgotForm.email"
              type="email"
              placeholder="请输入注册邮箱"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>

          <div class="forgot-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>我们将向您的邮箱发送重置密码链接</span>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="forgot-btn"
              :loading="loading"
              @click="handleForgotPassword"
            >
              <el-icon><Promotion /></el-icon>
              发送重置链接
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  Reading, Message, Lock, User, UserFilled, View, Hide,
  ArrowLeft, InfoFilled, Promotion
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store'

const router = useRouter()
const userStore = useUserStore()

// 响应式数据
const currentForm = ref<'login' | 'register' | 'forgot'>('login')
const loading = ref(false)
const showPassword = ref(false)

// 表单引用
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const forgotFormRef = ref<FormInstance>()

// 登录表单
const loginForm = reactive({
  email: '',
  password: '',
  remember: false
})

// 注册表单
const registerForm = reactive({
  name: '',
  realName: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 忘记密码表单
const forgotForm = reactive({
  email: ''
})

// 表单验证规则
const loginRules: FormRules = {
  email: [
    { required: true, message: '请输入用户名或邮箱地址', trigger: 'blur' },
    { min: 3, message: '用户名或邮箱长度不能少于3位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ]
}

const registerRules: FormRules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' },
    { 
      pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, 
      message: '用户名只能包含字母、数字、下划线和中文字符', 
      trigger: 'blur' 
    }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
    { max: 100, message: '邮箱地址长度不能超过100个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度在8到32个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const forgotRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    await loginFormRef.value.validate()
    loading.value = true
    
    const result = await userStore.login({
      login: loginForm.email,
      password: loginForm.password,
      remember_me: loginForm.remember
    })
    
    if (result.success) {
      ElMessage.success(result.message)
      
      // 保存记住密码设置
      if (loginForm.remember) {
        localStorage.setItem('rememberLogin', 'true')
        localStorage.setItem('savedEmail', loginForm.email)
      } else {
        localStorage.removeItem('rememberLogin')
        localStorage.removeItem('savedEmail')
      }
      
      // 等待状态同步完成后再进行路由跳转
      await nextTick()
      
      // 检查是否有重定向参数
      const redirectPath = router.currentRoute.value.query.redirect as string
      const targetPath = redirectPath || '/dashboard'
      
      console.log('登录成功，准备跳转到:', targetPath)
      
      // 使用replace而不是push，避免在历史记录中留下登录页
      await router.replace(targetPath)
    } else {
      ElMessage.error(result.message)
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    loading.value = true
    
    const result = await userStore.register({
      username: registerForm.name,
      email: registerForm.email,
      password: registerForm.password,
      confirm_password: registerForm.confirmPassword,
      real_name: registerForm.realName || undefined
    })
    
    if (result.success) {
      ElMessage.success(result.message)
      currentForm.value = 'login'
      
      // 自动填充邮箱
      loginForm.email = registerForm.email
      
      // 清空注册表单
      Object.assign(registerForm, {
        name: '',
        realName: '',
        email: '',
        password: '',
        confirmPassword: ''
      })
    } else {
      ElMessage.error(result.message)
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}

// 处理忘记密码
const handleForgotPassword = async () => {
  if (!forgotFormRef.value) return
  
  try {
    await forgotFormRef.value.validate()
    loading.value = true
    
    const result = await userStore.requestPasswordReset(forgotForm.email)
    
    if (result.success) {
      ElMessage.success(result.message)
      currentForm.value = 'login'
      
      // 清空忘记密码表单
      forgotForm.email = ''
    } else {
      ElMessage.error(result.message)
    }
  } catch (error: any) {
    console.error('发送重置链接失败:', error)
    ElMessage.error(error.message || '发送失败，请重试')
  } finally {
    loading.value = false
  }
}

// 页面加载时恢复记住的登录信息
onMounted(() => {
  const rememberLogin = localStorage.getItem('rememberLogin')
  const savedEmail = localStorage.getItem('savedEmail')
  
  if (rememberLogin === 'true' && savedEmail) {
    loginForm.email = savedEmail
    loginForm.remember = true
  }
})
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  padding: 48px;
  width: 100%;
  max-width: 400px;
  position: relative;
  z-index: 10;
  
  @media (max-width: 640px) {
    padding: 32px 24px;
    margin: 16px;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  margin-bottom: 16px;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.form-container {
  animation: fadeIn 0.3s ease-in-out;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  
  h2 {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0;
  }
}

.login-form,
.register-form,
.forgot-form {
  .el-form-item {
    margin-bottom: 20px;
  }
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.login-btn,
.register-btn,
.forgot-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.password-toggle {
  cursor: pointer;
  color: var(--el-text-color-placeholder);
  
  &:hover {
    color: var(--el-text-color-secondary);
  }
}

.forgot-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: var(--el-color-info-light-9);
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 13px;
  color: var(--el-color-info);
}

.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 200px;
  height: 200px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 150px;
  height: 150px;
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.circle-3 {
  width: 100px;
  height: 100px;
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}
</style>