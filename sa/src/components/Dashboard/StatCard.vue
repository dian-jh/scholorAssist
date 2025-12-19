<template>
  <div class="stat-card" :class="`stat-card--${color}`">
    <div class="stat-content">
      <div class="stat-info">
        <div class="stat-title">{{ title }}</div>
        <div class="stat-value">{{ value }}</div>
      </div>
      <div class="stat-icon">
        <el-icon :size="24">
          <component :is="icon" />
        </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  value: number | string
  icon: string
  color: 'primary' | 'success' | 'warning' | 'info'
}

defineProps<Props>()
</script>

<style lang="scss" scoped>
.stat-card {
  padding: 20px;
  border-radius: 8px;
  color: white;
  
  &--primary {
    background: linear-gradient(135deg, #409EFF 0%, #337ECC 100%);
  }
  
  &--success {
    background: linear-gradient(135deg, #67C23A 0%, #529B2E 100%);
  }
  
  &--warning {
    background: linear-gradient(135deg, #E6A23C 0%, #B88230 100%);
  }
  
  &--info {
    background: linear-gradient(135deg, #909399 0%, #73767A 100%);
  }
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-icon {
  opacity: 0.8;
}
</style>