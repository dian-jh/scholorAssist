/*
 * 文献辅助阅读系统 - PostgreSQL数据库建表脚本
 * 
 * 文件名: tables.sql
 * 生成时间: 2024-01-21
 * 数据库版本: PostgreSQL 12+
 * 字符编码: UTF-8
 * 
 * 说明: 本脚本基于 docs/tables.md 文档生成，修正了MySQL语法错误，
 *       使用PostgreSQL标准语法创建数据库表结构。
 * 
 * 使用方法:
 *   1. 确保PostgreSQL服务正在运行
 *   2. 创建数据库: CREATE DATABASE scholar_assist;
 *   3. 连接到数据库: \c scholar_assist
 *   4. 执行此脚本: \i tables.sql
 */

-- 开始事务
BEGIN;

-- 安装必要的PostgreSQL扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- UUID生成
CREATE EXTENSION IF NOT EXISTS "vector";     -- 向量相似度搜索（需要pgvector）

-- =============================================================================
-- 用户表：存储用户基本信息和认证数据
-- =============================================================================

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    status VARCHAR(20) NOT NULL DEFAULT 'pending_verification',
    avatar_url VARCHAR(255),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户表索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_user_id ON users(user_id);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 用户表注释
COMMENT ON TABLE users IS '用户表：存储系统用户的基本信息、认证数据和状态信息';
COMMENT ON COLUMN users.id IS '主键ID';
COMMENT ON COLUMN users.user_id IS '用户唯一标识，格式：user_xxxxxxxxx';
COMMENT ON COLUMN users.username IS '用户名，3-20字符';
COMMENT ON COLUMN users.email IS '邮箱地址';
COMMENT ON COLUMN users.password_hash IS '密码哈希值';
COMMENT ON COLUMN users.real_name IS '真实姓名';
COMMENT ON COLUMN users.role IS '用户角色：user/admin/super_admin';
COMMENT ON COLUMN users.status IS '用户状态：pending_verification/active/suspended';
COMMENT ON COLUMN users.avatar_url IS '头像URL';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';

-- =============================================================================
-- 分类表：存储文档分类信息，支持层级结构
-- =============================================================================

CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    category_id VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    parent_id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    document_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 分类表索引
CREATE INDEX idx_categories_category_id ON categories(category_id);
CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_user_parent ON categories(user_id, parent_id);
CREATE INDEX idx_categories_sort_order ON categories(sort_order);

-- 分类表注释
COMMENT ON TABLE categories IS '分类表：存储文档分类信息，支持多级嵌套的层级结构';
COMMENT ON COLUMN categories.id IS '主键ID';
COMMENT ON COLUMN categories.category_id IS '分类唯一标识，格式：cat_xxxxxxxxx';
COMMENT ON COLUMN categories.user_id IS '所属用户ID';
COMMENT ON COLUMN categories.parent_id IS '父分类ID，NULL表示根分类';
COMMENT ON COLUMN categories.name IS '分类名称';
COMMENT ON COLUMN categories.description IS '分类描述';
COMMENT ON COLUMN categories.sort_order IS '排序顺序';
COMMENT ON COLUMN categories.document_count IS '该分类下的文档数量';
COMMENT ON COLUMN categories.created_at IS '创建时间';
COMMENT ON COLUMN categories.updated_at IS '更新时间';

-- =============================================================================
-- 文档表：存储PDF文档的基本信息和元数据
-- =============================================================================

CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    document_id VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    category_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    abstract TEXT,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_size_display VARCHAR(20),
    pages INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'processing',
    thumbnail_url VARCHAR(500),
    tags TEXT[],
    read_progress DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 文档表索引
CREATE INDEX idx_documents_document_id ON documents(document_id);
CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_category_id ON documents(category_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_upload_date ON documents(upload_date);
CREATE INDEX idx_documents_title ON documents USING gin(to_tsvector('english', title));
CREATE INDEX idx_documents_tags ON documents USING gin(tags);

-- 文档表注释
COMMENT ON TABLE documents IS '文档表：存储PDF文档的基本信息、元数据和处理状态';
COMMENT ON COLUMN documents.id IS '主键ID';
COMMENT ON COLUMN documents.document_id IS '文档唯一标识，格式：doc_xxxxxxxxx';
COMMENT ON COLUMN documents.user_id IS '所属用户ID';
COMMENT ON COLUMN documents.category_id IS '所属分类ID';
COMMENT ON COLUMN documents.title IS '文档标题';
COMMENT ON COLUMN documents.filename IS '原始文件名';
COMMENT ON COLUMN documents.author IS '作者信息';
COMMENT ON COLUMN documents.abstract IS '文档摘要';
COMMENT ON COLUMN documents.file_path IS '文件存储路径';
COMMENT ON COLUMN documents.file_size IS '文件大小（字节）';
COMMENT ON COLUMN documents.file_size_display IS '文件大小显示（如：2.3 MB）';
COMMENT ON COLUMN documents.pages IS '页数';
COMMENT ON COLUMN documents.status IS '处理状态：processing/ready/failed';
COMMENT ON COLUMN documents.thumbnail_url IS '缩略图URL';
COMMENT ON COLUMN documents.tags IS '标签数组';
COMMENT ON COLUMN documents.read_progress IS '阅读进度（0-1）';
COMMENT ON COLUMN documents.upload_date IS '上传时间';
COMMENT ON COLUMN documents.processed_at IS '处理完成时间';
COMMENT ON COLUMN documents.created_at IS '创建时间';
COMMENT ON COLUMN documents.updated_at IS '更新时间';

-- =============================================================================
-- 文档分片表：存储文档的文本片段，用于AI向量检索
-- =============================================================================

CREATE TABLE document_chunks (
    id SERIAL PRIMARY KEY,
    chunk_id VARCHAR(50) NOT NULL UNIQUE,
    document_id VARCHAR(50) NOT NULL,
    page_number INTEGER NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_length INTEGER NOT NULL,
    embedding_vector VECTOR(1536),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 文档分片表索引
CREATE INDEX idx_chunks_chunk_id ON document_chunks(chunk_id);
CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_chunks_page_number ON document_chunks(page_number);
CREATE INDEX idx_chunks_document_page ON document_chunks(document_id, page_number);

-- 向量相似度搜索索引（需要安装pgvector扩展）
-- CREATE INDEX idx_chunks_embedding ON document_chunks USING ivfflat (embedding_vector vector_cosine_ops) WITH (lists = 100);

-- 文档分片表注释
COMMENT ON TABLE document_chunks IS '文档分片表：存储文档的文本片段和向量嵌入，用于AI语义检索';
COMMENT ON COLUMN document_chunks.id IS '主键ID';
COMMENT ON COLUMN document_chunks.chunk_id IS '分片唯一标识，格式：chunk_xxxxxxxxx';
COMMENT ON COLUMN document_chunks.document_id IS '所属文档ID';
COMMENT ON COLUMN document_chunks.page_number IS '所在页码';
COMMENT ON COLUMN document_chunks.chunk_index IS '在页面中的分片索引';
COMMENT ON COLUMN document_chunks.content IS '分片文本内容';
COMMENT ON COLUMN document_chunks.content_length IS '内容长度';
COMMENT ON COLUMN document_chunks.embedding_vector IS '向量嵌入（OpenAI ada-002维度）';
COMMENT ON COLUMN document_chunks.created_at IS '创建时间';

-- =============================================================================
-- 笔记表：存储用户在阅读文档时创建的笔记
-- =============================================================================

CREATE TABLE notes (
    id SERIAL PRIMARY KEY,
    note_id VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    document_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER,
    selected_text TEXT,
    position_info JSONB,
    tags TEXT[],
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 笔记表索引
CREATE INDEX idx_notes_note_id ON notes(note_id);
CREATE INDEX idx_notes_user_id ON notes(user_id);
CREATE INDEX idx_notes_document_id ON notes(document_id);
CREATE INDEX idx_notes_user_document ON notes(user_id, document_id);
CREATE INDEX idx_notes_page_number ON notes(page_number);
CREATE INDEX idx_notes_is_favorite ON notes(is_favorite);
CREATE INDEX idx_notes_created_at ON notes(created_at);
CREATE INDEX idx_notes_tags ON notes USING gin(tags);
CREATE INDEX idx_notes_content ON notes USING gin(to_tsvector('english', content));

-- 笔记表注释
COMMENT ON TABLE notes IS '笔记表：存储用户在阅读文档时创建的笔记和标注信息';
COMMENT ON COLUMN notes.id IS '主键ID';
COMMENT ON COLUMN notes.note_id IS '笔记唯一标识，格式：note_xxxxxxxxx';
COMMENT ON COLUMN notes.user_id IS '所属用户ID';
COMMENT ON COLUMN notes.document_id IS '关联的文档ID';
COMMENT ON COLUMN notes.title IS '笔记标题';
COMMENT ON COLUMN notes.content IS '笔记内容';
COMMENT ON COLUMN notes.page_number IS '关联的页码';
COMMENT ON COLUMN notes.selected_text IS '选中的原文文本';
COMMENT ON COLUMN notes.position_info IS '位置信息（坐标、高亮区域等）';
COMMENT ON COLUMN notes.tags IS '标签数组';
COMMENT ON COLUMN notes.is_favorite IS '是否收藏';
COMMENT ON COLUMN notes.created_at IS '创建时间';
COMMENT ON COLUMN notes.updated_at IS '更新时间';

-- =============================================================================
-- AI对话表：存储AI对话会话信息
-- =============================================================================

CREATE TABLE ai_conversations (
    id SERIAL PRIMARY KEY,
    conversation_id VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    document_id VARCHAR(50),
    title VARCHAR(255),
    model VARCHAR(50) NOT NULL,
    total_messages INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI对话表索引
CREATE INDEX idx_conversations_conversation_id ON ai_conversations(conversation_id);
CREATE INDEX idx_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_conversations_document_id ON ai_conversations(document_id);
CREATE INDEX idx_conversations_status ON ai_conversations(status);
CREATE INDEX idx_conversations_created_at ON ai_conversations(created_at);

-- AI对话表注释
COMMENT ON TABLE ai_conversations IS 'AI对话表：存储AI对话会话的基本信息和统计数据';
COMMENT ON COLUMN ai_conversations.id IS '主键ID';
COMMENT ON COLUMN ai_conversations.conversation_id IS '对话唯一标识，格式：conv_xxxxxxxxx';
COMMENT ON COLUMN ai_conversations.user_id IS '所属用户ID';
COMMENT ON COLUMN ai_conversations.document_id IS '关联的文档ID（可选）';
COMMENT ON COLUMN ai_conversations.title IS '对话标题';
COMMENT ON COLUMN ai_conversations.model IS '使用的AI模型';
COMMENT ON COLUMN ai_conversations.total_messages IS '消息总数';
COMMENT ON COLUMN ai_conversations.total_tokens IS '总token消耗';
COMMENT ON COLUMN ai_conversations.status IS '对话状态：active/archived/deleted';
COMMENT ON COLUMN ai_conversations.created_at IS '创建时间';
COMMENT ON COLUMN ai_conversations.updated_at IS '更新时间';

-- =============================================================================
-- AI消息表：存储AI对话中的具体消息内容
-- =============================================================================

CREATE TABLE ai_messages (
    id SERIAL PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL UNIQUE,
    conversation_id VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    tokens_used INTEGER,
    model VARCHAR(50),
    temperature DECIMAL(3,2),
    max_tokens INTEGER,
    response_time INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI消息表索引
CREATE INDEX idx_messages_message_id ON ai_messages(message_id);
CREATE INDEX idx_messages_conversation_id ON ai_messages(conversation_id);
CREATE INDEX idx_messages_role ON ai_messages(role);
CREATE INDEX idx_messages_created_at ON ai_messages(created_at);
CREATE INDEX idx_messages_conversation_created ON ai_messages(conversation_id, created_at);

-- AI消息表注释
COMMENT ON TABLE ai_messages IS 'AI消息表：存储AI对话中的具体消息内容和元数据';
COMMENT ON COLUMN ai_messages.id IS '主键ID';
COMMENT ON COLUMN ai_messages.message_id IS '消息唯一标识，格式：msg_xxxxxxxxx';
COMMENT ON COLUMN ai_messages.conversation_id IS '所属对话ID';
COMMENT ON COLUMN ai_messages.role IS '消息角色：user/assistant/system';
COMMENT ON COLUMN ai_messages.content IS '消息内容';
COMMENT ON COLUMN ai_messages.tokens_used IS '该消息使用的token数';
COMMENT ON COLUMN ai_messages.model IS '使用的AI模型';
COMMENT ON COLUMN ai_messages.temperature IS '温度参数';
COMMENT ON COLUMN ai_messages.max_tokens IS '最大token限制';
COMMENT ON COLUMN ai_messages.response_time IS '响应时间（毫秒）';
COMMENT ON COLUMN ai_messages.created_at IS '创建时间';

-- =============================================================================
-- 用户会话表：存储用户登录会话信息
-- =============================================================================

CREATE TABLE user_sessions (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_info JSONB,
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户会话表索引
CREATE INDEX idx_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX idx_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_sessions_is_active ON user_sessions(is_active);

-- 用户会话表注释
COMMENT ON TABLE user_sessions IS '用户会话表：存储用户登录会话信息，用于token管理和安全控制';
COMMENT ON COLUMN user_sessions.id IS '主键ID';
COMMENT ON COLUMN user_sessions.session_id IS '会话唯一标识';
COMMENT ON COLUMN user_sessions.user_id IS '所属用户ID';
COMMENT ON COLUMN user_sessions.token_hash IS 'JWT Token哈希值';
COMMENT ON COLUMN user_sessions.device_info IS '设备信息';
COMMENT ON COLUMN user_sessions.ip_address IS 'IP地址';
COMMENT ON COLUMN user_sessions.user_agent IS '用户代理字符串';
COMMENT ON COLUMN user_sessions.expires_at IS '过期时间';
COMMENT ON COLUMN user_sessions.is_active IS '是否活跃';
COMMENT ON COLUMN user_sessions.created_at IS '创建时间';
COMMENT ON COLUMN user_sessions.last_accessed_at IS '最后访问时间';

-- =============================================================================
-- 触发器和函数
-- =============================================================================

-- 更新时间自动更新函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要的表添加更新时间触发器
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at 
    BEFORE UPDATE ON categories 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at 
    BEFORE UPDATE ON documents 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notes_updated_at 
    BEFORE UPDATE ON notes 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversations_updated_at 
    BEFORE UPDATE ON ai_conversations 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- 数据库视图
-- =============================================================================

-- 文档统计视图
CREATE VIEW document_stats AS
SELECT 
    u.user_id,
    u.username,
    COUNT(d.id) as total_documents,
    COUNT(CASE WHEN d.status = 'ready' THEN 1 END) as ready_documents,
    COUNT(CASE WHEN d.status = 'processing' THEN 1 END) as processing_documents,
    SUM(d.file_size) as total_file_size,
    AVG(d.read_progress) as avg_read_progress
FROM users u
LEFT JOIN documents d ON u.user_id = d.user_id
GROUP BY u.user_id, u.username;

-- 分类文档统计视图
CREATE VIEW category_document_stats AS
SELECT 
    c.category_id,
    c.name as category_name,
    c.user_id,
    COUNT(d.id) as document_count,
    SUM(d.file_size) as total_size
FROM categories c
LEFT JOIN documents d ON c.category_id = d.category_id
GROUP BY c.category_id, c.name, c.user_id;

-- AI对话统计视图
CREATE VIEW ai_conversation_stats AS
SELECT 
    u.user_id,
    u.username,
    COUNT(ac.id) as total_conversations,
    SUM(ac.total_messages) as total_messages,
    SUM(ac.total_tokens) as total_tokens,
    AVG(ac.total_tokens) as avg_tokens_per_conversation
FROM users u
LEFT JOIN ai_conversations ac ON u.user_id = ac.user_id
GROUP BY u.user_id, u.username;

-- =============================================================================
-- 测试数据插入
-- =============================================================================

-- 用户测试数据
INSERT INTO users (user_id, username, email, password_hash, real_name, role, status, last_login_at) VALUES 
('user_123456789', 'admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig.1h8kNjKSi', '系统管理员', 'admin', 'active', '2024-01-21 10:30:00'),
('user_987654321', 'john_doe', 'john@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig.1h8kNjKSi', '张三', 'user', 'active', '2024-01-21 09:15:00'),
('user_456789123', 'jane_smith', 'jane@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig.1h8kNjKSi', '李四', 'user', 'active', '2024-01-20 16:45:00');

-- 分类测试数据
INSERT INTO categories (category_id, user_id, parent_id, name, description, sort_order, document_count) VALUES 
('cat_001', 'user_123456789', NULL, '机器学习', '机器学习相关论文和资料', 1, 15),
('cat_002', 'user_123456789', 'cat_001', '深度学习', '深度学习算法和应用', 1, 8),
('cat_003', 'user_123456789', 'cat_001', '强化学习', '强化学习理论和实践', 2, 7),
('cat_004', 'user_123456789', NULL, '自然语言处理', 'NLP相关研究', 2, 12),
('cat_005', 'user_987654321', NULL, '计算机视觉', 'CV相关论文', 1, 5);

-- 文档测试数据
INSERT INTO documents (document_id, user_id, category_id, title, filename, author, abstract, file_path, file_size, file_size_display, pages, status, tags, read_progress) VALUES 
('doc_001', 'user_123456789', 'cat_002', 'Attention Is All You Need', 'attention_is_all_you_need.pdf', 'Vaswani et al.', 'The dominant sequence transduction models are based on complex recurrent or convolutional neural networks...', '/uploads/2024/01/attention_is_all_you_need.pdf', 2411520, '2.3 MB', 15, 'ready', ARRAY['transformer', 'attention', 'nlp'], 0.60),
('doc_002', 'user_123456789', 'cat_002', 'BERT: Pre-training of Deep Bidirectional Transformers', 'bert_paper.pdf', 'Devlin et al.', 'We introduce a new language representation model called BERT...', '/uploads/2024/01/bert_paper.pdf', 1843200, '1.8 MB', 16, 'ready', ARRAY['bert', 'transformer', 'pretraining'], 0.35),
('doc_003', 'user_987654321', 'cat_005', 'ResNet: Deep Residual Learning for Image Recognition', 'resnet_paper.pdf', 'He et al.', 'Deeper neural networks are more difficult to train...', '/uploads/2024/01/resnet_paper.pdf', 3145728, '3.0 MB', 12, 'ready', ARRAY['resnet', 'cnn', 'computer_vision'], 0.80),
('doc_004', 'user_123456789', 'cat_003', 'Playing Atari with Deep Reinforcement Learning', 'dqn_paper.pdf', 'Mnih et al.', 'We present the first deep learning model to successfully learn control policies...', '/uploads/2024/01/dqn_paper.pdf', 1572864, '1.5 MB', 9, 'processing', ARRAY['dqn', 'reinforcement_learning', 'atari'], 0.00);

-- 文档分片测试数据
INSERT INTO document_chunks (chunk_id, document_id, page_number, chunk_index, content, content_length) VALUES 
('chunk_001', 'doc_001', 1, 1, 'The Transformer, a model architecture eschewing recurrence and instead relying entirely on an attention mechanism to draw global dependencies between input and output.', 156),
('chunk_002', 'doc_001', 1, 2, 'The Transformer allows for significantly more parallelization and can reach a new state of the art in translation quality after being trained for as little as twelve hours on eight P100 GPUs.', 178),
('chunk_003', 'doc_002', 1, 1, 'BERT is designed to pre-train deep bidirectional representations from unlabeled text by jointly conditioning on both left and right context in all layers.', 148);

-- 笔记测试数据
INSERT INTO notes (note_id, user_id, document_id, title, content, page_number, selected_text, tags, is_favorite) VALUES 
('note_001', 'user_123456789', 'doc_001', 'Transformer架构要点', 'Transformer完全基于注意力机制，摒弃了循环和卷积结构。这是一个重要的架构创新，使得模型可以并行化训练。', 3, 'The Transformer, a model architecture eschewing recurrence', ARRAY['transformer', 'architecture'], TRUE),
('note_002', 'user_123456789', 'doc_001', '多头注意力机制', '多头注意力允许模型在不同的表示子空间中关注不同的位置信息，这比单一注意力头更加强大。', 5, 'Multi-head attention allows the model to jointly attend to information', ARRAY['attention', 'multi-head'], FALSE),
('note_003', 'user_987654321', 'doc_003', 'ResNet残差连接', '残差连接解决了深度网络的梯度消失问题，使得训练更深的网络成为可能。', 2, 'Deep residual learning framework to ease the training', ARRAY['resnet', 'residual'], TRUE),
('note_004', 'user_123456789', 'doc_002', 'BERT预训练任务', 'BERT使用掩码语言模型和下一句预测两个任务进行预训练，这种设计很巧妙。', 4, 'BERT is pre-trained using two unsupervised tasks', ARRAY['bert', 'pretraining'], FALSE);

-- AI对话测试数据
INSERT INTO ai_conversations (conversation_id, user_id, document_id, title, model, total_messages, total_tokens, status) VALUES 
('conv_001', 'user_123456789', 'doc_001', 'Transformer论文讨论', 'gpt-3.5-turbo', 8, 1250, 'active'),
('conv_002', 'user_123456789', 'doc_002', 'BERT模型解析', 'gpt-4', 6, 2100, 'active'),
('conv_003', 'user_987654321', 'doc_003', 'ResNet架构分析', 'gpt-3.5-turbo', 4, 850, 'active'),
('conv_004', 'user_123456789', NULL, '通用AI助手', 'gpt-3.5-turbo', 12, 1800, 'active');

-- AI消息测试数据
INSERT INTO ai_messages (message_id, conversation_id, role, content, tokens_used, model, temperature, max_tokens, response_time) VALUES 
('msg_001', 'conv_001', 'user', '请解释一下Transformer中的自注意力机制是如何工作的？', 15, NULL, NULL, NULL, NULL),
('msg_002', 'conv_001', 'assistant', 'Transformer中的自注意力机制是一种让模型能够关注输入序列中不同位置信息的机制。具体工作原理如下：\n\n1. **查询、键、值矩阵**：首先将输入通过线性变换得到Query(Q)、Key(K)、Value(V)三个矩阵..', 150, 'gpt-3.5-turbo', 0.7, 2048, 1200),
('msg_003', 'conv_002', 'user', 'BERT的预训练任务有哪些？各自的作用是什么？', 12, NULL, NULL, NULL, NULL),
('msg_004', 'conv_002', 'assistant', 'BERT使用了两个主要的预训练任务：\n\n**1. 掩码语言模型（Masked Language Model, MLM）**\n- 随机掩盖输入中15%的token\n- 让模型预测被掩盖的token\n- 使模型学习双向的上下文表示..', 180, 'gpt-4', 0.7, 2048, 1500);

-- 提交事务
COMMIT;

-- =============================================================================
-- 脚本执行完成提示
-- =============================================================================

\echo '数据库表结构创建完成！'
\echo '已创建的表：'
\echo '  - users (用户表)'
\echo '  - categories (分类表)'
\echo '  - documents (文档表)'
\echo '  - document_chunks (文档分片表)'
\echo '  - notes (笔记表)'
\echo '  - ai_conversations (AI对话表)'
\echo '  - ai_messages (AI消息表)'
\echo '  - user_sessions (用户会话表)'
\echo ''
\echo '已创建的视图：'
\echo '  - document_stats (文档统计视图)'
\echo '  - category_document_stats (分类文档统计视图)'
\echo '  - ai_conversation_stats (AI对话统计视图)'
\echo ''
\echo '注意事项：'
\echo '  1. 向量搜索功能需要安装pgvector扩展'
\echo '  2. 如需启用向量索引，请取消注释相关CREATE INDEX语句'
\echo '  3. 建议在生产环境中根据数据量调整索引策略'
\echo '  4. 定期执行VACUUM和ANALYZE维护数据库性能'