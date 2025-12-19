import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import DocumentCard from '../DocumentCard.vue'
import type { MockDocument } from '@/api/mockManager'

const mockDocument: MockDocument = {
  id: 'doc_1',
  title: 'Test Document',
  filename: 'test.pdf',
  category_id: 'cat_1',
  author: 'Test Author',
  upload_date: '2024-01-15T10:30:00Z',
  file_size: '2.3 MB',
  pages: 15,
  status: 'ready',
  thumbnail: 'https://example.com/thumbnail.jpg',
  abstract: 'This is a test document abstract',
  tags: ['test', 'document'],
  read_progress: 0.6
}

describe('DocumentCard', () => {
  it('should render document information correctly', () => {
    const wrapper = mount(DocumentCard, {
      props: {
        document: mockDocument
      }
    })

    expect(wrapper.find('.document-title').text()).toBe('Test Document')
    expect(wrapper.find('.document-author').text()).toBe('Test Author')
    expect(wrapper.find('.document-abstract').text()).toBe('This is a test document abstract')
    expect(wrapper.text()).toContain('15 页')
    expect(wrapper.text()).toContain('2.3 MB')
  })

  it('should show correct status badge', () => {
    const wrapper = mount(DocumentCard, {
      props: {
        document: mockDocument
      }
    })

    expect(wrapper.text()).toContain('已解析')
  })

  it('should show processing status for processing documents', () => {
    const processingDoc = { ...mockDocument, status: 'processing' as const }
    const wrapper = mount(DocumentCard, {
      props: {
        document: processingDoc
      }
    })

    expect(wrapper.text()).toContain('解析中')
  })

  it('should show reading progress when available', () => {
    const wrapper = mount(DocumentCard, {
      props: {
        document: mockDocument
      }
    })

    expect(wrapper.text()).toContain('阅读进度: 60%')
  })

  it('should emit click event when card is clicked', async () => {
    const wrapper = mount(DocumentCard, {
      props: {
        document: mockDocument
      }
    })

    await wrapper.find('.document-card').trigger('click')

    expect(wrapper.emitted('click')).toBeTruthy()
    expect(wrapper.emitted('click')?.[0]).toEqual(['doc_1'])
  })

  it('should render tags correctly', () => {
    const wrapper = mount(DocumentCard, {
      props: {
        document: mockDocument
      }
    })

    const tags = wrapper.findAll('.tag-item')
    expect(tags).toHaveLength(2)
    expect(tags[0].text()).toBe('test')
    expect(tags[1].text()).toBe('document')
  })
})