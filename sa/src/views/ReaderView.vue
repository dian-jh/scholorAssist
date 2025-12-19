<template>
  <div class="reader-view" :class="readerViewClass">
    <!-- 左侧文件浏览器面板 -->
  <div class="file-browser-panel" v-if="showSidebar">
      <FileBrowser 
        :selected-document-id="selectedDocument?.id"
        @document-selected="handleDocumentSelect"
        @document-opened="handleDocumentOpen"
        @upload-document="handleUploadDocument"
      />
    </div>

    <!-- 右侧文档阅读器面板 -->
    <div class="reader-panel" :class="{ 'expanded': isReaderExpanded }">
      <ModernPdfReader 
        v-if="selectedDocument"
        :document="selectedDocument"
        @close="closeDocument"
        @progress-update="updateReadProgress"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import FileBrowser from '@/components/Common/FileBrowser.vue'
import ModernPdfReader from '@/components/Reader/ModernPdfReader.vue'
import type { MockDocument } from '@/api/mockManager'
import { getDocumentDetail, updateReadingProgress } from '@/api/DocumentApi'
import { useDocumentStore } from '@/store'

// 响应式数据
const selectedDocument = ref<MockDocument>()
const isReaderExpanded = ref(false)

// 计算属性
const route = useRoute()
const showSidebar = computed(() => !route.params.id)
const readerViewClass = computed(() => ({
  'reader-expanded': isReaderExpanded.value || !!route.params.id
}))

// 方法
const handleDocumentSelect = async (document: MockDocument) => {
  try {
    const res = await getDocumentDetail(document.id)
    if (res.code === 200) {
      selectedDocument.value = res.data
      // 选择不自动展开阅读器，保持与Windows交互一致（单击选择，双击打开）
    }
  } catch (err) {
    console.error('获取文档详情失败:', err)
    ElMessage.error('获取文档详情失败')
  }
}

const handleDocumentOpen = async (document: MockDocument) => {
  try {
    const res = await getDocumentDetail(document.id)
    if (res.code === 200) {
      selectedDocument.value = res.data
      isReaderExpanded.value = true
      ElMessage.success(`正在打开文档: ${res.data.title}`)
    }
  } catch (err) {
    console.error('打开文档失败:', err)
    ElMessage.error('打开文档失败')
  }
}

const closeDocument = () => {
  selectedDocument.value = undefined
  isReaderExpanded.value = false
}

const updateReadProgress = async (progress: number) => {
  if (selectedDocument.value) {
    try {
      // 更新阅读进度
      await updateReadingProgress(selectedDocument.value.id, progress)
      selectedDocument.value.read_progress = progress
      ElMessage.success(`阅读进度已更新: ${Math.round(progress * 100)}%`)
    } catch (error) {
      console.error('更新阅读进度失败:', error)
      ElMessage.error('更新阅读进度失败')
    }
  }
}

const handleUploadDocument = () => {
  ElMessage.info('文档上传功能开发中...')
}

// 路由参数支持：直接根据URL中的文档ID加载详情并展开
onMounted(async () => {
  const docId = route.params.id as string | undefined
  if (docId) {
    try {
      const res = await getDocumentDetail(docId)
      if (res.code === 200) {
        selectedDocument.value = res.data
        isReaderExpanded.value = true
      } else {
        // 回退：使用已加载的文档列表匹配ID
        const docStore = useDocumentStore()
        const localDoc = docStore.getDocuments.find(d => d.id === docId) as MockDocument | undefined
        if (localDoc) {
          selectedDocument.value = localDoc
          isReaderExpanded.value = true
        } else {
          ElMessage.warning('未找到文档详情，请返回列表重试')
        }
      }
    } catch (err) {
      console.error('根据路由加载文档失败:', err)
      // 回退：尝试从已加载的文档列表中找到对应文档
      const docStore = useDocumentStore()
      const localDoc = docStore.getDocuments.find(d => d.id === docId) as MockDocument | undefined
      if (localDoc) {
        selectedDocument.value = localDoc
        isReaderExpanded.value = true
      } else {
        ElMessage.error('文档加载失败')
      }
    }
  }
})

// 监听路由变化（例如从Dashboard跳转不同文档）
watch(() => route.params.id, async (newId) => {
  const docId = newId as string | undefined
  if (docId) {
    try {
      const res = await getDocumentDetail(docId)
      if (res.code === 200) {
        selectedDocument.value = res.data
        isReaderExpanded.value = true
      } else {
        const docStore = useDocumentStore()
        const localDoc = docStore.getDocuments.find(d => d.id === docId) as MockDocument | undefined
        if (localDoc) {
          selectedDocument.value = localDoc
          isReaderExpanded.value = true
        } else {
          ElMessage.warning('未找到文档详情，请返回列表重试')
        }
      }
    } catch (err) {
      console.error('根据路由更新文档失败:', err)
      const docStore = useDocumentStore()
      const localDoc = docStore.getDocuments.find(d => d.id === docId) as MockDocument | undefined
      if (localDoc) {
        selectedDocument.value = localDoc
        isReaderExpanded.value = true
      } else {
        ElMessage.error('文档加载失败')
      }
    }
  }
})
</script>

<style lang="scss" scoped>
.reader-view {
  height: 100vh;
  display: flex;
  background: #f5f7fa;
}

.reader-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.file-browser-panel {
  width: 350px;
  min-width: 300px;
  max-width: 500px;
  border-right: 1px solid #e4e7ed;
  background: #fff;
  transition: width 0.3s ease;
}

.reader-panel {
  flex: 1;
  background: #fff;
  transition: all 0.3s ease;
  overflow: hidden;
}

.reader-panel.expanded {
  flex: 2;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .file-browser-panel {
    width: 300px;
  }
}

@media (max-width: 768px) {
  .reader-view {
    flex-direction: column;
  }
  
  .file-browser-panel {
    width: 100%;
    height: 40vh;
    border-right: none;
    border-bottom: 1px solid #e4e7ed;
  }
  
  .reader-panel {
    height: 60vh;
  }
}

/* 当阅读器展开时的样式调整 */
.reader-view.reader-expanded .file-browser-panel {
  width: 280px;
}

@media (max-width: 768px) {
  .reader-view.reader-expanded .file-browser-panel {
    height: 30vh;
  }
  
  .reader-view.reader-expanded .reader-panel {
    height: 70vh;
  }
}
</style>