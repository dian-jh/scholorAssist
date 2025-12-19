import { get } from './index'
import type { ApiResponse } from './index'

// 活动类型枚举
export enum ActivityType {
  UPLOAD = 'upload',
  COMPLETE = 'complete', 
  NOTE = 'note',
  AI_CHAT = 'ai_chat'
}

// 统计数据接口
export interface StatisticsData {
  totalDocuments: number
  completedReading: number
  aiQuestions: number
  totalNotes: number
  todayReading: number
  weeklyReading: number
  monthlyReading: number
  averageReadingTime: number
  favoriteCategories: FavoriteCategory[]
  recentActivity: RecentActivity[]
}

// 常用分类接口
export interface FavoriteCategory {
  category_id: string
  category_name: string
  document_count: number
}

// 最近活动接口
export interface RecentActivity {
  type: ActivityType
  description: string
  timestamp: string
}

/**
 * 获取统计数据
 * 功能描述：获取用户的各项统计数据
 * 入参：无
 * 返回参数：{ code, msg, data: StatisticsData }
 * URL：/api/statistics
 * 请求方式：GET
 */
export function getStatistics(): Promise<ApiResponse<StatisticsData>> {
  return get<StatisticsData>('/statistics')
}

// 活动类型工具函数
export class ActivityTypeHelper {
  /**
   * 获取活动类型的显示名称
   */
  static getDisplayName(type: ActivityType): string {
    const displayNames = {
      [ActivityType.UPLOAD]: '上传文档',
      [ActivityType.COMPLETE]: '完成阅读',
      [ActivityType.NOTE]: '创建笔记',
      [ActivityType.AI_CHAT]: 'AI对话'
    }
    return displayNames[type] || '未知活动'
  }

  /**
   * 获取活动类型的图标
   */
  static getIcon(type: ActivityType): string {
    const icons = {
      [ActivityType.UPLOAD]: 'Upload',
      [ActivityType.COMPLETE]: 'Check',
      [ActivityType.NOTE]: 'Edit',
      [ActivityType.AI_CHAT]: 'ChatDotRound'
    }
    return icons[type] || 'InfoFilled'
  }

  /**
   * 获取活动类型的颜色
   */
  static getColor(type: ActivityType): string {
    const colors = {
      [ActivityType.UPLOAD]: '#409EFF',
      [ActivityType.COMPLETE]: '#67C23A',
      [ActivityType.NOTE]: '#E6A23C',
      [ActivityType.AI_CHAT]: '#F56C6C'
    }
    return colors[type] || '#909399'
  }

  /**
   * 验证活动类型
   */
  static isValidType(type: string): type is ActivityType {
    return Object.values(ActivityType).includes(type as ActivityType)
  }
}