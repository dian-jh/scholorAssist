<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="header-content">
        <!-- 左侧：Logo和系统名称 -->
        <div class="header-left">
          <el-button
            v-if="isMobile"
            :icon="Menu"
            circle
            @click="toggleSidebar"
            class="sidebar-toggle"
          />
          
          <div class="logo-section">
            <div class="logo-icon">
              <el-icon><Reading /></el-icon>
            </div>
            <div class="logo-text">
              <h1>文献辅助阅读系统</h1>
              <p class="subtitle">Literature Assisted Reading System</p>
            </div>
          </div>
        </div>
        
        <!-- 中间：全局搜索 -->
        <div class="header-center">
          <el-input
            v-model="searchQuery"
            placeholder="搜索文档、笔记、对话..."
            :prefix-icon="Search"
            class="global-search"
            @keyup.enter="handleGlobalSearch"
            clearable
          />
        </div>
        
        <!-- 右侧：通知和用户菜单 -->
        <div class="header-right">
          <!-- 通知 -->
          <el-badge :value="notificationCount" :hidden="notificationCount === 0">
            <el-button :icon="Bell" circle @click="showNotifications" />
          </el-badge>
          
          <!-- 帮助 -->
          <el-button :icon="QuestionFilled" circle @click="showHelp" />
          
          <!-- 主题切换 -->
          <el-button :icon="isDark ? Sunny : Moon" circle @click="toggleTheme" />
          
          <!-- 用户菜单 -->
          <el-dropdown @command="handleUserCommand">
            <div class="user-menu">
              <el-avatar :size="32" :src="userInfo?.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ userInfo?.real_name || userInfo?.username || '用户' }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人设置
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Key /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主体容器 -->
    <el-container class="main-container" :class="{ 'reader-route': shouldHideSidebar }">
      <!-- 侧边栏 -->
      <el-aside 
        v-if="!shouldHideSidebar"
        :width="sidebarWidth" 
        class="layout-sidebar"
        :class="{ 'sidebar-collapsed': sidebarCollapsed }"
      >
        <div class="sidebar-content">
          <!-- 快速操作 -->
          <div class="quick-actions">
            <el-button 
              type="primary" 
              class="action-btn"
              @click="navigateToReading"
            >
              <el-icon><Plus /></el-icon>
              <span v-if="!sidebarCollapsed">开始阅读</span>
            </el-button>
            <el-button 
              type="success" 
              class="action-btn"
              @click="triggerUpload"
            >
              <el-icon><Upload /></el-icon>
              <span v-if="!sidebarCollapsed">上传文档</span>
            </el-button>
          </div>
          
          <!-- 导航菜单 -->
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            :collapse="sidebarCollapsed"
            router
          >
            <el-menu-item index="/dashboard">
              <el-icon><Odometer /></el-icon>
              <template #title>工作台</template>
            </el-menu-item>
            
            <el-menu-item index="/category">
              <el-icon><FolderOpened /></el-icon>
              <template #title>分类管理</template>
            </el-menu-item>
            
            <el-menu-item index="/notes">
              <el-icon><Document /></el-icon>
              <template #title>
                笔记管理
                <el-badge :value="12" class="menu-badge" />
              </template>
            </el-menu-item>
            
            <el-menu-item index="/ai-chat">
              <el-icon><ChatDotRound /></el-icon>
              <template #title>
                AI对话
                <el-badge :value="8" class="menu-badge" />
              </template>
            </el-menu-item>
          </el-menu>
          
          <!-- 最近文档 -->
          <div v-if="!sidebarCollapsed" class="recent-section">
            <div class="section-title">最近阅读</div>
            <div class="recent-documents">
              <div 
                v-for="doc in recentDocuments" 
                :key="doc.id"
                class="recent-item"
                @click="openDocument(doc.id)"
              >
                <el-icon class="doc-icon"><Document /></el-icon>
                <span class="doc-title">{{ doc.title }}</span>
              </div>
            </div>
          </div>
          
          <!-- 快捷标签 -->
          <div v-if="!sidebarCollapsed" class="tags-section">
            <div class="section-title">快捷标签</div>
            <div class="tag-list">
              <el-tag size="small" class="tag-item">重要</el-tag>
              <el-tag size="small" type="success" class="tag-item">待读</el-tag>
              <el-tag size="small" type="warning" class="tag-item">收藏</el-tag>
            </div>
          </div>
        </div>
      </el-aside>

      <!-- 主内容区域 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 通知面板 -->
    <NotificationPanel 
      v-model:visible="notificationVisible" 
      :notifications="notifications"
    />
    
    <!-- 帮助面板 -->
    <HelpPanel v-model:visible="helpVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Menu, Search, Bell, QuestionFilled, User, ArrowDown, 
  Plus, Upload, Odometer, FolderOpened, Document, 
  ChatDotRound, Key, SwitchButton, Reading, Moon, Sunny
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore, useUserStore, useDocumentStore } from '@/store'
import NotificationPanel from '@/components/Common/NotificationPanel.vue'
import HelpPanel from '@/components/Common/HelpPanel.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const documentStore = useDocumentStore()

// 响应式数据
const searchQuery = ref('')
const notificationVisible = ref(false)
const helpVisible = ref(false)
const notificationCount = ref(3)
const notifications = ref([
  {
    id: 1,
    type: 'info' as const,
    title: '系统更新',
    message: '新版本已发布，包含多项功能改进',
    time: '2小时前',
    read: false
  }
])

// 计算属性
const sidebarCollapsed = computed(() => appStore.getSidebarCollapsed)
const sidebarWidth = computed(() => sidebarCollapsed.value ? '64px' : '256px')
const userInfo = computed(() => userStore.getUserInfo)
const activeMenu = computed(() => route.path)
const isDark = computed(() => appStore.getTheme === 'dark')
const isMobile = computed(() => window.innerWidth < 768)
const recentDocuments = computed(() => documentStore.getRecentDocuments(5))
// 阅读页面隐藏左侧主页（侧边栏）
const shouldHideSidebar = computed(() => {
  const p = route.path
  return p.startsWith('/reader') || p.startsWith('/chatgpt-reader')
})

// 方法
const toggleSidebar = () => {
  appStore.toggleSidebar()
}

const toggleTheme = () => {
  appStore.toggleTheme()
}

const handleGlobalSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({
      path: '/dashboard',
      query: { search: searchQuery.value }
    })
  }
}

const showNotifications = () => {
  notificationVisible.value = true
}

const showHelp = () => {
  helpVisible.value = true
}

const navigateToReading = () => {
  router.push('/dashboard')
}

const triggerUpload = () => {
  // 触发上传事件，由子组件处理
  window.dispatchEvent(new CustomEvent('trigger-upload'))
}

const openDocument = (docId: string) => {
  router.push(`/reader/${docId}`)
}

const handleUserCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      ElMessage.info('修改密码功能开发中')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        userStore.logout()
        router.push('/login')
      } catch {
        // 用户取消
      }
      break
  }
}

// 生命周期
onMounted(() => {
  appStore.initializeApp()
  userStore.initializeAuth()
  documentStore.fetchDocuments()
})
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  background: white;
  border-bottom: 1px solid var(--el-border-color-light);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0;
  height: 64px !important;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-content {
  height: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sidebar-toggle {
  display: none;
  
  @media (max-width: 768px) {
    display: flex;
  }
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: $primary-color;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.logo-text {
  h1 {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
    color: var(--el-text-color-primary);
  }
  
  .subtitle {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin: 0;
    
    @media (max-width: 640px) {
      display: none;
    }
  }
}

.header-center {
  flex: 1;
  max-width: 400px;
  margin: 0 32px;
  
  @media (max-width: 768px) {
    display: none;
  }
}

.global-search {
  width: 100%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.2s;
  
  &:hover {
    background-color: var(--el-fill-color-light);
  }
}

.username {
  font-size: 14px;
  font-weight: 500;
  
  @media (max-width: 640px) {
    display: none;
  }
}

.dropdown-icon {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.main-container {
  margin-top: 64px;
  height: calc(100vh - 64px);
}

.layout-sidebar {
  background: white;
  border-right: 1px solid var(--el-border-color-light);
  transition: width $transition-normal;
  overflow: hidden;
  
  @media (max-width: 768px) {
    position: fixed;
    top: 64px;
    left: 0;
    height: calc(100vh - 64px);
    z-index: 999;
    transform: translateX(-100%);
    
    &:not(.sidebar-collapsed) {
      transform: translateX(0);
    }
  }
}

.sidebar-content {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.quick-actions {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  width: 100%;
  justify-content: flex-start;
  
  .sidebar-collapsed & {
    justify-content: center;
    
    span {
      display: none;
    }
  }
}

.sidebar-menu {
  border: none;
  flex: 1;
}

.menu-badge {
  margin-left: auto;
}

.recent-section,
.tags-section {
  margin-top: 24px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.recent-documents {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
  
  &:hover {
    background-color: var(--el-fill-color-light);
  }
}

.doc-icon {
  color: var(--el-color-danger);
  font-size: 14px;
}

.doc-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  cursor: pointer;
  
  &:hover {
    opacity: 0.8;
  }
}

.layout-main {
  padding: 24px;
  background-color: var(--el-bg-color-page);
  overflow-y: auto;
  
  @media (max-width: 768px) {
    padding: 16px;
  }
}
</style>