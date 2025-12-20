<template>
  <div class="ai-chat-panel">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="ai-info">
        <div class="ai-avatar">
          <el-icon><Avatar /></el-icon>
        </div>
        <div class="ai-details">
          <h4>AI 助手</h4>
          <span class="ai-status online">在线</span>
        </div>
      </div>
      
      <div class="chat-actions">
        <el-tooltip content="历史记录">
          <el-button size="small" text @click="toggleHistory">
            <el-icon><Timer /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="新对话">
          <el-button size="small" text @click="startNewChat">
            <el-icon><Plus /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 历史记录抽屉 -->
    <el-drawer
      v-model="showHistory"
      title="对话历史"
      direction="rtl"
      size="80%"
      :append-to-body="false"
      class="history-drawer"
    >
      <div v-loading="historyLoading" class="history-list">
        <div v-if="historyList.length === 0" class="empty-history">
          暂无历史记录
        </div>
        <div
          v-for="id in historyList"
          :key="id"
          class="history-item"
          :class="{ active: id === currentChatId }"
          @click="loadChatSession(id)"
        >
          <div class="history-title">会话 {{ id.slice(0, 8) }}...</div>
          <el-button 
            type="danger" 
            link 
            size="small" 
            @click.stop="handleDeleteChat(id)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </el-drawer>

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
              <p>你好！我是你的文献阅读助手。</p>
              <p>你可以：</p>
              <ul>
                <li>询问这篇文档的核心观点</li>
                <li>解释复杂的概念或公式</li>
                <li>总结特定章节的内容</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div 
          v-for="message in messages"
          :key="message.id"
          class="message-item"
          :class="{ 'user-message': message.role === 'user', 'ai-message': message.role === 'assistant' }"
        >
          <div class="message-avatar" v-if="message.role === 'assistant'">
            <el-icon><Avatar /></el-icon>
          </div>
          
          <div class="message-content">
            <div class="message-bubble" :class="message.role">
              <!-- 用户消息 -->
              <div v-if="message.role === 'user'" class="user-text">
                {{ message.content }}
              </div>
              
              <!-- AI消息 -->
              <div v-else class="ai-text">
                <div v-if="message.isTyping && !message.content" class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <!-- Markdown 渲染 -->
                <div v-else class="markdown-body" v-html="renderMarkdown(message.content)"></div>
                
                <div v-if="message.isError" class="error-text">
                  <el-icon><Warning /></el-icon> 生成出错，请重试
                </div>
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
              </div>
            </div>
          </div>
          
          <div class="message-avatar" v-if="message.role === 'user'">
            <el-icon><User /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <div class="input-container">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          placeholder="输入你的问题..."
          resize="none"
          @keydown.enter.prevent="handleKeydown"
          :disabled="isLoading"
        />
        
        <div class="input-actions">
          <el-button 
            v-if="isLoading"
            type="danger" 
            circle
            @click="stopGeneration"
            title="停止生成"
          >
            <el-icon><VideoPause /></el-icon>
          </el-button>
          <el-button 
            v-else
            type="primary" 
            :disabled="!inputMessage.trim()"
            @click="handleSendMessage"
          >
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
      
      <div class="input-footer">
        <span class="input-hint">Enter 发送，Shift + Enter 换行</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Avatar, 
  User, 
  CopyDocument, 
  Promotion, 
  Timer, 
  Plus, 
  Delete, 
  VideoPause, 
  Warning
} from '@element-plus/icons-vue'
import { v4 as uuidv4 } from 'uuid'
import MarkdownIt from 'markdown-it'
import { AiStreamingService } from '@/api/AiStreamingService'
import { getDocumentChatHistory, getChatDetails, deleteChat } from '@/api/AiApi'
import type { ChatMessage } from '@/types/ai'

// Props
interface Props {
  documentId?: string
}

const props = defineProps<Props>()

// Markdown parser
const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
})

// State
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const currentChatId = ref<string>('')
const messagesContainer = ref<HTMLElement | null>(null)
const abortController = ref<AbortController | null>(null)

// History State
const showHistory = ref(false)
const historyList = ref<string[]>([])
const historyLoading = ref(false)

// Lifecycle
onMounted(() => {
  if (props.documentId) {
    loadHistoryList()
  }
})

// Watch document change
watch(() => props.documentId, (newId) => {
  if (newId) {
    resetChat()
    loadHistoryList()
  }
})

// Methods
const renderMarkdown = (content: string) => {
  return md.render(content || '')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const startNewChat = () => {
  resetChat()
}

const resetChat = () => {
  currentChatId.value = ''
  messages.value = []
  inputMessage.value = ''
  isLoading.value = false
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
}

const toggleHistory = () => {
  showHistory.value = !showHistory.value
  if (showHistory.value && props.documentId) {
    loadHistoryList()
  }
}

const loadHistoryList = async () => {
  if (!props.documentId) return
  try {
    historyLoading.value = true
    const res = await getDocumentChatHistory(props.documentId)
    if (res.code === 200) {
      historyList.value = res.data
    }
  } catch (error) {
    console.error('Failed to load history', error)
  } finally {
    historyLoading.value = false
  }
}

const loadChatSession = async (chatId: string) => {
  if (!props.documentId) return
  if (isLoading.value) return // Don't switch while generating
  
  try {
    const res = await getChatDetails(props.documentId, chatId)
    if (res.code === 200) {
      currentChatId.value = chatId
      messages.value = res.data.map(msg => ({
        id: uuidv4(), // Generate temp ID for view
        role: msg.role === 'assistant' ? 'assistant' : 'user', // Normalize role
        content: msg.content,
        timestamp: Date.now()
      } as ChatMessage))
      showHistory.value = false
      scrollToBottom()
    }
  } catch (error) {
    ElMessage.error('加载会话失败')
  }
}

const handleDeleteChat = async (chatId: string) => {
  if (!props.documentId) return
  try {
    await ElMessageBox.confirm('确定要删除这条对话记录吗？', '提示', {
      type: 'warning'
    })
    
    const res = await deleteChat(props.documentId, chatId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadHistoryList()
      if (currentChatId.value === chatId) {
        resetChat()
      }
    }
  } catch (e) {
    // Cancelled or failed
  }
}

const handleKeydown = (e: Event) => {
  const ke = e as KeyboardEvent
  if (ke.shiftKey) return
  handleSendMessage()
}

const stopGeneration = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
    isLoading.value = false
    // Mark last message as not typing
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'assistant') {
      lastMsg.isTyping = false
    }
  }
}

const handleSendMessage = async () => {
  const text = inputMessage.value.trim()
  if (!text || isLoading.value || !props.documentId) return

  // 1. Generate chatId if new
  if (!currentChatId.value) {
    currentChatId.value = uuidv4()
    // Refresh history list later to show new chat
    setTimeout(loadHistoryList, 1000) 
  }

  // 2. Add user message
  const userMsg: ChatMessage = {
    id: uuidv4(),
    role: 'user',
    content: text,
    timestamp: Date.now()
  }
  messages.value.push(userMsg)
  inputMessage.value = ''
  scrollToBottom()

  // 3. Add placeholder AI message
  const aiMsgId = uuidv4()
  const aiMsg: ChatMessage = {
    id: aiMsgId,
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    isTyping: true
  }
  messages.value.push(aiMsg)
  isLoading.value = true
  scrollToBottom()

  // 4. Start streaming
  abortController.value = new AbortController()

  await AiStreamingService.sendMessage({
    chatId: currentChatId.value,
    documentId: props.documentId,
    prompt: text
  }, {
    signal: abortController.value.signal,
    onMessage: (chunk) => {
      // Find the message and append content
      const msg = messages.value.find(m => m.id === aiMsgId)
      if (msg) {
        msg.content += chunk
        scrollToBottom()
      }
    },
    onError: (err) => {
      const msg = messages.value.find(m => m.id === aiMsgId)
      if (msg) {
        msg.isError = true
        msg.isTyping = false
        msg.content += '\n\n[网络错误或请求失败]'
      }
      isLoading.value = false
      abortController.value = null
    },
    onComplete: () => {
      const msg = messages.value.find(m => m.id === aiMsgId)
      if (msg) {
        msg.isTyping = false
      }
      isLoading.value = false
      abortController.value = null
    }
  })
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped lang="scss">
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #fff;
  border-left: 1px solid var(--el-border-color-light);
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;

  .ai-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .ai-avatar {
      background-color: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      width: 40px;
      height: 40px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .ai-details {
      h4 {
        margin: 0;
        font-size: 16px;
        color: var(--el-text-color-primary);
      }

      .ai-status {
        font-size: 12px;
        display: flex;
        align-items: center;
        gap: 4px;

        &::before {
          content: "";
          display: block;
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background-color: #909399;
        }

        &.online {
          color: #67c23a;
          &::before {
            background-color: #67c23a;
          }
        }
      }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f9fafe;

  .messages-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 100%;

  &.user-message {
    flex-direction: row-reverse;
    
    .message-bubble {
      background-color: var(--el-color-primary);
      color: #fff;
      border-bottom-right-radius: 4px;
    }
  }

  &.ai-message {
    .message-bubble {
      background-color: #fff;
      border: 1px solid var(--el-border-color-light);
      border-bottom-left-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    }
  }

  .message-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background-color: #fff;
    border: 1px solid var(--el-border-color-light);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--el-text-color-regular);
    flex-shrink: 0;
  }

  .message-content {
    max-width: 80%;
    min-width: 100px;
  }

  .message-bubble {
    padding: 12px 16px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.6;
    position: relative;
    word-break: break-word;

    .markdown-body {
      /* Reset some default markdown styles for chat bubble */
      p { margin-bottom: 8px; }
      p:last-child { margin-bottom: 0; }
      pre { 
        background: #f4f4f5; 
        padding: 8px; 
        border-radius: 4px; 
        overflow-x: auto;
      }
    }
  }

  .message-actions {
    position: absolute;
    bottom: -24px;
    right: 0;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .message-actions {
    opacity: 1;
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  span {
    width: 6px;
    height: 6px;
    background-color: var(--el-text-color-secondary);
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-input {
  padding: 16px;
  background-color: #fff;
  border-top: 1px solid var(--el-border-color-light);

  .input-container {
    position: relative;
    border: 1px solid var(--el-border-color);
    border-radius: 8px;
    background-color: #fff;
    transition: all 0.3s;
    padding-right: 50px;

    &:focus-within {
      border-color: var(--el-color-primary);
      box-shadow: 0 0 0 1px var(--el-color-primary-light-8);
    }

    :deep(.el-textarea__inner) {
      border: none;
      box-shadow: none;
      padding: 12px;
      padding-right: 12px;
      max-height: 150px;
    }

    .input-actions {
      position: absolute;
      bottom: 8px;
      right: 8px;
    }
  }

  .input-footer {
    margin-top: 8px;
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.history-list {
  padding: 10px;
}

.history-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  background-color: #f5f7fa;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;

  &:hover {
    background-color: #e6e8eb;
  }

  &.active {
    background-color: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }

  .history-title {
    font-size: 14px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.empty-history {
  text-align: center;
  color: #909399;
  margin-top: 40px;
  font-size: 14px;
}
</style>
