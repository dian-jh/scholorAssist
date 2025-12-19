<template>
  <div class="document-table">
    <el-table :data="documents" @row-click="handleRowClick">
      <el-table-column prop="title" label="文献标题" min-width="200" />
      <el-table-column prop="author" label="作者" width="150" />
      <el-table-column prop="upload_date" label="上传时间" width="120" />
      <el-table-column prop="read_progress" label="阅读进度" width="100">
        <template #default="{ row }">
          {{ Math.round(row.read_progress * 100) }}%
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="danger" text @click.stop="$emit('delete', row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import type { MockDocument } from '@/api/mockManager'

interface Props {
  documents: MockDocument[]
}

interface Emits {
  (e: 'row-click', docId: string): void
  (e: 'delete', docId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const handleRowClick = (row: MockDocument) => {
  emit('row-click', row.id)
}
</script>