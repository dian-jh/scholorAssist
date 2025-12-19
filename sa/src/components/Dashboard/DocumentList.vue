<template>
  <div class="document-list">
    <div v-if="documents.length > 0" class="document-grid">
      <div v-for="doc in documents" :key="doc.id" class="document-card" @click="handleDocClick(doc.id)">
        <div class="document-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="document-info">
          <h4 class="document-title">{{ doc.title }}</h4>
          <p class="document-meta">{{ formatMeta(doc) }}</p>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      <el-empty description="该分类下暂无文献">
        <el-button type="primary">上传文献</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDocumentStore } from '@/store'
import { Document } from '@element-plus/icons-vue'

const props = defineProps<{ categoryId: string }>()

const router = useRouter()
const documentStore = useDocumentStore()

const documents = computed(() => documentStore.getDocumentsByCategory(props.categoryId))

const handleDocClick = (docId: string) => {
  router.push({ name: 'Reader', params: { id: docId } })
}

// 统一格式化文档元信息（作者 · 日期 · 大小 · 页数）
const formatMeta = (doc: any) => {
  const author = doc.author || '未署名'
  const dateStr = doc.upload_date ?? doc.uploadDate
  const date = dateStr ? new Date(dateStr).toLocaleDateString() : ''
  const size = doc.file_size ?? doc.fileSize
  const pages = doc.pages ? `${doc.pages}页` : ''
  return [author, date, size, pages].filter(Boolean).join(' · ')
}
</script>

<style lang="scss" scoped>
.document-list {
  height: 100%;
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  padding: 8px;
}

.document-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 12px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    border-color: #d1d5db;
  }

  .document-icon {
    font-size: 28px;
    color: #4f46e5;
    margin-bottom: 12px;
  }

  .document-info {
    .document-title {
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
      margin: 0 0 4px 0;
      line-height: 1.3;
    }

    .document-meta {
      font-size: 12px;
      color: #6b7280;
      margin: 0;
    }
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>