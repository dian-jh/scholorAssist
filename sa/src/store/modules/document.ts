import { defineStore } from 'pinia'
import type { MockDocument } from '@/api/mockManager'
import { getDocumentList, getDocumentDetail, uploadDocument } from '@/api/DocumentApi'

interface DocumentState {
  documents: MockDocument[]
  currentDocument: MockDocument | null
  loading: boolean
  uploadProgress: number
}

export const useDocumentStore = defineStore('document', {
  state: (): DocumentState => ({
    documents: [],
    currentDocument: null,
    loading: false,
    uploadProgress: 0
  }),

  getters: {
    getDocuments: (state) => state.documents,
    getCurrentDocument: (state) => state.currentDocument,
    getLoading: (state) => state.loading,
    getUploadProgress: (state) => state.uploadProgress,
    
    getDocumentsByCategory: (state) => (categoryId: string) => {
      if (categoryId === 'all') return state.documents
      return state.documents.filter(doc => {
        const cid = (doc as any).category_id ?? (doc as any).categoryId
        return cid === categoryId
      })
    },
    
    getRecentDocuments: (state) => (limit = 5) => {
      return state.documents
        .sort((a, b) => new Date((b as any).upload_date ?? (b as any).uploadDate).getTime() - new Date((a as any).upload_date ?? (a as any).uploadDate).getTime())
        .slice(0, limit)
    }
  },

  actions: {
    // 统一将后端返回的字段映射为内部使用的字段
    normalizeDocument(raw: any): MockDocument {
      return {
        id: raw.id,
        title: raw.title,
        filename: raw.filename,
        category_id: raw.category_id ?? raw.categoryId ?? 'all',
        author: raw.author ?? '',
        upload_date: raw.upload_date ?? raw.uploadDate ?? '',
        file_size: raw.file_size ?? raw.fileSize ?? '',
        pages: raw.pages ?? 0,
        status: raw.status ?? 'ready',
        thumbnail: raw.thumbnail ?? '',
        abstract: raw.abstract ?? raw.abstractText ?? '',
        tags: raw.tags ?? [],
        read_progress: raw.read_progress ?? raw.readProgress ?? 0
      }
    },

    async fetchDocuments(params?: {
      category_id?: string
      search?: string
      page?: number
      pageSize?: number
    }) {
      this.loading = true
      try {
        const response = await getDocumentList(params || {})
        this.documents = (response.data || []).map((d: any) => this.normalizeDocument(d))
        return response
      } catch (error) {
        console.error('获取文档列表失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchDocumentDetail(id: string) {
      this.loading = true
      try {
        const response = await getDocumentDetail(id)
        this.currentDocument = response.data ? this.normalizeDocument(response.data) : null
        return response
      } catch (error) {
        console.error('获取文档详情失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async uploadDocumentFile(file: File, title?: string, categoryId?: string) {
      this.uploadProgress = 0
      
      try {
        // 模拟上传进度
        const progressInterval = setInterval(() => {
          if (this.uploadProgress < 90) {
            this.uploadProgress += Math.random() * 20
          }
        }, 200)

        const response = await uploadDocument({
          file,
          title,
          category_id: categoryId
        })

        clearInterval(progressInterval)
        this.uploadProgress = 100

        // 添加到文档列表（规范化后）
        this.documents.unshift(this.normalizeDocument(response.data))
        
        return response
      } catch (error) {
        this.uploadProgress = 0
        console.error('上传文档失败:', error)
        
        // 提供更详细的错误信息
        if (error instanceof Error) {
          if (error.message.includes('网络')) {
            throw new Error('网络连接失败，请检查网络连接')
          } else if (error.message.includes('超时')) {
            throw new Error('上传超时，请检查网络连接')
          } else if (error.message.includes('文件大小') || error.message.includes('size')) {
            throw new Error('文件大小超出限制')
          } else if (error.message.includes('格式') || error.message.includes('format')) {
            throw new Error('文件格式不支持')
          } else if (error.message.includes('401') || error.message.includes('权限')) {
            throw new Error('没有上传权限')
          } else if (error.message.includes('413')) {
            throw new Error('文件过大，请选择小于50MB的文件')
          } else if (error.message.includes('507')) {
            throw new Error('存储空间不足')
          } else if (error.message.includes('500')) {
            throw new Error('服务器内部错误，请稍后重试')
          }
        }
        
        throw error
      }
    },

    setCurrentDocument(document: MockDocument | null) {
      this.currentDocument = document
    },

    updateDocumentProgress(id: string, progress: number) {
      const document = this.documents.find(doc => doc.id === id)
      if (document) {
        document.read_progress = progress
      }
      
      if (this.currentDocument && this.currentDocument.id === id) {
        this.currentDocument.read_progress = progress
      }
    },

    removeDocument(id: string) {
      const index = this.documents.findIndex(doc => doc.id === id)
      if (index > -1) {
        this.documents.splice(index, 1)
      }
      
      if (this.currentDocument && this.currentDocument.id === id) {
        this.currentDocument = null
      }
    },

    searchDocuments(query: string) {
      if (!query.trim()) {
        return this.documents
      }
      
      const searchTerm = query.toLowerCase()
      return this.documents.filter(doc =>
        (doc.title || '').toLowerCase().includes(searchTerm) ||
        ((doc.author || '') as string).toLowerCase().includes(searchTerm) ||
        ((doc.abstract || (doc as any).abstractText || '') as string).toLowerCase().includes(searchTerm) ||
        (doc.tags || []).some(tag => (tag || '').toLowerCase().includes(searchTerm))
      )
    }
  }
})