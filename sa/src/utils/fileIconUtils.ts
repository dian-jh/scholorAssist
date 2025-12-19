import { 
  Reading, 
  Document, 
  Files, 
  Picture, 
  VideoPlay, 
  Headset,
  DocumentCopy,
  Notebook,
  DataAnalysis
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

// 文件类型定义
export interface FileTypeInfo {
  icon: Component
  color: string
  category: string
  description: string
}

// 文件类型映射
const FILE_TYPE_MAP: Record<string, FileTypeInfo> = {
  // PDF文档
  pdf: {
    icon: Reading,
    color: '#FF4444',
    category: 'document',
    description: 'PDF文档'
  },
  
  // Word文档
  doc: {
    icon: Document,
    color: '#2B579A',
    category: 'document',
    description: 'Word文档'
  },
  docx: {
    icon: Document,
    color: '#2B579A',
    category: 'document',
    description: 'Word文档'
  },
  
  // PowerPoint
  ppt: {
    icon: DocumentCopy,
    color: '#D24726',
    category: 'presentation',
    description: 'PowerPoint演示文稿'
  },
  pptx: {
    icon: DocumentCopy,
    color: '#D24726',
    category: 'presentation',
    description: 'PowerPoint演示文稿'
  },
  
  // Excel
  xls: {
    icon: DataAnalysis,
    color: '#217346',
    category: 'spreadsheet',
    description: 'Excel表格'
  },
  xlsx: {
    icon: DataAnalysis,
    color: '#217346',
    category: 'spreadsheet',
    description: 'Excel表格'
  },
  
  // 文本文件
  txt: {
    icon: Notebook,
    color: '#666666',
    category: 'text',
    description: '文本文件'
  },
  md: {
    icon: Notebook,
    color: '#083FA1',
    category: 'text',
    description: 'Markdown文档'
  },
  
  // 图片文件
  jpg: {
    icon: Picture,
    color: '#FF6B35',
    category: 'image',
    description: 'JPEG图片'
  },
  jpeg: {
    icon: Picture,
    color: '#FF6B35',
    category: 'image',
    description: 'JPEG图片'
  },
  png: {
    icon: Picture,
    color: '#FF6B35',
    category: 'image',
    description: 'PNG图片'
  },
  gif: {
    icon: Picture,
    color: '#FF6B35',
    category: 'image',
    description: 'GIF图片'
  },
  svg: {
    icon: Picture,
    color: '#FF6B35',
    category: 'image',
    description: 'SVG矢量图'
  },
  
  // 视频文件
  mp4: {
    icon: VideoPlay,
    color: '#9C27B0',
    category: 'video',
    description: 'MP4视频'
  },
  avi: {
    icon: VideoPlay,
    color: '#9C27B0',
    category: 'video',
    description: 'AVI视频'
  },
  mov: {
    icon: VideoPlay,
    color: '#9C27B0',
    category: 'video',
    description: 'MOV视频'
  },
  
  // 音频文件
  mp3: {
    icon: Headset,
    color: '#FF9800',
    category: 'audio',
    description: 'MP3音频'
  },
  wav: {
    icon: Headset,
    color: '#FF9800',
    category: 'audio',
    description: 'WAV音频'
  },
  
  // 默认文件类型
  default: {
    icon: Files,
    color: '#666666',
    category: 'unknown',
    description: '未知文件类型'
  }
}

// 学术文献特殊标识
const ACADEMIC_KEYWORDS = [
  'paper', 'thesis', 'dissertation', 'journal', 'conference',
  'research', 'study', 'analysis', 'review', 'survey',
  '论文', '研究', '分析', '综述', '调研', '学术'
]

/**
 * 根据文件名获取文件类型信息
 */
export function getFileTypeInfo(filename: string): FileTypeInfo {
  const extension = getFileExtension(filename)
  return FILE_TYPE_MAP[extension] || FILE_TYPE_MAP.default
}

/**
 * 获取文件扩展名
 */
export function getFileExtension(filename: string): string {
  const parts = filename.split('.')
  return parts.length > 1 ? parts.pop()!.toLowerCase() : ''
}

/**
 * 判断是否为学术文献
 */
export function isAcademicDocument(filename: string, title?: string): boolean {
  const searchText = `${filename} ${title || ''}`.toLowerCase()
  return ACADEMIC_KEYWORDS.some(keyword => searchText.includes(keyword))
}

/**
 * 获取文件图标组件
 */
export function getFileIcon(filename: string): Component {
  return getFileTypeInfo(filename).icon
}

/**
 * 获取文件图标颜色
 */
export function getFileIconColor(filename: string, isAcademic?: boolean): string {
  const typeInfo = getFileTypeInfo(filename)
  
  // 学术文献使用特殊颜色
  if (isAcademic && typeInfo.category === 'document') {
    return '#1890FF' // 学术蓝
  }
  
  return typeInfo.color
}

/**
 * 获取文件类别
 */
export function getFileCategory(filename: string): string {
  return getFileTypeInfo(filename).category
}

/**
 * 获取文件描述
 */
export function getFileDescription(filename: string): string {
  return getFileTypeInfo(filename).description
}

/**
 * 根据文件大小格式化显示
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

/**
 * 解析文件大小字符串为数字
 */
export function parseFileSize(sizeStr: string): number {
  const match = sizeStr.match(/^([\d.]+)\s*(B|KB|MB|GB|TB)$/i)
  if (!match) return 0
  
  const value = parseFloat(match[1])
  const unit = match[2].toUpperCase()
  
  const multipliers: Record<string, number> = {
    'B': 1,
    'KB': 1024,
    'MB': 1024 * 1024,
    'GB': 1024 * 1024 * 1024,
    'TB': 1024 * 1024 * 1024 * 1024
  }
  
  return value * (multipliers[unit] || 1)
}

/**
 * 获取支持的文件类型列表
 */
export function getSupportedFileTypes(): string[] {
  return Object.keys(FILE_TYPE_MAP).filter(key => key !== 'default')
}

/**
 * 检查文件类型是否支持
 */
export function isSupportedFileType(filename: string): boolean {
  const extension = getFileExtension(filename)
  return extension in FILE_TYPE_MAP
}

/**
 * 获取文件类型统计
 */
export function getFileTypeStats(filenames: string[]): Record<string, number> {
  const stats: Record<string, number> = {}
  
  filenames.forEach(filename => {
    const category = getFileCategory(filename)
    stats[category] = (stats[category] || 0) + 1
  })
  
  return stats
}

// 导出常用的文件类型常量
export const FILE_CATEGORIES = {
  DOCUMENT: 'document',
  PRESENTATION: 'presentation',
  SPREADSHEET: 'spreadsheet',
  TEXT: 'text',
  IMAGE: 'image',
  VIDEO: 'video',
  AUDIO: 'audio',
  UNKNOWN: 'unknown'
} as const

export const ACADEMIC_FILE_COLOR = '#1890FF'
export const DEFAULT_FILE_COLOR = '#666666'