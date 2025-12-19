<template>
  <div class="chatgpt-reader-view">
    <ChatGptStyleReader :document-id="documentId" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ChatGptStyleReader from '@/components/Reader/ChatGptStyleReader.vue'

const route = useRoute()

// 从路由参数获取文档ID
const documentId = computed(() => route.params.id as string)
</script>

<style lang="scss" scoped>
.chatgpt-reader-view {
  height: 100vh;
  overflow: hidden;
}
</style>