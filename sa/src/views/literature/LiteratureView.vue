<template>
  <div class="literature-view">
    <LiteratureNote />
    <div class="main-content">
      <!-- The main content, like the PDF viewer, will go here -->
      <p>Main Content Area</p>
    </div>
    <AiChatPanel :document-id="documentId" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LiteratureNote from './components/LiteratureNote.vue'
import AiChatPanel from '@/components/Reader/AiChatPanel.vue'

const route = useRoute()
const documentId = computed(() => route.params.id as string || '')
</script>

<style scoped lang="scss">
.literature-view {
  display: flex;
  height: 100vh;
}

.main-content {
  flex-grow: 1;
  padding: 20px;
  margin-left: 300px; /* Same as LiteratureNote width */
  margin-right: 400px; /* Same as AiChatPanel width */
  overflow-y: auto;
}
</style>