<template>
  <div class="category-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
            添加分类
          </el-button>
        </div>
      </template>
      
      <div class="category-content">
        <CategoryTree
          v-model:selected="selectedCategoryId"
          :categories="categories"
          @add="handleAddCategory"
          @edit="handleEditCategory"
          @delete="handleDeleteCategory"
        />
      </div>
    </el-card>
    
    <AddCategoryDialog
      v-model:visible="showAddDialog"
      @success="handleAddSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCategoryStore } from '@/store'
import CategoryTree from '@/components/Dashboard/CategoryTree.vue'
import AddCategoryDialog from '@/components/Dashboard/AddCategoryDialog.vue'

const categoryStore = useCategoryStore()

const selectedCategoryId = ref('all')
const showAddDialog = ref(false)

const categories = computed(() => categoryStore.getCategories)

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
})
</script>

<style lang="scss" scoped>
.category-view {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.category-content {
  min-height: 400px;
}
</style>