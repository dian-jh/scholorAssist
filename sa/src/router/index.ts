import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { TokenManager } from '@/utils/tokenManager'
import { ElMessage } from 'element-plus'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/Layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: {
          title: '工作台',
          icon: 'Odometer'
        }
      },
      {
        path: '/category',
        name: 'Category',
        component: () => import('@/views/WorkspaceView.vue'),
        meta: { title: '文献分类' }
      },
      {
        path: '/notes',
        name: 'Notes',
        component: () => import('@/views/NotesView.vue'),
        meta: {
          title: '笔记管理',
          icon: 'Document'
        }
      },
      {
        path: '/ai-chat',
        name: 'AiChat',
        component: () => import('@/views/AiChatView.vue'),
        meta: {
          title: 'AI对话',
          icon: 'ChatDotRound'
        }
      },
      {
        path: '/reader/:id?',
        name: 'Reader',
        component: () => import('@/views/ReaderView.vue'),
        meta: {
          title: '文档阅读',
          icon: 'Reading'
        }
      },
      {
        path: '/chatgpt-reader/:id?',
        name: 'ChatGptReader',
        component: () => import('@/views/ChatGptReaderView.vue'),
        meta: {
          title: 'ChatGPT风格阅读器',
          icon: 'ChatDotRound'
        }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: {
          title: '个人设置',
          icon: 'User'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  console.log(`路由守卫: ${from.path} -> ${to.path}`)
  
  // 获取用户store
  const userStore = useUserStore()
  
  // 检查认证状态
  const isAuthenticated = await userStore.checkAuthStatus()
  const hasValidToken = TokenManager.isTokenValid()
  
  console.log('认证状态检查:', {
    isAuthenticated,
    hasValidToken,
    isLoggedIn: userStore.isLoggedIn,
    targetPath: to.path,
    requiresAuth: to.meta.requiresAuth
  })
  
  // 如果目标路由需要认证：仅在token无效或缺失时重定向
  if (to.meta.requiresAuth) {
    if (!hasValidToken) {
      console.log('无有效Token访问受保护路由，重定向到登录页')
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  
  // 如果已认证用户访问登录页
  // 已有有效token访问登录页，直接重定向到目标页
  if (to.path === '/login' && hasValidToken) {
    console.log('已认证用户访问登录页，重定向到目标页面')
    // 检查是否有重定向参数
    const redirectPath = to.query.redirect as string
    const targetPath = redirectPath || '/dashboard'
    next(targetPath)
    return
  }
  
  // 其他情况正常通过
  next()
})

// 路由后置守卫
router.afterEach((to) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - ScholarAssist`
  } else {
    document.title = 'ScholarAssist'
  }
})

export default router