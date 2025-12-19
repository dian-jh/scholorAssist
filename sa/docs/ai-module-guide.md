# AI模块使用指南

## 📖 概述

本指南详细介绍了如何在项目中集成和使用AI模块，包括基础聊天功能、流式传输、错误处理等完整的使用示例。

## 🚀 快速开始

### 1. 基础配置

首先确保项目中已正确配置API基础URL和认证信息：

```typescript
// .env 文件
VITE_API_BASE_URL=http://localhost:3000/api

// 或在代码中动态配置
import { aiApi } from '@/api/AiApi'

// 设置默认配置
aiApi.setDefaultConfig({
  model: 'gpt-3.5-turbo',
  temperature: 0.7,
  max_tokens: 2048
})
```

### 2. 基础AI聊天

```typescript
import { sendAiChat } from '@/api/AiApi'
import type { AiChatRequest } from '@/api/AiApi'

async function basicChat() {
  try {
    const request: AiChatRequest = {
      message: '请介绍一下人工智能的发展历程',
      model: 'gpt-3.5-turbo',
      temperature: 0.7
    }

    const response = await sendAiChat(request)
    
    if (response.code === 200) {
      console.log('AI回复:', response.data.message)
      console.log('对话ID:', response.data.conversation_id)
      console.log('Token使用:', response.data.usage)
    }
  } catch (error) {
    console.error('聊天失败:', error)
  }
}
```

## 🔄 流式传输使用

### WebSocket流式聊天

```typescript
import { createWebSocketStream } from '@/api/AiStreamingService'
import { aiResponseHandler } from '@/api/AiResponseHandler'

function startWebSocketChat() {
  const wsStream = createWebSocketStream(
    {
      url: 'ws://localhost:3000/api/ai/chat/stream',
      reconnectAttempts: 3,
      reconnectDelay: 1000,
      heartbeatInterval: 30000
    },
    {
      onOpen: () => {
        console.log('WebSocket连接已建立')
        // 连接成功后发送消息
        wsStream.sendStreamRequest({
          message: '请详细解释深度学习的原理',
          model: 'gpt-4',
          stream: true
        })
      },
      
      onMessage: (chunk) => {
        // 提取流式内容
        const content = aiResponseHandler.extractStreamContent(chunk)
        if (content) {
          // 实时显示AI回复
          appendToChat(content)
        }
        
        // 检查是否完成
        if (aiResponseHandler.isStreamFinished(chunk)) {
          console.log('流式传输完成')
          wsStream.disconnect()
        }
      },
      
      onError: (error) => {
        console.error('WebSocket错误:', error)
        showErrorMessage('连接出现问题，请稍后重试')
      },
      
      onClose: (code, reason) => {
        console.log('连接已关闭:', code, reason)
      },
      
      onReconnect: (attempt) => {
        console.log(`正在进行第${attempt}次重连...`)
        showReconnectMessage(attempt)
      }
    }
  )

  // 建立连接
  wsStream.connect().catch(error => {
    console.error('连接失败:', error)
  })

  return wsStream
}

// 辅助函数
function appendToChat(content: string) {
  const chatContainer = document.getElementById('chat-messages')
  if (chatContainer) {
    chatContainer.innerHTML += content
  }
}

function showErrorMessage(message: string) {
  // 显示错误提示
  console.error(message)
}

function showReconnectMessage(attempt: number) {
  // 显示重连提示
  console.log(`重连中... (${attempt}/3)`)
}
```

### SSE流式聊天

```typescript
import { createSSEStream } from '@/api/AiStreamingService'

function startSSEChat() {
  const sseStream = createSSEStream(
    {
      url: 'http://localhost:3000/api/ai/chat/stream',
      reconnectAttempts: 3,
      reconnectDelay: 2000
    },
    {
      onOpen: () => {
        console.log('SSE连接已建立')
      },
      
      onMessage: (chunk) => {
        const content = aiResponseHandler.extractStreamContent(chunk)
        if (content) {
          appendToChat(content)
        }
      },
      
      onError: (error) => {
        console.error('SSE错误:', error)
      }
    }
  )

  // 建立连接
  sseStream.connect().then(() => {
    // 发送流式请求
    return sseStream.sendStreamRequest({
      message: '请解释量子计算的基本概念',
      stream: true
    })
  }).catch(error => {
    console.error('SSE聊天失败:', error)
  })

  return sseStream
}
```

## 🎯 Vue 3 组件集成示例

### 基础聊天组件

```vue
<template>
  <div class="ai-chat-container">
    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="message in messages" 
        :key="message.id"
        :class="['message', message.role]"
      >
        <div class="message-content">{{ message.content }}</div>
        <div class="message-meta">
          <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
          <span v-if="message.usage" class="token-usage">
            Tokens: {{ message.usage.total_tokens }}
          </span>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="3"
        placeholder="请输入您的问题..."
        :disabled="loading"
        @keydown.ctrl.enter="sendMessage"
      />
      <div class="input-actions">
        <el-select v-model="selectedModel" placeholder="选择模型">
          <el-option
            v-for="model in availableModels"
            :key="model"
            :label="model"
            :value="model"
          />
        </el-select>
        <el-button 
          type="primary" 
          :loading="loading"
          @click="sendMessage"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { sendAiChat, getAiModels } from '@/api/AiApi'
import type { AiChatRequest, AiChatResponse } from '@/api/AiApi'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  usage?: {
    prompt_tokens: number
    completion_tokens: number
    total_tokens: number
  }
}

// 响应式数据
const messages = ref<Message[]>([])
const inputMessage = ref('')
const loading = ref(false)
const selectedModel = ref('gpt-3.5-turbo')
const availableModels = ref<string[]>([])
const conversationId = ref<string>('')
const messagesContainer = ref<HTMLElement>()

// 组件挂载时获取可用模型
onMounted(async () => {
  try {
    const response = await getAiModels()
    availableModels.value = response.data
  } catch (error) {
    ElMessage.error('获取模型列表失败')
  }
})

// 发送消息
async function sendMessage() {
  if (!inputMessage.value.trim() || loading.value) return

  const userMessage: Message = {
    id: `user_${Date.now()}`,
    role: 'user',
    content: inputMessage.value,
    timestamp: new Date().toISOString()
  }

  messages.value.push(userMessage)
  const messageContent = inputMessage.value
  inputMessage.value = ''
  loading.value = true

  try {
    const request: AiChatRequest = {
      message: messageContent,
      model: selectedModel.value,
      conversation_id: conversationId.value || undefined,
      temperature: 0.7,
      max_tokens: 2048
    }

    const response = await sendAiChat(request)
    
    if (response.code === 200) {
      const aiMessage: Message = {
        id: `ai_${Date.now()}`,
        role: 'assistant',
        content: response.data.message,
        timestamp: new Date().toISOString(),
        usage: response.data.usage
      }

      messages.value.push(aiMessage)
      conversationId.value = response.data.conversation_id

      // 滚动到底部
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    ElMessage.error('发送消息失败，请重试')
    console.error('AI聊天错误:', error)
  } finally {
    loading.value = false
  }
}

// 滚动到底部
function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 格式化时间
function formatTime(timestamp: string): string {
  return new Date(timestamp).toLocaleTimeString()
}
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 600px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #f9f9f9;
}

.message {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  max-width: 80%;
}

.message.user {
  background-color: #007bff;
  color: white;
  margin-left: auto;
}

.message.assistant {
  background-color: white;
  border: 1px solid #e0e0e0;
}

.message-content {
  margin-bottom: 8px;
  line-height: 1.5;
}

.message-meta {
  font-size: 12px;
  opacity: 0.7;
  display: flex;
  justify-content: space-between;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
  background-color: white;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
</style>
```

### 流式聊天组件

```vue
<template>
  <div class="streaming-chat">
    <div class="connection-status">
      <el-tag :type="connectionStatusType">
        {{ connectionStatusText }}
      </el-tag>
      <el-button 
        v-if="connectionState === 'disconnected'"
        size="small"
        @click="connect"
      >
        连接
      </el-button>
      <el-button 
        v-else-if="connectionState === 'connected'"
        size="small"
        @click="disconnect"
      >
        断开
      </el-button>
    </div>

    <div class="streaming-messages" ref="streamingContainer">
      <div class="current-response" v-if="currentResponse">
        <div class="response-content">{{ currentResponse }}</div>
        <div class="typing-indicator" v-if="isStreaming">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div class="streaming-input">
      <el-input
        v-model="streamMessage"
        placeholder="输入消息进行流式对话..."
        :disabled="!isConnected || isStreaming"
        @keydown.enter="sendStreamMessage"
      >
        <template #append>
          <el-button 
            :disabled="!isConnected || isStreaming"
            @click="sendStreamMessage"
          >
            发送
          </el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createWebSocketStream } from '@/api/AiStreamingService'
import { aiResponseHandler } from '@/api/AiResponseHandler'
import type { WebSocketStreamingService, ConnectionState } from '@/api/AiStreamingService'

// 响应式数据
const streamMessage = ref('')
const currentResponse = ref('')
const isStreaming = ref(false)
const connectionState = ref<ConnectionState>('disconnected')
const wsStream = ref<WebSocketStreamingService | null>(null)
const streamingContainer = ref<HTMLElement>()

// 计算属性
const isConnected = computed(() => connectionState.value === 'connected')

const connectionStatusType = computed(() => {
  switch (connectionState.value) {
    case 'connected': return 'success'
    case 'connecting': return 'warning'
    case 'reconnecting': return 'warning'
    case 'failed': return 'danger'
    default: return 'info'
  }
})

const connectionStatusText = computed(() => {
  switch (connectionState.value) {
    case 'connected': return '已连接'
    case 'connecting': return '连接中...'
    case 'reconnecting': return '重连中...'
    case 'failed': return '连接失败'
    default: return '未连接'
  }
})

// 建立连接
async function connect() {
  if (wsStream.value) {
    wsStream.value.disconnect()
  }

  connectionState.value = 'connecting'

  wsStream.value = createWebSocketStream(
    {
      url: 'ws://localhost:3000/api/ai/chat/stream',
      reconnectAttempts: 3,
      reconnectDelay: 1000
    },
    {
      onOpen: () => {
        connectionState.value = 'connected'
        ElMessage.success('WebSocket连接已建立')
      },
      
      onMessage: (chunk) => {
        const content = aiResponseHandler.extractStreamContent(chunk)
        if (content) {
          currentResponse.value += content
          scrollToBottom()
        }
        
        if (aiResponseHandler.isStreamFinished(chunk)) {
          isStreaming.value = false
          ElMessage.success('回复完成')
        }
      },
      
      onError: (error) => {
        connectionState.value = 'failed'
        ElMessage.error(`连接错误: ${error.message}`)
      },
      
      onClose: () => {
        connectionState.value = 'disconnected'
        isStreaming.value = false
      },
      
      onReconnect: (attempt) => {
        connectionState.value = 'reconnecting'
        ElMessage.info(`正在进行第${attempt}次重连...`)
      }
    }
  )

  try {
    await wsStream.value.connect()
  } catch (error) {
    connectionState.value = 'failed'
    ElMessage.error('连接失败')
  }
}

// 断开连接
function disconnect() {
  if (wsStream.value) {
    wsStream.value.disconnect()
    wsStream.value = null
  }
  connectionState.value = 'disconnected'
}

// 发送流式消息
async function sendStreamMessage() {
  if (!streamMessage.value.trim() || !wsStream.value || !isConnected.value) {
    return
  }

  isStreaming.value = true
  currentResponse.value = ''

  try {
    await wsStream.value.sendStreamRequest({
      message: streamMessage.value,
      stream: true
    })
    
    streamMessage.value = ''
  } catch (error) {
    isStreaming.value = false
    ElMessage.error('发送消息失败')
  }
}

// 滚动到底部
function scrollToBottom() {
  if (streamingContainer.value) {
    streamingContainer.value.scrollTop = streamingContainer.value.scrollHeight
  }
}

// 组件卸载时断开连接
onUnmounted(() => {
  disconnect()
})
</script>

<style scoped>
.streaming-chat {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.connection-status {
  padding: 12px 16px;
  background-color: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.streaming-messages {
  height: 300px;
  padding: 16px;
  overflow-y: auto;
  background-color: #fafafa;
}

.current-response {
  background-color: white;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.response-content {
  line-height: 1.6;
  margin-bottom: 8px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #007bff;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.streaming-input {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
}
</style>
```

## 🛠️ 高级用法

### 错误处理和重试机制

```typescript
import { sendAiChat } from '@/api/AiApi'
import { handleAiError } from '@/api/AiResponseHandler'

class AiChatService {
  private maxRetries = 3
  private retryDelay = 1000

  async sendWithRetry(request: AiChatRequest, retries = 0): Promise<any> {
    try {
      return await sendAiChat(request)
    } catch (error) {
      const errorInfo = handleAiError(error)
      
      // 根据错误类型决定是否重试
      if (this.shouldRetry(errorInfo.code) && retries < this.maxRetries) {
        console.log(`第${retries + 1}次重试...`)
        
        // 指数退避
        const delay = this.retryDelay * Math.pow(2, retries)
        await this.sleep(delay)
        
        return this.sendWithRetry(request, retries + 1)
      }
      
      throw error
    }
  }

  private shouldRetry(errorCode: number): boolean {
    // 网络错误、服务器错误可以重试
    return [408, 429, 500, 502, 503, 504].includes(errorCode)
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }
}

// 使用示例
const aiService = new AiChatService()

async function robustChat() {
  try {
    const response = await aiService.sendWithRetry({
      message: '这是一个需要重试机制的请求'
    })
    console.log('成功获得回复:', response.data.message)
  } catch (error) {
    console.error('最终失败:', error)
  }
}
```

### 批量处理和队列管理

```typescript
import { sendAiChat } from '@/api/AiApi'
import type { AiChatRequest } from '@/api/AiApi'

class AiRequestQueue {
  private queue: Array<{
    request: AiChatRequest
    resolve: (value: any) => void
    reject: (error: any) => void
  }> = []
  
  private processing = false
  private concurrency = 3
  private activeRequests = 0

  async add(request: AiChatRequest): Promise<any> {
    return new Promise((resolve, reject) => {
      this.queue.push({ request, resolve, reject })
      this.process()
    })
  }

  private async process() {
    if (this.processing || this.activeRequests >= this.concurrency) {
      return
    }

    this.processing = true

    while (this.queue.length > 0 && this.activeRequests < this.concurrency) {
      const item = this.queue.shift()!
      this.activeRequests++

      this.executeRequest(item).finally(() => {
        this.activeRequests--
        this.process()
      })
    }

    this.processing = false
  }

  private async executeRequest(item: {
    request: AiChatRequest
    resolve: (value: any) => void
    reject: (error: any) => void
  }) {
    try {
      const response = await sendAiChat(item.request)
      item.resolve(response)
    } catch (error) {
      item.reject(error)
    }
  }
}

// 使用示例
const requestQueue = new AiRequestQueue()

async function batchProcess() {
  const requests = [
    { message: '请求1' },
    { message: '请求2' },
    { message: '请求3' },
    { message: '请求4' },
    { message: '请求5' }
  ]

  const promises = requests.map(request => requestQueue.add(request))
  
  try {
    const responses = await Promise.all(promises)
    console.log('所有请求完成:', responses)
  } catch (error) {
    console.error('批量处理失败:', error)
  }
}
```

### 缓存和性能优化

```typescript
class AiCacheService {
  private cache = new Map<string, any>()
  private cacheExpiry = new Map<string, number>()
  private defaultTTL = 5 * 60 * 1000 // 5分钟

  async getCachedResponse(request: AiChatRequest): Promise<any | null> {
    const key = this.generateCacheKey(request)
    
    if (this.cache.has(key)) {
      const expiry = this.cacheExpiry.get(key)!
      
      if (Date.now() < expiry) {
        console.log('使用缓存响应')
        return this.cache.get(key)
      } else {
        // 清除过期缓存
        this.cache.delete(key)
        this.cacheExpiry.delete(key)
      }
    }
    
    return null
  }

  setCachedResponse(request: AiChatRequest, response: any, ttl = this.defaultTTL) {
    const key = this.generateCacheKey(request)
    this.cache.set(key, response)
    this.cacheExpiry.set(key, Date.now() + ttl)
  }

  private generateCacheKey(request: AiChatRequest): string {
    // 生成基于请求内容的缓存键
    const keyData = {
      message: request.message,
      model: request.model,
      temperature: request.temperature
    }
    return btoa(JSON.stringify(keyData))
  }

  clearCache() {
    this.cache.clear()
    this.cacheExpiry.clear()
  }
}

// 带缓存的AI服务
class CachedAiService {
  private cacheService = new AiCacheService()

  async sendChatWithCache(request: AiChatRequest): Promise<any> {
    // 尝试从缓存获取
    const cached = await this.cacheService.getCachedResponse(request)
    if (cached) {
      return cached
    }

    // 发送实际请求
    const response = await sendAiChat(request)
    
    // 缓存响应（排除包含对话ID的响应）
    if (!request.conversation_id) {
      this.cacheService.setCachedResponse(request, response)
    }

    return response
  }
}
```

## 📊 性能监控和分析

```typescript
class AiPerformanceMonitor {
  private metrics = {
    totalRequests: 0,
    successfulRequests: 0,
    failedRequests: 0,
    totalResponseTime: 0,
    tokenUsage: {
      totalPromptTokens: 0,
      totalCompletionTokens: 0,
      totalTokens: 0
    }
  }

  async monitorRequest<T>(
    requestFn: () => Promise<T>,
    requestInfo?: { message: string; model?: string }
  ): Promise<T> {
    const startTime = Date.now()
    this.metrics.totalRequests++

    try {
      const result = await requestFn()
      
      // 记录成功指标
      this.metrics.successfulRequests++
      this.metrics.totalResponseTime += Date.now() - startTime

      // 记录token使用情况
      if (result && typeof result === 'object' && 'data' in result) {
        const data = (result as any).data
        if (data.usage) {
          this.metrics.tokenUsage.totalPromptTokens += data.usage.prompt_tokens
          this.metrics.tokenUsage.totalCompletionTokens += data.usage.completion_tokens
          this.metrics.tokenUsage.totalTokens += data.usage.total_tokens
        }
      }

      return result
    } catch (error) {
      this.metrics.failedRequests++
      throw error
    }
  }

  getMetrics() {
    return {
      ...this.metrics,
      averageResponseTime: this.metrics.totalRequests > 0 
        ? this.metrics.totalResponseTime / this.metrics.successfulRequests 
        : 0,
      successRate: this.metrics.totalRequests > 0 
        ? (this.metrics.successfulRequests / this.metrics.totalRequests) * 100 
        : 0
    }
  }

  reset() {
    this.metrics = {
      totalRequests: 0,
      successfulRequests: 0,
      failedRequests: 0,
      totalResponseTime: 0,
      tokenUsage: {
        totalPromptTokens: 0,
        totalCompletionTokens: 0,
        totalTokens: 0
      }
    }
  }
}

// 使用示例
const monitor = new AiPerformanceMonitor()

async function monitoredChat() {
  try {
    const response = await monitor.monitorRequest(
      () => sendAiChat({ message: '测试性能监控' }),
      { message: '测试性能监控', model: 'gpt-3.5-turbo' }
    )
    
    console.log('响应:', response)
    console.log('性能指标:', monitor.getMetrics())
  } catch (error) {
    console.error('请求失败:', error)
  }
}
```

## 🔧 测试和调试

### 单元测试示例

```typescript
// tests/ai-service.test.ts
import { describe, it, expect, vi } from 'vitest'
import { sendAiChat } from '@/api/AiApi'

// Mock API
vi.mock('@/api/AiApi', () => ({
  sendAiChat: vi.fn()
}))

describe('AI服务测试', () => {
  it('应该正确处理AI聊天请求', async () => {
    const mockResponse = {
      code: 200,
      msg: 'success',
      data: {
        message: '测试回复',
        conversation_id: 'test_conv',
        model: 'gpt-3.5-turbo',
        usage: { prompt_tokens: 10, completion_tokens: 15, total_tokens: 25 }
      }
    }

    vi.mocked(sendAiChat).mockResolvedValue(mockResponse)

    const result = await sendAiChat({ message: '测试消息' })
    
    expect(result.code).toBe(200)
    expect(result.data.message).toBe('测试回复')
  })
})
```

### 调试工具

```typescript
class AiDebugger {
  private debugMode = process.env.NODE_ENV === 'development'

  log(message: string, data?: any) {
    if (this.debugMode) {
      console.log(`[AI Debug] ${message}`, data)
    }
  }

  logRequest(request: AiChatRequest) {
    this.log('发送请求:', {
      message: request.message.substring(0, 100) + '...',
      model: request.model,
      temperature: request.temperature,
      timestamp: new Date().toISOString()
    })
  }

  logResponse(response: any, duration: number) {
    this.log('收到响应:', {
      code: response.code,
      messageLength: response.data?.message?.length || 0,
      tokenUsage: response.data?.usage,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    })
  }

  logError(error: any) {
    this.log('请求错误:', {
      message: error.message,
      code: error.code,
      stack: error.stack,
      timestamp: new Date().toISOString()
    })
  }
}

// 使用调试器包装请求
const debugger = new AiDebugger()

async function debuggedChat(request: AiChatRequest) {
  const startTime = Date.now()
  
  try {
    debugger.logRequest(request)
    const response = await sendAiChat(request)
    debugger.logResponse(response, Date.now() - startTime)
    return response
  } catch (error) {
    debugger.logError(error)
    throw error
  }
}
```

## 📝 最佳实践

### 1. 错误处理
- 始终使用try-catch包装AI请求
- 实现适当的重试机制
- 为用户提供友好的错误提示

### 2. 性能优化
- 合理使用缓存减少重复请求
- 实现请求队列控制并发数
- 监控token使用量避免超出配额

### 3. 用户体验
- 使用流式传输提供实时反馈
- 显示连接状态和加载指示器
- 实现断线重连机制

### 4. 安全考虑
- 验证用户输入防止注入攻击
- 实现请求频率限制
- 不在前端暴露敏感配置

### 5. 可维护性
- 使用TypeScript确保类型安全
- 编写完整的单元测试和集成测试
- 保持API文档与代码同步

## 🚀 部署和生产环境

### 环境配置

```typescript
// config/ai.config.ts
export const aiConfig = {
  development: {
    apiBaseURL: 'http://localhost:3000/api',
    wsURL: 'ws://localhost:3000/api/ai/chat/stream',
    timeout: 30000,
    retryAttempts: 3
  },
  production: {
    apiBaseURL: 'https://api.yourdomain.com/api',
    wsURL: 'wss://api.yourdomain.com/api/ai/chat/stream',
    timeout: 60000,
    retryAttempts: 5
  }
}

export const getCurrentConfig = () => {
  return aiConfig[process.env.NODE_ENV as keyof typeof aiConfig] || aiConfig.development
}
```

### 监控和日志

```typescript
// utils/ai-logger.ts
class AiLogger {
  private logLevel = process.env.VITE_LOG_LEVEL || 'info'

  info(message: string, data?: any) {
    if (this.shouldLog('info')) {
      console.log(`[AI Info] ${message}`, data)
    }
  }

  warn(message: string, data?: any) {
    if (this.shouldLog('warn')) {
      console.warn(`[AI Warn] ${message}`, data)
    }
  }

  error(message: string, error?: any) {
    if (this.shouldLog('error')) {
      console.error(`[AI Error] ${message}`, error)
      
      // 生产环境发送错误到监控服务
      if (process.env.NODE_ENV === 'production') {
        this.sendToMonitoring(message, error)
      }
    }
  }

  private shouldLog(level: string): boolean {
    const levels = ['error', 'warn', 'info', 'debug']
    const currentLevelIndex = levels.indexOf(this.logLevel)
    const messageLevelIndex = levels.indexOf(level)
    return messageLevelIndex <= currentLevelIndex
  }

  private sendToMonitoring(message: string, error: any) {
    // 实现发送到监控服务的逻辑
    // 例如：Sentry, LogRocket 等
  }
}

export const aiLogger = new AiLogger()
```

这个完整的使用指南涵盖了AI模块的所有主要功能和最佳实践，可以帮助开发者快速集成和使用AI功能。