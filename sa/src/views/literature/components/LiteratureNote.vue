<template>
  <div class="literature-note-container">
    <div class="note-header">
      <span class="title">笔记</span>
      <el-button type="primary" @click="addNote">+ 新建笔记</el-button>
    </div>
    <div class="note-list">
      <div v-for="note in notes" :key="note.id" class="note-item">
        <div class="note-content">{{ note.content }}</div>
        <div class="note-page">第{{ note.page }}页</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const notes = ref([
  { id: 1, content: '这是一条笔记', page: 1 },
  { id: 2, content: '这是另一条笔记', page: 3 },
])

const addNote = () => {
  console.log('add note')
}
</script>

<style scoped lang="scss">
.literature-note-container {
  position: fixed;
  left: 0;
  top: 60px; /* Assuming a header of 60px */
  width: 300px;
  height: calc(100vh - 60px);
  background-color: #fff;
  border-right: 1px solid #dcdfe6;
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .title {
    font-size: 18px;
    font-weight: bold;
  }
}

.note-list {
  flex-grow: 1;
  overflow-y: auto;

  .note-item {
    background-color: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
    margin-bottom: 12px;

    .note-content {
      font-size: 14px;
      margin-bottom: 8px;
    }

    .note-page {
      font-size: 12px;
      color: #909399;
      text-align: right;
    }
  }
}
</style>