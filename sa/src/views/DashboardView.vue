<template>
  <div class="dashboard-container">
    <!-- 左侧分类树 -->
    <div class="category-panel">
      <div class="panel-header">
        <h3>文献分类</h3>
        <el-button 
          :icon="Plus" 
          circle 
          size="small" 
          @click="showAddCategoryDialog = true"
        />
      </div>
      
      <div class="category-tree">
        <CategoryTree
          v-model:selected="selectedCategoryId"
          :categories="categories"
          @add="handleAddCategory"
          @edit="handleEditCategory"
          @delete="handleDeleteCategory"
        />
      </div>
    </div>

    <!-- 中间文档列表 -->
    <div class="document-panel">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <h3>{{ currentCategoryTitle }}</h3>
          <span class="document-count">{{ filteredDocuments.length }} 篇文献</span>
        </div>
        
        <div class="toolbar-right">
          <!-- 搜索 -->
          <el-input
            v-model="searchQuery"
            placeholder="搜索文献..."
            :prefix-icon="Search"
            class="search-input"
            clearable
            @input="handleSearch"
          />
          
          <!-- 排序 -->
          <el-select v-model="sortBy" placeholder="排序方式" class="sort-select">
            <el-option label="按上传时间" value="upload_date" />
            <el-option label="按标题" value="title" />
            <el-option label="按作者" value="author" />
            <el-option label="按阅读进度" value="read_progress" />
          </el-select>
          
          <!-- 视图切换 -->
          <el-button-group class="view-toggle">
            <el-button 
              :type="viewMode === 'grid' ? 'primary' : 'default'"
              :icon="Grid"
              @click="viewMode = 'grid'"
            />
            <el-button 
              :type="viewMode === 'list' ? 'primary' : 'default'"
              :icon="List"
              @click="viewMode = 'list'"
            />
          </el-button-group>
          
          <!-- 上传按钮 -->
          <el-button 
            type="primary" 
            :icon="Upload"
            @click="showUploadDialog = true"
          >
            上传文献
          </el-button>
        </div>
      </div>
      
      <!-- 文档网格视图 -->
      <div v-if="viewMode === 'grid'" class="documents-grid">
        <DocumentCard
          v-for="doc in paginatedDocuments"
          :key="doc.id"
          :document="doc"
          @click="openDocument(doc.id)"
          @delete="handleDeleteDocument"
        />
      </div>
      
      <!-- 文档列表视图 -->
      <div v-else class="documents-list">
        <DocumentTable
          :documents="paginatedDocuments"
          @row-click="openDocument"
          @delete="handleDeleteDocument"
        />
      </div>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 48, 96]"
          :total="filteredDocuments.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 右侧统计面板 -->
    <div class="stats-panel">
      <div class="panel-header">
        <h3>统计概览</h3>
      </div>
      
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <StatCard
          title="总文献数"
          :value="statistics.totalDocuments"
          icon="Document"
          color="primary"
        />
        <StatCard
          title="已完成阅读"
          :value="statistics.completedReading"
          icon="CircleCheck"
          color="success"
        />
        <StatCard
          title="AI问答次数"
          :value="statistics.aiQuestions"
          icon="ChatDotRound"
          color="warning"
        />
        <StatCard
          title="笔记数量"
          :value="statistics.totalNotes"
          icon="EditPen"
          color="info"
        />
      </div>
      
      <!-- 最近活动 -->
      <div class="recent-activity">
        <h4>最近活动</h4>
        <div class="activity-list">
          <div 
            v-for="activity in recentActivities" 
            :key="activity.id"
            class="activity-item"
          >
            <div class="activity-icon">
              <el-icon :color="activity.color">
                <component :is="activity.icon" />
              </el-icon>
            </div>
            <div class="activity-content">
              <div class="activity-title">{{ activity.title }}</div>
              <div class="activity-time">{{ activity.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传文档对话框 -->
    <UploadDialog 
      v-model:visible="showUploadDialog"
      @success="handleUploadSuccess"
    />
    
    <!-- 添加分类对话框 -->
    <AddCategoryDialog
      v-model:visible="showAddCategoryDialog"
      :parent-id="selectedCategoryId"
      @success="handleAddCategorySuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Grid, List, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDocumentStore, useCategoryStore } from '@/store'
import type { MockDocument } from '@/api/mockManager'

// 组件导入
import CategoryTree from '@/components/Dashboard/CategoryTree.vue'
import DocumentCard from '@/components/Dashboard/DocumentCard.vue'
import DocumentTable from '@/components/Dashboard/DocumentTable.vue'
import StatCard from '@/components/Dashboard/StatCard.vue'
import UploadDialog from '@/components/Dashboard/UploadDialog.vue'
import AddCategoryDialog from '@/components/Dashboard/AddCategoryDialog.vue'

const router = useRouter()
const documentStore = useDocumentStore()
const categoryStore = useCategoryStore()

// 响应式数据
const selectedCategoryId = ref<string>('all')
const searchQuery = ref('')
const sortBy = ref('upload_date')
const viewMode = ref<'grid' | 'list'>('grid')
const currentPage = ref(1)
const pageSize = ref(24)
const showUploadDialog = ref(false)
const showAddCategoryDialog = ref(false)

// 计算属性
const categories = computed(() => categoryStore.getCategories)
const documents = computed(() => documentStore.getDocuments)
const loading = computed(() => documentStore.getLoading)

const currentCategoryTitle = computed(() => {
  if (selectedCategoryId.value === 'all') return '全部文献'
  const category = categoryStore.getCategoryById(selectedCategoryId.value)
  return category?.name || '未知分类'
})

const filteredDocuments = computed(() => {
  let result = documents.value

  // 按分类筛选
  if (selectedCategoryId.value !== 'all') {
    result = result.filter(doc => doc.category_id === selectedCategoryId.value)
  }

  // 按搜索关键词筛选
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(doc =>
      doc.title.toLowerCase().includes(query) ||
      doc.author.toLowerCase().includes(query) ||
      doc.abstract.toLowerCase().includes(query) ||
      doc.tags.some(tag => tag.toLowerCase().includes(query))
    )
  }

  // 排序
  result.sort((a, b) => {
    switch (sortBy.value) {
      case 'title':
        return a.title.localeCompare(b.title)
      case 'author':
        return a.author.localeCompare(b.author)
      case 'read_progress':
        return b.read_progress - a.read_progress
      case 'upload_date':
      default:
        return new Date(b.upload_date).getTime() - new Date(a.upload_date).getTime()
    }
  })

  return result
})

const paginatedDocuments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredDocuments.value.slice(start, end)
})

const statistics = ref({
  totalDocuments: 0,
  completedReading: 0,
  aiQuestions: 156,
  totalNotes: 48
})

const recentActivities = ref([
  {
    id: 1,
    title: '上传了《Attention Is All You Need》',
    time: '2小时前',
    icon: 'Upload',
    color: '#409EFF'
  },
  {
    id: 2,
    title: '完成了《ResNet论文》的阅读',
    time: '1天前',
    icon: 'CircleCheck',
    color: '#67C23A'
  },
  {
    id: 3,
    title: '创建了5条新笔记',
    time: '2天前',
    icon: 'EditPen',
    color: '#E6A23C'
  }
])

// 方法
const handleSearch = () => {
  currentPage.value = 1
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

const openDocument = (docId: string) => {
  router.push(`/reader/${docId}`)
}

const handleDeleteDocument = async (docId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文献吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    documentStore.removeDocument(docId)
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

const handleUploadSuccess = (document: MockDocument) => {
  ElMessage.success('文档上传成功')
  showUploadDialog.value = false
}

const handleAddCategory = (parentId?: string) => {
  showAddCategoryDialog.value = true
}

const handleEditCategory = (categoryId: string) => {
  ElMessage.info('编辑分类功能开发中')
}

const handleDeleteCategory = async (categoryId: string) => {
  try {
    await ElMessageBox.confirm('确定要删除这个分类吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await categoryStore.deleteCategoryById(categoryId)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleAddCategorySuccess = () => {
  showAddCategoryDialog.value = false
  ElMessage.success('分类创建成功')
}

// 监听分类变化：改为根据选中分类向后端请求该分类下文档
watch(selectedCategoryId, async (newCategoryId) => {
  try {
    currentPage.value = 1
    // 同步选中分类到全局store（用于跨页面状态）
    categoryStore.setSelectedCategory(newCategoryId === 'all' ? null : newCategoryId)

    // 根据分类从后端拉取文档列表（而非仅在前端过滤）
    await documentStore.fetchDocuments({
      category_id: newCategoryId === 'all' ? undefined : newCategoryId,
      page: 1,
      pageSize: pageSize.value
    })

    // 更新统计数据
    statistics.value.totalDocuments = documents.value.length
    statistics.value.completedReading = documents.value.filter(doc => doc.read_progress >= 1).length
  } catch (error) {
    console.error('根据分类加载文档失败:', error)
    ElMessage.error('加载分类文档失败')
  }
})

// 监听上传事件
onMounted(() => {
  window.addEventListener('trigger-upload', () => {
    showUploadDialog.value = true
  })
})

// 生命周期
onMounted(async () => {
  try {
    await Promise.all([
      documentStore.fetchDocuments(),
      categoryStore.fetchCategories()
    ])
    
    // 更新统计数据
    statistics.value.totalDocuments = documents.value.length
    statistics.value.completedReading = documents.value.filter(doc => doc.read_progress >= 1).length
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  }
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  display: flex;
  height: calc(100vh - 112px); // 减去header和padding
  gap: 24px;
}

.category-panel {
  width: 280px;
  background: white;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  
  @media (max-width: 1200px) {
    width: 240px;
  }
  
  @media (max-width: 768px) {
    display: none;
  }
}

.document-panel {
  flex: 1;
  background: white;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stats-panel {
  width: 320px;
  background: white;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  
  @media (max-width: 1400px) {
    width: 280px;
  }
  
  @media (max-width: 1024px) {
    display: none;
  }
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

.category-tree {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.toolbar {
  padding: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  
  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

.document-count {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 240px;
  
  @media (max-width: 768px) {
    width: 180px;
  }
}

.sort-select {
  width: 120px;
}

.view-toggle {
  .el-button {
    padding: 8px 12px;
  }
}

.documents-grid {
  flex: 1;
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  overflow-y: auto;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}

.documents-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.pagination-container {
  padding: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: center;
}

.stats-cards {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recent-activity {
  flex: 1;
  padding: 0 20px 20px;
  
  h4 {
    margin: 0 0 16px 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: background-color 0.2s;
  
  &:hover {
    background-color: var(--el-fill-color-light);
  }
}

.activity-icon {
  margin-top: 2px;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>