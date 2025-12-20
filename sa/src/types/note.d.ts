
import { PageResult } from './api'

/**
 * Zotero 风格的高亮数据结构
 */
export interface HighlightPosition {
  pageNumber: number; // 关联的页码 (1-based)
  rects: Array<{      // 支持多行高亮，所以是数组
    x: number;        // PDF 坐标系下的 x (比率或点)
    y: number;        // PDF 坐标系下的 y
    width: number;
    height: number;
  }>;
}

/**
 * 笔记/标注对象 (DTO)
 */
export interface NoteDTO {
  id: string;
  documentId: string;
  content: string; // 笔记内容 (可为空)
  referenceText: string; // 引用原文
  coord: string; // 坐标信息 (JSON字符串, 格式为 HighlightPosition)
  color: string;
  tags: string[];
  createTime: string;
}

/**
 * 创建笔记请求
 */
export interface NoteCreateRequest {
  document_id: string;
  content?: string;
  selected_text?: string;
  position_info: HighlightPosition;
  page_number?: number;
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
