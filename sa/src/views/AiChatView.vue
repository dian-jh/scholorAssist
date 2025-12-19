<template>
  <div class="ai-chat-view">
    <el-card>
      <template #header>
        <span>AI对话</span>
      </template>
      
      <div class="chat-content">
        <el-empty description="AI对话功能开发中" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
</script>

<style lang="scss" scoped>
.chat-content {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>