export interface ChatRequest {
  /** 会话 ID (前端生成 UUID) */
  chatId: string;
  /** 文献 ID */
  documentId: string;
  /** 用户提问内容 */
  prompt: string;
}

export interface MessageResponse {
  /** 角色: user | assistant */
  role: 'user' | 'assistant';
  /** 消息内容 */
  content: string;
}

/**
 * 历史会话 ID 列表响应
 * 后端直接返回 string[]
 */
export type HistoryListResponse = string[];

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
  isTyping?: boolean;
  isError?: boolean;
}

export interface AiChatResponse {
  message: string;
  conversation_id: string;
  model: string;
  usage: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
  };
}

export interface AiStreamChunk {
  id: string;
  object: string;
  created: number;
  model: string;
  choices: {
    index: number;
    delta: {
      content?: string;
      role?: string;
    };
    finish_reason?: string | null;
  }[];
}
