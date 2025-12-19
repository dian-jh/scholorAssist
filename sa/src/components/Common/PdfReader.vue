<template>
  <div class="pdf-reader" v-if="document">
    <!-- 阅读器头部 -->
    <div class="reader-header">
      <div class="document-info">
        <el-icon size="20" :color="getDocumentIconColor(document)">
          <component :is="getDocumentIcon(document)" />
        </el-icon>
        <div class="document-details">
          <h3 class="document-title">{{ document.title }}</h3>
          <span class="document-meta">
            {{ document.file_size }} · {{ document.pages }} 页 · 
            阅读进度: {{ Math.round(document.read_progress * 100) }}%
          </span>
        </div>
      </div>
      
      <div class="reader-controls">
        <el-button-group>
          <el-button size="small" @click="zoomOut" :disabled="scale <= 0.5">
            <el-icon><ZoomOut /></el-icon>
          </el-button>
          <el-button size="small" @click="resetZoom">
            {{ Math.round(scale * 100) }}%
          </el-button>
          <el-button size="small" @click="zoomIn" :disabled="scale >= 3">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
        </el-button-group>
        
        <el-button size="small" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
          全屏
        </el-button>
        
        <el-button size="small" @click="$emit('close')">
          <el-icon><Close /></el-icon>
          关闭
        </el-button>
      </div>
    </div>

    <!-- PDF 内容区域 -->
    <div class="pdf-content" ref="pdfContainer">
      <div class="pdf-loading" v-if="loading">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>正在加载PDF文档...</p>
      </div>
      
      <div class="pdf-error" v-else-if="error">
        <el-icon size="48" color="#f56c6c"><Warning /></el-icon>
        <p>{{ error }}</p>
        <el-button type="primary" @click="loadPdf">重新加载</el-button>
      </div>
      
      <div class="pdf-viewer" v-else>
        <!-- PDF页面将在这里渲染 -->
        <canvas 
          v-for="pageNum in totalPages" 
          :key="pageNum"
          :ref="el => setCanvasRef(el, pageNum)"
          class="pdf-page"
          :style="({ transform: `scale(${scale})` } as Record<string, string>)"
        ></canvas>
      </div>
    </div>

    <!-- 页面导航 -->
    <div class="page-navigation" v-if="totalPages > 0">
      <el-button-group>
        <el-button size="small" @click="goToPage(1)" :disabled="currentPage === 1">
          <el-icon><DArrowLeft /></el-icon>
        </el-button>
        <el-button size="small" @click="previousPage" :disabled="currentPage === 1">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
      </el-button-group>
      
      <span class="page-info">
        第 {{ currentPage }} 页，共 {{ totalPages }} 页
      </span>
      
      <el-button-group>
        <el-button size="small" @click="nextPage" :disabled="currentPage === totalPages">
          <el-icon><ArrowRight /></el-icon>
        </el-button>
        <el-button size="small" @click="goToPage(totalPages)" :disabled="currentPage === totalPages">
          <el-icon><DArrowRight /></el-icon>
        </el-button>
      </el-button-group>
    </div>
  </div>
  
  <div class="pdf-reader-empty" v-else>
    <el-empty description="请选择一个文档进行阅读" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  ZoomIn, 
  ZoomOut, 
  FullScreen, 
  Close, 
  Warning,
  Loading,
  DArrowLeft,
  DArrowRight,
  ArrowLeft,
  ArrowRight
} from '@element-plus/icons-vue'
import type { MockDocument } from '@/api/mockManager'
import { getFileIcon, getFileIconColor, formatFileSize } from '@/utils/fileIconUtils'

// Props
interface Props {
  document?: MockDocument
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'close': []
  'progress-update': [progress: number]
}>()

// 响应式数据
const loading = ref(false)
const error = ref('')
const scale = ref(1)
const currentPage = ref(1)
const totalPages = ref(0)
const pdfContainer = ref<HTMLElement>()
const canvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map())

// PDF.js 相关
let pdfDoc: any = null
let renderTasks: any[] = []

// 方法
const setCanvasRef = (el: Element | ComponentPublicInstance | null, pageNum: number) => {
  if (el && el instanceof HTMLCanvasElement) {
    canvasRefs.value.set(pageNum, el)
  }
}

const getDocumentIcon = (document: MockDocument) => {
  return getFileIcon(document.filename)
}

const getDocumentIconColor = (document: MockDocument) => {
  return getFileIconColor(document.filename)
}

const loadPdf = async () => {
  if (!props.document) return
  
  loading.value = true
  error.value = ''
  
  try {
    // 这里应该加载实际的PDF文件
    // 由于是演示，我们模拟加载过程
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 模拟PDF文档数据
    totalPages.value = props.document.pages
    currentPage.value = Math.max(1, Math.round(props.document.read_progress * props.document.pages))
    
    // 渲染PDF页面（这里是模拟）
    await renderAllPages()
    
  } catch (err) {
    console.error('PDF加载失败:', err)
    error.value = 'PDF文档加载失败，请检查文件是否存在或格式是否正确'
  } finally {
    loading.value = false
  }
}

const renderAllPages = async () => {
  // 模拟渲染所有页面
  await nextTick()
  
  for (let pageNum = 1; pageNum <= totalPages.value; pageNum++) {
    const canvas = canvasRefs.value.get(pageNum)
    if (canvas) {
      const ctx = canvas.getContext('2d')
      if (ctx) {
        // 设置画布大小
        canvas.width = 600
        canvas.height = 800
        
        // 绘制模拟的PDF页面
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        
        ctx.fillStyle = '#333333'
        ctx.font = '16px Arial'
        ctx.fillText(`第 ${pageNum} 页`, 50, 50)
        ctx.fillText(`文档: ${props.document?.title}`, 50, 80)
        
        // 绘制一些模拟内容
        ctx.font = '14px Arial'
        for (let i = 0; i < 20; i++) {
          ctx.fillText(`这是第${pageNum}页的第${i + 1}行内容...`, 50, 120 + i * 25)
        }
        
        // 绘制边框
        ctx.strokeStyle = '#cccccc'
        ctx.strokeRect(0, 0, canvas.width, canvas.height)
      }
    }
  }
}

const zoomIn = () => {
  if (scale.value < 3) {
    scale.value = Math.min(3, scale.value + 0.25)
  }
}

const zoomOut = () => {
  if (scale.value > 0.5) {
    scale.value = Math.max(0.5, scale.value - 0.25)
  }
}

const resetZoom = () => {
  scale.value = 1
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    updateReadProgress()
  }
}

const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    updateReadProgress()
  }
}

const goToPage = (pageNum: number) => {
  if (pageNum >= 1 && pageNum <= totalPages.value) {
    currentPage.value = pageNum
    updateReadProgress()
  }
}

const updateReadProgress = () => {
  if (props.document && totalPages.value > 0) {
    const progress = currentPage.value / totalPages.value
    emit('progress-update', progress)
  }
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    pdfContainer.value?.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 监听文档变化
watch(() => props.document, (newDoc) => {
  if (newDoc) {
    loadPdf()
  } else {
    // 清理状态
    totalPages.value = 0
    currentPage.value = 1
    error.value = ''
    canvasRefs.value.clear()
  }
}, { immediate: true })

// 键盘事件处理
const handleKeydown = (event: KeyboardEvent) => {
  switch (event.key) {
    case 'ArrowLeft':
      previousPage()
      break
    case 'ArrowRight':
      nextPage()
      break
    case 'Home':
      goToPage(1)
      break
    case 'End':
      goToPage(totalPages.value)
      break
    case 'Escape':
      if (document.fullscreenElement) {
        document.exitFullscreen()
      }
      break
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  
  // 清理渲染任务
  renderTasks.forEach(task => {
    if (task.cancel) {
      task.cancel()
    }
  })
})
</script>

<style scoped>
.pdf-reader {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.reader-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.document-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.document-details {
  flex: 1;
}

.document-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.document-meta {
  font-size: 12px;
  color: #909399;
}

.reader-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pdf-content {
  flex: 1;
  overflow: auto;
  position: relative;
  background: #f5f5f5;
}

.pdf-loading,
.pdf-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #909399;
}

.pdf-viewer {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.pdf-loading .loading-icon {
  font-size: 36px;
  animation: spin 1s linear infinite;
}

.pdf-page {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  background: #fff;
  transform-origin: center top;
  transition: transform 0.2s ease;
}

.page-navigation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.page-info {
  font-size: 14px;
  color: #606266;
  min-width: 120px;
  text-align: center;
}

.pdf-reader-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

/* 滚动条样式 */
.pdf-content::-webkit-scrollbar {
  width: 8px;
}

.pdf-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.pdf-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.pdf-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 全屏样式 */
.pdf-reader:fullscreen {
  background: #000;
}

.pdf-reader:fullscreen .pdf-content {
  background: #000;
}

.pdf-reader:fullscreen .pdf-page {
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .reader-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .document-info {
    justify-content: center;
  }
  
  .reader-controls {
    justify-content: center;
  }
  
  .page-navigation {
    flex-wrap: wrap;
    gap: 8px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>