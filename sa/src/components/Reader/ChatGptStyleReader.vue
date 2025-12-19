<template>
  <div class="chatgpt-reader" :class="readerClasses" :style="readerStyleVars">
    <!-- 左侧栏 - 笔记列表 -->
    <div 
      class="sidebar sidebar-left" 
      :class="{ 
        'collapsed': !leftSidebarVisible,
        'mobile-visible': isMobile && leftSidebarVisible
      }"
      ref="leftSidebar"
    >
      <div class="sidebar-header">
        <h3 class="sidebar-title">
          <el-icon><Document /></el-icon>
          笔记列表
        </h3>
        <el-button 
          class="collapse-btn"
          size="small" 
          text 
          @click="toggleLeftSidebar"
        >
          <el-icon>
            <template v-if="isMobile">
              <ArrowLeft />
            </template>
            <template v-else>
              <ArrowLeft v-if="leftSidebarVisible" />
              <ArrowRight v-else />
            </template>
          </el-icon>
        </el-button>
      </div>
      
      <div class="sidebar-content" v-show="leftSidebarVisible || !isMobile">
        <NotesPanel 
          :document-id="currentDocumentId"
          @note-select="handleNoteSelect"
          @note-create="handleNoteCreate"
        />
      </div>
    </div>

    <!-- 主内容区 - PDF显示 -->
    <div class="main-content" :style="mainContentStyle">
      <div class="content-header">
        <div class="document-info" v-if="currentDocument">
          <el-icon class="doc-icon"><Reading /></el-icon>
          <span class="doc-title">{{ currentDocument.title }}</span>
          <el-tag size="small" type="info">{{ currentDocument.pages }} 页</el-tag>
        </div>
        
        <div class="content-controls">
          <el-button-group size="small">
            <el-button @click="toggleLeftSidebar" :type="leftSidebarVisible ? 'primary' : ''">
              <el-icon><Menu /></el-icon>
              笔记
            </el-button>
            <el-button @click="toggleRightSidebar" :type="rightSidebarVisible ? 'primary' : ''">
              <el-icon><ChatDotRound /></el-icon>
              AI助手
            </el-button>
          </el-button-group>
        </div>
      </div>
      
  <div class="pdf-container">
        <div class="pdf-wrapper" :style="pdfWrapperStyle">
          <PdfViewer 
            :document="currentDocument"
            :scale="pdfScale"
            @page-change="handlePageChange"
            @progress-update="handleProgressUpdate"
          />
        </div>
      </div>
    </div>

    <!-- 右侧栏 - AI对话 -->
    <div 
      class="sidebar sidebar-right" 
      :class="{ 
        'collapsed': !rightSidebarVisible,
        'mobile-visible': isMobile && rightSidebarVisible
      }"
      ref="rightSidebar"
    >
      <div class="sidebar-header">
        <h3 class="sidebar-title">
          <el-icon><ChatDotRound /></el-icon>
          AI助手
        </h3>
        <el-button 
          class="collapse-btn"
          size="small" 
          text 
          @click="toggleRightSidebar"
        >
          <el-icon>
            <template v-if="isMobile">
              <ArrowRight />
            </template>
            <template v-else>
              <ArrowRight v-if="rightSidebarVisible" />
              <ArrowLeft v-else />
            </template>
          </el-icon>
        </el-button>
      </div>
      
      <div class="sidebar-content" v-show="rightSidebarVisible || !isMobile">
        <AiChatPanel 
          :document="currentDocument"
          :current-page="currentPage"
          @message-send="handleAiMessage"
        />
      </div>
    </div>

    <!-- 移动端遮罩层 -->
    <div 
      class="mobile-overlay" 
      v-if="isMobile && (leftSidebarVisible || rightSidebarVisible)"
      @click="closeMobileSidebars"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Document, 
  Reading, 
  ChatDotRound, 
  Menu, 
  ArrowLeft, 
  ArrowRight 
} from '@element-plus/icons-vue'

import NotesPanel from './NotesPanel.vue'
import PdfViewer from './PdfViewer.vue'
import AiChatPanel from './AiChatPanel.vue'

import type { MockDocument } from '@/api/mockManager'
import { ReaderLayoutConfig, getMainWidth, getMainHeight, getSidebarWidths } from '@/views/readerLayoutConfig'

// Props
interface Props {
  documentId?: string
}

const props = withDefaults(defineProps<Props>(), {
  documentId: ''
})

// 响应式数据
const route = useRoute()
const leftSidebarVisible = ref(false)
const rightSidebarVisible = ref(false)
const currentDocument = ref<MockDocument>()
const currentDocumentId = ref(props.documentId || route.params.id as string)
const currentPage = ref(1)
const pdfScale = ref(1)
const isMobile = ref(false)
const isTablet = ref(false)

// 3:7 比例下的宽度计算（桌面端 >=1024px）并使用 clamp 保障最小/最大值
const readerClasses = computed(() => {
  const classes = {
    'mobile': isMobile.value,
    'left-sidebar-open': leftSidebarVisible.value,
    'right-sidebar-open': rightSidebarVisible.value,
    'both-sidebars-open': leftSidebarVisible.value && rightSidebarVisible.value,
  }
  return classes
})

// 样式变量（侧栏宽度与主内容尺寸）
const readerStyleVars = computed(() => {
  const widths = getSidebarWidths(isMobile.value, isTablet.value)
  return {
    '--left-sidebar-width': widths.left,
    '--right-sidebar-width': widths.right
  }
})

const mainContentStyle = computed(() => {
  return {
    display: 'flex',
    flexDirection: 'column'
  }
})

const pdfWrapperStyle = computed(() => {
  return {
    width: getMainWidth(isMobile.value, isTablet.value),
    height: getMainHeight(isMobile.value, isTablet.value),
    margin: '0 auto'
  }
})

// 方法
const toggleLeftSidebar = () => {
  leftSidebarVisible.value = !leftSidebarVisible.value
  
  // 移动端只能打开一个侧边栏
  if (isMobile.value && leftSidebarVisible.value) {
    rightSidebarVisible.value = false
  }
}

const toggleRightSidebar = () => {
  rightSidebarVisible.value = !rightSidebarVisible.value
  
  // 移动端只能打开一个侧边栏
  if (isMobile.value && rightSidebarVisible.value) {
    leftSidebarVisible.value = false
  }
}

const closeMobileSidebars = () => {
  if (isMobile.value) {
    leftSidebarVisible.value = false
    rightSidebarVisible.value = false
  }
}

const handleNoteSelect = (noteId: string) => {
  ElMessage.success(`选中笔记: ${noteId}`)
}

const handleNoteCreate = (content: string) => {
  ElMessage.success('创建笔记成功')
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const handleProgressUpdate = (progress: number) => {
  // 更新阅读进度
  console.log('阅读进度更新:', progress)
}

const handleAiMessage = (message: string) => {
  ElMessage.success(`AI消息: ${message}`)
}

// 响应式检测
const checkMobile = () => {
  const w = window.innerWidth
  isMobile.value = w < 1024
  isTablet.value = w >= 769 && w < 1024
  
  // 移动端默认关闭侧边栏
  if (isMobile.value) {
    leftSidebarVisible.value = false
    rightSidebarVisible.value = false
  } else {
    // 桌面端默认打开
    leftSidebarVisible.value = true
    rightSidebarVisible.value = true
  }
}

// 生命周期
onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  
  // 加载文档数据
  if (currentDocumentId.value) {
    // TODO: 从API加载文档数据
    currentDocument.value = {
      id: currentDocumentId.value,
      title: '示例PDF文档',
      filename: 'example.pdf',
      category_id: 'cat_1',
      author: '示例作者',
      upload_date: new Date().toISOString(),
      file_size: '5 MB',
      pages: 100,
      status: 'ready',
      thumbnail: '',
      abstract: '这是一个示例PDF文档',
      tags: ['示例'],
      read_progress: 0.3
    } as MockDocument
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style lang="scss" scoped>
/* 基础布局 */
.chatgpt-reader {
  display: grid;
  height: 100vh;
  overflow: hidden;
  background-color: var(--bg-secondary);
  /* 默认三栏布局：左侧栏、主内容、右侧栏 */
  grid-template-columns: var(--left-sidebar-width, 320px) 1fr var(--right-sidebar-width, 380px);
  transition: grid-template-columns 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 侧边栏状态变化 */
.chatgpt-reader.left-sidebar-open {
  --left-sidebar-width: 320px;
}
.chatgpt-reader:not(.left-sidebar-open) {
  --left-sidebar-width: 0px;
}
.chatgpt-reader.right-sidebar-open {
  --right-sidebar-width: 380px;
}
.chatgpt-reader:not(.right-sidebar-open) {
  --right-sidebar-width: 0px;
}

/* 侧边栏通用样式 */
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--gradient-sidebar);
  border-left: 1px solid var(--border-light);
  border-right: 1px solid var(--border-light);
  overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-left {
  width: var(--left-sidebar-width);
}

.sidebar-right {
  width: var(--right-sidebar-width);
}

.sidebar.collapsed {
  width: 0;
  border-left: none;
  border-right: none;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  background: linear-gradient(135deg, #fafafa, #f5f5f5);
  flex-shrink: 0;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.collapse-btn {
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.collapse-btn:hover {
  background: #e5e7eb;
  transform: scale(1.1);
}

.sidebar-content {
  flex-grow: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 主内容区 */
.main-content {
  display: flex;
  flex-direction: column;
  min-width: 600px; /* 最小宽度限制 */
  background-color: var(--bg-primary);
  border-left: 1px solid var(--border-light);
  border-right: 1px solid var(--border-light);
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--border-light);
  background: var(--gradient-bg);
  box-shadow: var(--shadow-md);
  flex-shrink: 0;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.doc-title {
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.pdf-container {
  flex-grow: 1;
  overflow: auto;
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.pdf-wrapper {
  max-width: 100%;
  max-height: 100%;
}

/* 移动端适配 */
@media (max-width: 1023px) {
  .chatgpt-reader {
    grid-template-columns: 1fr; /* 单栏布局 */
  }

  .sidebar {
    position: fixed;
    top: 0;
    height: 100vh;
    z-index: 1000;
    box-shadow: var(--shadow-xl);
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .sidebar-left {
    left: 0;
    width: 85vw;
    max-width: 320px;
  }

  .sidebar-right {
    right: 0;
    transform: translateX(100%);
    width: 90vw;
    max-width: 380px;
  }

  .sidebar.mobile-visible {
    transform: translateX(0);
  }

  .main-content {
    min-width: 100%;
    border-left: none;
    border-right: none;
  }

  .mobile-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 999;
    backdrop-filter: blur(2px);
  }
}

/* 全局风格变量 */
:root {
  --sidebar-width: 280px;
  --header-height: 60px;
  --primary-color: #10a37f;
  --primary-hover: #0d8f6b;
  --primary-light: rgba(16, 163, 127, 0.1);
  --primary-dark: #0a7c5a;
  --bg-primary: #ffffff;
  --bg-secondary: #f7f7f8;
  --bg-tertiary: #f1f3f4;
  --bg-dark: #343541;
  --bg-darker: #202123;
  --border-light: #e5e7eb;
  --border-medium: #d1d5db;
  --border-dark: #9ca3af;
  --text-primary: #374151;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;
  --text-inverse: #ffffff;
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  --gradient-primary: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
  --gradient-bg: linear-gradient(135deg, #fafafa, #f5f5f5);
  --gradient-sidebar: linear-gradient(180deg, #ffffff, #f8f9fa);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e5e5;
  background: linear-gradient(135deg, #fafafa, #f5f5f5);
  
  .sidebar-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #374151;
    transition: color 0.2s ease;
  }
  
  .collapse-btn {
    padding: 8px;
    border-radius: 8px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    
    &:hover {
      background: #e5e7eb;
      transform: scale(1.1);
    }
    
    &:active {
      transform: scale(0.95);
    }
  }
}

.sidebar-content {
  flex: 1;
  overflow: hidden;
}

// 主内容区样式
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  min-width: 0;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--border-light);
  background: var(--gradient-bg);
  box-shadow: var(--shadow-md);
  position: relative;
  z-index: 10;
  
  .document-info {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .doc-icon {
      color: var(--primary-color);
      transition: color 0.2s ease;
    }
    
    .doc-title {
      font-weight: 600;
      color: var(--text-primary);
      transition: color 0.2s ease;
      max-width: 300px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
  
  .content-controls {
    .el-button-group {
      .el-button {
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        border-radius: 8px;
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(16, 163, 127, 0.3);
        }
        
        &:active {
          transform: translateY(0);
        }
      }
    }
  }
}

.pdf-container {
  flex: 1;
  overflow: auto;
  background: var(--bg-tertiary);
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--border-light), transparent);
  }
}

// 动画关键帧
@keyframes slideInLeft {
  from {
    transform: translateX(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

// 移动端样式
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 99;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.3s ease;
}

// 移动端侧边栏样式
.mobile-sidebar {
  position: fixed;
  top: 0;
  bottom: 0;
  height: 100vh;
  width: 320px;
  background: var(--gradient-sidebar);
  border: 1px solid var(--border-light);
  z-index: 100;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-xl);
  
  &.sidebar-left {
    left: 0;
    transform: translateX(-100%);
    border-right: 1px solid var(--border-light);
    border-left: none;
    
    &.show {
      transform: translateX(0);
    }
  }
  
  &.sidebar-right {
    right: 0;
    transform: translateX(100%);
    border-left: 1px solid var(--border-light);
    border-right: none;
    
    &.show {
      transform: translateX(0);
    }
  }
  
  .sidebar-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-light);
    background: var(--gradient-bg);
    display: flex;
    align-items: center;
    justify-content: space-between;
    
    .sidebar-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
    }
    
    .close-btn {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      background: var(--bg-primary);
      border: 1px solid var(--border-light);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.2s ease;
      
      &:hover {
        background: var(--primary-color);
        border-color: var(--primary-color);
        color: var(--text-inverse);
      }
    }
  }
  
  .sidebar-content {
    flex: 1;
    overflow: hidden;
  }
}

// 移动端适配
@media (max-width: 768px) {
  .chatgpt-reader.mobile {
    .sidebar {
      width: 280px;
      box-shadow: 0 0 25px rgba(0, 0, 0, 0.2);
      
      &.sidebar-right {
        width: 320px;
      }
    }
    
    .main-content {
      margin-left: 0 !important;
      margin-right: 0 !important;
      
      .content-header {
        padding: 0 16px;
        height: 56px;
        
        .document-info {
          gap: 12px;
          
          .doc-title {
            font-size: 16px;
            max-width: 200px;
          }
          
          .doc-meta {
            display: none;
          }
        }
        
        .header-actions {
          gap: 8px;
          
          .toggle-btn {
            width: 36px;
            height: 36px;
            border-radius: 10px;
          }
        }
      }
    }
    
    .content-controls {
      .el-button-group {
        .el-button {
          &:hover {
            transform: scale(1.05);
          }
        }
      }
    }
  }
}

// 平板适配
@media (max-width: 1024px) and (min-width: 769px) {
  .chatgpt-reader {
    .left-sidebar,
    .right-sidebar {
      width: 260px;
    }
    
    .main-content {
      .content-header {
        padding: 0 20px;
        
        .document-info {
          .doc-title {
            max-width: 250px;
          }
        }
      }
    }
  }
  
  .sidebar {
    &.sidebar-left {
      width: 300px;
    }

    &.sidebar-right {
      width: 360px;
    }
  }
}

// 大屏幕优化
@media (min-width: 1440px) {
  .chatgpt-reader {
    .left-sidebar,
    .right-sidebar {
      width: 320px;
    }
    
    .main-content {
      .content-header {
        padding: 0 32px;
        
        .document-info {
          .doc-title {
            font-size: 20px;
            max-width: 400px;
          }
          
          .doc-meta {
            font-size: 14px;
          }
        }
      }
    }
  }
  
  .sidebar {
    &.sidebar-left {
      width: 360px;
    }

    &.sidebar-right {
      width: 420px;
    }
  }
}

// 横屏模式适配
@media (orientation: landscape) and (max-height: 600px) {
  .chatgpt-reader {
    .content-header {
      height: 48px;
      padding: 0 16px;
      
      .document-info {
        .doc-title {
          font-size: 14px;
        }
      }
    }
  }
}

// 高分辨率屏幕适配
@media (min-resolution: 2dppx) {
  .chatgpt-reader {
    .toggle-btn {
      border-width: 0.5px;
    }
    
    .content-header {
      border-bottom-width: 0.5px;
    }
  }
}

// 暗色模式支持
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #1f2937;
    --bg-secondary: #111827;
    --bg-tertiary: #374151;
    --text-primary: #f9fafb;
    --text-secondary: #d1d5db;
    --text-tertiary: #9ca3af;
    --text-inverse: #111827;
    --border-light: #374151;
    --border-medium: #4b5563;
    --border-dark: #6b7280;
    --gradient-bg: linear-gradient(135deg, #1f2937, #111827);
    --gradient-sidebar: linear-gradient(180deg, #1f2937, #111827);
    --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.3);
    --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.4);
    --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
    --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.6);
  }
}

// 动画性能优化
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

// 加载动画
@keyframes shimmer {
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
}

.loading-shimmer {
  background: linear-gradient(90deg, var(--bg-tertiary) 25%, var(--bg-secondary) 50%, var(--bg-tertiary) 75%);
  background-size: 200px 100%;
  animation: shimmer 1.5s infinite;
}

// ChatGPT风格的配色
:deep(.el-button-group .el-button) {
  border-color: #d1d5db;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  &.is-active,
  &:hover {
    background: linear-gradient(135deg, #10a37f, #0d8f6b);
    border-color: #10a37f;
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(16, 163, 127, 0.4);
  }
}

:deep(.el-tag) {
  background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
  border-color: #0ea5e9;
  color: #0369a1;
  transition: all 0.2s ease;
  
  &:hover {
    transform: scale(1.05);
  }
}
</style>