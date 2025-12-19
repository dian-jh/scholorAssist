<template>
  <div class="notes-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>笔记管理</span>
          <el-button type="primary" :icon="Plus">
            新建笔记
          </el-button>
        </div>
      </template>
      
      <div class="notes-content">
        <el-empty description="笔记功能开发中" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
</script>

<style lang="scss" scoped>
.notes-view {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.notes-content {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>