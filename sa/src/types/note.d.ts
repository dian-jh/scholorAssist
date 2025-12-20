import { PageResult } from './api'

/**
 * 笔记/标注对象 (DTO)
 */
export interface NoteDTO {
  id: string;
  documentId: string;
  content: string; // 笔记内容 (可为空)
  referenceText: string; // 引用原文
  coord: string; // 坐标信息 (JSON字符串)
  color: string;
  tags: string[];
  createTime: string;
}

/**
 * 创建笔记请求
 */
export interface NoteCreateRequest {
  documentId: string;
  content?: string;
  referenceText?: string;
  coord: string; // JSON String
  color?: string;
  tags?: string[];
}

/**
 * 查询笔记请求
 */
export interface NoteQueryRequest {
  documentId?: string;
  keyword?: string;
  tags?: string[];
  page?: number;
  pageSize?: number;
}

/**
 * 更新笔记请求
 */
export interface NoteUpdateRequest {
  id: string;
  content?: string;
  color?: string;
  tags?: string[];
}
