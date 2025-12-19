import { describe, it, expect } from 'vitest'
import { 
  createSuccessResponse, 
  createErrorResponse, 
  getMockHandler,
  mockDocuments,
  mockCategories
} from '../mockManager'

describe('Mock Manager', () => {
  describe('Response Helpers', () => {
    it('should create success response correctly', () => {
      const data = { id: 1, name: 'test' }
      const response = createSuccessResponse(data, 'Success message')
      
      expect(response).toEqual({
        code: 200,
        msg: 'Success message',
        data: data
      })
    })

    it('should create error response correctly', () => {
      const response = createErrorResponse('Error message')
      
      expect(response).toEqual({
        code: 400,
        msg: 'Error message',
        data: null
      })
    })

    it('should create error response with custom code', () => {
      const response = createErrorResponse('Not found', 404)
      
      expect(response).toEqual({
        code: 404,
        msg: 'Not found',
        data: null
      })
    })
  })

  describe('Mock Data', () => {
    it('should have mock documents', () => {
      expect(mockDocuments).toBeDefined()
      expect(Array.isArray(mockDocuments)).toBe(true)
      expect(mockDocuments.length).toBeGreaterThan(0)
    })

    it('should have mock categories', () => {
      expect(mockCategories).toBeDefined()
      expect(Array.isArray(mockCategories)).toBe(true)
      expect(mockCategories.length).toBeGreaterThan(0)
    })

    it('should have valid document structure', () => {
      const doc = mockDocuments[0]
      expect(doc).toHaveProperty('id')
      expect(doc).toHaveProperty('title')
      expect(doc).toHaveProperty('author')
      expect(doc).toHaveProperty('status')
      expect(doc).toHaveProperty('read_progress')
    })

    it('should have valid category structure', () => {
      const category = mockCategories[0]
      expect(category).toHaveProperty('id')
      expect(category).toHaveProperty('name')
      expect(category).toHaveProperty('document_count')
    })
  })

  describe('Mock API Handlers', () => {
    it('should get documents handler', () => {
      const handler = getMockHandler('GET', '/api/documents')
      expect(handler).toBeDefined()
      expect(typeof handler).toBe('function')
    })

    it('should get categories handler', () => {
      const handler = getMockHandler('GET', '/api/categories')
      expect(handler).toBeDefined()
      expect(typeof handler).toBe('function')
    })

    it('should return undefined for non-existent handler', () => {
      const handler = getMockHandler('GET', '/api/non-existent')
      expect(handler).toBeUndefined()
    })
  })

  describe('Mock API Responses', () => {
    it('should handle documents API call', async () => {
      const handler = getMockHandler('GET', '/api/documents')
      if (handler) {
        const response = await handler()
        expect(response.code).toBe(200)
        expect(response.data).toBeDefined()
        expect(Array.isArray(response.data)).toBe(true)
      }
    })

    it('should handle categories API call', async () => {
      const handler = getMockHandler('GET', '/api/categories')
      if (handler) {
        const response = await handler()
        expect(response.code).toBe(200)
        expect(response.data).toBeDefined()
        expect(Array.isArray(response.data)).toBe(true)
      }
    })

    it('should filter documents by category', async () => {
      const handler = getMockHandler('GET', '/api/documents')
      if (handler) {
        const response = await handler({ category_id: 'cat_1' })
        expect(response.code).toBe(200)
        expect(response.data.every((doc: any) => doc.category_id === 'cat_1')).toBe(true)
      }
    })

    it('should search documents by query', async () => {
      const handler = getMockHandler('GET', '/api/documents')
      if (handler) {
        const response = await handler({ search: 'attention' })
        expect(response.code).toBe(200)
        expect(response.data.length).toBeGreaterThan(0)
      }
    })
  })
})