import type { ApiResponse } from './index'
import type { UserInfo, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from './UserApi'
import type { StatisticsData, ActivityType } from './StatisticsApi'
import { ActivityType as ActivityTypeEnum } from './StatisticsApi'

// Mock数据类型定义
export interface MockDocument {
  id: string
  title: string
  filename: string
  category_id: string
  author: string
  upload_date: string
  file_size: string
  pages: number
  status: 'ready' | 'processing'
  thumbnail: string
  abstract: string
  tags: string[]
  read_progress: number
}

export interface MockCategory {
  id: string
  name: string
  parent_id: string | null
  created_at: string
  document_count: number
  children?: MockCategory[]
}

export interface MockNote {
  id: string
  document_id: string
  title: string
  content: string
  page_number: number
  created_at: string
  updated_at: string
  tags: string[]
}

// Mock用户数据
let mockUsers: UserInfo[] = [
  {
    user_id: "user_1",
    username: "admin",
    email: "admin@example.com",
    real_name: "系统管理员",
    role: "admin",
    status: "active",
    created_at: "2024-01-01T00:00:00Z",
    last_login_at: "2024-01-21T10:30:00Z",
    email_verified: true
  },
  {
    user_id: "user_2", 
    username: "testuser",
    email: "test@example.com",
    real_name: "测试用户",
    role: "user",
    status: "active",
    created_at: "2024-01-15T10:30:00Z",
    last_login_at: "2024-01-20T15:20:00Z",
    email_verified: true
  }
]

// Mock用户密码（实际项目中不应该存储明文密码）
const mockPasswords: Record<string, string> = {
  "admin@example.com": "Admin123!",
  "admin": "Admin123!",
  "test@example.com": "Test123!",
  "testuser": "Test123!"
}

// Mock数据存储
let mockDocuments: MockDocument[] = [
  {
    id: "doc_1",
    title: "Attention Is All You Need",
    filename: "attention_is_all_you_need.pdf",
    category_id: "cat_2",
    author: "Vaswani et al.",
    upload_date: "2024-01-15T10:30:00Z",
    file_size: "2.3 MB",
    pages: 15,
    status: "ready",
    thumbnail: "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=200&h=280&fit=crop",
    abstract: "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks...",
    tags: ["transformer", "attention", "nlp"],
    read_progress: 0.6
  },
  {
    id: "doc_2", 
    title: "Deep Residual Learning for Image Recognition",
    filename: "resnet_paper.pdf",
    category_id: "cat_3",
    author: "He et al.",
    upload_date: "2024-01-16T14:20:00Z",
    file_size: "1.8 MB", 
    pages: 12,
    status: "ready",
    thumbnail: "https://images.unsplash.com/photo-1555949963-aa79dcee981c?w=200&h=280&fit=crop",
    abstract: "Deeper neural networks are more difficult to train. We present a residual learning framework...",
    tags: ["cnn", "resnet", "computer vision"],
    read_progress: 0.3
  },
  {
    id: "doc_3",
    title: "BERT: Pre-training of Deep Bidirectional Transformers",
    filename: "bert_paper.pdf", 
    category_id: "cat_2_1",
    author: "Devlin et al.",
    upload_date: "2024-01-17T09:45:00Z",
    file_size: "3.1 MB",
    pages: 16,
    status: "processing",
    thumbnail: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=280&fit=crop",
    abstract: "We introduce a new language representation model called BERT...",
    tags: ["bert", "transformer", "pre-training"],
    read_progress: 0.0
  }
]

let mockCategories: MockCategory[] = [
  {
    id: "cat_1",
    name: "机器学习",
    parent_id: null,
    created_at: "2024-01-15T10:30:00Z",
    document_count: 15,
    children: [
      {
        id: "cat_1_1",
        name: "深度学习",
        parent_id: "cat_1",
        created_at: "2024-01-16T09:20:00Z",
        document_count: 8
      },
      {
        id: "cat_1_2", 
        name: "强化学习",
        parent_id: "cat_1",
        created_at: "2024-01-17T14:45:00Z",
        document_count: 7
      }
    ]
  },
  {
    id: "cat_2",
    name: "自然语言处理",
    parent_id: null,
    created_at: "2024-01-18T11:15:00Z",
    document_count: 12,
    children: [
      {
        id: "cat_2_1",
        name: "文本分类",
        parent_id: "cat_2", 
        created_at: "2024-01-19T16:30:00Z",
        document_count: 5
      }
    ]
  },
  {
    id: "cat_3",
    name: "计算机视觉",
    parent_id: null,
    created_at: "2024-01-21T08:40:00Z",
    document_count: 9
  }
]

let mockNotes: MockNote[] = [
  {
    id: "note_1",
    document_id: "doc_1",
    title: "Transformer架构要点",
    content: "Transformer完全基于注意力机制，摒弃了循环和卷积结构...",
    page_number: 3,
    created_at: "2024-01-20T10:30:00Z",
    updated_at: "2024-01-20T10:30:00Z",
    tags: ["transformer", "architecture"]
  }
]

// 创建成功响应
export function createSuccessResponse<T>(data: T, message = 'success'): ApiResponse<T> {
  return {
    code: 200,
    msg: message,
    data: data
  }
}

// 创建错误响应
export function createErrorResponse(message: string, code = 400): ApiResponse<null> {
  return {
    code: code,
    msg: message,
    data: null
  }
}

// Mock API注册器
type MockHandler = (params?: any) => Promise<ApiResponse<any>>
const mockApis = new Map<string, MockHandler>()

export function registerMockApi(method: string, url: string, handler: MockHandler) {
  const key = `${method.toUpperCase()} ${url}`
  mockApis.set(key, handler)
}

// 获取Mock API处理器
export function getMockHandler(method: string, url: string): MockHandler | undefined {
  const key = `${method.toUpperCase()} ${url}`
  return mockApis.get(key)
}

// 注册所有Mock接口
function registerAllMockApis() {
  // 文档相关接口
  registerMockApi('GET', '/api/documents', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 500)) // 模拟网络延迟
    
    let filteredDocs = [...mockDocuments]
    
    if (params?.category_id && params.category_id !== 'all') {
      filteredDocs = filteredDocs.filter(doc => doc.category_id === params.category_id)
    }
    
    if (params?.search) {
      const searchTerm = params.search.toLowerCase()
      filteredDocs = filteredDocs.filter(doc => 
        doc.title.toLowerCase().includes(searchTerm) ||
        doc.author.toLowerCase().includes(searchTerm) ||
        doc.abstract.toLowerCase().includes(searchTerm)
      )
    }
    
    return createSuccessResponse(filteredDocs, '获取文档列表成功')
  })

  registerMockApi('GET', '/api/documents/:id', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 300))
    
    const doc = mockDocuments.find(d => d.id === params.id)
    if (!doc) {
      return createErrorResponse('文档不存在')
    }
    
    return createSuccessResponse(doc, '获取文档详情成功')
  })

  registerMockApi('POST', '/api/documents/upload', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 2000)) // 模拟上传时间
    
    const newDoc: MockDocument = {
      id: `doc_${Date.now()}`,
      title: params.title || '新上传文档',
      filename: params.filename,
      category_id: params.category_id || 'cat_1',
      author: '未知作者',
      upload_date: new Date().toISOString(),
      file_size: params.file_size || '1.0 MB',
      pages: params.pages || 10,
      status: 'processing',
      thumbnail: 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=200&h=280&fit=crop',
      abstract: '文档正在处理中...',
      tags: [],
      read_progress: 0
    }
    
    mockDocuments.unshift(newDoc)
    return createSuccessResponse(newDoc, '文档上传成功')
  })

  // 分类相关接口
  registerMockApi('GET', '/api/categories', async () => {
    await new Promise(resolve => setTimeout(resolve, 300))
    return createSuccessResponse(mockCategories, '获取分类列表成功')
  })

  registerMockApi('POST', '/api/categories', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 500))
    
    const newCategory: MockCategory = {
      id: `cat_${Date.now()}`,
      name: params.name,
      parent_id: params.parent_id || null,
      created_at: new Date().toISOString(),
      document_count: 0
    }
    
    mockCategories.push(newCategory)
    return createSuccessResponse(newCategory, '创建分类成功')
  })

  // 笔记相关接口
  registerMockApi('GET', '/api/notes', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 300))
    
    let filteredNotes = [...mockNotes]
    if (params?.document_id) {
      filteredNotes = filteredNotes.filter(note => note.document_id === params.document_id)
    }
    
    return createSuccessResponse(filteredNotes, '获取笔记列表成功')
  })

  registerMockApi('POST', '/api/notes', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 500))
    
    const newNote: MockNote = {
      id: `note_${Date.now()}`,
      document_id: params.document_id,
      title: params.title,
      content: params.content,
      page_number: params.page_number || 1,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      tags: params.tags || []
    }
    
    mockNotes.unshift(newNote)
    return createSuccessResponse(newNote, '创建笔记成功')
  })

  // 统计相关接口
  registerMockApi('GET', '/api/statistics', async () => {
    await new Promise(resolve => setTimeout(resolve, 200))
    
    const stats: StatisticsData = {
      totalDocuments: mockDocuments.length,
      completedReading: mockDocuments.filter(doc => doc.read_progress >= 1).length,
      aiQuestions: 156,
      totalNotes: mockNotes.length,
      todayReading: 3,
      weeklyReading: 12,
      monthlyReading: 25,
      averageReadingTime: 45,
      favoriteCategories: [
        {
          category_id: "cat_1",
          category_name: "机器学习",
          document_count: 12
        },
        {
          category_id: "cat_2",
          category_name: "自然语言处理",
          document_count: 8
        }
      ],
      recentActivity: [
        {
          type: ActivityTypeEnum.UPLOAD,
          description: "上传了《Attention Is All You Need》",
          timestamp: "2024-01-21T10:30:00Z"
        },
        {
          type: ActivityTypeEnum.COMPLETE,
          description: "完成了《ResNet论文》的阅读",
          timestamp: "2024-01-20T15:20:00Z"
        },
        {
          type: ActivityTypeEnum.NOTE,
          description: "创建了5条新笔记",
          timestamp: "2024-01-20T14:10:00Z"
        },
        {
          type: ActivityTypeEnum.AI_CHAT,
          description: "进行了AI问答对话",
          timestamp: "2024-01-20T12:30:00Z"
        }
      ]
    }
    
    return createSuccessResponse(stats, '获取统计数据成功')
  })

  // 用户认证相关接口
  registerMockApi('POST', '/api/users/login', async (params: LoginRequest) => {
    await new Promise(resolve => setTimeout(resolve, 800)) // 模拟网络延迟
    
    const { login, password } = params
    
    // 查找用户（支持用户名或邮箱登录）
    const user = mockUsers.find(u => 
      u.email === login || u.username === login
    )
    
    if (!user) {
      return createErrorResponse('用户不存在', 404)
    }
    
    // 验证密码
    const correctPassword = mockPasswords[login]
    if (!correctPassword || correctPassword !== password) {
      return createErrorResponse('密码错误', 401)
    }
    
    // 检查用户状态
    if (user.status !== 'active') {
      return createErrorResponse('账号已被禁用', 403)
    }
    
    // 生成Token
    const token = `mock-jwt-token-${user.user_id}-${Date.now()}`
    
    // 更新最后登录时间
    user.last_login_at = new Date().toISOString()
    
    // 计算过期时间（2小时后）
    const expiresAt = new Date(Date.now() + 7200 * 1000).toISOString()
    
    const loginResponse: LoginResponse = {
      user_id: user.user_id,
      username: user.username,
      email: user.email,
      real_name: user.real_name,
      role: user.role,
      status: user.status,
      token: token,
      expires_at: expiresAt,
      last_login_at: user.last_login_at
    }
    
    return createSuccessResponse(loginResponse, '登录成功')
  })

  registerMockApi('POST', '/api/users/register', async (params: RegisterRequest) => {
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    const { username, email, password, real_name } = params
    
    // 检查用户名是否已存在
    const existingUserByUsername = mockUsers.find(u => u.username === username)
    if (existingUserByUsername) {
      return createErrorResponse('用户名已存在', 409)
    }
    
    // 检查邮箱是否已存在
    const existingUserByEmail = mockUsers.find(u => u.email === email)
    if (existingUserByEmail) {
      return createErrorResponse('邮箱已被注册', 409)
    }
    
    // 创建新用户
    const newUser: UserInfo = {
      user_id: `user_${Date.now()}`,
      username: username,
      email: email,
      real_name: real_name,
      role: 'user',
      status: 'pending_verification',
      created_at: new Date().toISOString(),
      email_verified: false
    }
    
    // 添加到用户列表
    mockUsers.push(newUser)
    
    // 保存密码
    mockPasswords[username] = password
    mockPasswords[email] = password
    
    const registerResponse: RegisterResponse = {
      user_id: newUser.user_id,
      username: newUser.username,
      email: newUser.email,
      real_name: newUser.real_name,
      status: newUser.status,
      role: newUser.role,
      created_at: newUser.created_at
    }
    
    return createSuccessResponse(registerResponse, '注册成功')
  })

  registerMockApi('POST', '/api/users/logout', async () => {
    await new Promise(resolve => setTimeout(resolve, 300))
    return createSuccessResponse(null, '退出登录成功')
  })

  registerMockApi('GET', '/api/users/profile', async () => {
    await new Promise(resolve => setTimeout(resolve, 400))
    
    // 模拟获取当前登录用户信息
    const currentUser = mockUsers[0] // 假设第一个用户是当前登录用户
    
    return createSuccessResponse(currentUser, '获取用户信息成功')
  })

  registerMockApi('POST', '/api/users/forgot-password', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 1200))
    
    const { email } = params
    
    // 检查邮箱是否存在
    const user = mockUsers.find(u => u.email === email)
    if (!user) {
      return createErrorResponse('邮箱不存在', 404)
    }
    
    return createSuccessResponse(
      { message: '密码重置邮件已发送' },
      '重置链接已发送到您的邮箱'
    )
  })

  registerMockApi('POST', '/api/users/change-password', async (params) => {
    await new Promise(resolve => setTimeout(resolve, 600))
    
    const { current_password, new_password } = params
    
    // 模拟验证当前密码
    const currentUser = mockUsers[0] // 假设第一个用户是当前登录用户
    const correctPassword = mockPasswords[currentUser.email]
    
    if (correctPassword !== current_password) {
      return createErrorResponse('当前密码不正确', 400)
    }
    
    // 更新密码
    mockPasswords[currentUser.username] = new_password
    mockPasswords[currentUser.email] = new_password
    
    return createSuccessResponse(
      {
        message: '密码修改成功',
        updated_at: new Date().toISOString()
      },
      '密码修改成功'
    )
  })
}

// 初始化Mock接口
registerAllMockApis()

export { mockDocuments, mockCategories, mockNotes }