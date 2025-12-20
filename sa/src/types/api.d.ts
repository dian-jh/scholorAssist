/**
 * 通用响应包装
 */
export interface ApiResponse<T = any> {
  code: number;      // 200 表示成功
  msg: string;       // 提示信息
  data: T;           // 业务数据
  requestId?: string;// 链路追踪 ID
}

/**
 * 分页响应包装
 */
export interface PageResult<T> {
  records: T[];    // 列表数据
  total: number;   // 总数
  size: number;    // 页大小
  current: number; // 当前页
}
