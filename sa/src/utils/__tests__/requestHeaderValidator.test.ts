import { describe, it, expect, beforeEach } from 'vitest'
import { RequestHeaderValidator, ALLOWED_HEADERS, PUBLIC_PATHS, CORS_CONFIG } from '../requestHeaderValidator'

describe('RequestHeaderValidator', () => {
  describe('requiresAuthorization', () => {
    it('应该正确识别需要Authorization的路径', () => {
      const protectedPaths = [
        'http://localhost:8080/api/documents',
        'http://localhost:8080/api/categories',
        'http://localhost:8080/api/notes',
        'http://localhost:8080/api/users/profile',
        'http://localhost:8080/api/statistics'
      ]

      protectedPaths.forEach(path => {
        expect(RequestHeaderValidator.requiresAuthorization(path)).toBe(true)
      })
    })

    it('应该正确识别不需要Authorization的公开路径', () => {
      const publicPaths = [
        'http://localhost:8080/api/users/register',
        'http://localhost:8080/api/users/login',
        'http://localhost:8080/api/users/check-username',
        'http://localhost:8080/api/users/check-email',
        'http://localhost:8080/actuator/health',
        'http://localhost:8080/swagger-ui/index.html',
        'http://localhost:8080/v3/api-docs',
        'http://localhost:8080/favicon.ico',
        'http://localhost:8080/error'
      ]

      publicPaths.forEach(path => {
        expect(RequestHeaderValidator.requiresAuthorization(path)).toBe(false)
      })
    })

    it('应该处理带查询参数的URL', () => {
      const urlWithQuery = 'http://localhost:8080/api/users/login?redirect=/dashboard'
      expect(RequestHeaderValidator.requiresAuthorization(urlWithQuery)).toBe(false)
    })

    it('应该处理相对路径', () => {
      const relativePath = '/api/users/register'
      expect(RequestHeaderValidator.requiresAuthorization(relativePath)).toBe(false)
    })
  })

  describe('validateHeaders', () => {
    it('应该只保留允许的请求头', () => {
      const headers = {
        'Authorization': 'Bearer token123',
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Origin': 'http://localhost:3000',
        'X-Requested-With': 'XMLHttpRequest',
        'X-User-Id': '123', // 应该被移除
        'X-Username': 'testuser', // 应该被移除
        'X-Custom-Header': 'custom' // 应该被移除
      }

      const validHeaders = RequestHeaderValidator.validateHeaders(headers)

      expect(validHeaders).toEqual({
        'Authorization': 'Bearer token123',
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Origin': 'http://localhost:3000',
        'X-Requested-With': 'XMLHttpRequest'
      })
    })

    it('应该处理空值和null值', () => {
      const headers = {
        'Authorization': 'Bearer token123',
        'Content-Type': null,
        'Accept': undefined,
        'Origin': ''
      }

      const validHeaders = RequestHeaderValidator.validateHeaders(headers)

      expect(validHeaders).toEqual({
        'Authorization': 'Bearer token123',
        'Origin': ''
      })
    })

    it('应该处理大小写不敏感的请求头', () => {
      const headers = {
        'authorization': 'Bearer token123',
        'content-type': 'application/json',
        'ACCEPT': 'application/json'
      }

      const validHeaders = RequestHeaderValidator.validateHeaders(headers)

      expect(validHeaders).toEqual({
        'Authorization': 'Bearer token123',
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      })
    })
  })

  describe('configureHeaders', () => {
    it('应该为需要认证的路径添加Authorization头', () => {
      const url = 'http://localhost:8080/api/documents'
      const headers = { 'Content-Type': 'application/json' }
      const token = 'test-token-123'

      const configuredHeaders = RequestHeaderValidator.configureHeaders(url, headers, token)

      expect(configuredHeaders).toEqual({
        'Content-Type': 'application/json',
        'Accept': 'application/json, text/plain, */*',
        'Authorization': 'Bearer test-token-123'
      })
    })

    it('应该为公开路径不添加Authorization头', () => {
      const url = 'http://localhost:8080/api/users/login'
      const headers = { 'Content-Type': 'application/json' }
      const token = 'test-token-123'

      const configuredHeaders = RequestHeaderValidator.configureHeaders(url, headers, token)

      expect(configuredHeaders).toEqual({
        'Content-Type': 'application/json',
        'Accept': 'application/json, text/plain, */*'
      })
      expect(configuredHeaders.Authorization).toBeUndefined()
    })

    it('应该设置默认的Content-Type和Accept头', () => {
      const url = 'http://localhost:8080/api/documents'
      const headers = {}
      const token = 'test-token-123'

      const configuredHeaders = RequestHeaderValidator.configureHeaders(url, headers, token)

      expect(configuredHeaders['Content-Type']).toBe('application/json')
      expect(configuredHeaders['Accept']).toBe('application/json, text/plain, */*')
    })

    it('应该保留已存在的Content-Type和Accept头', () => {
      const url = 'http://localhost:8080/api/documents'
      const headers = {
        'Content-Type': 'multipart/form-data',
        'Accept': 'text/html'
      }
      const token = 'test-token-123'

      const configuredHeaders = RequestHeaderValidator.configureHeaders(url, headers, token)

      expect(configuredHeaders['Content-Type']).toBe('multipart/form-data')
      expect(configuredHeaders['Accept']).toBe('text/html')
    })

    it('应该在没有token时不添加Authorization头', () => {
      const url = 'http://localhost:8080/api/documents'
      const headers = { 'Content-Type': 'application/json' }

      const configuredHeaders = RequestHeaderValidator.configureHeaders(url, headers)

      expect(configuredHeaders.Authorization).toBeUndefined()
    })
  })

  describe('getDisallowedHeaders', () => {
    it('应该识别不允许的请求头', () => {
      const headers = {
        'Authorization': 'Bearer token123',
        'Content-Type': 'application/json',
        'X-User-Id': '123',
        'X-Custom-Header': 'custom',
        'X-Request-ID': 'req-123'
      }

      const disallowed = RequestHeaderValidator.getDisallowedHeaders(headers)

      expect(disallowed).toEqual(['X-User-Id', 'X-Custom-Header', 'X-Request-ID'])
    })

    it('应该在所有请求头都允许时返回空数组', () => {
      const headers = {
        'Authorization': 'Bearer token123',
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Origin': 'http://localhost:3000',
        'X-Requested-With': 'XMLHttpRequest'
      }

      const disallowed = RequestHeaderValidator.getDisallowedHeaders(headers)

      expect(disallowed).toEqual([])
    })
  })

  describe('常量验证', () => {
    it('ALLOWED_HEADERS应该包含所有必需的请求头', () => {
      const expectedHeaders = [
        'Authorization',
        'Content-Type',
        'Accept',
        'Origin',
        'X-Requested-With'
      ]

      expectedHeaders.forEach(header => {
        expect(ALLOWED_HEADERS).toContain(header)
      })
    })

    it('PUBLIC_PATHS应该包含所有公开路径', () => {
      const expectedPaths = [
        '/api/users/register',
        '/api/users/login',
        '/api/users/check-username',
        '/api/users/check-email',
        '/actuator',
        '/swagger-ui',
        '/v3/api-docs',
        '/favicon.ico',
        '/error'
      ]

      expectedPaths.forEach(path => {
        expect(PUBLIC_PATHS).toContain(path)
      })
    })

    it('CORS_CONFIG应该有正确的配置', () => {
      expect(CORS_CONFIG.credentials).toBe(true)
      expect(CORS_CONFIG.maxAge).toBe(3600)
    })
  })
})