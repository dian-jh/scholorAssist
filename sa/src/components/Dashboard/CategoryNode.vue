<template>
  <div class="category-node">
    <div 
      class="category-item"
      :class="{ active: selected === category.id }"
      :style="{ paddingLeft: `${level * 16 + 12}px` }"
      @click="selectCategory"
    >
      <div class="category-content">
        <!-- 展开/收起图标 -->
        <el-icon 
          v-if="hasChildren"
          class="expand-icon"
          :class="{ expanded: isExpanded }"
          @click.stop="toggleExpand"
        >
          <ArrowRight />
        </el-icon>
        <div v-else class="expand-placeholder"></div>
        
        <!-- 分类图标 -->
        <el-icon class="category-icon">
          <Folder />
        </el-icon>
        
        <!-- 分类名称和文档数量 -->
        <span class="category-name">{{ category.name }}</span>
        <span class="document-count">({{ category.document_count }})</span>
      </div>
      
      <!-- 操作按钮 -->
      <div class="category-actions" @click.stop>
        <el-dropdown trigger="click" @command="handleCommand">
          <el-button :icon="MoreFilled" text size="small" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="add">
                <el-icon><Plus /></el-icon>添加子分类
              </el-dropdown-item>
              <el-dropdown-item command="edit">
                <el-icon><Edit /></el-icon>编辑分类
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <el-icon><Delete /></el-icon>删除分类
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    
    <!-- 子分类 -->
    <div v-if="hasChildren && isExpanded" class="children">
      <CategoryNode
        v-for="child in category.children"
        :key="child.id"
        :category="child"
        :selected="selected"
        :level="level + 1"
        @select="$emit('select', $event)"
        @add="$emit('add', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowRight, Folder, MoreFilled, Plus, Edit, Delete } from '@element-plus/icons-vue'
import type { MockCategory } from '@/api/mockManager'

interface Props {
  category: MockCategory
  selected: string
  level: number
}

interface Emits {
  (e: 'select', categoryId: string): void
  (e: 'add', parentId: string): void
  (e: 'edit', categoryId: string): void
  (e: 'delete', categoryId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const isExpanded = ref(true)

const hasChildren = computed(() => {
  return props.category.children && props.category.children.length > 0
})

const selectCategory = () => {
  emit('select', props.category.id)
}

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'add':
      emit('add', props.category.id)
      break
    case 'edit':
      emit('edit', props.category.id)
      break
    case 'delete':
      emit('delete', props.category.id)
      break
  }
}
</script>

<style lang="scss" scoped>
.category-node {
  width: 100%;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 2px;
  
  &:hover {
    background-color: var(--el-fill-color-light);
    
    .category-actions {
      opacity: 1;
    }
  }
  
  &.active {
    background-color: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    border-right: 3px solid var(--el-color-primary);
    
    .category-icon {
      color: var(--el-color-primary);
    }
  }
}

.category-content {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.expand-icon {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  transition: transform 0.2s;
  cursor: pointer;
  
  &.expanded {
    transform: rotate(90deg);
  }
  
  &:hover {
    color: var(--el-text-color-primary);
  }
}

.expand-placeholder {
  width: 12px;
  height: 12px;
}

.category-icon {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.category-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-count {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.category-actions {
  opacity: 0;
  transition: opacity 0.2s;
  
  .el-button {
    padding: 2px 4px;
  }
}

.children {
  margin-left: 8px;
}
</style>