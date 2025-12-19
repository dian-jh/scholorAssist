<template>
  <div class="workspace-view">
    <splitpanes class="default-theme" @resize="paneSize = $event[0].size">
      <pane :size="30" min-size="20">
        <div class="sidebar-container">
          <div class="sidebar-header">
            <h3>分类</h3>
            <el-button type="primary" :icon="Plus" circle @click="showAddDialog = true" />
          </div>
          <div class="category-tree-wrapper">
            <CategoryTree
              v-model:selected="selectedCategoryId"
              :categories="categories"
              @add="handleAddCategory"
              @edit="handleEditCategory"
              @delete="handleDeleteCategory"
            />
          </div>
        </div>
      </pane>
      <pane :size="70">
        <div class="document-list-container">
          <DocumentList :category-id="selectedCategoryId" />
        </div>
      </pane>
    </splitpanes>

    <AddCategoryDialog
      v-model:visible="showAddDialog"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import { useCategoryStore, useDocumentStore } from '@/store'
import CategoryTree from '@/components/Dashboard/CategoryTree.vue'
import AddCategoryDialog from '@/components/Dashboard/AddCategoryDialog.vue'
import DocumentList from '@/components/Dashboard/DocumentList.vue'

const categoryStore = useCategoryStore()
const documentStore = useDocumentStore()

const selectedCategoryId = ref('all')
const showAddDialog = ref(false)
const paneSize = ref(30)

const categories = computed(() => categoryStore.getCategories)

watch(selectedCategoryId, (newVal) => {
  if (newVal) {
    documentStore.fetchDocuments({ category_id: newVal })
  }
})

const handleAddCategory = () => {
  showAddDialog.value = true
}

const handleEditCategory = (categoryId: string) => {
  ElMessage.info('编辑功能开发中')
}

const handleDeleteCategory = (categoryId: string) => {
  ElMessage.info('删除功能开发中')
}

const handleAddSuccess = () => {
  showAddDialog.value = false
}

onMounted(() => {
  categoryStore.fetchCategories()
  documentStore.fetchDocuments({ category_id: 'all' })
})
</script>

<style lang="scss" scoped>
.workspace-view {
  height: calc(100vh - 110px); 
  background: #f8fafc;
  padding: 12px;
}

.sidebar-container,
.document-list-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.category-tree-wrapper {
  flex-grow: 1;
  overflow-y: auto;
  padding: 8px;
}

.document-list-container {
  padding: 8px;
}

.splitpanes.default-theme {
  .splitpanes__pane {
    background-color: transparent;
  }
  .splitpanes__splitter {
    background-color: #f8fafc;
    position: relative;
    width: 12px;
    border: none;

    &::before {
      content: '';
      position: absolute;
      left: 5px;
      top: 50%;
      transform: translateY(-50%);
      width: 2px;
      height: 32px;
      background: #d1d5db;
      border-radius: 2px;
    }

    &:hover::before {
      background: #9ca3af;
    }
  }
}
</style>