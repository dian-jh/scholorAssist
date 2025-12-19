<template>
  <div class="document-card" @click="$emit('click', document.id)">
    <div class="card-header">
      <div class="thumbnail">
        <img :src="document.thumbnail" :alt="document.title" />
        <div class="status-badge">
          <el-tag 
            :type="document.status === 'ready' ? 'success' : 'warning'"
            size="small"
          >
            {{ document.status === 'ready' ? '已解析' : '解析中' }}
          </el-tag>
        </div>
        <div v-if="document.read_progress > 0" class="progress-overlay">
          <div class="progress-text">
            阅读进度: {{ Math.round(document.read_progress * 100) }}%
          </div>
        </div>
      </div>
    </div>
    
    <div class="card-body">
      <h4 class="document-title">{{ document.title }}</h4>
      <p class="document-author">{{ document.author }}</p>
      <p class="document-abstract">{{ document.abstract }}</p>
      
      <div class="document-meta">
        <span class="meta-item">{{ document.pages }} 页</span>
        <span class="meta-item">{{ document.file_size }}</span>
      </div>
      
      <div class="document-tags">
        <el-tag 
          v-for="tag in document.tags" 
          :key="tag"
          size="small"
          class="tag-item"
        >
          {{ tag }}
        </el-tag>
      </div>
    </div>
    
    <div class="card-actions">
      <el-button :icon="Reading" type="primary" text size="small">
        阅读
      </el-button>
      <el-button :icon="ChatDotRound" text size="small">
        问答
      </el-button>
      <el-dropdown @command="handleCommand">
        <el-button :icon="MoreFilled" text size="small" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="download">
              <el-icon><Download /></el-icon>下载
            </el-dropdown-item>
            <el-dropdown-item command="share">
              <el-icon><Share /></el-icon>分享
            </el-dropdown-item>
            <el-dropdown-item command="delete" divided>
              <el-icon><Delete /></el-icon>删除
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Reading, ChatDotRound, MoreFilled, Download, Share, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { MockDocument } from '@/api/mockManager'

interface Props {
  document: MockDocument
}

interface Emits {
  (e: 'click', docId: string): void
  (e: 'delete', docId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const handleCommand = (command: string) => {
  switch (command) {
    case 'download':
      ElMessage.info('下载功能开发中')
      break
    case 'share':
      ElMessage.info('分享功能开发中')
      break
    case 'delete':
      emit('delete', props.document.id)
      break
  }
}
</script>

<style lang="scss" scoped>
.document-card {
  background: white;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
    
    .card-actions {
      opacity: 1;
    }
  }
}

.card-header {
  position: relative;
}

.thumbnail {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.status-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}

.progress-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px;
  font-size: 12px;
  text-align: center;
}

.card-body {
  padding: 16px;
}

.document-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.document-author {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin: 0 0 8px 0;
}

.document-abstract {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.document-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.meta-item {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.document-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-item {
  font-size: 10px;
}

.card-actions {
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: space-between;
  align-items: center;
  opacity: 0;
  transition: opacity 0.2s;
}
</style>