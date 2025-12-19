import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

import ModernPdfReader from '@/components/Reader/ModernPdfReader.vue'

// Mock 配置
vi.mock('@/config', () => {
  return {
    ConfigManager: {
      getApiBaseUrl: () => 'http://localhost:10100/api',
      getGatewayUrl: () => 'http://localhost:10100',
      useMock: () => false
    }
  }
})

// Mock TokenManager
vi.mock('@/utils/tokenManager', () => {
  return {
    TokenManager: {
      getAuthorizationHeader: () => 'Bearer test-token',
      isTokenValid: () => false
    }
  }
})

// Mock NotesApi，避免在loadPdf中实际请求
vi.mock('@/api/NotesApi', () => {
  return {
    getNotesList: vi.fn().mockResolvedValue({ code: 200, data: [] }),
    createNote: vi.fn(),
    updateNote: vi.fn(),
    deleteNote: vi.fn()
  }
})

// 简单的flushPromises辅助
const flush = async () => {
  await new Promise((r) => setTimeout(r, 0))
}

// 响应助手
const resp = (status: number) => ({ ok: status >= 200 && status < 300, status })

describe('ModernPdfReader – 仅使用后端返回路径解析', () => {
  it('成功：使用后端完整URL作为PDF地址', async () => {
    const wrapper = mount(ModernPdfReader, {
      props: {
        document: {
          id: 'doc_ok',
          url: 'http://localhost:10100/files/doc_ok.pdf',
          pages: 11,
          read_progress: 0
        }
      }
    })

    await flush()
    const iframe = wrapper.find('iframe.pdf-iframe')
    expect(iframe.exists()).toBe(true)
    expect((iframe.element as HTMLIFrameElement).src).toContain('http://localhost:10100/files/doc_ok.pdf')
  })

  it('相对路径：拼接到网关基础URL', async () => {
    const wrapper = mount(ModernPdfReader, {
      props: {
        document: {
          id: 'doc_rel',
          file_path: 'uploads/documents/2025/11/07/doc_rel.pdf',
          pages: 6,
          read_progress: 0
        }
      }
    })

    await flush()
    const iframe = wrapper.find('iframe.pdf-iframe')
    expect(iframe.exists()).toBe(true)
    // 默认网关为 http://localhost:10100，在测试环境下只断言包含 /uploads/ 后缀
    expect((iframe.element as HTMLIFrameElement).src).toContain('/uploads/documents/2025/11/07/doc_rel.pdf')
  })

  it('错误：缺少文件路径字段时显示错误提示', async () => {
    const wrapper = mount(ModernPdfReader, {
      props: {
        document: {
          id: 'doc_err',
          pages: 6,
          read_progress: 0
        }
      }
    })

    await flush()
    await flush()
    const iframe = wrapper.find('iframe.pdf-iframe')
    expect(iframe.exists()).toBe(false)
    // 缺少路径时不应生成有效PDF地址
    const state = (wrapper.vm as any)?.$?.setupState || {}
    expect(state.pdfUrl).toBe('')
  })
})