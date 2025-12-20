<template>
  <div class="modern-pdf-reader" :class="{ 'fullscreen': isFullscreen, 'dark-mode': isDarkMode }">
    <!-- 顶部工具栏 -->
    <div class="reader-toolbar">
      <div class="toolbar-left">
        <div class="document-info">
          <el-icon size="20" color="#409eff">
            <Document />
          </el-icon>
          <div class="doc-details">
            <h3 class="doc-title">{{ document?.title || '未选择文档' }}</h3>
            <span class="doc-meta" v-if="document">
              {{ displayFileSize }} · {{ document?.pages || totalPages || '-' }} 页 · 
              进度: {{ displayProgress }}%
            </span>
          </div>
        </div>
      </div>

      <div class="toolbar-center">
        <!-- 页面导航 -->
        <div class="page-controls">
          <el-button-group size="small">
            <el-button @click="goToPage(1)" :disabled="currentPage === 1">
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
              :max="safeTotalPages"
              :disabled="!hasDocument || totalPages === 0"
              size="small"
              controls-position="right"
              @change="goToPage"
            />
            <span class="page-total">/ {{ totalPages || '-' }}</span>
          </div>
          
          <el-button-group size="small">
            <el-button @click="nextPage" :disabled="currentPage === totalPages">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button @click="goToPage(totalPages)" :disabled="currentPage === totalPages">
              <el-icon><DArrowRight /></el-icon>
            </el-button>
          </el-button-group>
        </div>
      </div>

      <div class="toolbar-right">
        <!-- 缩放控制 -->
        <div class="zoom-controls">
          <el-button-group size="small">
            <el-button @click="zoomOut" :disabled="scale <= 0.5">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <el-button @click="resetZoom">
              {{ Math.round(scale * 100) }}%
            </el-button>
            <el-button @click="zoomIn" :disabled="scale >= 3">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
          </el-button-group>
        </div>

        <!-- 工具按钮 -->
        <div class="tool-buttons">
          <el-tooltip content="搜索文档">
            <el-button size="small" @click="toggleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip content="笔记面板">
            <el-button size="small" @click="toggleNotesPanel" :type="showNotesPanel ? 'primary' : 'default'">
              <el-icon><EditPen /></el-icon>
            </el-button>
          </el-tooltip>

          <el-tooltip content="AI助手">
            <el-button size="small" @click="toggleAiPanel" :type="showAiPanel ? 'primary' : 'default'">
              <el-icon><ChatDotRound /></el-icon>
            </el-button>
          </el-tooltip>

          <!-- 旋转控制 -->
          <el-tooltip content="旋转">
            <el-button-group size="small">
              <el-button @click="rotateLeft">
                <el-icon><RefreshLeft /></el-icon>
              </el-button>
              <el-button @click="rotateRight">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
            </el-button-group>
          </el-tooltip>

          <el-tooltip content="全屏阅读">
            <el-button size="small" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-button size="small" @click="$emit('close')">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar" v-show="showSearch">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文档内容..."
        size="small"
        @keyup.enter="performSearch"
        clearable
      >
        <template #append>
          <el-button @click="performSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <div class="search-results" v-if="searchResults.length > 0">
        找到 {{ searchResults.length }} 个结果
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="reader-content" @mouseup="handleTextSelection">
      <!-- Notes Panel (Left) -->
      <notes-panel
          v-if="showNotesPanel"
          :notes="currentDocumentNotes"
          :document-id="effectiveDocumentId"
          @refresh-notes="refreshNotes"
          @note-selected="handleNoteSelected"
          class="notes-panel-container"
      />

      <!-- PDF 阅读区域（居中显示） -->
      <div class="pdf-viewer-container" :class="{ 'with-notes-panel': showNotesPanel }">
        <div class="pdf-loading" v-if="loading">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p>正在加载PDF文档...</p>
        </div>
        
        <div class="pdf-error" v-else-if="error">
          <el-icon size="48" color="#f56c6c"><Warning /></el-icon>
          <p>{{ error }}</p>
          <el-button type="primary" @click="loadPdf">重新加载</el-button>
        </div>
        
        <!-- 使用 Canvas 渲染的阅读视图 -->
        <div 
          class="pdf-viewer" 
          v-else-if="hasDocument && useCanvasViewer" 
          ref="pdfViewerContainer" 
          @scroll="handleScroll"
        >
          <div 
            class="pdf-page-container" 
            v-for="pageNum in pagesToRender" 
            :key="pageNum"
            :data-page="pageNum"
            :style="getPageStyle(pageNum)"
            :ref="el => setPageContainerRef(el, pageNum)"
          >
            <!-- Canvas 层 -->
            <canvas 
              class="pdf-page"
              :ref="el => setCanvasRef(el, pageNum)"
            ></canvas>
            
            <!-- 文本层 (用于选择) -->
            <div 
              class="textLayer"
              :ref="el => setTextLayerRef(el, pageNum)"
            ></div>

            <!-- 笔记高亮层 -->
            <div class="annotationLayer">
               <div 
                v-for="note in getPageNotes(pageNum)" 
                :key="note.id"
                class="highlight-rect"
                :style="getNoteStyle(note)"
                @click.stop="selectNote(note)"
               ></div>
            </div>
            
            <div class="page-number-indicator">第 {{ pageNum }} 页</div>
          </div>
        </div>

        <div class="pdf-empty" v-else>
          <el-empty description="未选择文档或内容未加载" />
        </div>
      </div>

      <!-- AI Chat Panel (Right) -->
      <ai-chat-panel
          v-if="showAiPanel"
          :document-id="effectiveDocumentId"
          class="ai-panel-container"
      />
    </div>

    <!-- 行为气泡 -->
    <action-bubble
      :visible="bubbleVisible"
      :x="bubblePos.x"
      :y="bubblePos.y"
      @highlight="createHighlight('#ffeb3b')"
      @add-note="createNoteFromSelection"
      @ask-ai="askAiFromSelection"
      @copy="onCopySelection"
    />

    <!-- 笔记编辑对话框 -->
    <el-dialog
      v-model="showNoteDialog"
      :title="selectedNote ? '编辑笔记' : '创建笔记'"
      width="500px"
    >
      <el-form :model="noteForm" label-width="80px">
        <el-form-item label="内容">
          <el-input v-model="noteForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="标签">
          <el-tag
            v-for="tag in noteForm.tags"
            :key="tag"
            closable
            @close="removeTag(tag)"
            style="margin-right: 5px"
          >
            {{ tag }}
          </el-tag>
          <el-button size="small" @click="showInput">+ 标签</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoteDialog = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import type { CSSProperties } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as pdfjsLib from 'pdfjs-dist'
import pdfjsWorkerSrc from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import 'pdfjs-dist/web/pdf_viewer.css' // 引入标准 PDF.js 样式

import {
  Document, DArrowLeft, DArrowRight, ArrowLeft, ArrowRight,
  ZoomIn, ZoomOut, FullScreen, Close, Search, EditPen, ChatDotRound,
  Loading, Warning, RefreshLeft, RefreshRight
} from '@element-plus/icons-vue'
import type { MockDocument } from '@/api/mockManager'
import type { NoteDTO, NoteCreateRequest } from '@/types/note'
import { searchDocumentContent } from '@/api/DocumentApi'
import { 
  getNotesList, 
  createNote as createNoteApi, 
  updateNote as updateNoteApi, 
  deleteNote as deleteNoteApi 
} from '@/api/NotesApi'
import { fetchDocumentComposite, invalidateComposite } from './useDocumentData'
import ActionBubble from './ActionBubble.vue'
import AiChatPanel from './AiChatPanel.vue'
import NotesPanel from './NotesPanel.vue'
import type { HighlightPosition } from '@/types/note'

// 设置 Worker
pdfjsLib.GlobalWorkerOptions.workerSrc = pdfjsWorkerSrc

// Props
interface Props {
  document?: MockDocument
  documentId?: string // 显式传递的文档ID，优先级最高
}

const props = defineProps<Props>()
const route = useRoute()

// Emits
const emit = defineEmits<{
  'close': []
  'progress-update': [progress: number]
}>()

// 状态
const loading = ref(false)
const error = ref('')
const scale = ref(1.2)
const lastRenderedScale = ref(1.2) // 记录上次渲染的缩放比例
const currentPage = ref(1)
const totalPages = ref(0)
const hasDocument = computed(() => !!props.document)
const safeTotalPages = computed(() => Math.max(1, totalPages.value || 0))

// 健壮的 documentId 获取逻辑
const effectiveDocumentId = computed(() => {
  // 1. 优先使用显式传递的 prop
  if (props.documentId) return props.documentId

  // 2. 尝试从 props.document 获取
  const d: any = props.document
  let id = d?.documentId ?? d?.id
  
  // 3. 如果 props 中没有，尝试从路由参数获取 (作为兜底)
  if (!id && route.params.id) {
    id = route.params.id
  }
  
  return String(id || '')
})

const isFullscreen = ref(false)
const isDarkMode = ref(false)
const showSearch = ref(false)
const showNotesPanel = ref(true)
const showAiPanel = ref(true)
const showNoteDialog = ref(false)
const useCanvasViewer = ref(true) 
const rotation = ref(0)
const pdfViewerContainer = ref<HTMLElement>()

// PDF Objects
let pdfDoc: any = null
const pagesToRender = ref<number[]>([]) // 存储所有页码 [1, 2, ..., N]
const canvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map())
const textLayerRefs = ref<Map<number, HTMLDivElement>>(new Map())
const pageContainerRefs = ref<Map<number, HTMLElement>>(new Map())
const pageDimensions = ref<Map<number, any>>(new Map())
const baseViewports = ref<Map<number, any>>(new Map()) // 存储 scale=1.0 的 viewport
const renderedPages = ref<Set<number>>(new Set()) // 记录已渲染的页面
let observer: IntersectionObserver | null = null
let renderDebounceTimer: any = null

// 工具函数：防抖
const debounce = (fn: Function, delay: number) => {
  return (...args: any[]) => {
    if (renderDebounceTimer) clearTimeout(renderDebounceTimer)
    renderDebounceTimer = setTimeout(() => {
      fn(...args)
    }, delay)
  }
}

// 滚动处理
const handleScroll = (event: Event) => {
  const container = event.target as HTMLElement
  if (!container) return
  
  // 更新当前页码指示器 (基于中心点可见性)
  const containerRect = container.getBoundingClientRect()
  const midY = containerRect.top + containerRect.height / 2
  
  let closestPage = currentPage.value
  let minDistance = Infinity

  pageContainerRefs.value.forEach((el, pageNum) => {
    const rect = el.getBoundingClientRect()
    const distance = Math.abs(rect.top + rect.height / 2 - midY)
    if (distance < minDistance) {
      minDistance = distance
      closestPage = pageNum
    }
  })

  if (closestPage !== currentPage.value) {
    currentPage.value = closestPage
    emit('progress-update', currentPage.value / totalPages.value)
  }
}

// 搜索
const searchKeyword = ref('')
const searchResults = ref<any[]>([])

// 选区与笔记
const selectedText = ref('')
const selectionRange = ref<Range | null>(null)
const bubbleVisible = ref(false)
const bubblePos = ref({ x: 0, y: 0 })
const currentDocumentNotes = ref<NoteDTO[]>([])
const selectedNote = ref<NoteDTO | undefined>(undefined)
const noteForm = ref({
  content: '',
  selectedText: '',
  positionInfo: null as HighlightPosition | null,
  color: '#ffeb3b',
  tags: [] as string[]
})

// 文件信息
const displayFileSize = computed(() => {
  const d: any = props.document
  if (!d) return ''
  const str = d.file_size || d.fileSizeDisplay || (typeof d.fileSize === 'string' ? d.fileSize : '')
  if (str) return String(str)
  if (typeof d.fileSizeBytes === 'number' && d.fileSizeBytes > 0) return formatFileSize(d.fileSizeBytes)
  return ''
})

const displayProgress = computed(() => {
  if (totalPages.value === 0) return 0
  return Math.round((currentPage.value / totalPages.value) * 100)
})

function formatFileSize(size: number): string {
  const mb = size / (1024 * 1024)
  return `${mb.toFixed(1)} MB`
}

// Refs Setters
const setCanvasRef = (el: any, pageNum: number) => {
  if (el) canvasRefs.value.set(pageNum, el)
}
const setTextLayerRef = (el: any, pageNum: number) => {
  if (el) textLayerRefs.value.set(pageNum, el)
}
const setPageContainerRef = (el: any, pageNum: number) => {
  if (el) {
    pageContainerRefs.value.set(pageNum, el)
    observePage(el, pageNum)
  }
}

// Intersection Observer
const initObserver = () => {
  if (observer) observer.disconnect()
  
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const pageNum = Number(entry.target.getAttribute('data-page'))
        if (pageNum && !renderedPages.value.has(pageNum)) {
          renderPage(pageNum)
        }
      }
    })
  }, {
    root: pdfViewerContainer.value,
    rootMargin: '200px 0px', // 预加载前后 200px
    threshold: 0.1
  })
}

const observePage = (el: HTMLElement, pageNum: number) => {
  if (observer && !renderedPages.value.has(pageNum)) {
    observer.observe(el)
  }
}

// 加载 PDF
const loadPdf = async () => {
  if (!effectiveDocumentId.value) {
    console.error('ModernPdfReader: Document ID is missing', props.document)
    ElMessage.error('无效的文档ID')
    return
  }
  
  loading.value = true
  error.value = ''
  pdfDoc = null
  totalPages.value = 0
  currentPage.value = 1
  pagesToRender.value = []
  renderedPages.value.clear()
  canvasRefs.value.clear()
  textLayerRefs.value.clear()
  pageContainerRefs.value.clear()
  pageDimensions.value.clear()
  baseViewports.value.clear()
  lastRenderedScale.value = scale.value
  
  try {
    // 获取数据
    const composite = await fetchDocumentComposite(effectiveDocumentId.value)
    currentDocumentNotes.value = composite.notes as NoteDTO[]
    
    // 加载 PDF
    const loadingTask = pdfjsLib.getDocument({
      data: composite.bytes,
      cMapUrl: 'https://cdn.jsdelivr.net/npm/pdfjs-dist@5.4.394/cmaps/',
      cMapPacked: true,
      enableXfa: true
    })
    pdfDoc = await loadingTask.promise
    totalPages.value = pdfDoc.numPages
    
    // 初始化页面列表
    pagesToRender.value = Array.from({ length: totalPages.value }, (_, i) => i + 1)
    
    useCanvasViewer.value = true
    
    await nextTick()
    initObserver()
    
    // 预加载第一页以获取尺寸
    await renderPage(1)
    
  } catch (err) {
    console.error('PDF Load Error:', err)
    error.value = '无法加载文档，请检查文件是否损坏'
    useCanvasViewer.value = false 
  } finally {
    loading.value = false
  }
}

const renderPage = async (pageNum: number) => {
  if (!pdfDoc || !useCanvasViewer.value) return
  if (renderedPages.value.has(pageNum)) return

  try {
    const page = await pdfDoc.getPage(pageNum)
    
    // 1. 获取 base viewport (scale=1.0) 并缓存
    let baseViewport = baseViewports.value.get(pageNum)
    if (!baseViewport) {
      baseViewport = page.getViewport({ scale: 1.0, rotation: rotation.value })
      baseViewports.value.set(pageNum, baseViewport)
    }

    // 2. 计算当前 viewport
    const viewport = page.getViewport({ scale: scale.value, rotation: rotation.value })
    pageDimensions.value.set(pageNum, viewport)
    
    const canvas = canvasRefs.value.get(pageNum)
    const textLayerDiv = textLayerRefs.value.get(pageNum)
    
    if (canvas) {
      const context = canvas.getContext('2d')
      
      // 2. 处理高分屏 (Retina/High DPI) 渲染
      const dpr = window.devicePixelRatio || 1
      canvas.width = Math.floor(viewport.width * dpr)
      canvas.height = Math.floor(viewport.height * dpr)
      
      // CSS 尺寸 - 强制设置样式以匹配视口大小 (修复模糊和选区错位)
      canvas.style.width = Math.floor(viewport.width) + "px"
      canvas.style.height = Math.floor(viewport.height) + "px"
      
      // 3. 渲染
      const renderContext = {
        canvasContext: context!,
        viewport: viewport,
        transform: dpr !== 1 ? [dpr, 0, 0, dpr, 0, 0] : undefined // 应用 DPR 缩放矩阵
      }
      await page.render(renderContext).promise
    }
    
    if (textLayerDiv) {
      textLayerDiv.innerHTML = '' 
      textLayerDiv.style.height = `${viewport.height}px`
      textLayerDiv.style.width = `${viewport.width}px`
      // 必须设置该 CSS 变量，pdf_viewer.css 依赖它来缩放文本层
      textLayerDiv.style.setProperty('--scale-factor', `${viewport.scale}`)
      
      const textContent = await page.getTextContent()
      const textLayer = new pdfjsLib.TextLayer({
        textContentSource: textContent,
        container: textLayerDiv,
        viewport: viewport // TextLayer 使用原始 viewport (CSS像素)
      })
      await textLayer.render()
    }

    renderedPages.value.add(pageNum)
    
    // 停止观察已渲染的页面
    const el = pageContainerRefs.value.get(pageNum)
    if (el && observer) observer.unobserve(el)

  } catch (e) {
    console.error(`Render page ${pageNum} failed`, e)
  }
}

// 页面样式
const getPageStyle = (pageNum: number) => {
  // 优先使用 baseViewport 计算当前尺寸，实现无缝缩放
  const baseViewport = baseViewports.value.get(pageNum) || baseViewports.value.get(1)
  
  if (baseViewport) {
    // 动态计算宽高
    const width = baseViewport.width * scale.value
    const height = baseViewport.height * scale.value
    
    return {
      width: `${width}px`,
      height: `${height}px`,
      margin: '20px auto',
      position: 'relative' as const,
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
    }
  }
  
  // 降级：使用旧的 pageDimensions
  const dim = pageDimensions.value.get(pageNum)
  if (dim) {
    return {
      width: `${dim.width}px`,
      height: `${dim.height}px`,
      margin: '20px auto',
      position: 'relative' as const,
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
    }
  }

  // 默认尺寸占位
  return {
    width: `${600 * scale.value}px`,
    height: `${800 * scale.value}px`,
    margin: '20px auto'
  }
}

// 获取某页的笔记
const getPageNotes = (pageNum: number) => {
  return currentDocumentNotes.value.filter(note => {
    try {
      const coord = JSON.parse(note.coord)
      // 兼容旧格式 (coord 可能是简单的对象) 或新格式 HighlightPosition
      const notePage = coord.pageNumber || coord.page
      return Number(notePage) === pageNum
    } catch {
      return false
    }
  })
}

// 获取笔记的所有高亮区域
const getNoteRects = (note: NoteDTO) => {
  try {
    const coord = JSON.parse(note.coord)
    if (coord.rects && Array.isArray(coord.rects)) {
      return coord.rects
    }
    // 兼容旧格式或单一区域
    // 假设旧格式没有 rects，或者我们构造一个假的
    return [] 
  } catch {
    return []
  }
}

// 获取高亮区域样式
const getRectStyle = (rect: any, pageNum: number, color: string): CSSProperties => {
  const baseViewport = baseViewports.value.get(pageNum) || baseViewports.value.get(1)
  if (!baseViewport) return {}

  // 使用 PDF.js viewport 将 PDF 坐标转回屏幕像素
  // 注意：这里我们需要一个对应当前 scale 的 viewport
  // 为了性能，我们可以基于 baseViewport * scale.value 实时计算
  // 或者使用 pageDimensions (如果它是最新的)
  
  // 更好的方式：rect 的坐标是 PDF 坐标系 (Points)。
  // 我们需要转换成当前 container 的像素坐标。
  // scale = scale.value
  
  // rect: {x, y, width, height} (PDF points)
  // viewport transformation: [scale, 0, 0, -scale, 0, viewportHeight] (Default PDF coords are bottom-left)
  // BUT: standard PDF.js `convertToViewportRectangle` handles this.
  
  // 我们手动构建一个临时的 viewport
  const viewport = baseViewport.clone({ scale: scale.value })
  
  const [x, y, w, h] = viewport.convertToViewportRectangle([
    rect.x, 
    rect.y, 
    rect.x + rect.width, 
    rect.y + rect.height
  ]);

  // convertToViewportRectangle returns [xMin, yMin, xMax, yMax]
  
  return {
    position: 'absolute',
    left: `${Math.min(x, w)}px`,
    top: `${Math.min(y, h)}px`,
    width: `${Math.abs(x - w)}px`,
    height: `${Math.abs(y - h)}px`,
    backgroundColor: color || '#ffeb3b',
    opacity: 0.4,
    cursor: 'pointer'
  }
}

// 文本层样式（用于缩放时的模糊过渡）
const getTextLayerStyle = (pageNum: number) => {
  if (lastRenderedScale.value === scale.value) return {}
  
  const ratio = scale.value / lastRenderedScale.value
  return {
    transformOrigin: '0 0',
    transform: `scale(${ratio})`
  }
}

// 笔记样式 (Legacy - can be removed or kept for backward compatibility)
const getNoteStyle = (note: NoteDTO): CSSProperties => {
  return {}
}


// 创建高亮 (ActionBubble 调用)
const createHighlight = async (color: string) => {
  if (!effectiveDocumentId.value) {
    ElMessage.error('系统错误：文档ID丢失，无法创建高亮')
    console.error('Missing documentId in createHighlight')
    return
  }

  if (!selectedText.value || !noteForm.value.positionInfo) {
    // 再次尝试获取选区（如果可能）
    if (window.getSelection()?.toString()) {
       // handleTextSelection 可能还没运行？但 bubble 可见说明运行过了。
       // 这种情况通常是 bubbleVisible 错误地保持 true，而数据已丢失。
       ElMessage.warning('选区信息已失效，请重新选择')
       bubbleVisible.value = false
       return
    }
    return
  }
  
  const req: NoteCreateRequest = {
    document_id: effectiveDocumentId.value,
    content: '',
    selected_text: selectedText.value,
    position_info: noteForm.value.positionInfo!,
    page_number: noteForm.value.positionInfo!.pageNumber,
    color: color,
    tags: []
  }
  
  try {
    const res = await createNoteApi(req)
    if (res.code === 200) {
      ElMessage.success('标注成功')
      refreshNotes()
      bubbleVisible.value = false
      window.getSelection()?.removeAllRanges()
    }
  } catch (e) {
    ElMessage.error('标注失败')
  }
}

// 辅助功能
const createNoteFromSelection = () => {
  if (!selectedText.value || !noteForm.value.positionInfo) {
    ElMessage.warning('无法确定选区位置')
    return
  }

  selectedNote.value = undefined
  // noteForm 已在 handleTextSelection 中预填充
  showNoteDialog.value = true
}

const askAiFromSelection = () => {
  showAiPanel.value = true
  ElMessage.info('已发送给AI助手')
}

const onCopySelection = async () => {
  if (selectedText.value) {
    await navigator.clipboard.writeText(selectedText.value)
    ElMessage.success('复制成功')
    bubbleVisible.value = false
  }
}

const removeTag = (tag: string) => {
  noteForm.value.tags = noteForm.value.tags.filter(t => t !== tag)
}

const showInput = () => {
  ElMessageBox.prompt('请输入标签', '添加标签').then(({ value }) => {
    if (value && !noteForm.value.tags.includes(value)) {
      noteForm.value.tags.push(value)
    }
  })
}

const saveNote = async () => {
  if (!effectiveDocumentId.value) {
    ElMessage.error('系统错误：文档ID丢失，无法保存笔记')
    console.error('Missing documentId in saveNote')
    return
  }
  
  // 校验位置信息 (对于新建笔记)
  if (!selectedNote.value && !noteForm.value.positionInfo) {
      ElMessage.error('位置信息丢失，请重新选择文本')
      return
  }

  const req: NoteCreateRequest = {
    document_id: effectiveDocumentId.value,
    content: noteForm.value.content,
    selected_text: noteForm.value.selectedText,
    position_info: noteForm.value.positionInfo!,
    page_number: noteForm.value.positionInfo!.pageNumber,
    color: noteForm.value.color,
    tags: noteForm.value.tags
  }

  try {
    let res
    if (selectedNote.value) {
       const updateData = {
        id: selectedNote.value.id,
        content: noteForm.value.content,
        color: noteForm.value.color,
        tags: noteForm.value.tags
      }
      res = await updateNoteApi(updateData)
    } else {
      res = await createNoteApi(req)
    }

    if (res.code === 200) {
      ElMessage.success('保存成功')
      refreshNotes()
      showNoteDialog.value = false
      bubbleVisible.value = false
      window.getSelection()?.removeAllRanges()
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// 导航
const nextPage = () => { if (currentPage.value < totalPages.value) { currentPage.value++; scrollToPage(currentPage.value) } }
const previousPage = () => { if (currentPage.value > 1) { currentPage.value--; scrollToPage(currentPage.value) } }
const goToPage = (p: number | undefined) => { 
  if (p && p >= 1 && p <= totalPages.value) { 
    currentPage.value = p; 
    scrollToPage(p)
  } 
}

const scrollToPage = (pageNum: number) => {
  const el = pageContainerRefs.value.get(pageNum)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}


// 缩放提交（防抖）
const commitZoom = debounce(() => {
  lastRenderedScale.value = scale.value
  renderedPages.value.clear()
  initObserver()
}, 300)

const zoomIn = () => { 
  scale.value = Math.min(3, scale.value + 0.2)
  commitZoom()
}
const zoomOut = () => { 
  scale.value = Math.max(0.5, scale.value - 0.2)
  commitZoom()
}
const resetZoom = () => { 
  scale.value = 1.2
  commitZoom()
}
const rotateLeft = () => { 
  rotation.value = (rotation.value - 90 + 360) % 360
  baseViewports.value.clear() // 清除旧旋转角度的 viewport 缓存
  pageDimensions.value.clear()
  renderedPages.value.clear()
  initObserver()
}
const rotateRight = () => { 
  rotation.value = (rotation.value + 90) % 360
  baseViewports.value.clear() // 清除旧旋转角度的 viewport 缓存
  pageDimensions.value.clear()
  renderedPages.value.clear()
  initObserver()
}

/**
 * 将浏览器的 DOM 选区转换为 PDF 页面内的标准坐标
 */
const getSelectionCoords = (
  selection: Selection,
  pageElement: HTMLElement,
  viewport: any
): HighlightPosition['rects'] | null => {
  if (!selection.rangeCount) return null;

  const range = selection.getRangeAt(0);
  const clientRects = range.getClientRects(); // 获取所有选中行的屏幕坐标
  const pageRect = pageElement.getBoundingClientRect(); // 获取 PDF 页面的屏幕坐标

  const pdfRects = [];

  for (const rect of clientRects) {
    // 1. 计算相对于 PDF 页面左上角的像素坐标
    const x = rect.left - pageRect.left;
    const y = rect.top - pageRect.top;

    // 2. 使用 PDF.js 的 viewport 转换方法将像素转为 PDF 点坐标 (Points)
    // viewport.convertToPdfPoint 返回 [x, y]
    // 注意：当前 viewport 必须与页面当前显示的尺寸一致 (即包含 current scale)
    const [x1, y1] = viewport.convertToPdfPoint(x, y);
    const [x2, y2] = viewport.convertToPdfPoint(x + rect.width, y + rect.height);
    
    // 3. 规范化矩形数据 (PDF 坐标系)
    pdfRects.push({
      x: Math.min(x1, x2),
      y: Math.min(y1, y2),
      width: Math.abs(x1 - x2),
      height: Math.abs(y1 - y2)
    });
  }

  return pdfRects.length > 0 ? pdfRects : null;
};

// 选中文本处理
const handleTextSelection = (event: MouseEvent) => {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed) {
    bubbleVisible.value = false // 隐藏气泡
    return
  }
  
  const text = selection.toString().trim()
  if (!text) {
    bubbleVisible.value = false // 隐藏气泡
    return
  }

  // 找到当前选区所在的页面
  let targetNode = selection.anchorNode
  // 向上查找 .pdf-page-container
  while (targetNode && targetNode.nodeType !== Node.ELEMENT_NODE) {
    targetNode = targetNode.parentNode
  }
  let pageElement = (targetNode as Element)?.closest('.pdf-page-container') as HTMLElement
  
  if (!pageElement) return
  
  const pageNum = Number(pageElement.getAttribute('data-page'))
  
  // 获取 Viewport (需要当前 scale 的 viewport)
  // 如果 pageDimensions 中的 viewport 是旧的（未渲染），我们需要重新计算
  // 但通常 selection 发生时页面已经渲染了
  let viewport = pageDimensions.value.get(pageNum)
  if (!viewport) {
     const baseViewport = baseViewports.value.get(pageNum)
     if (baseViewport) {
        viewport = baseViewport.clone({ scale: scale.value })
     }
  }

  if (pageElement && viewport) {
    const rects = getSelectionCoords(selection, pageElement, viewport)
    
    if (rects) {
      const positionInfo: HighlightPosition = {
        pageNumber: pageNum,
        rects: rects
      }

      selectedText.value = text
      selectionRange.value = selection.getRangeAt(0)
      
      // 更新气泡位置
      bubblePos.value = {
        x: event.clientX,
        y: event.clientY
      }
      bubbleVisible.value = true
      
      // 预填充笔记表单
      noteForm.value = {
        content: '',
        selectedText: text,
        positionInfo: positionInfo,
        color: '#ffeb3b',
        tags: []
      }
    }
  }
}

const toggleSearch = () => showSearch.value = !showSearch.value
const toggleNotesPanel = () => showNotesPanel.value = !showNotesPanel.value
const toggleAiPanel = () => showAiPanel.value = !showAiPanel.value
const toggleFullscreen = () => isFullscreen.value = !isFullscreen.value
const performSearch = async () => { /* ... */ }
const refreshNotes = async () => {
  if (!effectiveDocumentId.value) return
  invalidateComposite(effectiveDocumentId.value)
  const comp = await fetchDocumentComposite(effectiveDocumentId.value)
  currentDocumentNotes.value = comp.notes as NoteDTO[]
}
const handleNoteSelected = (noteId: string) => {
  const note = currentDocumentNotes.value.find(n => n.id === noteId)
  if (note) selectNote(note)
}
const selectNote = (note: NoteDTO) => {
  if (note.coord) {
    try {
      const coord = JSON.parse(note.coord)
      // 兼容旧格式
      const page = coord.pageNumber || coord.page
      if (page) goToPage(Number(page))
    } catch {}
  }
  selectedNote.value = note
}

// 监听 Document 变化
watch(() => props.document, () => {
  if (props.document) {
    loadPdf()
  }
}, { immediate: true })

onUnmounted(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.modern-pdf-reader {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f0f2f5;
  color: #333;
  position: relative;
  overflow: hidden;
}

.dark-mode {
  background-color: #1a1a1a;
  color: #e5e5e5;
}

.reader-toolbar {
  height: 56px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  z-index: 100;
}

.dark-mode .reader-toolbar {
  background-color: #2c2c2c;
  border-bottom-color: #333;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.doc-details h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-meta {
  font-size: 12px;
  color: #888;
}

.page-controls, .zoom-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-total {
  font-size: 12px;
  color: #666;
}

.reader-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.pdf-viewer-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background-color: #525659;
  position: relative;
}

.pdf-viewer {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pdf-page-container {
  background-color: #fff;
  position: relative;
}

.pdf-page {
  display: block;
  user-select: none;
  /* width: 100% !important; */
  /* height: 100% !important; */
}

.textLayer {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  opacity: 0.2;
  line-height: 1.0;
  background-color: transparent;
}

.annotationLayer {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.highlight-rect {
  pointer-events: auto;
  mix-blend-mode: multiply;
  transition: transform 0.2s;
}

.highlight-rect:hover {
  transform: scale(1.02);
  z-index: 10;
  border: 1px solid #f56c6c;
}

.page-number-indicator {
  position: absolute;
  bottom: -25px;
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  font-size: 12px;
  opacity: 0.8;
}

.search-bar {
  position: absolute;
  top: 60px;
  right: 20px;
  width: 300px;
  background: #fff;
  padding: 10px;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 99;
}

.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
}

.pdf-loading, .pdf-error, .pdf-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #fff;
  gap: 16px;
}

.loading-icon {
  font-size: 40px;
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 适配侧边栏 */
.notes-panel-container {
  width: 300px;
  border-right: 1px solid #e0e0e0;
  background: #fff;
}

.ai-panel-container {
  width: 350px;
  border-left: 1px solid #e0e0e0;
  background: #fff;
}
</style>