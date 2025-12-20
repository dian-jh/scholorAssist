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

          <!-- 显示模式 -->
          <el-tooltip content="显示模式">
            <el-button-group size="small">
              <el-button :type="displayMode === 'single' ? 'primary' : 'default'" @click="setDisplayMode('single')">单页</el-button>
              <el-button :type="displayMode === 'double' ? 'primary' : 'default'" @click="setDisplayMode('double')">双页</el-button>
              <el-button :type="displayMode === 'continuous' ? 'primary' : 'default'" @click="setDisplayMode('continuous')">连续</el-button>
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
          :document-id="documentId"
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
        <div class="pdf-viewer" v-else-if="hasDocument && useCanvasViewer" ref="pdfViewer" @scroll="handleScroll">
          <div 
            class="pdf-page-container" 
            v-for="pageNum in pagesToRender" 
            :key="pageNum"
            :data-page="pageNum"
            :style="getPageStyle(pageNum)"
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

        <!-- 作为兜底的 iframe 嵌入（仅在 Canvas 加载失败或不支持时使用）-->
        <div class="pdf-embed" v-else-if="hasDocument">
          <iframe 
            v-if="pdfUrl || iframeSrc" 
            :src="iframeSrc || pdfUrl" 
            class="pdf-iframe"
            title="PDF文档预览"
            aria-label="PDF文档预览"
          />
          <el-empty v-else description="未提供文件路径" />
        </div>
        <div class="pdf-empty" v-else>
          <el-empty description="未选择文档或内容未加载" />
        </div>
      </div>

      <!-- AI Chat Panel (Right) -->
      <ai-chat-panel
          v-if="showAiPanel"
          :document-id="documentId"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import * as pdfjsLib from 'pdfjs-dist'
import pdfjsWorkerSrc from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
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

// 设置 Worker
pdfjsLib.GlobalWorkerOptions.workerSrc = pdfjsWorkerSrc

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

// 状态
const loading = ref(false)
const error = ref('')
const scale = ref(1.2)
const currentPage = ref(1)
const totalPages = ref(0)
const hasDocument = computed(() => !!props.document)
const safeTotalPages = computed(() => Math.max(1, totalPages.value || 0))
const documentId = computed(() => {
  const d: any = props.document
  const id = d?.documentId ?? d?.id ?? ''
  return String(id)
})
const isFullscreen = ref(false)
const isDarkMode = ref(false)
const showSearch = ref(false)
const showNotesPanel = ref(true)
const showAiPanel = ref(true)
const showNoteDialog = ref(false)
const useCanvasViewer = ref(true) // 默认为true，尝试使用Canvas渲染
const rotation = ref(0)
const displayMode = ref<'single' | 'double' | 'continuous'>('single')
const pdfViewer = ref<HTMLElement>()

// PDF Objects
let pdfDoc: any = null
const pagesToRender = ref<number[]>([])
const canvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map())
const textLayerRefs = ref<Map<number, HTMLDivElement>>(new Map())
const pageDimensions = ref<Map<number, any>>(new Map())
const pdfUrl = ref('')
const iframeSrc = ref('')

// 滚动处理
const handleScroll = (event: Event) => {
  const container = event.target as HTMLElement
  if (!container) return
  
  // 简单可见性检测
  const pages = container.querySelectorAll('.pdf-page-container')
  let maxVisibility = 0
  let visiblePage = currentPage.value
  
  pages.forEach((page: any) => {
    const rect = page.getBoundingClientRect()
    const containerRect = container.getBoundingClientRect()
    
    const intersectionHeight = Math.max(0, Math.min(rect.bottom, containerRect.bottom) - Math.max(rect.top, containerRect.top))
    const visibility = intersectionHeight / rect.height
    
    if (visibility > maxVisibility) {
      maxVisibility = visibility
      visiblePage = Number(page.getAttribute('data-page'))
    }
  })
  
  if (visiblePage !== currentPage.value) {
    currentPage.value = visiblePage
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
  referenceText: '',
  coord: '',
  color: '#ffeb3b',
  tags: [] as string[]
})
const inputVisible = ref(false)
const inputValue = ref('')

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

// 加载 PDF
const loadPdf = async () => {
  if (!documentId.value) return
  
  loading.value = true
  error.value = ''
  pdfDoc = null
  totalPages.value = 0
  currentPage.value = 1
  pagesToRender.value = []
  canvasRefs.value.clear()
  textLayerRefs.value.clear()
  pageDimensions.value.clear()
  
  try {
    // 获取数据
    const composite = await fetchDocumentComposite(documentId.value)
    currentDocumentNotes.value = composite.notes as NoteDTO[]
    
    // 加载 PDF
    // 修正：封装为对象，并添加 cMap 配置以支持中文文档（适配 pdfjs-dist v5.x）
    const loadingTask = pdfjsLib.getDocument({
      data: composite.bytes,
      cMapUrl: 'https://cdn.jsdelivr.net/npm/pdfjs-dist@5.4.394/cmaps/',
      cMapPacked: true,
      enableXfa: true
    })
    pdfDoc = await loadingTask.promise
    totalPages.value = pdfDoc.numPages
    
    useCanvasViewer.value = true
    await renderVisiblePages()
    
  } catch (err) {
    console.error('PDF Load Error:', err)
    error.value = '无法加载文档，将尝试使用浏览器默认预览'
    useCanvasViewer.value = false // 降级
  } finally {
    loading.value = false
  }
}

// 渲染逻辑
const renderVisiblePages = async () => {
  if (!pdfDoc || !useCanvasViewer.value) return
  
  // 简单逻辑：渲染当前页 (待支持多页模式)
  let pages = [currentPage.value]
  if (displayMode.value === 'double' && currentPage.value < totalPages.value) {
    pages.push(currentPage.value + 1)
  } else if (displayMode.value === 'continuous') {
    // 连续模式：渲染前后几页
    const start = Math.max(1, currentPage.value - 1)
    const end = Math.min(totalPages.value, currentPage.value + 2)
    pages = []
    for (let i = start; i <= end; i++) pages.push(i)
  }
  
  pagesToRender.value = pages
  
  await nextTick()
  
  for (const pageNum of pages) {
    renderPage(pageNum)
  }
}

const renderPage = async (pageNum: number) => {
  try {
    const page = await pdfDoc.getPage(pageNum)
    const viewport = page.getViewport({ scale: scale.value, rotation: rotation.value })
    pageDimensions.value.set(pageNum, viewport)
    
    const canvas = canvasRefs.value.get(pageNum)
    const textLayerDiv = textLayerRefs.value.get(pageNum)
    
    if (canvas) {
      const context = canvas.getContext('2d')
      canvas.height = viewport.height
      canvas.width = viewport.width
      
      const renderContext = {
        canvasContext: context!,
        viewport: viewport
      }
      await page.render(renderContext).promise
    }
    
    if (textLayerDiv) {
      textLayerDiv.innerHTML = '' // 清除旧内容
      textLayerDiv.style.height = `${viewport.height}px`
      textLayerDiv.style.width = `${viewport.width}px`
      textLayerDiv.style.setProperty('--scale-factor', `${viewport.scale}`)
      
      const textContent = await page.getTextContent()
      const textLayer = new pdfjsLib.TextLayer({
        textContentSource: textContent,
        container: textLayerDiv,
        viewport: viewport
      })
      await textLayer.render()
    }
  } catch (e) {
    console.error(`Render page ${pageNum} failed`, e)
  }
}

// 页面样式
const getPageStyle = (pageNum: number) => {
  // 可以在这里设置页面容器的尺寸，避免加载时跳动
  return {}
}

// 获取某页的笔记
const getPageNotes = (pageNum: number) => {
  return currentDocumentNotes.value.filter(note => {
    try {
      const coord = JSON.parse(note.coord)
      return Number(coord.page) === pageNum
    } catch {
      return false
    }
  })
}

// 笔记样式
const getNoteStyle = (note: NoteDTO): CSSProperties => {
  if (!note.coord) return {}
  try {
    const coord = JSON.parse(note.coord)
    
    // 简化：假设 coord.rect 是 normalized (0-1)
    if (coord.normalized && coord.rect) {
      return {
        left: `${coord.rect.x * 100}%`,
        top: `${coord.rect.y * 100}%`,
        width: `${coord.rect.w * 100}%`,
        height: `${coord.rect.h * 100}%`,
        backgroundColor: note.color || '#ffeb3b',
        opacity: 0.4,
        position: 'absolute',
        cursor: 'pointer'
      }
    }
    
    return { display: 'none' }
  } catch (e) {
    return { display: 'none' }
  }
}

// 文本选择处理
const handleTextSelection = (event: MouseEvent) => {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed) {
    bubbleVisible.value = false
    return
  }
  
  const text = selection.toString().trim()
  if (!text) {
    bubbleVisible.value = false
    return
  }
  
  selectedText.value = text
  selectionRange.value = selection.getRangeAt(0)
  
  // 计算气泡位置
  const rangeRect = selectionRange.value.getBoundingClientRect()
  bubblePos.value = {
    x: rangeRect.left + rangeRect.width / 2 - 50, // 居中
    y: rangeRect.top - 60
  }
  bubbleVisible.value = true
}

// 计算选区坐标
const getSelectionCoord = (): string | null => {
  if (!selectionRange.value) return null
  
  const range = selectionRange.value
  const node = range.commonAncestorContainer
  const element = node.nodeType === 1 ? node as HTMLElement : node.parentElement
  if (!element) return null
  
  const pageContainer = element.closest('.pdf-page-container') as HTMLElement
  if (!pageContainer) return null
  
  const pageNum = Number(pageContainer.getAttribute('data-page'))
  if (!pageNum || isNaN(pageNum)) return null
  
  // Get rects relative to viewport
  // We use the textLayer as the reference frame since it matches the viewport size
  const textLayer = pageContainer.querySelector('.textLayer') as HTMLElement
  if (!textLayer) return null
  
  const containerRect = textLayer.getBoundingClientRect()
  const rangeRect = range.getBoundingClientRect()
  
  // Calculate relative coordinates (0-1)
  const x = (rangeRect.left - containerRect.left) / containerRect.width
  const y = (rangeRect.top - containerRect.top) / containerRect.height
  const w = rangeRect.width / containerRect.width
  const h = rangeRect.height / containerRect.height
  
  return JSON.stringify({
    page: pageNum,
    normalized: true,
    rect: {
      x: Math.max(0, x),
      y: Math.max(0, y),
      w: Math.min(1, w),
      h: Math.min(1, h)
    }
  })
}

// 创建高亮
const createHighlight = async (color: string) => {
  if (!selectedText.value || !selectionRange.value) return

  const coord = getSelectionCoord()
  if (!coord) {
    ElMessage.warning('无法确定选区位置')
    return
  }
  
  const req: NoteCreateRequest = {
    documentId: documentId.value,
    content: '',
    referenceText: selectedText.value,
    coord: coord,
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
  if (!selectedText.value) return
  
  const coord = getSelectionCoord()
  if (!coord) {
    ElMessage.warning('无法确定选区位置')
    return
  }

  selectedNote.value = undefined
  noteForm.value = {
    content: '',
    referenceText: selectedText.value,
    coord: coord,
    color: '#ffeb3b',
    tags: []
  }
  showNoteDialog.value = true
}

const askAiFromSelection = () => {
  // 切换到 AI 面板并发送消息
  showAiPanel.value = true
  // TODO: 通过 EventBus 或 Store 发送消息给 AiChatPanel
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
  if (!documentId.value) return
  
  const req: NoteCreateRequest = {
    documentId: documentId.value,
    content: noteForm.value.content,
    referenceText: noteForm.value.referenceText,
    coord: noteForm.value.coord,
    color: noteForm.value.color,
    tags: noteForm.value.tags
  }

  try {
    let res
    if (selectedNote.value) {
      // 编辑
       const updateData = {
        id: selectedNote.value.id,
        content: noteForm.value.content,
        color: noteForm.value.color,
        tags: noteForm.value.tags
      }
      res = await updateNoteApi(updateData)
    } else {
      // 创建
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
const nextPage = () => { if (currentPage.value < totalPages.value) { currentPage.value++; renderVisiblePages() } }
const previousPage = () => { if (currentPage.value > 1) { currentPage.value--; renderVisiblePages() } }
const goToPage = (p: number | undefined) => { 
  if (p && p >= 1 && p <= totalPages.value) { 
    currentPage.value = p; 
    renderVisiblePages() 
  } 
}
const zoomIn = () => { scale.value = Math.min(3, scale.value + 0.2); renderVisiblePages() }
const zoomOut = () => { scale.value = Math.max(0.5, scale.value - 0.2); renderVisiblePages() }
const resetZoom = () => { scale.value = 1.2; renderVisiblePages() }
const rotateLeft = () => { rotation.value = (rotation.value - 90 + 360) % 360; renderVisiblePages() }
const rotateRight = () => { rotation.value = (rotation.value + 90) % 360; renderVisiblePages() }
const setDisplayMode = (m: any) => { displayMode.value = m; renderVisiblePages() }
const toggleSearch = () => showSearch.value = !showSearch.value
const toggleNotesPanel = () => showNotesPanel.value = !showNotesPanel.value
const toggleAiPanel = () => showAiPanel.value = !showAiPanel.value
const toggleFullscreen = () => isFullscreen.value = !isFullscreen.value
const performSearch = async () => { /* ... */ }
const refreshNotes = async () => {
  if (!documentId.value) return
  invalidateComposite(documentId.value)
  const comp = await fetchDocumentComposite(documentId.value)
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
      if (coord.page) goToPage(Number(coord.page))
    } catch {}
  }
  selectedNote.value = note
}

// Watchers
watch(() => props.document, loadPdf, { immediate: true })

</script>

<style lang="scss" scoped>
.modern-pdf-reader {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  
  &.fullscreen {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    z-index: 9999;
    background: #fff;
  }
}

.reader-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  
  .toolbar-left, .toolbar-right { flex: 1; display: flex; align-items: center; }
  .toolbar-right { justify-content: flex-end; gap: 8px; }
  .toolbar-center { flex: 0 0 auto; }
}

.document-info {
  display: flex;
  align-items: center;
  gap: 10px;
  .doc-title { margin: 0; font-size: 16px; font-weight: 600; }
  .doc-meta { font-size: 12px; color: #666; }
}

.reader-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.notes-panel-container { width: 300px; border-right: 1px solid #eee; }
.ai-panel-container { width: 350px; border-left: 1px solid #eee; }

.pdf-viewer-container {
  flex: 1;
  background: #f0f2f5;
  overflow: auto;
  display: flex;
  justify-content: center;
  padding: 20px;
}

.pdf-viewer {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pdf-page-container {
  position: relative;
  margin-bottom: 20px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.pdf-page {
  display: block;
}

.textLayer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  opacity: 0.2;
  line-height: 1.0;
  ::v-deep(span) {
    color: transparent;
    position: absolute;
    white-space: pre;
    cursor: text;
    transform-origin: 0% 0%;
  }
}

.annotationLayer {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none; // Allow clicks to pass through to text layer unless on an annotation
}

.highlight-rect {
  pointer-events: auto;
  transition: opacity 0.2s;
  &:hover { opacity: 0.6 !important; }
}

.page-number-indicator {
  position: absolute;
  bottom: -25px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #666;
}

.pdf-embed { width: 100%; height: 100%; }
.pdf-iframe { width: 100%; height: 100%; border: none; }

.pdf-loading, .pdf-error, .pdf-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
  gap: 10px;
}
</style>
