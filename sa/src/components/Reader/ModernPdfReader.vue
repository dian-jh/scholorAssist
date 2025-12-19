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
              {{ displayFileSize }} · {{ document?.pages || '-' }} 页 · 
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

          <!-- 书签 -->
          <el-popover placement="bottom" width="240" trigger="click">
            <template #reference>
              <el-tooltip content="书签">
                <el-button size="small" @click="addBookmark">
                  📑 添加书签
                </el-button>
              </el-tooltip>
            </template>
            <div>
              <div v-if="bookmarks.length === 0" style="color:#6b7280;">暂无书签</div>
              <div v-else>
                <div v-for="bm in bookmarks" :key="bm.id" style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:6px;">
                  <span style="font-size:12px;">{{ bm.title }}</span>
                  <div style="display:flex;gap:6px;">
                    <el-button size="small" text @click="jumpBookmark(bm)">跳转</el-button>
                    <el-button size="small" text type="danger" @click="removeBookmark(bm.id)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>
          
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
    <div class="reader-content">
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
          >
            <div class="page-header">
              <span class="page-number">第 {{ pageNum }} 页</span>
            </div>
            <canvas 
              class="pdf-page"
              :ref="el => setCanvasRef(el, pageNum)"
              @click="handlePageClick"
            ></canvas>
            <!-- 简易标注图层：显示该页的笔记锚点，点击可查看/跳转 -->
            <div class="note-markers-layer">
              <div 
                v-for="note in getPageNotes(pageNum)" 
                :key="note.id"
                class="note-marker"
                :style="{ left: '12px', top: '12px' }"
                @click="showNoteDetail(note)"
              >
                <el-icon><EditPen /></el-icon>
                <div class="note-tooltip">
                  <h4>{{ note.title }}</h4>
                  <p>{{ note.content }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 作为兜底的 iframe 嵌入（部分浏览器策略或跨域异常时）-->
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { TokenManager } from '@/utils/tokenManager'
import { ConfigManager } from '@/config'
// 使用 pdf.js 的ESM入口，并显式配置worker，避免“No Global workerOptions”错误
import * as pdfjsLib from 'pdfjs-dist/build/pdf'
import pdfjsWorkerSrc from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
pdfjsLib.GlobalWorkerOptions.workerSrc = pdfjsWorkerSrc
import {
  Document,
  DArrowLeft,
  DArrowRight,
  ArrowLeft,
  ArrowRight,
  ZoomIn,
  ZoomOut,
  FullScreen,
  Close,
  Search,
  EditPen,
  ChatDotRound, // 添加此行
  Loading,
  Warning,
  Plus,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import { RefreshLeft, RefreshRight } from '@element-plus/icons-vue'
import type { MockDocument } from '@/api/mockManager'
import type { Note } from '@/api/NotesApi'
import { 
  extractDocumentText, 
  searchDocumentContent 
} from '@/api/DocumentApi'
import { getDocumentBytes } from '@/api/DocumentApi'
import { 
  getNotesList, 
  createNote, 
  updateNote, 
  deleteNote as deleteNoteApi 
} from '@/api/NotesApi'
// 复合加载器：并发获取字节流与笔记，并做缓存与请求合并
import { fetchDocumentComposite, invalidateComposite } from './useDocumentData'
// 文本选择行为浮动气泡
import ActionBubble from './ActionBubble.vue'
import AiChatPanel from '@/views/literature/components/AiChatPanel.vue'

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
const scale = ref(1.2)
const currentPage = ref(1)
const totalPages = ref(0)
const hasDocument = computed(() => !!props.document)
const safeTotalPages = computed(() => Math.max(1, totalPages.value || 0))
// 统一向 NotesPanel / AiChatPanel 传递的文档ID
const documentId = computed(() => {
  const d: any = props.document
  const id = d?.documentId ?? d?.id ?? ''
  return String(id)
})
const isFullscreen = ref(false)
const showSearch = ref(false)
const showNotesPanel = ref(true)
const showAiPanel = ref(true)
const showColorPicker = ref(false)
const showNoteDialog = ref(false)
// 显示与渲染控制
const useCanvasViewer = ref(false)
const rotation = ref(0) // 0/90/180/270
const displayMode = ref<'single' | 'double' | 'continuous'>('single')
const pdfViewer = ref<HTMLElement>()
const canvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map())
let pdfDoc: any = null
const renderTasks = ref<Map<number, any>>(new Map())

// 搜索相关
const searchKeyword = ref('')
const searchResults = ref<any[]>([])

// 文本选择相关
const selectedText = ref('')
const selectionRange = ref<any>(null)
const colorPickerPosition = ref({ x: 0, y: 0 })
// 行为气泡控制
const bubbleVisible = ref(false)
const bubblePos = ref({ x: 0, y: 0 })

// 笔记相关
const currentDocumentNotes = ref<Note[]>([])
const selectedNote = ref<Note>()
const noteForm = ref({
  title: '',
  content: '',
  tags: [] as string[]
})
const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref()

// PDF内容数据
const pdfPages = ref<any[]>([])
const highlights = ref<any[]>([])

// 高亮颜色选项
const highlightColors = [
  { name: '黄色', value: '#ffeb3b' },
  { name: '绿色', value: '#4caf50' },
  { name: '蓝色', value: '#2196f3' },
  { name: '红色', value: '#f44336' },
  { name: '紫色', value: '#9c27b0' },
  { name: '橙色', value: '#ff9800' }
]

// 计算属性
const pdfUrl = ref<string>('')
// 实际用于 iframe 的地址；优先直链，必要时回退为 Blob URL
const iframeSrc = ref<string>('')
// 兼容显示：文件大小与阅读进度（避免模板直接访问不存在的属性）
const displayFileSize = computed(() => {
  const d: any = props.document
  if (!d) return ''
  // 优先使用已格式化的字符串（兼容后端返回 fileSize: "2.2 MB"）
  const str = d.file_size || d.fileSizeDisplay || (typeof d.fileSize === 'string' ? d.fileSize : '')
  if (str) return String(str)
  // 次选使用数值型字节数 fileSizeBytes
  if (typeof d.fileSizeBytes === 'number' && d.fileSizeBytes > 0) {
    return formatFileSize(d.fileSizeBytes)
  }
  // 兼容旧字段：数值型 fileSize（字节）
  if (typeof d.fileSize === 'number' && d.fileSize > 0) {
    return formatFileSize(d.fileSize)
  }
  return ''
})

const displayProgress = computed(() => {
  const d: any = props.document
  const p = d?.read_progress ?? d?.readProgress ?? 0
  return Math.round(p * 100)
})

// 核心逻辑：计算并返回最终用于 iframe 或下载的 PDF URL。
const computeFinalPdfUrl = () => {
  const doc = props.document;
  if (!doc || !doc.id) {
    console.warn("computePdfUrl: document or document.id is missing.");
    return null;
  }

  // 优先级1: previewUrl 或 file_url (通常是完整的静态URL)
  let finalUrl = doc.previewUrl || doc.file_url || doc.fileUrl;
  if (finalUrl && finalUrl.startsWith('http')) {
    return finalUrl;
  }

  // 优先级2: filePath (通常是 /files/... 格式的相对路径)
  finalUrl = doc.filePath || doc.file_path;
  if (finalUrl && finalUrl.startsWith('/')) {
    return finalUrl.replace(/\\/g, '/');
  }
  
  // 优先级3: 如果有相对路径但没有斜杠开头，则补上
  if(finalUrl) {
    return `/${finalUrl.replace(/\\/g, '/')}`;
  }

  // 优先级4: 回退到 /download 接口
  console.log(`无法从文档对象中找到直接的文件路径, 将回退到 /download 接口。`);
  return `/api/files/documents/${doc.id}/download`;
};

// 加载PDF的主函数
const loadPdf = async () => {
  if (!props.document || !props.document.id) {
    iframeSrc.value = '';
    console.log("PDF加载中止：文档数据不完整。");
    return;
  }

  loading.value = true;
  error.value = '';
  console.log("开始加载PDF...", props.document);

  try {
    const docId = String(((props.document as any).id ?? (props.document as any).documentId))
    // 使用复合加载器并发拉取字节流与笔记，自动去重与缓存
    const composite = await fetchDocumentComposite(docId, { pageSize: 200 })
    pdfUrl.value = composite.blobUrl
    iframeSrc.value = composite.blobUrl
    currentDocumentNotes.value = composite.notes
    useCanvasViewer.value = false
    console.log('PDF与笔记已通过复合加载器拉取，使用Blob URL展示')
  } catch (err) {
    console.error('PDF或笔记加载失败:', err);
    error.value = 'PDF文档或笔记加载失败，请检查文件是否存在或网络连接是否正常';
    // 兜底：尝试直链URL预览
    const finalUrl = computeFinalPdfUrl()
    if (finalUrl) {
      pdfUrl.value = finalUrl
      iframeSrc.value = finalUrl
      useCanvasViewer.value = false
      error.value = ''
      try { ElMessage && ElMessage.warning('已回退至直链预览') } catch {}
    }
  } finally {
    loading.value = false;
  }
};

// 仅保留此唯一侦听器，并在文档对象变化时触发加载
watch(() => props.document, loadPdf, { immediate: true, deep: true });


const pagesToRender = ref<number[]>([]) // 保留定义以避免模板错误
const setCanvasRef = (el: Element | null, pageNum: number) => {}

let currentBlobUrl: string | null = null
async function prepareIframeSrc() {
  // 保持 iframeSrc 与当前 pdfUrl 一致（可能是 Blob URL 或直链）
  iframeSrc.value = pdfUrl.value || ''
}

onUnmounted(() => {
  if (currentBlobUrl) {
    try { URL.revokeObjectURL(currentBlobUrl) } catch {}
    currentBlobUrl = null
  }
})

// 以复合加载器刷新笔记（用于创建/更新/删除后）
const refreshNotes = async () => {
  if (!props.document) return
  const docId = String(((props.document as any).documentId ?? (props.document as any).id ?? ''))
  // 使缓存失效以获取最新数据
  invalidateComposite(docId)
  try {
    const composite = await fetchDocumentComposite(docId, { pageSize: 200 })
    currentDocumentNotes.value = composite.notes
  } catch (err) {
    console.error('刷新笔记失败:', err)
  }
}

const getPageTextBlocks = (pageNum: number) => {
  const page = pdfPages.value.find(p => p.page_number === pageNum)
  return page?.text_blocks || []
}

const getPageHighlights = (pageNum: number) => {
  return highlights.value.filter(h => h.page_number === pageNum)
}

const getPageNotes = (pageNum: number) => {
  return currentDocumentNotes.value.filter(n => n.page_number === pageNum)
}

// 文件大小显示
function formatFileSize(size?: number): string {
  if (!size || size <= 0) return ''
  const mb = size / (1024 * 1024)
  return `${mb.toFixed(1)} MB`
}

// 缩放控制 (功能已禁用，因为iframe接管)
const zoomIn = () => {
  if (scale.value < 3) {
    scale.value = Math.min(3, scale.value + 0.2)
  }
}

const zoomOut = () => {
  if (scale.value > 0.5) {
    scale.value = Math.max(0.5, scale.value - 0.2)
  }
}

const resetZoom = () => {
  scale.value = 1.2
}

// 页面导航 (功能已禁用，因为iframe接管)
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const goToPage = (pageNum?: number) => {
  if (pageNum && pageNum >= 1 && pageNum <= totalPages.value) {
    currentPage.value = pageNum
  }
}

const updateReadProgress = () => {
  if (props.document && totalPages.value > 0) {
    const progress = currentPage.value / totalPages.value
    emit('progress-update', progress)
  }
}

// 界面控制
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

// 旋转
const rotateLeft = () => {
  rotation.value = (rotation.value + 270) % 360
  renderVisiblePages()
}
const rotateRight = () => {
  rotation.value = (rotation.value + 90) % 360
  renderVisiblePages()
}

// 显示模式切换
const setDisplayMode = (mode: 'single' | 'double' | 'continuous') => {
  displayMode.value = mode
  renderVisiblePages()
}

// 滚动事件（用于连续模式估算当前页）
const handleScroll = () => {
  if (!pdfViewer.value || displayMode.value !== 'continuous') return
  const containers = pdfViewer.value.querySelectorAll('.pdf-page-container')
  const viewerRect = pdfViewer.value.getBoundingClientRect()
  let current = currentPage.value
  containers.forEach((el) => {
    const rect = el.getBoundingClientRect()
    const num = Number((el as HTMLElement).querySelector('.page-number')?.textContent?.replace(/[^0-9]/g, '') || current)
    if (rect.top <= viewerRect.top + 100) {
      current = num
    }
  })
  if (current !== currentPage.value) {
    currentPage.value = current
    updateReadProgress()
  }
}

const handlePageClick = (event: MouseEvent) => {
  // 可用于标注入口；此处保留日志以便后续扩展
  console.log('页面点击:', event)
}

// 书签（本地存储）
const bookmarks = ref<{ id: string; page: number; title: string }[]>([])
const bookmarkKey = computed(() => {
  const d: any = props.document
  const id = d?.documentId ?? d?.id ?? 'unknown'
  return `pdfBookmarks:${id}`
})

const loadBookmarks = () => {
  try {
    const raw = localStorage.getItem(bookmarkKey.value)
    bookmarks.value = raw ? JSON.parse(raw) : []
  } catch {
    bookmarks.value = []
  }
}

const saveBookmarks = () => {
  try {
    localStorage.setItem(bookmarkKey.value, JSON.stringify(bookmarks.value))
  } catch {}
}

const addBookmark = () => {
  const bm = {
    id: Date.now().toString(),
    page: currentPage.value,
    title: `第${currentPage.value}页 - ${(props.document as any)?.title || '书签'}`
  }
  bookmarks.value.push(bm)
  saveBookmarks()
  ElMessage.success('已添加书签')
}

const jumpBookmark = (bm: { page: number }) => {
  goToPage(bm.page)
}

const removeBookmark = (id: string) => {
  bookmarks.value = bookmarks.value.filter(b => b.id !== id)
  saveBookmarks()
}

watch(bookmarkKey, () => loadBookmarks(), { immediate: true })

const toggleSearch = () => {
  showSearch.value = !showSearch.value
}

// 面板开关（带localStorage持久化）
const notesPanelKey = computed(() => {
  const d: any = props.document
  const id = d?.documentId ?? d?.id ?? 'global'
  return `reader:notes-panel:${id}`
})
const loadPanelPrefs = () => {
  try {
    const n = localStorage.getItem(notesPanelKey.value)
    showNotesPanel.value = n == null ? true : JSON.parse(n)
  } catch { showNotesPanel.value = true }
}

watch(notesPanelKey, loadPanelPrefs, { immediate: true })

const toggleNotesPanel = () => {
  showNotesPanel.value = !showNotesPanel.value
  try { localStorage.setItem(notesPanelKey.value, JSON.stringify(showNotesPanel.value)) } catch {}
}

const toggleAiPanel = () => {
  showAiPanel.value = !showAiPanel.value
}

// 搜索功能
const performSearch = async () => {
  if (!props.document || !searchKeyword.value.trim()) return
  
  try {
    const response = await searchDocumentContent(props.document.id, {
      keyword: searchKeyword.value.trim()
    })
    if (response.code === 200) {
      searchResults.value = response.data.results
      ElMessage.success(`找到 ${response.data.results.length} 个搜索结果`)
    }
  } catch (err) {
    console.error('搜索失败:', err)
    ElMessage.error('搜索失败')
  }
}

// 文本选择和高亮
const startTextSelection = (event: MouseEvent) => {
  // 开始文本选择
}

const updateTextSelection = (event: MouseEvent) => {
  // 更新文本选择
}

const endTextSelection = (event: MouseEvent) => {
  // 结束文本选择
}

const handleTextSelection = (event: MouseEvent) => {
  const selection = window.getSelection()
  if (selection && selection.toString().trim()) {
    selectedText.value = selection.toString().trim()
    selectionRange.value = selection.getRangeAt(0)
    // 使用行为气泡替代颜色选择器
    bubblePos.value = { x: event.clientX, y: Math.max(0, event.clientY - 50) }
    bubbleVisible.value = true
    showColorPicker.value = false
  } else {
    bubbleVisible.value = false
    showColorPicker.value = false
  }
}

const handleContextMenu = (event: MouseEvent) => {
  event.preventDefault()
}

const createHighlight = (color: string) => {
  if (!selectedText.value || !selectionRange.value) return
  
  // 创建高亮标记
  const highlight = {
    id: Date.now().toString(),
    page_number: currentPage.value,
    text: selectedText.value,
    color: color,
    position: {
      x: 100, // 实际应该根据选择范围计算
      y: 100,
      width: 200,
      height: 20
    }
  }
  
  highlights.value.push(highlight)
  showColorPicker.value = false
  bubbleVisible.value = false
  
  ElMessage.success('高亮标记已创建')
}

const createNoteFromSelection = () => {
  if (!selectedText.value) return
  
  noteForm.value = {
    title: '新笔记',
    content: selectedText.value,
    tags: []
  }
  
  showNoteDialog.value = true
  showColorPicker.value = false
  bubbleVisible.value = false
}

// 行为气泡事件
const onAddNoteFromBubble = () => {
  createNoteFromSelection()
}
const onCopySelection = async () => {
  if (!selectedText.value) return
  try {
    await navigator.clipboard.writeText(selectedText.value)
    ElMessage.success('已复制选中文本')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

// 笔记管理
const createNewNote = () => {
  noteForm.value = {
    title: '',
    content: '',
    tags: []
  }
  showNoteDialog.value = true
}

const selectNote = (note: Note) => {
  selectedNote.value = note
  // 跳转到笔记所在页面
  goToPage(note.page_number)
}

const editNote = (note: Note) => {
  noteForm.value = {
    title: note.title,
    content: note.content,
    tags: [...note.tags]
  }
  selectedNote.value = note
  showNoteDialog.value = true
}

const saveNote = async () => {
  if (!props.document) return
  
  try {
    if (selectedNote.value) {
      // 更新笔记
      await updateNote(selectedNote.value.id, noteForm.value)
      ElMessage.success('笔记更新成功')
    } else {
      // 创建新笔记
      await createNote({
        document_id: props.document.id,
        title: noteForm.value.title || '无标题',
        content: noteForm.value.content,
        page_number: currentPage.value,
        tags: noteForm.value.tags
      })
      ElMessage.success('笔记创建成功')
    }
    
    showNoteDialog.value = false
    await refreshNotes()
  } catch (err) {
    console.error('保存笔记失败:', err)
    ElMessage.error('保存笔记失败')
  }
}

const deleteNote = async (noteId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这条笔记吗？', '确认删除', {
      type: 'warning'
    })
    
    await deleteNoteApi(noteId)
    ElMessage.success('笔记删除成功')
    await refreshNotes()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除笔记失败:', err)
      ElMessage.error('删除笔记失败')
    }
  }
}

const showHighlightNote = (highlight: any) => {
  // 显示高亮相关的笔记
}

const showNoteDetail = (note: Note) => {
  selectNote(note)
}

const handleNoteDialogClose = () => {
  selectedNote.value = undefined
  showNoteDialog.value = false
}

// 标签管理
const removeTag = (tag: string) => {
  const index = noteForm.value.tags.indexOf(tag)
  if (index > -1) {
    noteForm.value.tags.splice(index, 1)
  }
}

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleInputConfirm = () => {
  if (inputValue.value && !noteForm.value.tags.includes(inputValue.value)) {
    noteForm.value.tags.push(inputValue.value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

// 生命周期
// 初始加载由 watch(immediate) 触发，避免与 onMounted 重复导致双请求

// 删除重复侦听器，保留顶部唯一的 watch(() => props.document, loadPdf, { immediate: true, deep: true })

// 键盘快捷键
onMounted(() => {
  const handleKeydown = (event: KeyboardEvent) => {
    if (event.ctrlKey || event.metaKey) {
      switch (event.key) {
        case 'f':
          event.preventDefault()
          toggleSearch()
          break
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
        case 'Escape':
          showColorPicker.value = false
          showSearch.value = false
          break
      }
    }
  }
  
  document.addEventListener('keydown', handleKeydown)
  
  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })
})

// 点击外部关闭颜色选择器
onMounted(() => {
  const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as HTMLElement
    if (!target.closest('.color-picker-popup')) {
      showColorPicker.value = false
    }
  }
  
  document.addEventListener('click', handleClickOutside)
  
  onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
  })
})

// 清理资源
onUnmounted(() => {
  renderTasks.value.forEach((task) => {
    if (task && task.cancel) {
      try { task.cancel() } catch {}
    }
  })
  canvasRefs.value.clear()
  if (pdfDoc && pdfDoc.cleanup) {
    try { pdfDoc.cleanup() } catch {}
  }
  pdfDoc = null
  // 释放复合加载器中的 Blob URL 并清理缓存
  try {
    const docId = String(((props.document as any)?.documentId ?? (props.document as any)?.id ?? ''))
    if (docId) invalidateComposite(docId)
  } catch {}
})
</script>

<style lang="scss" scoped>
.modern-pdf-reader {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  
  &.fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    background: #fff;
  }
}

.reader-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  
  .toolbar-left {
    flex: 1;
    
    .document-info {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .doc-details {
        .doc-title {
          margin: 0;
          font-size: 16px;
          font-weight: 600;
          color: #1f2937;
          line-height: 1.2;
        }
        
        .doc-meta {
          font-size: 12px;
          color: #6b7280;
        }
      }
    }
  }
  
  .toolbar-center {
    flex: 0 0 auto;
    
    .page-controls {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .page-input {
        display: flex;
        align-items: center;
        gap: 4px;
        
        .el-input-number {
          width: 80px;
        }
        
        .page-total {
          font-size: 14px;
          color: #6b7280;
          white-space: nowrap;
        }
      }
    }
  }
  
  .toolbar-right {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    
    .zoom-controls,
    .tool-buttons {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.search-bar {
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  
  .el-input {
    max-width: 400px;
  }
  
  .search-results {
    margin-top: 8px;
    font-size: 12px;
    color: #6b7280;
  }
}

.reader-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.notes-panel-container {
  width: 320px;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  background: #ffffff; /* 清晰的背景区分 */
}

.pdf-viewer-container {
  flex: 1; /* Fill remaining space */
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f0f2f5; /* A slightly different background for the central panel */
  padding: 24px; /* 适当的内边距，保证与周围元素有间距 */
}

/* 当笔记面板存在时，为文档容器添加视觉分隔线（位于左侧） */
.pdf-viewer-container.with-notes-panel {
  /* 分隔线仅由 .notes-panel-container 提供，这里不再重复 */
}

/* 居中显示的兜底 iframe 样式 */
.pdf-embed {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow: auto;
  padding: 24px;
  background: #f8fafc;
}

.pdf-iframe {
  width: 100%;
  max-width: 1200px;
  min-height: 75vh;
  border: none;
  border-radius: 8px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
  background: #fff;
}

/* Canvas 渲染视图的居中与间距 */
.pdf-viewer {
  flex: 1;
  overflow: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center; /* 居中页面 */
  background: #f8fafc;
}

.pdf-page-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto 20px; /* 页面间距与居中 */
}

.ai-panel-container {
  width: 360px;
  flex-shrink: 0;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.color-picker-popup {
  position: fixed;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  padding: 12px;
  z-index: 1000;
  
  .color-options {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
    
    .color-option {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      cursor: pointer;
      border: 2px solid #fff;
      box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
      transition: transform 0.2s ease;
      
      &:hover {
        transform: scale(1.1);
      }
    }
  }
  
  .picker-actions {
    border-top: 1px solid #e5e7eb;
    padding-top: 8px;
  }
}

// 响应式设计
@media (max-width: 1024px) {
  .reader-toolbar {
    padding: 8px 12px;
    
    .toolbar-center {
      .page-controls {
        gap: 8px;
        
        .page-input .el-input-number {
          width: 70px;
        }
      }
    }
  }
  
  .notes-panel-container { width: 300px; }
  .ai-panel-container { width: 300px; }
  
  .pdf-embed,
  .pdf-viewer {
    padding: 12px;
    
    .pdf-page {
      width: 500px;
      min-height: 667px;
    }
  }
}

@media (max-width: 768px) {
  .modern-pdf-reader {
    .reader-toolbar {
      flex-wrap: wrap;
      gap: 8px;
      
      .toolbar-left,
      .toolbar-right {
        flex: none;
      }
      
      .toolbar-center {
        order: 3;
        flex-basis: 100%;
        justify-content: center;
      }
    }
    
    .reader-content { flex-direction: column; }
    
    .pdf-viewer-container.with-notes-panel {
      border-left: none; /* 移动端不显示侧边分隔线 */
      border-bottom: 1px solid #e5e7eb;
    }
    
    .notes-panel-container {
      width: 100%;
      max-height: 300px;
      border-right: none;
      border-top: 1px solid #e5e7eb; /* 改为顶部分隔线以适配底部布局 */
      order: 2; /* 在移动端置于文档容器之后（底部） */
    }
    .ai-panel-container {
      width: 100%;
      max-height: 300px;
      border-left: none;
      border-top: 1px solid #e5e7eb;
      order: 3; /* 置于最下方 */
    }
  }
}

// 打印样式
@media print {
  .modern-pdf-reader {
    .reader-toolbar,
    .search-bar,
    .notes-panel {
      display: none !important;
    }
    
    .pdf-viewer-container {
      border: none !important;
    }
    
    .pdf-embed,
    .pdf-viewer {
      padding: 0 !important;
      
      .pdf-page {
        box-shadow: none !important;
        margin-bottom: 0 !important;
        page-break-after: always;
      }
    }
  }
}
</style>
// 统一向 NotesPanel / AiChatPanel 传递的文档ID
const documentId = computed(() => {
  const d: any = props.document
  const id = d?.documentId ?? d?.id ?? ''
  return String(id)
})