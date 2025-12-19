<template>
  <div class="ai-chat-panel">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="ai-info">
        <div class="ai-avatar">
          <el-icon><Avatar /></el-icon>
        </div>
        <div class="ai-details">
          <h4>AI助手</h4>
          <span class="ai-status" :class="{ 'online': isOnline }">
            {{ isOnline ? '在线' : '离线' }}
          </span>
        </div>
      </div>
      
      <div class="chat-actions">
        <el-dropdown @command="handleChatAction">
          <el-button size="small" text>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="clear">清空对话</el-dropdown-item>
              <el-dropdown-item command="export">导出对话</el-dropdown-item>
              <el-dropdown-item command="settings" divided>设置</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 聊天消息区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <div class="messages-list">
        <!-- 欢迎消息 -->
        <div class="message-item ai-message" v-if="messages.length === 0">
          <div class="message-avatar">
            <el-icon><Avatar /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <p>你好！我是AI助手，可以帮助你理解和分析PDF文档内容。</p>
              <p>你可以：</p>
              <ul>
                <li>询问文档中的具体内容</li>
                <li>请我总结某个章节</li>
                <li>解释复杂的概念</li>
                <li>生成学习笔记</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div 
          v-for="message in messages"
          :key="message.id"
          class="message-item"
          :class="{ 'user-message': message.type === 'user', 'ai-message': message.type === 'ai' }"
        >
          <div class="message-avatar" v-if="message.type === 'ai'">
            <el-icon><Avatar /></el-icon>
          </div>
          
          <div class="message-content">
            <div class="message-bubble" :class="message.type">
              <!-- 用户消息 -->
              <div v-if="message.type === 'user'" class="user-text">
                {{ message.content }}
              </div>
              
              <!-- AI消息 -->
              <div v-else class="ai-text">
                <div v-if="message.isTyping" class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div v-else v-html="formatAiMessage(message.content)"></div>
              </div>
              
              <!-- 消息操作 -->
              <div class="message-actions" v-if="!message.isTyping">
                <el-button 
                  size="small" 
                  text 
                  @click="copyMessage(message.content)"
                  title="复制"
                >
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
                <el-button 
                  v-if="message.type === 'ai'"
                  size="small" 
                  text 
                  @click="regenerateResponse(message)"
                  title="重新生成"
                >
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </div>
            
            <div class="message-time">
              {{ formatTime(message.timestamp) }}
            </div>
          </div>
          
          <div class="message-avatar" v-if="message.type === 'user'">
            <el-icon><User /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 快速操作按钮 -->
    <div class="quick-actions" v-if="document && messages.length === 0">
      <div class="action-title">快速开始</div>
      <div class="action-buttons">
        <el-button 
          v-for="action in quickActions"
          :key="action.key"
          size="small"
          @click="sendQuickMessage(action.message)"
        >
          <el-icon :class="action.icon"></el-icon>
          {{ action.label }}
        </el-button>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <div class="input-container">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="inputRows"
          placeholder="输入你的问题..."
          resize="none"
          @keydown="handleKeydown"
          @input="handleInput"
          :disabled="isLoading"
        />
        
        <div class="input-actions">
          <el-button 
            type="primary" 
            :loading="isLoading"
            :disabled="!inputMessage.trim()"
            @click="sendMessage"
          >
            <el-icon v-if="!isLoading"><Promotion /></el-icon>
            {{ isLoading ? '发送中...' : '发送' }}
          </el-button>
        </div>
      </div>
      
      <div class="input-footer">
        <span class="input-hint">
          按 Shift + Enter 换行，Enter 发送
        </span>
        <span class="character-count" :class="{ 'warning': inputMessage.length > 1000 }">
          {{ inputMessage.length }}/1500
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Avatar, 
  User, 
  MoreFilled, 
  CopyDocument, 
  Refresh, 
  Promotion,
  Document,
  QuestionFilled,
  EditPen,
  Collection
} from '@element-plus/icons-vue'

import type { MockDocument } from '@/api/mockManager'

// Props
interface Props {
  document?: MockDocument
  currentPage?: number
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  messageSend: [message: string]
}>()

// 消息类型定义
interface ChatMessage {
  id: string
  type: 'user' | 'ai'
  content: string
  timestamp: Date
  isTyping?: boolean
}

// 响应式数据
const messagesContainer = ref<HTMLElement>()
const inputMessage = ref('')
const messages = ref<ChatMessage[]>([])
const isLoading = ref(false)
const isOnline = ref(true)

// 快速操作配置
const quickActions = [
  {
    key: 'summarize',
    label: '总结文档',
    icon: 'Document',
    message: '请帮我总结这个PDF文档的主要内容'
  },
  {
    key: 'explain',
    label: '解释概念',
    icon: 'QuestionFilled',
    message: '请解释文档中的重要概念'
  },
  {
    key: 'notes',
    label: '生成笔记',
    icon: 'EditPen',
    message: '请帮我生成这个文档的学习笔记'
  },
  {
    key: 'questions',
    label: '提出问题',
    icon: 'Collection',
    message: '基于文档内容，请提出一些思考问题'
  }
]

// 计算属性
const inputRows = computed(() => {
  const lines = inputMessage.value.split('\n').length
  return Math.min(Math.max(lines, 1), 4)
})

// 方法
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  // 添加用户消息
  const userMessage: ChatMessage = {
    id: Date.now().toString(),
    type: 'user',
    content,
    timestamp: new Date()
  }
  messages.value.push(userMessage)

  // 清空输入
  inputMessage.value = ''
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 发送给父组件
  emit('messageSend', content)

  // 模拟AI回复
  await simulateAiResponse(content)
}

const simulateAiResponse = async (userMessage: string) => {
  isLoading.value = true

  // 添加AI消息（打字效果）
  const aiMessage: ChatMessage = {
    id: (Date.now() + 1).toString(),
    type: 'ai',
    content: '',
    timestamp: new Date(),
    isTyping: true
  }
  messages.value.push(aiMessage)

  await nextTick()
  scrollToBottom()

  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 1000))

  // 生成AI回复内容
  const response = generateAiResponse(userMessage)
  
  // 移除打字效果，显示内容
  aiMessage.isTyping = false
  aiMessage.content = response

  isLoading.value = false
  
  await nextTick()
  scrollToBottom()
}

const generateAiResponse = (userMessage: string): string => {
  const lowerMessage = userMessage.toLowerCase()
  
  if (lowerMessage.includes('总结') || lowerMessage.includes('概括')) {
    return `基于您的PDF文档，我为您总结如下要点：

**主要内容：**
1. 文档介绍了相关的理论基础和核心概念
2. 详细阐述了实施方法和步骤
3. 提供了实际应用案例和分析

**关键观点：**
- 理论与实践相结合的重要性
- 系统性思维在解决问题中的作用
- 持续学习和改进的必要性

您希望我进一步解释其中的某个部分吗？`
  }
  
  if (lowerMessage.includes('解释') || lowerMessage.includes('什么是')) {
    return `我很乐意为您解释文档中的概念。

根据文档内容，这个概念可以从以下几个方面来理解：

**定义：** 这是一个重要的理论概念，在该领域中具有基础性作用。

**特点：**
- 具有系统性和完整性
- 与其他概念存在密切联系
- 在实际应用中具有指导意义

**应用场景：**
- 理论研究中的基础框架
- 实践操作的指导原则
- 问题分析的思维工具

您想了解这个概念的哪个具体方面呢？`
  }
  
  if (lowerMessage.includes('笔记') || lowerMessage.includes('记录')) {
    return `我为您整理了以下学习笔记：

## 📚 文档学习笔记

### 🎯 核心要点
- **要点1：** 理论基础的重要性
- **要点2：** 实践方法的系统性
- **要点3：** 应用场景的多样性

### 💡 重要概念
1. **概念A：** 基础理论框架
2. **概念B：** 实施方法论
3. **概念C：** 评估标准体系

### 🔍 思考问题
- 如何将理论应用到实际场景中？
- 不同方法之间有什么联系和区别？
- 如何评估实施效果？

### 📝 行动建议
- 深入理解核心概念
- 结合实际案例练习
- 定期回顾和总结

这些笔记对您有帮助吗？需要我补充其他内容吗？`
  }
  
  return `感谢您的问题！基于您的PDF文档内容，我认为这是一个很好的问题。

让我从以下几个角度来回答：

**分析：** 根据文档中的相关内容，这个问题涉及到多个重要方面。

**建议：** 我建议您可以从理论和实践两个维度来思考这个问题。

**补充：** 如果您需要更详细的解释或者有其他相关问题，请随时告诉我。

您还有什么想了解的吗？`
}

const sendQuickMessage = (message: string) => {
  inputMessage.value = message
  sendMessage()
}

const handleKeydown = (event: Event) => {
  const keyboardEvent = event as KeyboardEvent
  if (keyboardEvent.key === 'Enter') {
    if (keyboardEvent.shiftKey) {
      // Shift + Enter 换行
      return
    } else {
      // Enter 发送
      keyboardEvent.preventDefault()
      sendMessage()
    }
  }
}

const handleInput = () => {
  // 限制字符数
  if (inputMessage.value.length > 1500) {
    inputMessage.value = inputMessage.value.slice(0, 1500)
    ElMessage.warning('消息长度不能超过1500字符')
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const regenerateResponse = async (message: ChatMessage) => {
  const index = messages.value.findIndex(m => m.id === message.id)
  if (index > 0) {
    const userMessage = messages.value[index - 1]
    if (userMessage.type === 'user') {
      // 移除当前AI回复
      messages.value.splice(index, 1)
      // 重新生成回复
      await simulateAiResponse(userMessage.content)
    }
  }
}

const handleChatAction = (command: string) => {
  switch (command) {
    case 'clear':
      ElMessageBox.confirm('确定要清空所有对话记录吗？', '确认清空', {
        type: 'warning'
      }).then(() => {
        messages.value = []
        ElMessage.success('对话记录已清空')
      }).catch(() => {
        // 用户取消
      })
      break
    case 'export':
      exportChat()
      break
    case 'settings':
      ElMessage.info('设置功能开发中...')
      break
  }
}

const exportChat = () => {
  const chatContent = messages.value.map(msg => {
    const time = formatTime(msg.timestamp)
    const sender = msg.type === 'user' ? '用户' : 'AI助手'
    return `[${time}] ${sender}: ${msg.content}`
  }).join('\n\n')
  
  const blob = new Blob([chatContent], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `AI对话记录_${new Date().toLocaleDateString()}.txt`
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('对话记录已导出')
}

const formatAiMessage = (content: string) => {
  // 简单的Markdown渲染
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

// 监听器
watch(() => props.document, (newDoc) => {
  if (newDoc) {
    // 文档变化时可以发送欢迎消息
    isOnline.value = true
  }
})

// 生命周期
onMounted(() => {
  // 模拟连接状态
  setTimeout(() => {
    isOnline.value = true
  }, 1000)
})
</script>

<style lang="scss" scoped>
.ai-chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #fafafa, #f5f5f5);
  position: relative;
  z-index: 10;
  
  .ai-info {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .ai-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: linear-gradient(135deg, #10a37f, #1a7f64);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ffffff;
      font-size: 18px;
      box-shadow: 0 4px 12px rgba(16, 163, 127, 0.3);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: -100%;
        width: 100%;
        height: 100%;
        background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
        transition: left 0.5s;
      }

      &:hover {
        transform: scale(1.05);
        box-shadow: 0 6px 20px rgba(16, 163, 127, 0.4);

        &::before {
          left: 100%;
        }
      }
    }
    
    .ai-details {
      h4 {
        margin: 0 0 4px 0;
        font-size: 16px;
        color: #374151;
        font-weight: 600;
        transition: color 0.2s ease;
      }
      
      .ai-status {
        font-size: 12px;
        color: #9ca3af;
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: 500;
        
        &.online {
          color: #10a37f;
        }
        
        &::before {
          content: '';
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #9ca3af;
          animation: pulse 2s infinite;
        }
        
        &.online::before {
          background: #10a37f;
        }
      }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
  background: #f8f9fa;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: linear-gradient(135deg, #c1c1c1, #a1a1a1);
    border-radius: 3px;
    transition: background 0.3s ease;

    &:hover {
      background: linear-gradient(135deg, #a1a1a1, #888);
    }
  }
}

.messages-list {
  padding: 0 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: messageSlideIn 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  
  &.user-message {
    flex-direction: row-reverse;
    
    .message-content {
      align-items: flex-end;
    }
    
    .message-bubble {
      background: linear-gradient(135deg, #10a37f, #0d8f6b);
      color: #ffffff;
      border-radius: 18px 18px 4px 18px;
      box-shadow: 0 4px 12px rgba(16, 163, 127, 0.3);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        box-shadow: 0 6px 20px rgba(16, 163, 127, 0.4);
        transform: translateY(-1px);
      }
      
      .message-actions {
        .el-button {
          color: rgba(255, 255, 255, 0.8);
          border-color: rgba(255, 255, 255, 0.3);
          
          &:hover {
            color: #ffffff;
            background: rgba(255, 255, 255, 0.1);
            border-color: rgba(255, 255, 255, 0.5);
            transform: translateY(-1px);
          }
        }
      }
    }
  }
  
  &.ai-message {
    .message-bubble {
      background: white;
      color: #374151;
      border: 1px solid #e5e7eb;
      border-radius: 18px 18px 18px 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        transform: translateY(-1px);
      }
    }
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
    transition: left 0.5s;
  }

  &:hover {
    transform: scale(1.1);

    &::before {
      left: 100%;
    }
  }
  
  .ai-message & {
    background: linear-gradient(135deg, #10a37f, #1a7f64);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(16, 163, 127, 0.3);
  }
  
  .user-message & {
    background: linear-gradient(135deg, #3b82f6, #1d4ed8);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  }
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: calc(100% - 44px);
}

.message-bubble {
  position: relative;
  padding: 12px 16px;
  border-radius: 18px;
  max-width: 100%;
  word-wrap: break-word;
  
  .user-text,
  .ai-text {
    line-height: 1.5;
    
    :deep(strong) {
      font-weight: 600;
    }
    
    :deep(em) {
      font-style: italic;
    }
    
    :deep(code) {
      background: rgba(0, 0, 0, 0.1);
      padding: 2px 4px;
      border-radius: 4px;
      font-family: 'Courier New', monospace;
      font-size: 0.9em;
    }
    
    :deep(ul) {
      margin: 8px 0;
      padding-left: 20px;
    }
    
    :deep(li) {
      margin: 4px 0;
    }
  }
  
  .typing-indicator {
    display: flex;
    gap: 4px;
    align-items: center;
    
    span {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #10a37f;
      animation: typingBounce 1.4s infinite ease-in-out;
      
      &:nth-child(1) { animation-delay: -0.32s; }
      &:nth-child(2) { animation-delay: -0.16s; }
      &:nth-child(3) { animation-delay: 0s; }
    }
  }
  
  .message-actions {
    display: flex;
    gap: 8px;
    margin-top: 8px;
    opacity: 0;
    transition: all 0.3s ease;
    transform: translateY(5px);
    
    .el-button {
      padding: 4px 8px;
      min-height: auto;
      font-size: 11px;
      border-radius: 12px;
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-1px);
      }
    }
  }
  
  &:hover .message-actions {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-time {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
  padding: 0 4px;
  transition: opacity 0.2s ease;
}

.quick-actions {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: white;
  
  .action-title {
    font-size: 14px;
    font-weight: 500;
    color: #6b7280;
    margin-bottom: 12px;
  }
  
  .action-buttons {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    
    .el-button {
      justify-content: flex-start;
      padding: 8px 12px;
      height: auto;
      border-color: #e5e7eb;
      border-radius: 12px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: -100%;
        width: 100%;
        height: 100%;
        background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
        transition: left 0.5s;
      }
      
      &:hover {
        border-color: #10a37f;
        color: #10a37f;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(16, 163, 127, 0.2);

        &::before {
          left: 100%;
        }
      }

      &:active {
        transform: translateY(-1px);
      }
    }
  }
}

.chat-input {
  border-top: 1px solid #f0f0f0;
  background: #ffffff;
  
  .input-container {
    padding: 16px 20px 8px 20px;
    
    :deep(.el-textarea) {
      .el-textarea__inner {
        border: 1px solid #e5e7eb;
        border-radius: 12px;
        padding: 12px 16px;
        resize: none;
        font-size: 14px;
        line-height: 1.5;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        
        &:hover {
          border-color: #d1d5db;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        }
        
        &:focus {
          border-color: #10a37f;
          box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.1);
          background: white;
        }

        &::placeholder {
          color: #9ca3af;
          transition: color 0.2s ease;
        }

        &:focus::placeholder {
          color: #d1d5db;
        }
      }
    }
    
    .input-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 8px;
      
      .el-button {
        border-radius: 8px;
        padding: 8px 16px;
        background: linear-gradient(135deg, #10a37f, #0d8f6b);
        border-color: #10a37f;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        position: relative;
        overflow: hidden;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
          transition: left 0.5s;
        }

        &:hover:not(:disabled) {
          background: linear-gradient(135deg, #0d8f6b, #0a7c5a);
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(16, 163, 127, 0.4);

          &::before {
            left: 100%;
          }
        }

        &:active {
          transform: translateY(0);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
          transform: none;
          box-shadow: none;

          &:hover {
            background: linear-gradient(135deg, #10a37f, #0d8f6b);
          }
        }
      }
    }
  }
  
  .input-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px 12px 20px;
    
    .input-hint {
      font-size: 12px;
      color: #9ca3af;
      display: flex;
      align-items: center;
      gap: 12px;

      .shortcut-key {
        background: #f3f4f6;
        border: 1px solid #d1d5db;
        border-radius: 4px;
        padding: 2px 6px;
        font-size: 10px;
        font-weight: 500;
        color: #374151;
      }
    }
    
    .character-count {
      font-size: 12px;
      color: #9ca3af;
      transition: color 0.2s ease;
      
      &.warning {
        color: #f59e0b;
      }

      &.error {
        color: #ef4444;
      }
    }
  }
}

// 动画关键帧
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes typingBounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

// 移动端适配
@media (max-width: 768px) {
  .chat-header {
    padding: 12px 16px;

    .ai-info {
      gap: 8px;

      .ai-avatar {
        width: 32px;
        height: 32px;
        font-size: 16px;
      }

      .ai-details {
        h4 {
          font-size: 14px;
        }

        .ai-status {
          font-size: 11px;

          &::before {
            width: 6px;
            height: 6px;
          }
        }
      }
    }
  }

  .messages-list {
    padding: 0 16px;
  }

  .message-item {
    gap: 8px;
    margin-bottom: 16px;

    .message-avatar {
      width: 28px;
      height: 28px;
      font-size: 12px;
    }

    .message-content {
      max-width: calc(100% - 36px);
    }

    .message-bubble {
      padding: 10px 12px;
      font-size: 13px;

      &.user-message {
        border-radius: 16px 16px 4px 16px;
      }

      &.ai-message {
        border-radius: 16px 16px 16px 4px;
      }

      .message-actions {
        gap: 6px;
        margin-top: 6px;

        .el-button {
          padding: 3px 6px;
          font-size: 10px;
        }
      }

      .typing-indicator {
        span {
          width: 6px;
          height: 6px;
        }
      }
    }

    .message-time {
      font-size: 10px;
    }
  }

  .quick-actions {
    padding: 12px 16px;

    .action-title {
      font-size: 13px;
      margin-bottom: 8px;
    }

    .action-buttons {
      gap: 6px;

      .el-button {
        padding: 6px 10px;
        font-size: 12px;
      }
    }
  }

  .chat-input {
    .input-container {
      padding: 12px 16px 6px 16px;

      :deep(.el-textarea) {
        .el-textarea__inner {
          padding: 10px 12px;
          font-size: 13px;
          border-radius: 10px;
        }
      }

      .input-actions {
        margin-top: 6px;

        .el-button {
          padding: 6px 12px;
          font-size: 13px;
        }
      }
    }

    .input-footer {
      padding: 0 16px 8px 16px;
      font-size: 11px;

      .input-hint {
        gap: 8px;

        .shortcut-key {
          padding: 1px 4px;
          font-size: 9px;
        }
      }
    }
  }
}

// 平板适配
@media (max-width: 1024px) and (min-width: 769px) {
  .chat-header {
    padding: 14px 18px;

    .ai-info {
      gap: 10px;

      .ai-avatar {
        width: 34px;
        height: 34px;
        font-size: 17px;
      }

      .ai-details {
        h4 {
          font-size: 15px;
        }
      }
    }
  }

  .messages-list {
    padding: 0 18px;
  }

  .message-item {
    gap: 10px;
    margin-bottom: 20px;

    .message-avatar {
      width: 30px;
      height: 30px;
      font-size: 13px;
    }

    .message-content {
      max-width: calc(100% - 40px);
    }

    .message-bubble {
      padding: 11px 14px;
    }
  }

  .quick-actions {
    padding: 14px 18px;

    .action-buttons {
      gap: 7px;

      .el-button {
        padding: 7px 11px;
      }
    }
  }

  .chat-input {
    .input-container {
      padding: 14px 18px 7px 18px;

      :deep(.el-textarea) {
        .el-textarea__inner {
          padding: 11px 14px;
        }
      }

      .input-actions {
        margin-top: 7px;

        .el-button {
          padding: 7px 14px;
        }
      }
    }

    .input-footer {
      padding: 0 18px 10px 18px;
    }
  }
}

// 横屏模式适配
@media (orientation: landscape) and (max-height: 600px) {
  .chat-header {
    padding: 8px 16px;

    .ai-info {
      .ai-avatar {
        width: 28px;
        height: 28px;
        font-size: 14px;
      }

      .ai-details {
        h4 {
          font-size: 13px;
        }

        .ai-status {
          font-size: 10px;
        }
      }
    }
  }

  .messages-list {
    padding: 0 16px;
  }

  .message-item {
    margin-bottom: 12px;
  }

  .quick-actions {
    padding: 8px 16px;

    .action-title {
      margin-bottom: 6px;
    }
  }

  .chat-input {
    .input-container {
      padding: 8px 16px 4px 16px;

      :deep(.el-textarea) {
        .el-textarea__inner {
          padding: 8px 12px;
        }
      }

      .input-actions {
        margin-top: 4px;

        .el-button {
          padding: 6px 12px;
        }
      }
    }

    .input-footer {
      padding: 0 16px 6px 16px;
    }
  }
}
</style>