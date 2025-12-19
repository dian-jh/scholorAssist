<template>
  <div class="category-tree">
    <!-- 全部文献选项 -->
    <div 
      class="category-item all-category"
      :class="{ active: selected === 'all' }"
      @click="selectCategory('all')"
    >
      <div class="category-content">
        <el-icon class="category-icon"><FolderOpened /></el-icon>
        <span class="category-name">全部文献</span>
        <span class="document-count">({{ totalDocuments }})</span>
      </div>
    </div>
    
    <!-- 分类树 -->
    <div class="tree-container">
      <CategoryNode
        v-for="category in categories"
        :key="category.id"
        :category="category"
        :selected="selected"
        :level="0"
        @select="selectCategory"
        @add="$emit('add', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { FolderOpened } from '@element-plus/icons-vue'
import type { MockCategory } from '@/api/mockManager'
import CategoryNode from './CategoryNode.vue'

interface Props {
  categories: MockCategory[]
  selected: string
  totalDocuments?: number
}

interface Emits {
  (e: 'update:selected', value: string): void
  (e: 'add', parentId?: string): void
  (e: 'edit', categoryId: string): void
  (e: 'delete', categoryId: string): void
}

const props = withDefaults(defineProps<Props>(), {
  totalDocuments: 0
})

const emit = defineEmits<Emits>()

const selectCategory = (categoryId: string) => {
  emit('update:selected', categoryId)
}
</script>

<style lang="scss" scoped>
.category-tree {
  width: 100%;
  font-family: 'Inter', sans-serif;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
  margin-bottom: 4px;
  
  &:hover {
    background-color: #f3f4f6; // 更柔和的悬停效果
  }
  
  &.active {
    background-color: #e0e7ff; // 柔和的蓝色背景
    color: #3730a3; // 深蓝色文本
    
    .category-icon,
    .category-name {
      color: #4f46e5; // 强调色
    }
  }
}

.all-category {
  margin-bottom: 12px;
}

.category-content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.category-icon {
  font-size: 18px;
  color: #6b7280;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.document-count {
  font-size: 12px;
  color: #9ca3af;
  margin-left: auto;
  padding-left: 8px;
}

.tree-container {
  max-height: calc(100vh - 320px); // 动态调整高度
  overflow-y: auto;
  padding-right: 4px; // 为滚动条留出空间

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #9ca3af;
  }
}
</style>