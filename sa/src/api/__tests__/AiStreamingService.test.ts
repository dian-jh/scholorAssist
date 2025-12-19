import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  WebSocketStreamingService,
  SSEStreamingService,
  StreamingManager,
  ConnectionState,
  createWebSocketStream,
  createSSEStream
} from '../AiStreamingService'
import type { StreamConfig, StreamEvents } from '../AiStreamingService'

// Mock WebSocket
class MockWebSocket {
  static CONNECTING = 0
  static OPEN = 1
  static CLOSING = 2
  static CLOSED = 3

  readyState = MockWebSocket.CONNECTING
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null

  constructor(public url: string) {
    // 模拟异步连接
    setTimeout(() => {
      this.readyState = MockWebSocket.OPEN
      this.onopen?.(new Event('open'))
    }, 10)
  }

  send(data: string) {
    if (this.readyState !== MockWebSocket.OPEN) {
      throw new Error('WebSocket is not open')
    }
  }

  close(code?: number, reason?: string) {
    this.readyState = MockWebSocket.CLOSED
    this.onclose?.(new CloseEvent('close', { code: code || 1000, reason }))
  }

  // 模拟接收消息
  simulateMessage(data: string) {
    if (this.readyState === MockWebSocket.OPEN) {
      this.onmessage?.(new MessageEvent('message', { data }))
    }
  }

  // 模拟连接错误
  simulateError() {
    this.onerror?.(new Event('error'))
  }
}

// Mock EventSource
class MockEventSource {
  static CONNECTING = 0
  static OPEN = 1
  static CLOSED = 2

  readyState = MockEventSource.CONNECTING
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null

  private listeners: Map<string, ((event: MessageEvent) => void)[]> = new Map()

  constructor(public url: string) {
    setTimeout(() => {
      this.readyState = MockEventSource.OPEN
      this.onopen?.(new Event('open'))
    }, 10)
  }

  addEventListener(type: string, listener: (event: MessageEvent) => void) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, [])
    }
    this.listeners.get(type)!.push(listener)
  }

  close() {
    this.readyState = MockEventSource.CLOSED
  }

  // 模拟接收消息
  simulateMessage(data: string, type = 'message') {
    const event = new MessageEvent(type, { data })
    if (type === 'message') {
      this.onmessage?.(event)
    } else {
      const listeners = this.listeners.get(type) || []
      listeners.forEach(listener => listener(event))
    }
  }

  // 模拟错误
  simulateError() {
    this.onerror?.(new Event('error'))
  }
}

// Mock fetch
const mockFetch = vi.fn()

// 设置全局mocks
beforeEach(() => {
  global.WebSocket = MockWebSocket as any
  global.EventSource = MockEventSource as any
  global.fetch = mockFetch
  vi.clearAllMocks()
})

describe('WebSocketStreamingService', () => {
  let config: StreamConfig
  let events: StreamEvents
  let service: WebSocketStreamingService

  beforeEach(() => {
    config = {
      url: 'ws://localhost:3000/api/ai/stream',
      reconnectAttempts: 3,
      reconnectDelay: 1000,
      heartbeatInterval: 30000,
      timeout: 60000
    }

    events = {
      onOpen: vi.fn(),
      onMessage: vi.fn(),
      onError: vi.fn(),
      onClose: vi.fn(),
      onReconnect: vi.fn()
    }

    service = new WebSocketStreamingService(config, events)
  })

  afterEach(() => {
    service.disconnect()
  })

  it('应该成功建立WebSocket连接', async () => {
    await service.connect()
    
    expect(service.getState()).toBe(ConnectionState.CONNECTED)
    expect(events.onOpen).toHaveBeenCalled()
  })

  it('应该处理连接超时', async () => {
    const shortTimeoutConfig = { ...config, timeout: 1 }
    const timeoutService = new WebSocketStreamingService(shortTimeoutConfig, events)

    await expect(timeoutService.connect()).rejects.toThrow('连接超时')
  })

  it('应该发送流式请求', async () => {
    await service.connect()
    
    const request = {
      message: '测试消息',
      stream: true
    }

    await service.sendStreamRequest(request)
    // 由于是mock，我们无法直接验证发送的内容，但可以验证没有抛出错误
  })

  it('应该在未连接时拒绝发送请求', async () => {
    const request = {
      message: '测试消息',
      stream: true
    }

    await expect(service.sendStreamRequest(request)).rejects.toThrow('WebSocket未连接')
  })

  it('应该处理接收到的消息', async () => {
    await service.connect()
    
    const mockMessage = JSON.stringify({
      type: 'ai_chat_chunk',
      data: {
        id: 'chunk_001',
        object: 'chat.completion.chunk',
        created: 1699123456,
        model: 'gpt-3.5-turbo',
        choices: [
          {
            index: 0,
            delta: { content: '测试内容' }
          }
        ]
      }
    })

    // 获取WebSocket实例并模拟消息
    const ws = (service as any).ws as MockWebSocket
    ws.simulateMessage(mockMessage)

    expect(events.onMessage).toHaveBeenCalled()
  })

  it('应该处理心跳消息', async () => {
    await service.connect()
    
    const ws = (service as any).ws as MockWebSocket
    ws.simulateMessage('pong')

    // 心跳消息不应该触发onMessage事件
    expect(events.onMessage).not.toHaveBeenCalled()
  })

  it('应该处理错误消息', async () => {
    await service.connect()
    
    const errorMessage = JSON.stringify({
      type: 'error',
      message: '服务器错误'
    })

    const ws = (service as any).ws as MockWebSocket
    ws.simulateMessage(errorMessage)

    expect(events.onError).toHaveBeenCalledWith(expect.any(Error))
  })

  it('应该处理无效的JSON消息', async () => {
    await service.connect()
    
    const ws = (service as any).ws as MockWebSocket
    ws.simulateMessage('invalid json')

    expect(events.onError).toHaveBeenCalledWith(expect.any(Error))
  })

  it('应该正确断开连接', async () => {
    await service.connect()
    
    service.disconnect()
    
    expect(service.getState()).toBe(ConnectionState.DISCONNECTED)
  })

  it('应该在连接关闭时尝试重连', async () => {
    await service.connect()
    
    const ws = (service as any).ws as MockWebSocket
    ws.close(1006, '异常关闭') // 非正常关闭

    // 等待重连逻辑执行
    await new Promise(resolve => setTimeout(resolve, 50))
    
    expect(events.onReconnect).toHaveBeenCalled()
  })

  it('应该在正常关闭时不重连', async () => {
    await service.connect()
    
    const ws = (service as any).ws as MockWebSocket
    ws.close(1000, '正常关闭')

    await new Promise(resolve => setTimeout(resolve, 50))
    
    expect(events.onReconnect).not.toHaveBeenCalled()
  })

  it('应该在达到最大重连次数后停止重连', async () => {
    const limitedConfig = { ...config, reconnectAttempts: 1 }
    const limitedService = new WebSocketStreamingService(limitedConfig, events)
    
    await limitedService.connect()
    
    // 模拟多次连接失败
    const ws = (limitedService as any).ws as MockWebSocket
    ws.close(1006, '异常关闭')
    
    await new Promise(resolve => setTimeout(resolve, 1200)) // 等待重连尝试
    
    expect(limitedService.getState()).toBe(ConnectionState.FAILED)
  })
})

describe('SSEStreamingService', () => {
  let config: StreamConfig
  let events: StreamEvents
  let service: SSEStreamingService

  beforeEach(() => {
    config = {
      url: 'http://localhost:3000/api/ai/stream',
      reconnectAttempts: 3,
      reconnectDelay: 1000,
      timeout: 60000
    }

    events = {
      onOpen: vi.fn(),
      onMessage: vi.fn(),
      onError: vi.fn(),
      onClose: vi.fn(),
      onReconnect: vi.fn()
    }

    service = new SSEStreamingService(config, events)
  })

  afterEach(() => {
    service.disconnect()
  })

  it('应该成功建立SSE连接', async () => {
    await service.connect()
    
    expect(service.getState()).toBe(ConnectionState.CONNECTED)
    expect(events.onOpen).toHaveBeenCalled()
  })

  it('应该处理连接超时', async () => {
    const shortTimeoutConfig = { ...config, timeout: 1 }
    const timeoutService = new SSEStreamingService(shortTimeoutConfig, events)

    await expect(timeoutService.connect()).rejects.toThrow('SSE连接超时')
  })

  it('应该发送流式请求', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200
    })

    await service.connect()
    
    const request = {
      message: '测试消息',
      stream: true
    }

    await service.sendStreamRequest(request)
    
    expect(mockFetch).toHaveBeenCalledWith('/api/ai/chat/stream', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json'
      }),
      body: JSON.stringify(request)
    }))
  })

  it('应该在请求失败时抛出错误', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500
    })

    await service.connect()
    
    const request = {
      message: '测试消息',
      stream: true
    }

    await expect(service.sendStreamRequest(request)).rejects.toThrow('请求失败: 500')
  })

  it('应该在未连接时拒绝发送请求', async () => {
    const request = {
      message: '测试消息',
      stream: true
    }

    await expect(service.sendStreamRequest(request)).rejects.toThrow('SSE未连接')
  })

  it('应该处理接收到的消息', async () => {
    await service.connect()
    
    const mockChunk = JSON.stringify({
      id: 'chunk_001',
      object: 'chat.completion.chunk',
      created: 1699123456,
      model: 'gpt-3.5-turbo',
      choices: [
        {
          index: 0,
          delta: { content: '测试内容' }
        }
      ]
    })

    const eventSource = (service as any).eventSource as MockEventSource
    eventSource.simulateMessage(mockChunk)

    expect(events.onMessage).toHaveBeenCalled()
  })

  it('应该处理自定义事件', async () => {
    await service.connect()
    
    const mockChunk = JSON.stringify({
      id: 'chunk_001',
      object: 'chat.completion.chunk',
      created: 1699123456,
      model: 'gpt-3.5-turbo',
      choices: [
        {
          index: 0,
          delta: { content: '测试内容' }
        }
      ]
    })

    const eventSource = (service as any).eventSource as MockEventSource
    eventSource.simulateMessage(mockChunk, 'ai_chunk')

    expect(events.onMessage).toHaveBeenCalled()
  })

  it('应该处理错误事件', async () => {
    await service.connect()
    
    const errorData = JSON.stringify({
      message: 'SSE错误'
    })

    const eventSource = (service as any).eventSource as MockEventSource
    eventSource.simulateMessage(errorData, 'error')

    expect(events.onError).toHaveBeenCalledWith(expect.any(Error))
  })

  it('应该正确断开连接', async () => {
    await service.connect()
    
    service.disconnect()
    
    expect(service.getState()).toBe(ConnectionState.DISCONNECTED)
  })
})

describe('StreamingManager', () => {
  let manager: StreamingManager

  beforeEach(() => {
    manager = new StreamingManager()
  })

  afterEach(() => {
    manager.disconnectAll()
  })

  it('应该创建WebSocket服务', () => {
    const config: StreamConfig = {
      url: 'ws://localhost:3000/api/ai/stream'
    }
    const events: StreamEvents = {}

    const service = manager.createWebSocketService(config, events)
    
    expect(service).toBeInstanceOf(WebSocketStreamingService)
    expect(manager.getCurrentService()).toBe(service)
  })

  it('应该创建SSE服务', () => {
    const config: StreamConfig = {
      url: 'http://localhost:3000/api/ai/stream'
    }
    const events: StreamEvents = {}

    const service = manager.createSSEService(config, events)
    
    expect(service).toBeInstanceOf(SSEStreamingService)
    expect(manager.getCurrentService()).toBe(service)
  })

  it('应该断开所有连接', async () => {
    const config: StreamConfig = {
      url: 'ws://localhost:3000/api/ai/stream'
    }
    const events: StreamEvents = {}

    const wsService = manager.createWebSocketService(config, events)
    await wsService.connect()

    manager.disconnectAll()
    
    expect(wsService.getState()).toBe(ConnectionState.DISCONNECTED)
    expect(manager.getCurrentService()).toBeNull()
  })

  it('应该检查浏览器支持情况', () => {
    const support = StreamingManager.checkSupport()
    
    expect(support).toHaveProperty('websocket')
    expect(support).toHaveProperty('sse')
    expect(typeof support.websocket).toBe('boolean')
    expect(typeof support.sse).toBe('boolean')
  })
})

describe('导出的便捷函数', () => {
  it('createWebSocketStream应该创建WebSocket服务', () => {
    const config: StreamConfig = {
      url: 'ws://localhost:3000/api/ai/stream'
    }
    const events: StreamEvents = {}

    const service = createWebSocketStream(config, events)
    
    expect(service).toBeInstanceOf(WebSocketStreamingService)
  })

  it('createSSEStream应该创建SSE服务', () => {
    const config: StreamConfig = {
      url: 'http://localhost:3000/api/ai/stream'
    }
    const events: StreamEvents = {}

    const service = createSSEStream(config, events)
    
    expect(service).toBeInstanceOf(SSEStreamingService)
  })
})

describe('错误处理和边界条件', () => {
  it('应该处理WebSocket构造函数异常', async () => {
    // 模拟WebSocket构造函数抛出异常
    global.WebSocket = class {
      constructor() {
        throw new Error('WebSocket构造失败')
      }
    } as any

    const config: StreamConfig = {
      url: 'ws://localhost:3000/api/ai/stream'
    }
    const service = new WebSocketStreamingService(config, {})

    await expect(service.connect()).rejects.toThrow()
    expect(service.getState()).toBe(ConnectionState.FAILED)
  })

  it('应该处理EventSource构造函数异常', async () => {
    // 模拟EventSource构造函数抛出异常
    global.EventSource = class {
      constructor() {
        throw new Error('EventSource构造失败')
      }
    } as any

    const config: StreamConfig = {
      url: 'http://localhost:3000/api/ai/stream'
    }
    const service = new SSEStreamingService(config, {})

    await expect(service.connect()).rejects.toThrow()
    expect(service.getState()).toBe(ConnectionState.FAILED)
  })

  it('应该处理重复连接请求', async () => {
    const config: StreamConfig = {
      url: 'ws://localhost:3000/api/ai/stream'
    }
    const service = new WebSocketStreamingService(config, {})

    await service.connect()
    
    // 第二次连接应该立即返回
    await service.connect()
    
    expect(service.getState()).toBe(ConnectionState.CONNECTED)
  })

  it('应该处理心跳定时器清理', async () => {
    const config: StreamConfig = {
      url: 'ws://localhost:3000/api/ai/stream',
      heartbeatInterval: 100
    }
    const service = new WebSocketStreamingService(config, {})

    await service.connect()
    
    // 验证心跳定时器已启动
    expect((service as any).heartbeatTimer).not.toBeNull()
    
    service.disconnect()
    
    // 验证心跳定时器已清理
    expect((service as any).heartbeatTimer).toBeNull()
  })
})