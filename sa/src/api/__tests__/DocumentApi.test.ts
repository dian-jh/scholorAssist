import { describe, it, expect, vi, beforeEach } from 'vitest'

// mock axios index以捕获路径
vi.mock('../index', () => {
  return {
    get: vi.fn(),
    post: vi.fn()
  }
})

// mock 配置，返回固定的baseApi
vi.mock('@/config', () => {
  return {
    ConfigManager: {
      getApiBaseUrl: () => 'http://localhost:10100/api',
      getGatewayUrl: () => 'http://localhost:10100',
      useMock: () => false
    }
  }
})

import { getDocumentFileUrl, updateReadingProgress, getPdfContent } from '../DocumentApi'
import { post } from '../index'

describe('DocumentApi 路径与URL构造', () => {
  beforeEach(() => {
    (post as unknown as ReturnType<typeof vi.fn>).mockReset()
  })

  it('getDocumentFileUrl 返回标准文件流接口URL', () => {
    const url = getDocumentFileUrl('abc123')
    expect(url).toBe('http://localhost:10100/api/documents/abc123/file')
  })

  it('updateReadingProgress 使用统一的 /documents/:id/progress 路径', async () => {
    ;(post as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({ code: 200, data: { progress: 0.5 } })
    await updateReadingProgress('xyz789', 0.5)
    // 断言post被以正确路径调用
    const call = (post as unknown as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(call[0]).toBe('/documents/xyz789/progress')
    expect(call[1]).toEqual({ progress: 0.5 })
  })

  it('getPdfContent 使用统一的 /documents/:id/content 路径', async () => {
    const getSpy = (await import('../index')).get as unknown as ReturnType<typeof vi.fn>
    getSpy.mockResolvedValue({ code: 200, data: { pages: [], total_pages: 0 } })
    await getPdfContent('doc1', { page: 1, pageSize: 10 })
    const call = getSpy.mock.calls[0]
    expect(call[0]).toBe('/documents/doc1/content')
    expect(call[1]).toEqual({ page: 1, pageSize: 10 })
  })
})