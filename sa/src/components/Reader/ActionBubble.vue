<template>
  <transition name="action-bubble-fade">
    <div 
      v-if="visible"
      class="action-bubble"
      :style="{ left: x + 'px', top: y + 'px' }"
      role="dialog"
      aria-label="文本操作浮动框"
    >
      <div class="bubble-content">
        <button class="bubble-btn" @click="$emit('highlight')" title="高亮文本">
          🖍️ 高亮
        </button>
        <button class="bubble-btn" @click="$emit('add-note')" title="添加笔记">
          📝 添加笔记
        </button>
        <button class="bubble-btn" @click="$emit('ask-ai')" title="询问AI">
          🤖 询问AI
        </button>
        <button class="bubble-btn" @click="$emit('copy')" title="复制文本">
          📋 复制
        </button>
      </div>
    </div>
  </transition>
  
</template>

<script setup lang="ts">
interface Props {
  visible: boolean
  x: number
  y: number
}
defineProps<Props>()
defineEmits<{
  'highlight': []
  'add-note': []
  'ask-ai': []
  'copy': []
}>()
</script>

<style scoped>
.action-bubble {
  position: fixed;
  z-index: 5000;
  background: #111827;
  color: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 24px rgba(0,0,0,0.18);
  padding: 8px 10px;
  transform-origin: center;
  backdrop-filter: saturate(180%) blur(6px);
}
.bubble-content {
  display: flex;
  gap: 8px;
}
.bubble-btn {
  appearance: none;
  border: none;
  padding: 6px 10px;
  border-radius: 8px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all .18s ease;
}
.bubble-btn:hover { 
  background: rgba(255,255,255,0.16);
  transform: translateY(-1px);
}
.bubble-btn:active { 
  transform: translateY(0);
}

/* 动画 */
.action-bubble-fade-enter-active,
.action-bubble-fade-leave-active {
  transition: opacity .16s ease, transform .16s ease;
}
.action-bubble-fade-enter-from,
.action-bubble-fade-leave-to {
  opacity: 0;
  transform: scale(0.96);
}
</style>