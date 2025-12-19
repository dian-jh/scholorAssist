<template>
  <div class="virtual-grid" ref="containerRef" @scroll="handleScroll">
    <div class="virtual-grid-content" :style="contentStyle">
      <div
        v-for="item in visibleItems"
        :key="item.index"
        class="virtual-grid-item"
        :style="getItemStyle(item)"
      >
        <slot :item="item.data" :index="item.index" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

// Props
interface Props {
  items: any[]
  itemWidth: number
  itemHeight: number
  gap?: number
  overscan?: number
}

const props = withDefaults(defineProps<Props>(), {
  gap: 16,
  overscan: 5
})

// 响应式数据
const containerRef = ref<HTMLElement>()
const scrollTop = ref(0)
const scrollLeft = ref(0)
const containerWidth = ref(0)
const containerHeight = ref(0)

// 计算属性
const columnsCount = computed(() => {
  if (containerWidth.value === 0) return 1
  return Math.floor((containerWidth.value + props.gap) / (props.itemWidth + props.gap))
})

const rowsCount = computed(() => {
  return Math.ceil(props.items.length / columnsCount.value)
})

const totalHeight = computed(() => {
  return rowsCount.value * (props.itemHeight + props.gap) - props.gap
})

const contentStyle = computed((): Record<string, string> => ({
  height: `${totalHeight.value}px`,
  position: 'relative'
}))

const visibleRange = computed(() => {
  const startRow = Math.floor(scrollTop.value / (props.itemHeight + props.gap))
  const endRow = Math.ceil((scrollTop.value + containerHeight.value) / (props.itemHeight + props.gap))
  
  const startIndex = Math.max(0, (startRow - props.overscan) * columnsCount.value)
  const endIndex = Math.min(
    props.items.length - 1,
    (endRow + props.overscan) * columnsCount.value - 1
  )
  
  return { startIndex, endIndex }
})

const visibleItems = computed(() => {
  const { startIndex, endIndex } = visibleRange.value
  const items = []
  
  for (let i = startIndex; i <= endIndex; i++) {
    if (i < props.items.length) {
      items.push({
        index: i,
        data: props.items[i]
      })
    }
  }
  
  return items
})

// 方法
const getItemStyle = (item: { index: number }): Record<string, string> => {
  const row = Math.floor(item.index / columnsCount.value)
  const col = item.index % columnsCount.value
  
  const top = row * (props.itemHeight + props.gap)
  const left = col * (props.itemWidth + props.gap)
  
  return {
    position: 'absolute',
    top: `${top}px`,
    left: `${left}px`,
    width: `${props.itemWidth}px`,
    height: `${props.itemHeight}px`
  }
}

const handleScroll = () => {
  if (containerRef.value) {
    scrollTop.value = containerRef.value.scrollTop
    scrollLeft.value = containerRef.value.scrollLeft
  }
}

const updateContainerSize = () => {
  if (containerRef.value) {
    const rect = containerRef.value.getBoundingClientRect()
    containerWidth.value = rect.width
    containerHeight.value = rect.height
  }
}

const resizeObserver = ref<ResizeObserver>()

// 生命周期
onMounted(() => {
  nextTick(() => {
    updateContainerSize()
    
    // 监听容器大小变化
    if (window.ResizeObserver && containerRef.value) {
      resizeObserver.value = new ResizeObserver(updateContainerSize)
      resizeObserver.value.observe(containerRef.value)
    }
  })
})

onUnmounted(() => {
  if (resizeObserver.value) {
    resizeObserver.value.disconnect()
  }
})

// 监听器
watch(() => props.items.length, () => {
  // 当数据变化时，重置滚动位置
  if (containerRef.value) {
    containerRef.value.scrollTop = 0
    scrollTop.value = 0
  }
})

// 暴露方法
defineExpose({
  scrollToIndex: (index: number) => {
    if (containerRef.value) {
      const row = Math.floor(index / columnsCount.value)
      const targetScrollTop = row * (props.itemHeight + props.gap)
      containerRef.value.scrollTop = targetScrollTop
    }
  },
  scrollToTop: () => {
    if (containerRef.value) {
      containerRef.value.scrollTop = 0
    }
  }
})
</script>

<style lang="scss" scoped>
.virtual-grid {
  width: 100%;
  height: 100%;
  overflow: auto;
  position: relative;
}

.virtual-grid-content {
  width: 100%;
  position: relative;
}

.virtual-grid-item {
  box-sizing: border-box;
}

// 优化滚动性能
.virtual-grid {
  // 启用硬件加速
  transform: translateZ(0);
  // 优化滚动
  -webkit-overflow-scrolling: touch;
  // 减少重绘
  will-change: scroll-position;
}
</style>