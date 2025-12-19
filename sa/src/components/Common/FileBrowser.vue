<template>
  <div class="file-browser">
    <!-- 导航栏 -->
    <div class="navigation-bar">
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item @click="navigateToRoot" class="breadcrumb-item">
            <el-icon><House /></el-icon>
            根目录
          </el-breadcrumb-item>
          <el-breadcrumb-item 
            v-for="(item, index) in breadcrumbPath" 
            :key="item.id"
            @click="navigateToCategory(item.id, index)"
            class="breadcrumb-item"
          >
            {{ item.name }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      
      <div class="toolbar">
        <el-button 
          :disabled="!canGoBack" 
          @click="goBack" 
          size="small" 
          :icon="ArrowLeft"
        >
          返回
        </el-button>
        <el-button 
          @click="refreshCurrentFolder" 
          size="small" 
          :icon="Refresh"
        >
          刷新
        </el-button>
      </div>
    </div>

    <!-- 文件列表区域 -->
    <div class="file-list-container" v-loading="loading">
      <!-- 使用虚拟滚动优化大量文件的渲染 -->
      <VirtualGrid
        v-if="allItems.length > 50"
        :items="allItems"
        :item-width="120"
        :item-height="100"
        :gap="16"
        class="virtual-file-grid"
      >
        <template #default="{ item }">
          <div 
            class="file-item"
            :class="{ 
              'folder-item': item.type === 'folder',
              'document-item': item.type === 'document',
              'selected': item.type === 'document' && selectedDocumentId === item.data.id
            }"
            @click="handleItemClick(item)"
            @dblclick="handleItemDoubleClick(item)"
          >
            <div class="file-icon">
              <el-icon size="32" :color="getItemIconColor(item)">
                <component :is="getItemIcon(item)" />
              </el-icon>
            </div>
            <div class="file-name" :title="getItemName(item)">
              {{ getItemName(item) }}
            </div>
            <div class="file-info">
              {{ getItemInfo(item) }}
            </div>
            <div class="file-progress" v-if="item.type === 'document' && item.data.read_progress > 0">
              <el-progress 
                :percentage="Math.round(item.data.read_progress * 100)" 
                :show-text="false" 
                :stroke-width="3"
              />
            </div>
          </div>
        </template>
      </VirtualGrid>
      
      <!-- 普通网格布局（少量文件时） -->
      <div v-else class="file-grid">
        <!-- 文件夹 -->
        <div 
          v-for="folder in folders" 
          :key="'folder-' + folder.id"
          class="file-item folder-item"
          @click="openFolder(folder)"
          @dblclick="openFolder(folder)"
        >
          <div class="file-icon">
            <el-icon size="32" color="#FFD700">
              <Folder />
            </el-icon>
          </div>
          <div class="file-name" :title="folder.name">
            {{ folder.name }}
          </div>
          <div class="file-info">
            {{ folder.document_count }} 个项目
          </div>
        </div>

        <!-- 文档文件 -->
        <div 
          v-for="document in documents" 
          :key="'doc-' + document.id"
          class="file-item document-item"
          @click="selectDocument(document)"
          @dblclick="openDocument(document)"
          :class="{ 'selected': selectedDocumentId === document.id }"
        >
          <div class="file-icon">
            <el-icon size="32" :color="getDocumentIconColor(document)">
              <component :is="getDocumentIcon(document)" />
            </el-icon>
          </div>
          <div class="file-name" :title="document.title">
            {{ document.title }}
          </div>
          <div class="file-info">
            {{ formatDocumentSize(document) }} · {{ document.pages }} 页
          </div>
          <div class="file-progress" v-if="document.read_progress > 0">
            <el-progress 
              :percentage="Math.round(document.read_progress * 100)" 
              :show-text="false" 
              :stroke-width="3"
            />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && folders.length === 0 && documents.length === 0" class="empty-state">
        <el-empty description="此文件夹为空">
          <el-button type="primary" @click="$emit('upload-document')">
            上传文档
          </el-button>
        </el-empty>
      </div>
    </div>

    <!-- 状态栏 -->
    <div class="status-bar">
      <span class="item-count">
        {{ folders.length }} 个文件夹，{{ documents.length }} 个文档
      </span>
      <span class="current-path">
        当前位置：{{ currentPathText }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { 
  House, 
  ArrowLeft, 
  Refresh, 
  Folder
} from '@element-plus/icons-vue'
import { getCategoryList } from '@/api/CategoryApi'
import { getDocumentList } from '@/api/DocumentApi'
import type { MockCategory, MockDocument } from '@/api/mockManager'
import { 
  getFileIcon, 
  getFileIconColor, 
  isAcademicDocument,
  formatFileSize,
  parseFileSize
} from '@/utils/fileIconUtils'
import VirtualGrid from './VirtualGrid.vue'

// Props
interface Props {
  initialCategoryId?: string
}

const props = withDefaults(defineProps<Props>(), {
  initialCategoryId: ''
})

// Emits
const emit = defineEmits<{
  'document-selected': [document: MockDocument]
  'document-opened': [document: MockDocument]
  'upload-document': []
}>()

// 响应式数据
const loading = ref(false)
const currentCategoryId = ref(props.initialCategoryId)
const selectedDocumentId = ref<string>('')
const allCategories = ref<MockCategory[]>([])
const folders = ref<MockCategory[]>([])
const documents = ref<MockDocument[]>([])
const navigationHistory = ref<string[]>([])

// 计算属性
const breadcrumbPath = computed(() => {
  if (!currentCategoryId.value) return []
  
  const path: MockCategory[] = []
  let categoryId = currentCategoryId.value
  
  while (categoryId) {
    const category = findCategoryById(categoryId)
    if (category) {
      path.unshift(category)
      categoryId = category.parent_id || ''
    } else {
      break
    }
  }
  
  return path
})

const canGoBack = computed(() => navigationHistory.value.length > 0)

const currentPathText = computed(() => {
  if (!currentCategoryId.value) return '根目录'
  const path = breadcrumbPath.value.map(item => item.name).join(' / ')
  return path || '根目录'
})

// 合并所有项目用于虚拟滚动
const allItems = computed(() => {
  const items: Array<{ type: 'folder' | 'document', data: MockCategory | MockDocument }> = []
  
  folders.value.forEach(folder => {
    items.push({ type: 'folder', data: folder })
  })
  
  documents.value.forEach(document => {
    items.push({ type: 'document', data: document })
  })
  
  return items
})

// 方法
const findCategoryById = (id: string): MockCategory | null => {
  const findInCategories = (categories: MockCategory[]): MockCategory | null => {
    for (const category of categories) {
      if (category.id === id) return category
      if (category.children) {
        const found = findInCategories(category.children)
        if (found) return found
      }
    }
    return null
  }
  return findInCategories(allCategories.value)
}

const loadCategories = async () => {
  try {
    const response = await getCategoryList()
    if (response.code === 200) {
      allCategories.value = response.data
      updateCurrentFolders()
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadDocuments = async () => {
  try {
    loading.value = true
    const response = await getDocumentList({
      category_id: currentCategoryId.value || undefined,
      page: 1,
      pageSize: 1000 // 加载更多文档用于测试虚拟滚动
    })
    if (response.code === 200) {
      documents.value = response.data
    }
  } catch (error) {
    console.error('加载文档失败:', error)
  } finally {
    loading.value = false
  }
}

const updateCurrentFolders = () => {
  if (!currentCategoryId.value) {
    // 根目录：显示顶级分类
    folders.value = allCategories.value.filter(cat => !cat.parent_id)
  } else {
    // 子目录：显示当前分类的子分类
    const currentCategory = findCategoryById(currentCategoryId.value)
    folders.value = currentCategory?.children || []
  }
}

const navigateToRoot = () => {
  if (currentCategoryId.value) {
    navigationHistory.value.push(currentCategoryId.value)
  }
  currentCategoryId.value = ''
  selectedDocumentId.value = ''
}

const navigateToCategory = (categoryId: string, breadcrumbIndex: number) => {
  if (categoryId !== currentCategoryId.value) {
    if (currentCategoryId.value) {
      navigationHistory.value.push(currentCategoryId.value)
    }
    currentCategoryId.value = categoryId
    selectedDocumentId.value = ''
  }
}

const goBack = () => {
  if (canGoBack.value) {
    const previousId = navigationHistory.value.pop()
    currentCategoryId.value = previousId || ''
    selectedDocumentId.value = ''
  }
}

const openFolder = (folder: MockCategory) => {
  if (currentCategoryId.value) {
    navigationHistory.value.push(currentCategoryId.value)
  }
  currentCategoryId.value = folder.id
  selectedDocumentId.value = ''
}

const selectDocument = (document: MockDocument) => {
  selectedDocumentId.value = document.id
  emit('document-selected', document)
}

const openDocument = (document: MockDocument) => {
  emit('document-opened', document)
}

const refreshCurrentFolder = () => {
  loadDocuments()
  loadCategories()
}

const getDocumentIcon = (document: MockDocument) => {
  return getFileIcon(document.filename)
}

const getDocumentIconColor = (document: MockDocument) => {
  const isAcademic = isAcademicDocument(document.filename, document.title)
  return getFileIconColor(document.filename, isAcademic)
}

const formatDocumentSize = (document: MockDocument) => {
  // 如果file_size已经是格式化的字符串，直接返回
  if (typeof document.file_size === 'string' && document.file_size.includes(' ')) {
    return document.file_size
  }
  // 如果是数字，进行格式化
  const sizeNum = typeof document.file_size === 'number' 
    ? document.file_size 
    : parseFileSize(document.file_size)
  return formatFileSize(sizeNum)
}

// 虚拟滚动项目处理函数
const getItemIcon = (item: { type: 'folder' | 'document', data: MockCategory | MockDocument }) => {
  if (item.type === 'folder') {
    return Folder
  }
  return getFileIcon((item.data as MockDocument).filename)
}

const getItemIconColor = (item: { type: 'folder' | 'document', data: MockCategory | MockDocument }) => {
  if (item.type === 'folder') {
    return '#FFD700'
  }
  const isAcademic = isAcademicDocument((item.data as MockDocument).filename, (item.data as MockDocument).title)
  return getFileIconColor((item.data as MockDocument).filename, isAcademic)
}

const getItemName = (item: { type: 'folder' | 'document', data: MockCategory | MockDocument }) => {
  if (item.type === 'folder') {
    return (item.data as MockCategory).name
  }
  return (item.data as MockDocument).title
}

const getItemInfo = (item: { type: 'folder' | 'document', data: MockCategory | MockDocument }) => {
  if (item.type === 'folder') {
    return `${(item.data as MockCategory).document_count} 个项目`
  }
  const doc = item.data as MockDocument
  return `${formatDocumentSize(doc)} · ${doc.pages} 页`
}

const handleItemClick = (item: { type: 'folder' | 'document', data: MockCategory | MockDocument }) => {
  if (item.type === 'folder') {
    openFolder(item.data as MockCategory)
  } else {
    selectDocument(item.data as MockDocument)
  }
}

const handleItemDoubleClick = (item: { type: 'folder' | 'document', data: MockCategory | MockDocument }) => {
  if (item.type === 'folder') {
    openFolder(item.data as MockCategory)
  } else {
    openDocument(item.data as MockDocument)
  }
}

// 监听器
watch(currentCategoryId, () => {
  updateCurrentFolders()
  loadDocuments()
})

// 生命周期
onMounted(() => {
  loadCategories()
  loadDocuments()
})
</script>

<style lang="scss" scoped>
.file-browser {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.navigation-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.breadcrumb {
  flex: 1;
  
  .breadcrumb-item {
    cursor: pointer;
    transition: color 0.2s;
    
    &:hover {
      color: var(--el-color-primary);
    }
  }
}

.toolbar {
  display: flex;
  gap: 8px;
}

.file-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.virtual-file-grid {
  height: 100%;
  padding: 16px;
}

.file-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
  user-select: none;
  border: 1px solid transparent;
  
  &:hover {
    background: #f5f7fa;
    border-color: var(--el-border-color-light);
  }
  
  &.selected {
    background: #eff9ff;
    border-color: var(--el-color-primary);
  }
}

.folder-item {
  &:hover {
    background: #fff9ed;
  }
}

.document-item {
  &.selected {
    background: #e6f7ff;
  }
}

.file-icon {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  text-align: center;
  line-height: 1.3;
  margin-bottom: 4px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.file-info {
  font-size: 11px;
  color: #909399;
  text-align: center;
  margin-bottom: 4px;
}

.file-progress {
  width: 100%;
  margin-top: 4px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
  font-size: 12px;
  color: #909399;
}

.item-count {
  font-weight: 500;
}

.current-path {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 虚拟滚动特定样式 */
.virtual-file-grid .file-item {
  animation: none; /* 禁用动画以提高虚拟滚动性能 */
}

// 响应式设计
@media (max-width: 768px) {
  .file-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 12px;
  }
  
  .navigation-bar {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }
  
  .status-bar {
    flex-direction: column;
    gap: 4px;
    align-items: flex-start;
  }
}
</style>