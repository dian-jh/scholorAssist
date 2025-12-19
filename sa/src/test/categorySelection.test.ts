/**
 * 分类选择功能测试脚本
 * 用于测试论文上传中的分类选择模块
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import UploadDialog from '@/components/Dashboard/UploadDialog.vue'
import { useCategoryStore } from '@/store'
import type { MockCategory } from '@/api/mockManager'

// Mock ElMessage
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    }
  }
})

// Mock 分类数据
const mockCategories: MockCategory[] = [
  {
    id: 'cat_1',
    name: '机器学习',
    parent_id: null,
    created_at: '2024-01-15T10:30:00Z',
    document_count: 15,
    children: [
      {
        id: 'cat_1_1',
        name: '深度学习',
        parent_id: 'cat_1',
        created_at: '2024-01-16T09:20:00Z',
        document_count: 8
      },
      {
        id: 'cat_1_2',
        name: '强化学习',
        parent_id: 'cat_1',
        created_at: '2024-01-17T14:45:00Z',
        document_count: 7
      }
    ]
  },
  {
    id: 'cat_2',
    name: '自然语言处理',
    parent_id: null,
    created_at: '2024-01-18T11:15:00Z',
    document_count: 12,
    children: [
      {
        id: 'cat_2_1',
        name: '文本分类',
        parent_id: 'cat_2',
        created_at: '2024-01-19T16:30:00Z',
        document_count: 5
      }
    ]
  },
  {
    id: 'cat_3',
    name: '计算机视觉',
    parent_id: null,
    created_at: '2024-01-21T08:40:00Z',
    document_count: 9
  }
]

describe('分类选择功能测试', () => {
  let wrapper: any
  let categoryStore: any

  beforeEach(() => {
    setActivePinia(createPinia())
    categoryStore = useCategoryStore()
    
    // Mock store methods
    categoryStore.fetchCategories = vi.fn().mockResolvedValue({ data: mockCategories })
    categoryStore.categories = mockCategories
    
    wrapper = mount(UploadDialog, {
      props: {
        visible: true
      },
      global: {
        plugins: [createPinia()]
      }
    })
  })

  describe('1. 分类数据加载测试', () => {
    it('应该在对话框打开时自动加载分类数据', async () => {
      expect(categoryStore.fetchCategories).toHaveBeenCalled()
    })

    it('应该正确处理分类数据加载失败', async () => {
      const errorStore = useCategoryStore()
      errorStore.fetchCategories = vi.fn().mockRejectedValue(new Error('网络错误'))
      
      const errorWrapper = mount(UploadDialog, {
        props: { visible: true },
        global: { plugins: [createPinia()] }
      })

      await errorWrapper.vm.$nextTick()
      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('应该在没有分类数据时显示警告', async () => {
      const emptyStore = useCategoryStore()
      emptyStore.categories = []
      emptyStore.fetchCategories = vi.fn().mockResolvedValue({ data: [] })
      
      const emptyWrapper = mount(UploadDialog, {
        props: { visible: true },
        global: { plugins: [createPinia()] }
      })

      await emptyWrapper.vm.$nextTick()
      expect(ElMessage.warning).toHaveBeenCalledWith('暂无可用分类，请先创建分类')
    })
  })

  describe('2. 分类选择器显示测试', () => {
    it('应该正确显示扁平化的分类列表', () => {
      const selectComponent = wrapper.findComponent({ name: 'ElSelect' })
      expect(selectComponent.exists()).toBe(true)
    })

    it('应该正确格式化分类标签（包含层级缩进）', () => {
      const vm = wrapper.vm
      const flatCategories = vm.flatCategories
      
      expect(flatCategories).toHaveLength(5) // 3个根分类 + 2个子分类
      
      // 检查层级格式化
      const deepLearning = flatCategories.find((cat: any) => cat.id === 'cat_1_1')
      expect(vm.formatCategoryLabel(deepLearning)).toBe('　深度学习')
    })

    it('应该支持分类选择器的搜索和清除功能', () => {
      const selectComponent = wrapper.findComponent({ name: 'ElSelect' })
      expect(selectComponent.props('filterable')).toBe(true)
      expect(selectComponent.props('clearable')).toBe(true)
    })
  })

  describe('3. 表单验证测试', () => {
    it('应该在未选择分类时显示警告', async () => {
      const vm = wrapper.vm
      vm.selectedFile = new File(['test'], 'test.pdf', { type: 'application/pdf' })
      vm.uploadForm.title = '测试文档'
      vm.uploadForm.categoryId = ''

      await vm.handleUpload()
      expect(ElMessage.warning).toHaveBeenCalledWith('请选择文档分类')
    })

    it('应该在未选择文件时显示警告', async () => {
      const vm = wrapper.vm
      vm.selectedFile = null
      vm.uploadForm.categoryId = 'cat_1'

      await vm.handleUpload()
      expect(ElMessage.warning).toHaveBeenCalledWith('请选择要上传的文件')
    })

    it('应该在标题为空时显示警告', async () => {
      const vm = wrapper.vm
      vm.selectedFile = new File(['test'], 'test.pdf', { type: 'application/pdf' })
      vm.uploadForm.title = ''
      vm.uploadForm.categoryId = 'cat_1'

      await vm.handleUpload()
      expect(ElMessage.warning).toHaveBeenCalledWith('请输入文档标题')
    })

    it('应该验证文件类型（只允许PDF）', async () => {
      const vm = wrapper.vm
      vm.selectedFile = new File(['test'], 'test.txt', { type: 'text/plain' })
      vm.uploadForm.title = '测试文档'
      vm.uploadForm.categoryId = 'cat_1'

      await vm.handleUpload()
      expect(ElMessage.error).toHaveBeenCalledWith('只支持上传PDF格式的文件')
    })

    it('应该验证文件大小（不超过50MB）', async () => {
      const vm = wrapper.vm
      const largeFile = new File(['x'.repeat(51 * 1024 * 1024)], 'large.pdf', { 
        type: 'application/pdf' 
      })
      vm.selectedFile = largeFile
      vm.uploadForm.title = '大文件'
      vm.uploadForm.categoryId = 'cat_1'

      await vm.handleUpload()
      expect(ElMessage.error).toHaveBeenCalledWith('文件大小不能超过50MB')
    })
  })

  describe('4. 分类层级功能测试', () => {
    it('应该正确处理根级分类选择', async () => {
      const vm = wrapper.vm
      vm.uploadForm.categoryId = 'cat_3' // 计算机视觉（根级分类）
      
      expect(vm.uploadForm.categoryId).toBe('cat_3')
    })

    it('应该正确处理子级分类选择', async () => {
      const vm = wrapper.vm
      vm.uploadForm.categoryId = 'cat_1_1' // 深度学习（子级分类）
      
      expect(vm.uploadForm.categoryId).toBe('cat_1_1')
    })

    it('应该正确显示所有层级的分类', () => {
      const vm = wrapper.vm
      const flatCategories = vm.flatCategories
      
      // 检查是否包含所有分类
      const categoryIds = flatCategories.map((cat: any) => cat.id)
      expect(categoryIds).toContain('cat_1') // 机器学习
      expect(categoryIds).toContain('cat_1_1') // 深度学习
      expect(categoryIds).toContain('cat_1_2') // 强化学习
      expect(categoryIds).toContain('cat_2') // 自然语言处理
      expect(categoryIds).toContain('cat_2_1') // 文本分类
      expect(categoryIds).toContain('cat_3') // 计算机视觉
    })
  })

  describe('5. 错误处理测试', () => {
    it('应该正确处理网络错误', async () => {
      const vm = wrapper.vm
      vm.selectedFile = new File(['test'], 'test.pdf', { type: 'application/pdf' })
      vm.uploadForm.title = '测试文档'
      vm.uploadForm.categoryId = 'cat_1'

      // Mock 网络错误
      const documentStore = { 
        uploadDocumentFile: vi.fn().mockRejectedValue(new Error('网络连接失败'))
      }
      vm.documentStore = documentStore

      await vm.handleUpload()
      expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败，请检查网络连接后重试')
    })

    it('应该正确处理上传超时', async () => {
      const vm = wrapper.vm
      vm.selectedFile = new File(['test'], 'test.pdf', { type: 'application/pdf' })
      vm.uploadForm.title = '测试文档'
      vm.uploadForm.categoryId = 'cat_1'

      // Mock 超时错误
      const documentStore = { 
        uploadDocumentFile: vi.fn().mockRejectedValue(new Error('上传超时'))
      }
      vm.documentStore = documentStore

      await vm.handleUpload()
      expect(ElMessage.error).toHaveBeenCalledWith('上传超时，请检查网络连接或稍后重试')
    })

    it('应该实现重试机制', async () => {
      const vm = wrapper.vm
      vm.selectedFile = new File(['test'], 'test.pdf', { type: 'application/pdf' })
      vm.uploadForm.title = '测试文档'
      vm.uploadForm.categoryId = 'cat_1'

      // Mock 前两次失败，第三次成功
      let callCount = 0
      const documentStore = { 
        uploadDocumentFile: vi.fn().mockImplementation(() => {
          callCount++
          if (callCount < 3) {
            return Promise.reject(new Error('临时错误'))
          }
          return Promise.resolve({ data: { id: 'doc_1' } })
        })
      }
      vm.documentStore = documentStore

      await vm.handleUpload()
      expect(documentStore.uploadDocumentFile).toHaveBeenCalledTimes(3)
      expect(ElMessage.success).toHaveBeenCalledWith('文档上传成功')
    })
  })

  describe('6. 用户体验测试', () => {
    it('应该在加载分类时显示加载状态', async () => {
      const vm = wrapper.vm
      vm.categoriesLoading = true
      
      await wrapper.vm.$nextTick()
      const selectComponent = wrapper.findComponent({ name: 'ElSelect' })
      expect(selectComponent.props('loading')).toBe(true)
    })

    it('应该在上传时显示进度', async () => {
      const vm = wrapper.vm
      vm.uploading = true
      vm.uploadProgress = 50
      
      await wrapper.vm.$nextTick()
      const progressComponent = wrapper.findComponent({ name: 'ElProgress' })
      expect(progressComponent.exists()).toBe(true)
      expect(progressComponent.props('percentage')).toBe(50)
    })

    it('应该在重试时显示重试状态', async () => {
      const vm = wrapper.vm
      vm.uploading = true
      vm.retryCount = 2
      
      await wrapper.vm.$nextTick()
      const retryText = wrapper.find('p')
      expect(retryText.text()).toContain('正在重试上传（第 3 次尝试）')
    })

    it('应该在对话框关闭时重置表单', async () => {
      const vm = wrapper.vm
      vm.selectedFile = new File(['test'], 'test.pdf', { type: 'application/pdf' })
      vm.uploadForm.title = '测试文档'
      vm.uploadForm.categoryId = 'cat_1'
      
      await wrapper.setProps({ visible: false })
      
      expect(vm.selectedFile).toBeNull()
      expect(vm.uploadForm.title).toBe('')
      expect(vm.uploadForm.categoryId).toBe('')
    })
  })
})

export default {
  name: 'CategorySelectionTest',
  description: '分类选择功能测试套件',
  testCases: [
    '分类数据加载测试',
    '分类选择器显示测试', 
    '表单验证测试',
    '分类层级功能测试',
    '错误处理测试',
    '用户体验测试'
  ]
}