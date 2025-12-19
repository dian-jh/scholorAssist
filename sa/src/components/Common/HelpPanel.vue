<template>
  <el-drawer
    v-model="visible"
    title="帮助中心"
    direction="rtl"
    size="400px"
    class="help-drawer"
  >
    <div class="help-content">
      <div class="help-section">
        <h3>快速入门</h3>
        <div class="help-items">
          <div class="help-item" @click="openGuide('basic')">
            <el-icon class="item-icon" color="#409EFF"><Reading /></el-icon>
            <div class="item-content">
              <div class="item-title">基础使用指南</div>
              <div class="item-desc">了解系统基本功能和操作流程</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
          
          <div class="help-item" @click="openGuide('upload')">
            <el-icon class="item-icon" color="#67C23A"><Upload /></el-icon>
            <div class="item-content">
              <div class="item-title">文档上传</div>
              <div class="item-desc">如何上传和管理PDF文档</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
          
          <div class="help-item" @click="openGuide('ai')">
            <el-icon class="item-icon" color="#E6A23C"><ChatDotRound /></el-icon>
            <div class="item-content">
              <div class="item-title">AI问答功能</div>
              <div class="item-desc">使用AI助手进行文档问答</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
      
      <div class="help-section">
        <h3>视频教程</h3>
        <div class="help-items">
          <div class="help-item" @click="openVideo('intro')">
            <el-icon class="item-icon" color="#F56C6C"><VideoPlay /></el-icon>
            <div class="item-content">
              <div class="item-title">系统介绍</div>
              <div class="item-desc">5分钟了解系统核心功能</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
          
          <div class="help-item" @click="openVideo('advanced')">
            <el-icon class="item-icon" color="#909399"><VideoPlay /></el-icon>
            <div class="item-content">
              <div class="item-title">高级功能</div>
              <div class="item-desc">深度使用技巧和最佳实践</div>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
      
      <div class="help-section">
        <h3>常见问题</h3>
        <el-collapse class="faq-collapse">
          <el-collapse-item title="如何提高文档解析准确率？" name="1">
            <div class="faq-content">
              <p>1. 确保PDF文档清晰，避免扫描件</p>
              <p>2. 文档格式规范，包含完整的文本信息</p>
              <p>3. 文件大小建议在50MB以内</p>
            </div>
          </el-collapse-item>
          
          <el-collapse-item title="AI问答不准确怎么办？" name="2">
            <div class="faq-content">
              <p>1. 尝试更具体和清晰的问题描述</p>
              <p>2. 确保问题与文档内容相关</p>
              <p>3. 可以引用具体的页码或段落</p>
            </div>
          </el-collapse-item>
          
          <el-collapse-item title="如何导出笔记和标注？" name="3">
            <div class="faq-content">
              <p>1. 在笔记管理页面选择要导出的笔记</p>
              <p>2. 点击导出按钮，选择格式（PDF/Word/Markdown）</p>
              <p>3. 系统会自动生成并下载文件</p>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
      
      <div class="help-section">
        <h3>联系我们</h3>
        <div class="contact-info">
          <div class="contact-item">
            <el-icon><Message /></el-icon>
            <span>support@example.com</span>
          </div>
          <div class="contact-item">
            <el-icon><Phone /></el-icon>
            <span>400-123-4567</span>
          </div>
          <div class="contact-item">
            <el-icon><ChatDotRound /></el-icon>
            <span>在线客服（工作日 9:00-18:00）</span>
          </div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <div class="help-footer">
        <el-button type="primary" @click="openFeedback">
          意见反馈
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { 
  Reading, Upload, ChatDotRound, VideoPlay, ArrowRight, 
  Message, Phone 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface Props {
  visible: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const openGuide = (type: string) => {
  ElMessage.info(`打开${type}指南`)
}

const openVideo = (type: string) => {
  ElMessage.info(`播放${type}视频`)
}

const openFeedback = () => {
  ElMessage.info('打开意见反馈')
}
</script>

<style lang="scss" scoped>
.help-content {
  padding-bottom: 20px;
}

.help-section {
  margin-bottom: 32px;
  
  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
}

.help-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.help-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    border-color: var(--el-color-primary);
    background-color: var(--el-color-primary-light-9);
  }
}

.item-icon {
  font-size: 20px;
}

.item-content {
  flex: 1;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.item-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.arrow-icon {
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}

.faq-collapse {
  border: none;
  
  :deep(.el-collapse-item__header) {
    background-color: transparent;
    border-bottom: 1px solid var(--el-border-color-lighter);
    font-size: 14px;
    font-weight: 500;
  }
  
  :deep(.el-collapse-item__content) {
    padding-bottom: 16px;
  }
}

.faq-content {
  p {
    margin: 8px 0;
    font-size: 13px;
    color: var(--el-text-color-regular);
    line-height: 1.5;
  }
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--el-text-color-regular);
  
  .el-icon {
    color: var(--el-color-primary);
  }
}

.help-footer {
  padding: 16px 0;
  text-align: center;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>