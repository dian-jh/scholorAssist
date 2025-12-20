import { getDocumentBytes } from '@/api/DocumentApi'
import { getNotesList } from '@/api/NotesApi'
import type { NoteDTO } from '@/types/note'

export interface DocumentCompositeData {
  docId: string
  bytes: ArrayBuffer
  blobUrl: string
  notes: NoteDTO[]
  fetchedAt: number
}

// 简单的内存缓存与请求合并，避免重复并发与重复加载
const cache = new Map<string, DocumentCompositeData>()
const inFlight = new Map<string, Promise<DocumentCompositeData>>()

// 默认缓存有效期（毫秒）。在阅读会话中 60s 足够避免频繁重复请求。
const DEFAULT_TTL = 60_000

/**
 * 获取文档的字节流与笔记数据（复合请求）。
 * 使用现有后端接口：
 * - `/files/documents/:id/bytes` 获取PDF字节流
 * - `/notes?document_id=:id` 获取笔记列表
 * 不新增后端API，仅在前端进行聚合与缓存。
 */
export async function fetchDocumentComposite(
  docId: string,
  options?: { ttl?: number; pageSize?: number }
): Promise<DocumentCompositeData> {
  const ttl = options?.ttl ?? DEFAULT_TTL
  const now = Date.now()

  // 命中缓存且未过期
  const cached = cache.get(docId)
  if (cached && now - cached.fetchedAt < ttl) {
    return cached
  }

  // 合并并发请求：若已有在途请求，直接复用
  const pending = inFlight.get(docId)
  if (pending) return pending

  const promise = (async () => {
    // 并行拉取，缩短总耗时
    const [bytes, notesResp] = await Promise.all([
      getDocumentBytes(docId),
      getNotesList({ documentId: docId, page: 1, pageSize: options?.pageSize ?? 200 })
    ])

    const blob = new Blob([bytes], { type: 'application/pdf' })
    const blobUrl = URL.createObjectURL(blob)

    const notes = notesResp?.data?.records ?? []

    const result: DocumentCompositeData = {
      docId,
      bytes,
      blobUrl,
      notes,
      fetchedAt: Date.now()
    }

    cache.set(docId, result)
    return result
  })()

  inFlight.set(docId, promise)
  try {
    const res = await promise
    return res
  } finally {
    // 请求结束后清理 inFlight 引用
    inFlight.delete(docId)
  }
}

/** 获取缓存（若存在且未过期） */
export function getCachedComposite(docId: string, ttl: number = DEFAULT_TTL): DocumentCompositeData | undefined {
  const item = cache.get(docId)
  if (!item) return undefined
  if (Date.now() - item.fetchedAt > ttl) return undefined
  return item
}

/** 主动失效缓存（例如文档或笔记更新后） */
export function invalidateComposite(docId: string): void {
  const item = cache.get(docId)
  if (item) {
    try { URL.revokeObjectURL(item.blobUrl) } catch {}
  }
  cache.delete(docId)
  inFlight.delete(docId)
}

/** 统计用：返回最近一次复合请求的耗时估计（仅供日志调试） */
export async function timedFetch(docId: string): Promise<{ data: DocumentCompositeData; durationMs: number }> {
  const start = performance.now()
  const data = await fetchDocumentComposite(docId)
  const durationMs = performance.now() - start
  return { data, durationMs }
}