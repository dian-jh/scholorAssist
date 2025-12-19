<template>
  <div class="pdf-viewer" ref="viewerContainer">
    <!-- PDF工具栏 -->
    <div class="pdf-toolbar" v-if="document">
      <div class="toolbar-left">
        <el-button-group size="small">
          <el-button @click="zoomOut" :disabled="scale <= 0.5">
            <el-icon><ZoomOut /></el-icon>
          </el-button>
          <el-button @click="resetZoom" class="zoom-display">
            {{ Math.round(scale * 100) }}%
          </el-button>
          <el-button @click="zoomIn" :disabled="scale >= 3">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
        </el-button-group>
        
        <el-divider direction="vertical" />
        
        <el-button-group size="small">
          <el-button @click="fitWidth">
            <el-icon><FullScreen /></el-icon>
            适合宽度
          </el-button>
          <el-button @click="fitPage">
            <el-icon><ScaleToOriginal /></el-icon>
            适合页面
          </el-button>
        </el-button-group>
      </div>
      
      <div class="toolbar-center">
        <div class="page-navigation">
          <el-button-group size="small">
            <el-button @click="goToFirstPage" :disabled="currentPage === 1">
              <el-icon><DArrowLeft /></el-icon>
            </el-button>
            <el-button @click="previousPage" :disabled="currentPage === 1">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
          </el-button-group>
          
          <div class="page-input">
            <el-input-number
              v-model="currentPage"
              :min="1"
              :max="totalPages"
              size="small"
              controls-position="right"
              @change="goToPage"
            />
            <span class="page-total">/ {{ totalPages }}</span>
          </div>
          
          <el-button-group size="small">
            <el-button @click="nextPage" :disabled="currentPage === totalPages">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button @click="goToLastPage" :disabled="currentPage === totalPages">
              <el-icon><DArrowRight /></el-icon>
            </el-button>
          </el-button-group>
        </div>
      </div>
      
      <div class="toolbar-right">
        <el-progress 
          :percentage="Math.round(readProgress * 100)"
          :width="80"
          type="circle"
          :stroke-width="4"
        />
      </div>
    </div>

    <!-- PDF内容区域 -->
    <div class="pdf-content" ref="pdfContent" @scroll="handleScroll">
      <!-- 加载状态 -->
      <div class="pdf-loading" v-if="loading">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>正在加载PDF文档...</p>
      </div>
      
      <!-- 错误状态 -->
      <div class="pdf-error" v-else-if="error">
        <el-icon size="48" color="#f56c6c"><Warning /></el-icon>
        <h3>加载失败</h3>
        <p>{{ error }}</p>
        <el-button type="primary" @click="loadDocument">重新加载</el-button>
      </div>
      
      <!-- PDF页面 -->
      <div class="pdf-pages" v-else-if="document">
        <div 
          v-for="pageNum in totalPages" 
          :key="pageNum"
          class="pdf-page-container"
          :class="{ 'current-page': pageNum === currentPage }"
        >
          <div class="page-number">第 {{ pageNum }} 页</div>
          <canvas 
            :ref="el => setCanvasRef(el, pageNum)"
            class="pdf-page"
            :style="{ transform: `scale(${scale})` }"
            @click="handlePageClick"
          ></canvas>
          
          <!-- 页面注释层 -->
          <div class="page-annotations" v-if="getPageAnnotations(pageNum).length > 0">
            <div 
              v-for="annotation in getPageAnnotations(pageNum)"
              :key="annotation.id"
              class="annotation"
              :style="annotation.style"
              @click="selectAnnotation(annotation)"
            >
              <el-tooltip :content="annotation.content" placement="top">
                <div class="annotation-marker" :class="annotation.type"></div>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div class="pdf-empty" v-else>
        <el-empty 
          :image-size="120"
          description="请选择一个PDF文档进行阅读"
        >
          <el-button type="primary">
            <el-icon><FolderOpened /></el-icon>
            选择文档
          </el-button>
        </el-empty>
      </div>
    </div>

    <!-- 快速跳转 -->
    <div class="quick-jump" v-if="showQuickJump">
      <el-input
        v-model="jumpToPage"
        placeholder="跳转到页码"
        size="small"
        @keyup.enter="handleQuickJump"
      >
        <template #append>
          <el-button @click="handleQuickJump">跳转</el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick, type ComponentPublicInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  ZoomOut, 
  ZoomIn, 
  FullScreen, 
  ScaleToOriginal,
  ArrowLeft, 
  ArrowRight, 
  DArrowLeft, 
  DArrowRight,
  Warning,
  Loading,
  FolderOpened
} from '@element-plus/icons-vue'

import type { MockDocument } from '@/api/mockManager'

// Props
interface Props {
  document?: MockDocument
  scale?: number
}

const props = withDefaults(defineProps<Props>(), {
  scale: 1
})

// Emits
const emit = defineEmits<{
  pageChange: [page: number]
  progressUpdate: [progress: number]
  scaleChange: [scale: number]
}>()

// 注释类型定义
interface Annotation {
  id: string
  page: number
  type: 'highlight' | 'note' | 'bookmark'
  content: string
  style: {
    left: string
    top: string
    width: string
    height: string
  }
}

// 响应式数据
const viewerContainer = ref<HTMLElement>()
const pdfContent = ref<HTMLElement>()
const loading = ref(false)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(props.scale)
const readProgress = ref(0)
const showQuickJump = ref(false)
const jumpToPage = ref('')
const canvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map())
const annotations = ref<Annotation[]>([])

// 计算属性
const pageHeight = computed(() => {
  return 842 * scale.value // A4页面高度
})

const pageWidth = computed(() => {
  return 595 * scale.value // A4页面宽度
})

// 方法
const setCanvasRef = (el: Element | ComponentPublicInstance | null, pageNum: number) => {
  if (el && el instanceof HTMLCanvasElement) {
    canvasRefs.value.set(pageNum, el)
  }
}

const loadDocument = async () => {
  if (!props.document) return
  
  loading.value = true
  error.value = ''
  
  try {
    // 模拟PDF加载
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    totalPages.value = props.document.pages || 100
    currentPage.value = 1
    
    // 渲染PDF页面
    await renderPages()
    
    loading.value = false
    ElMessage.success('PDF文档加载成功')
  } catch (err) {
    loading.value = false
    error.value = '加载PDF文档失败，请检查文件格式'
    ElMessage.error('PDF文档加载失败')
  }
}

const renderPages = async () => {
  // 模拟渲染PDF页面
  for (let i = 1; i <= Math.min(totalPages.value, 5); i++) {
    const canvas = canvasRefs.value.get(i)
    if (canvas) {
      const ctx = canvas.getContext('2d')
      if (ctx) {
        canvas.width = pageWidth.value
        canvas.height = pageHeight.value
        
        // 绘制页面背景
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        
        // 绘制页面边框
        ctx.strokeStyle = '#e5e5e5'
        ctx.lineWidth = 1
        ctx.strokeRect(0, 0, canvas.width, canvas.height)
        
        // 绘制示例内容
        ctx.fillStyle = '#333333'
        ctx.font = '16px Arial'
        ctx.fillText(`这是第 ${i} 页的内容`, 50, 50)
        ctx.fillText('这里是PDF文档的示例内容...', 50, 80)
        
        // 绘制更多示例文本
        for (let line = 0; line < 20; line++) {
          ctx.fillText(`第 ${line + 1} 行文本内容示例`, 50, 120 + line * 25)
        }
      }
    }
  }
}

const zoomIn = () => {
  if (scale.value < 3) {
    scale.value = Math.min(3, scale.value + 0.25)
    emit('scaleChange', scale.value)
    nextTick(() => renderPages())
  }
}

const zoomOut = () => {
  if (scale.value > 0.5) {
    scale.value = Math.max(0.5, scale.value - 0.25)
    emit('scaleChange', scale.value)
    nextTick(() => renderPages())
  }
}

const resetZoom = () => {
  scale.value = 1
  emit('scaleChange', scale.value)
  nextTick(() => renderPages())
}

const fitWidth = () => {
  if (viewerContainer.value) {
    const containerWidth = viewerContainer.value.clientWidth - 40
    scale.value = containerWidth / 595 // A4宽度
    emit('scaleChange', scale.value)
    nextTick(() => renderPages())
  }
}

const fitPage = () => {
  if (viewerContainer.value) {
    const containerWidth = viewerContainer.value.clientWidth - 40
    const containerHeight = viewerContainer.value.clientHeight - 100
    const scaleX = containerWidth / 595
    const scaleY = containerHeight / 842
    scale.value = Math.min(scaleX, scaleY)
    emit('scaleChange', scale.value)
    nextTick(() => renderPages())
  }
}

const goToPage = (page?: number) => {
  const targetPage = page || currentPage.value
  if (targetPage >= 1 && targetPage <= totalPages.value) {
    currentPage.value = targetPage
    emit('pageChange', currentPage.value)
    
    // 滚动到对应页面
    const pageElement = document.querySelector(`.pdf-page-container:nth-child(${targetPage})`)
    if (pageElement) {
      pageElement.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
    
    updateProgress()
  }
}

const previousPage = () => {
  if (currentPage.value > 1) {
    goToPage(currentPage.value - 1)
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    goToPage(currentPage.value + 1)
  }
}

const goToFirstPage = () => {
  goToPage(1)
}

const goToLastPage = () => {
  goToPage(totalPages.value)
}

const handleScroll = () => {
  if (!pdfContent.value) return
  
  const scrollTop = pdfContent.value.scrollTop
  const scrollHeight = pdfContent.value.scrollHeight
  const clientHeight = pdfContent.value.clientHeight
  
  // 更新阅读进度
  const progress = scrollTop / (scrollHeight - clientHeight)
  readProgress.value = Math.max(0, Math.min(1, progress))
  emit('progressUpdate', readProgress.value)
  
  // 更新当前页码（基于滚动位置）
  const pageElements = document.querySelectorAll('.pdf-page-container')
  let newCurrentPage = 1
  
  pageElements.forEach((element, index) => {
    const rect = element.getBoundingClientRect()
    const containerRect = pdfContent.value!.getBoundingClientRect()
    
    if (rect.top <= containerRect.top + 100) {
      newCurrentPage = index + 1
    }
  })
  
  if (newCurrentPage !== currentPage.value) {
    currentPage.value = newCurrentPage
    emit('pageChange', currentPage.value)
  }
}

const updateProgress = () => {
  const progress = (currentPage.value - 1) / Math.max(1, totalPages.value - 1)
  readProgress.value = progress
  emit('progressUpdate', progress)
}

const handlePageClick = (event: MouseEvent) => {
  // 处理页面点击，可以用于添加注释
  console.log('页面点击:', event)
}

const handleQuickJump = () => {
  const page = parseInt(jumpToPage.value)
  if (page && page >= 1 && page <= totalPages.value) {
    goToPage(page)
    showQuickJump.value = false
    jumpToPage.value = ''
  } else {
    ElMessage.warning('请输入有效的页码')
  }
}

const getPageAnnotations = (pageNum: number) => {
  return annotations.value.filter(annotation => annotation.page === pageNum)
}

const selectAnnotation = (annotation: Annotation) => {
  ElMessage.info(`选中注释: ${annotation.content}`)
}

// 键盘快捷键
const handleKeydown = (event: KeyboardEvent) => {
  if (event.ctrlKey || event.metaKey) {
    switch (event.key) {
      case '=':
      case '+':
        event.preventDefault()
        zoomIn()
        break
      case '-':
        event.preventDefault()
        zoomOut()
        break
      case '0':
        event.preventDefault()
        resetZoom()
        break
      case 'g':
        event.preventDefault()
        showQuickJump.value = !showQuickJump.value
        break
    }
  } else {
    switch (event.key) {
      case 'ArrowLeft':
        event.preventDefault()
        previousPage()
        break
      case 'ArrowRight':
        event.preventDefault()
        nextPage()
        break
      case 'Home':
        event.preventDefault()
        goToFirstPage()
        break
      case 'End':
        event.preventDefault()
        goToLastPage()
        break
    }
  }
}

// 监听器
watch(() => props.document, (newDoc) => {
  if (newDoc) {
    loadDocument()
  }
}, { immediate: true })

watch(() => props.scale, (newScale) => {
  scale.value = newScale
  nextTick(() => renderPages())
})

// 生命周期
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  
  // 添加示例注释
  annotations.value = [
    {
      id: '1',
      page: 1,
      type: 'highlight',
      content: '重要概念',
      style: {
        left: '100px',
        top: '150px',
        width: '200px',
        height: '20px'
      }
    }
  ]
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style lang="scss" scoped>
.pdf-viewer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f7f7f8;
}

.pdf-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #ffffff;
  border-bottom: 1px solid #e5e5e5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
  position: relative;
  
  .toolbar-left,
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .toolbar-center {
    flex: 1;
    display: flex;
    justify-content: center;
  }
  
  .zoom-display {
    min-width: 60px;
    font-weight: 500;
    background: #f9fafb;
    padding: 6px 12px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    transition: all 0.2s ease;

    &:hover {
      background: #f3f4f6;
    }
  }
  
  .page-navigation {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .page-input {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .page-total {
        color: #6b7280;
        font-size: 14px;
      }
    }
  }
}

.pdf-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f7f7f8;

  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: linear-gradient(135deg, #c1c1c1, #a1a1a1);
    border-radius: 4px;
    transition: background 0.3s ease;

    &:hover {
      background: linear-gradient(135deg, #a1a1a1, #888);
    }
  }
}

.pdf-loading,
.pdf-error,
.pdf-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
  animation: fadeIn 0.5s ease;
  
  h3 {
    margin: 16px 0 8px 0;
    color: #374151;
  }
  
  p {
    margin: 0 0 16px 0;
  }
}

.pdf-pages {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.pdf-page-container {
  position: relative;
  background: #ffffff;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  &:hover {
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
    transform: translateY(-2px);
  }
  
  &.current-page {
    box-shadow: 0 4px 20px rgba(16, 163, 127, 0.2);
    border: 2px solid #10a37f;
  }
  
  .page-number {
    position: absolute;
    top: -30px;
    left: 0;
    font-size: 12px;
    color: #6b7280;
    background: #ffffff;
    padding: 4px 8px;
    border-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
}

.pdf-page {
  display: block;
  max-width: 100%;
  cursor: crosshair;
  transition: transform 0.2s;
}

.page-annotations {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.annotation {
  position: absolute;
  pointer-events: auto;
  cursor: pointer;
  
  .annotation-marker {
    width: 100%;
    height: 100%;
    border-radius: 4px;
    opacity: 0.7;
    transition: all 0.2s ease;
    animation: highlightFadeIn 0.5s ease;
    
    &.highlight {
      background: #fef3c7;
      border: 1px solid #f59e0b;
    }
    
    &.note {
      background: #dbeafe;
      border: 1px solid #3b82f6;
    }
    
    &.bookmark {
      background: #fecaca;
      border: 1px solid #ef4444;
    }
    
    &:hover {
      opacity: 1;
      transform: scale(1.02);
    }
  }
}

.quick-jump {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1000;
  background: #ffffff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

// 动画关键帧
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes highlightFadeIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

// 响应式设计
@media (max-width: 480px) {
  .pdf-toolbar {
    flex-direction: column;
    padding: 8px;
    gap: 8px;
    
    .toolbar-left,
    .toolbar-center,
    .toolbar-right {
      width: 100%;
      justify-content: center;
    }
    
    .zoom-display {
      min-width: 50px;
      font-size: 12px;
      padding: 4px 8px;
    }
    
    .page-navigation {
      gap: 8px;
      
      .page-input {
        font-size: 12px;
        
        .page-total {
          font-size: 12px;
        }
      }
    }
  }
  
  .pdf-content {
    padding: 8px;
  }
  
  .pdf-pages {
    gap: 15px;
  }
  
  .pdf-page-container {
    &:hover {
      transform: none;
    }
  }
  
  .quick-jump {
    width: 90%;
    max-width: 300px;
    padding: 15px;
  }
}

@media (max-width: 768px) {
  .pdf-toolbar {
    flex-wrap: wrap;
    padding: 8px 12px;
    gap: 8px;
    
    .toolbar-left,
    .toolbar-center,
    .toolbar-right {
      flex: 1;
      min-width: 200px;
    }
    
    .zoom-display {
      min-width: 55px;
      font-size: 13px;
    }
    
    .page-navigation {
      gap: 10px;
      
      .page-input {
        font-size: 13px;
      }
    }
  }
  
  .pdf-content {
    padding: 12px;
  }
  
  .pdf-pages {
    gap: 16px;
  }
}

@media (max-width: 1024px) and (min-width: 769px) {
  .pdf-toolbar {
    padding: 10px 14px;
    
    .zoom-display {
      min-width: 58px;
      font-size: 13px;
    }
    
    .page-navigation {
      gap: 11px;
      
      .page-input {
        font-size: 13px;
      }
    }
  }
  
  .pdf-content {
    padding: 16px;
  }
  
  .pdf-pages {
    gap: 18px;
  }
}

// 横屏模式适配
@media (orientation: landscape) and (max-height: 600px) {
  .pdf-toolbar {
    padding: 6px 12px;
    
    .zoom-display {
      min-width: 50px;
      font-size: 12px;
      padding: 4px 8px;
    }
    
    .page-navigation {
      gap: 8px;
      
      .page-input {
        font-size: 12px;
      }
    }
  }
  
  .pdf-content {
    padding: 10px;
  }
}

// 高分辨率屏幕适配
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .pdf-page-container {
    .page-number {
      font-size: 11px;
    }
  }
  
  .annotation {
    .annotation-marker {
      border-width: 0.5px;
    }
  }
}

// 暗色模式支持
@media (prefers-color-scheme: dark) {
  .pdf-viewer {
    background: #1f2937;
  }
  
  .pdf-toolbar {
    background: #374151;
    border-bottom-color: #4b5563;
    color: white;
    
    .zoom-display {
      background: #4b5563;
      border-color: #6b7280;
      color: white;
      
      &:hover {
        background: #6b7280;
      }
    }
    
    .page-navigation {
      .page-input {
        .page-total {
          color: #9ca3af;
        }
      }
    }
  }
  
  .pdf-content {
    background: #1f2937;
    
    &::-webkit-scrollbar-track {
      background: #374151;
    }
    
    &::-webkit-scrollbar-thumb {
      background: linear-gradient(135deg, #6b7280, #4b5563);
      
      &:hover {
        background: linear-gradient(135deg, #9ca3af, #6b7280);
      }
    }
  }
  
  .pdf-loading,
  .pdf-error,
  .pdf-empty {
    color: #9ca3af;
    
    h3 {
      color: #f3f4f6;
    }
  }
  
  .pdf-page-container {
    background: #374151;
    
    .page-number {
      background: #4b5563;
      color: #e5e7eb;
    }
  }
  
  .quick-jump {
    background: #374151;
    color: white;
  }
  .pdf-loading .loading-icon {
    font-size: 36px;
    animation: spin 1s linear infinite;
  }
}

// Element Plus 样式覆盖
:deep(.el-progress-circle) {
  .el-progress__text {
    font-size: 12px !important;
  }
}

:deep(.el-input-number) {
  width: 80px;
  
  .el-input__inner {
    text-align: center;
  }
  
  @media (max-width: 768px) {
    width: 70px;
    
    .el-input__inner {
      font-size: 12px;
    }
  }
}

:deep(.el-button-group) {
  @media (max-width: 480px) {
    .el-button {
      padding: 6px 8px;
      font-size: 12px;
      
      .el-icon {
        font-size: 14px;
      }
    }
  }
}

:deep(.el-divider--vertical) {
  @media (max-width: 768px) {
    display: none;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>