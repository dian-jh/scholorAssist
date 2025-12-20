<template>
  <transition name="fade">
    <div 
      v-if="visible"
      class="action-bubble"
      :style="{ left: x + 'px', top: y + 'px' }"
      @mousedown.stop 
    >
      <div class="bubble-content">
        <button class="icon-btn" @click="$emit('highlight')" title="高亮">
           🖍️
        </button>
        <button class="icon-btn" @click="$emit('add-note')" title="添加笔记">
           📝
        </button>
        <div class="divider"></div>
        <button class="icon-btn" @click="$emit('ask-ai')" title="AI 解释">
           🤖
        </button>
        <button class="icon-btn" @click="$emit('copy')" title="复制">
           📋
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
interface Props {
  visible: boolean;
  x: number;
  y: number;
}
defineProps<Props>();
defineEmits(['highlight', 'add-note', 'ask-ai', 'copy']);
</script>

<style scoped>
.action-bubble {
  position: fixed; /* 使用 fixed 确保相对于窗口定位 */
  z-index: 9999;
  background: #333;
  border-radius: 6px;
  padding: 4px 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  transform: translateX(-50%); /* 居中对齐鼠标 */
  pointer-events: auto;
}

.bubble-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  background: rgba(255,255,255,0.2);
}

.divider {
  width: 1px;
  height: 16px;
  background: rgba(255,255,255,0.3);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px); /* 出现时有轻微上浮效果 */
}
</style>
