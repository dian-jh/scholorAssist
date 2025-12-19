<template>
  <el-drawer
    v-model="visible"
    title="通知"
    direction="rtl"
    size="400px"
    class="notification-drawer"
  >
    <div class="notification-content">
      <div v-if="notifications.length === 0" class="empty-state">
        <el-empty description="暂无通知" />
      </div>
      
      <div v-else class="notification-list">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ 'unread': !notification.read }"
          @click="markAsRead(notification.id)"
        >
          <div class="notification-icon">
            <el-icon v-if="notification.type === 'info'" color="#409EFF">
              <InfoFilled />
            </el-icon>
            <el-icon v-else-if="notification.type === 'success'" color="#67C23A">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else-if="notification.type === 'warning'" color="#E6A23C">
              <WarningFilled />
            </el-icon>
            <el-icon v-else-if="notification.type === 'error'" color="#F56C6C">
              <CircleCloseFilled />
            </el-icon>
          </div>
          
          <div class="notification-body">
            <div class="notification-title">{{ notification.title }}</div>
            <div class="notification-message">{{ notification.message }}</div>
            <div class="notification-time">{{ notification.time }}</div>
          </div>
          
          <div v-if="!notification.read" class="unread-dot"></div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <div class="notification-footer">
        <el-button type="primary" text @click="markAllAsRead">
          全部标记为已读
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { InfoFilled, SuccessFilled, WarningFilled, CircleCloseFilled } from '@element-plus/icons-vue'

interface Notification {
  id: number
  type: 'info' | 'success' | 'warning' | 'error'
  title: string
  message: string
  time: string
  read: boolean
}

interface Props {
  visible: boolean
  notifications: Notification[]
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'mark-read', id: number): void
  (e: 'mark-all-read'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const markAsRead = (id: number) => {
  emit('mark-read', id)
}

const markAllAsRead = () => {
  emit('mark-all-read')
}
</script>

<style lang="scss" scoped>
.notification-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.notification-list {
  flex: 1;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
  
  &:hover {
    background-color: var(--el-fill-color-light);
  }
  
  &.unread {
    background-color: var(--el-color-primary-light-9);
  }
  
  &:last-child {
    border-bottom: none;
  }
}

.notification-icon {
  margin-top: 2px;
}

.notification-body {
  flex: 1;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.notification-message {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
  margin-bottom: 8px;
}

.notification-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: var(--el-color-primary);
  border-radius: 50%;
  margin-top: 6px;
}

.notification-footer {
  padding: 16px 0;
  text-align: center;
  border-top: 1px solid var(--el-border-color-lighter);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>