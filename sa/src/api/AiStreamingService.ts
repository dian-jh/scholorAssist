import type { AiChatRequest, AiStreamChunk } from './AiApi'
import { aiResponseHandler } from './AiResponseHandler'

// 流式传输配置
export interface StreamConfig {
  url: string
  reconnectAttempts?: number
  reconnectDelay?: number
  heartbeatInterval?: number
  timeout?: number
}

// 流式传输事件
export interface StreamEvents {
  onOpen?: () => void
  onMessage?: (chunk: AiStreamChunk) => void
  onError?: (error: Error) => void
  onClose?: (code?: number, reason?: string) => void
  onReconnect?: (attempt: number) => void
}

// 连接状态
export enum ConnectionState {
  DISCONNECTED = 'disconnected',
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  RECONNECTING = 'reconnecting',
  FAILED = 'failed'
}

// WebSocket流式传输服务
export class WebSocketStreamingService {
  private ws: WebSocket | null = null
  private config: StreamConfig
  private events: StreamEvents
  private state: ConnectionState = ConnectionState.DISCONNECTED
  private reconnectCount = 0
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null

  constructor(config: StreamConfig, events: StreamEvents = {}) {
    this.config = {
      reconnectAttempts: 3,
      reconnectDelay: 1000,
      heartbeatInterval: 30000,
      timeout: 60000,
      ...config
    }
    this.events = events
  }

  /**
   * 建立WebSocket连接
   */
  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.state === ConnectionState.CONNECTED) {
        resolve()
        return
      }

      this.state = ConnectionState.CONNECTING
      
      try {
        this.ws = new WebSocket(this.config.url)
        
        // 设置连接超时
        const timeout = setTimeout(() => {
          if (this.state === ConnectionState.CONNECTING) {
            this.ws?.close()
            reject(new Error('连接超时'))
          }
        }, this.config.timeout)

        this.ws.onopen = () => {
          clearTimeout(timeout)
          this.state = ConnectionState.CONNECTED
          this.reconnectCount = 0
          this.startHeartbeat()
          this.events.onOpen?.()
          resolve()
        }

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.ws.onerror = (error) => {
          clearTimeout(timeout)
          const err = new Error('WebSocket连接错误')
          this.events.onError?.(err)
          if (this.state === ConnectionState.CONNECTING) {
            reject(err)
          }
        }

        this.ws.onclose = (event) => {
          clearTimeout(timeout)
          this.stopHeartbeat()
          this.state = ConnectionState.DISCONNECTED
          this.events.onClose?.(event.code, event.reason)
          
          // 自动重连
          if (this.shouldReconnect(event.code)) {
            this.attemptReconnect()
          }
        }
      } catch (error) {
        this.state = ConnectionState.FAILED
        reject(error)
      }
    })
  }

  /**
   * 发送流式聊天请求
   */
  public async sendStreamRequest(request: AiChatRequest): Promise<void> {
    if (this.state !== ConnectionState.CONNECTED) {
      throw new Error('WebSocket未连接')
    }

    const message = JSON.stringify({
      type: 'ai_chat_stream',
      data: { ...request, stream: true }
    })

    this.ws?.send(message)
  }

  /**
   * 关闭连接
   */
  public disconnect(): void {
    this.stopHeartbeat()
    this.clearReconnectTimer()
    
    if (this.ws) {
      this.ws.close(1000, '正常关闭')
      this.ws = null
    }
    
    this.state = ConnectionState.DISCONNECTED
  }

  /**
   * 获取连接状态
   */
  public getState(): ConnectionState {
    return this.state
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(data: string): void {
    try {
      // 处理心跳响应
      if (data === 'pong') {
        return
      }

      // 解析消息
      const message = JSON.parse(data)
      
      if (message.type === 'ai_chat_chunk') {
        const chunk = aiResponseHandler.handleStreamChunk(JSON.stringify(message.data))
        if (chunk) {
          this.events.onMessage?.(chunk)
        }
      } else if (message.type === 'error') {
        this.events.onError?.(new Error(message.message || '服务器错误'))
      }
    } catch (error) {
      console.error('消息处理失败:', error)
      this.events.onError?.(new Error('消息格式错误'))
    }
  }

  /**
   * 开始心跳检测
   */
  private startHeartbeat(): void {
    if (this.config.heartbeatInterval && this.config.heartbeatInterval > 0) {
      this.heartbeatTimer = window.setInterval(() => {
        if (this.ws?.readyState === WebSocket.OPEN) {
          this.ws.send('ping')
        }
      }, this.config.heartbeatInterval)
    }
  }

  /**
   * 停止心跳检测
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 判断是否应该重连
   */
  private shouldReconnect(code: number): boolean {
    // 正常关闭不重连
    if (code === 1000) {
      return false
    }
    
    return this.reconnectCount < (this.config.reconnectAttempts || 3)
  }

  /**
   * 尝试重连
   */
  private attemptReconnect(): void {
    if (this.reconnectCount >= (this.config.reconnectAttempts || 3)) {
      this.state = ConnectionState.FAILED
      return
    }

    this.state = ConnectionState.RECONNECTING
    this.reconnectCount++
    
    this.events.onReconnect?.(this.reconnectCount)
    
    const delay = (this.config.reconnectDelay || 1000) * Math.pow(2, this.reconnectCount - 1)
    
    this.reconnectTimer = window.setTimeout(() => {
      this.connect().catch(() => {
        // 重连失败，继续尝试
        this.attemptReconnect()
      })
    }, delay)
  }

  /**
   * 清除重连定时器
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
}

// SSE流式传输服务
export class SSEStreamingService {
  private eventSource: EventSource | null = null
  private config: StreamConfig
  private events: StreamEvents
  private state: ConnectionState = ConnectionState.DISCONNECTED
  private reconnectCount = 0
  private reconnectTimer: number | null = null

  constructor(config: StreamConfig, events: StreamEvents = {}) {
    this.config = {
      reconnectAttempts: 3,
      reconnectDelay: 1000,
      timeout: 60000,
      ...config
    }
    this.events = events
  }

  /**
   * 建立SSE连接
   */
  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.state === ConnectionState.CONNECTED) {
        resolve()
        return
      }

      this.state = ConnectionState.CONNECTING

      try {
        this.eventSource = new EventSource(this.config.url)

        // 设置连接超时
        const timeout = setTimeout(() => {
          if (this.state === ConnectionState.CONNECTING) {
            this.eventSource?.close()
            reject(new Error('SSE连接超时'))
          }
        }, this.config.timeout)

        this.eventSource.onopen = () => {
          clearTimeout(timeout)
          this.state = ConnectionState.CONNECTED
          this.reconnectCount = 0
          this.events.onOpen?.()
          resolve()
        }

        this.eventSource.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.eventSource.onerror = () => {
          clearTimeout(timeout)
          const wasConnecting = this.state === ConnectionState.CONNECTING
          this.state = ConnectionState.DISCONNECTED
          const error = new Error('SSE连接错误')
          this.events.onError?.(error)
          
          if (wasConnecting) {
            reject(error)
          }
          
          // 自动重连
          if (this.shouldReconnect()) {
            this.attemptReconnect()
          }
        }

        // 监听自定义事件
        this.eventSource.addEventListener('ai_chunk', (event: MessageEvent) => {
          this.handleMessage(event.data)
        })

        this.eventSource.addEventListener('error', (event: MessageEvent) => {
          const errorData = JSON.parse(event.data)
          this.events.onError?.(new Error(errorData.message || 'SSE错误'))
        })

      } catch (error) {
        this.state = ConnectionState.FAILED
        reject(error)
      }
    })
  }

  /**
   * 发送流式请求（通过POST请求触发SSE流）
   */
  public async sendStreamRequest(request: AiChatRequest): Promise<void> {
    if (this.state !== ConnectionState.CONNECTED) {
      throw new Error('SSE未连接')
    }

    // 通过POST请求触发服务器端流式响应
    const response = await fetch('/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({ ...request, stream: true })
    })

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`)
    }
  }

  /**
   * 关闭连接
   */
  public disconnect(): void {
    this.clearReconnectTimer()
    
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
    
    this.state = ConnectionState.DISCONNECTED
  }

  /**
   * 获取连接状态
   */
  public getState(): ConnectionState {
    return this.state
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(data: string): void {
    try {
      const chunk = aiResponseHandler.handleStreamChunk(data)
      if (chunk) {
        this.events.onMessage?.(chunk)
      }
    } catch (error) {
      console.error('SSE消息处理失败:', error)
      this.events.onError?.(new Error('消息格式错误'))
    }
  }

  /**
   * 判断是否应该重连
   */
  private shouldReconnect(): boolean {
    return this.reconnectCount < (this.config.reconnectAttempts || 3)
  }

  /**
   * 尝试重连
   */
  private attemptReconnect(): void {
    if (this.reconnectCount >= (this.config.reconnectAttempts || 3)) {
      this.state = ConnectionState.FAILED
      return
    }

    this.state = ConnectionState.RECONNECTING
    this.reconnectCount++
    
    this.events.onReconnect?.(this.reconnectCount)
    
    const delay = (this.config.reconnectDelay || 1000) * Math.pow(2, this.reconnectCount - 1)
    
    this.reconnectTimer = window.setTimeout(() => {
      this.connect().catch(() => {
        // 重连失败，继续尝试
        this.attemptReconnect()
      })
    }, delay)
  }

  /**
   * 清除重连定时器
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
}

// 流式传输管理器
export class StreamingManager {
  private wsService: WebSocketStreamingService | null = null
  private sseService: SSEStreamingService | null = null
  private currentService: 'websocket' | 'sse' | null = null

  /**
   * 创建WebSocket流式服务
   */
  public createWebSocketService(config: StreamConfig, events: StreamEvents): WebSocketStreamingService {
    this.wsService = new WebSocketStreamingService(config, events)
    this.currentService = 'websocket'
    return this.wsService
  }

  /**
   * 创建SSE流式服务
   */
  public createSSEService(config: StreamConfig, events: StreamEvents): SSEStreamingService {
    this.sseService = new SSEStreamingService(config, events)
    this.currentService = 'sse'
    return this.sseService
  }

  /**
   * 获取当前活跃的服务
   */
  public getCurrentService(): WebSocketStreamingService | SSEStreamingService | null {
    if (this.currentService === 'websocket') {
      return this.wsService
    } else if (this.currentService === 'sse') {
      return this.sseService
    }
    return null
  }

  /**
   * 断开所有连接
   */
  public disconnectAll(): void {
    this.wsService?.disconnect()
    this.sseService?.disconnect()
    this.currentService = null
  }

  /**
   * 检查浏览器支持情况
   */
  public static checkSupport(): {
    websocket: boolean
    sse: boolean
  } {
    return {
      websocket: typeof WebSocket !== 'undefined',
      sse: typeof EventSource !== 'undefined'
    }
  }
}

// 导出实例和工具函数
export const streamingManager = new StreamingManager()

/**
 * 创建WebSocket流式连接
 * @param config 配置参数
 * @param events 事件处理器
 * @returns WebSocket服务实例
 */
export function createWebSocketStream(config: StreamConfig, events: StreamEvents): WebSocketStreamingService {
  return streamingManager.createWebSocketService(config, events)
}

/**
 * 创建SSE流式连接
 * @param config 配置参数
 * @param events 事件处理器
 * @returns SSE服务实例
 */
export function createSSEStream(config: StreamConfig, events: StreamEvents): SSEStreamingService {
  return streamingManager.createSSEService(config, events)
}