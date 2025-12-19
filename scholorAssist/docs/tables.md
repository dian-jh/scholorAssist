# 数据库表结构设计

## 概述

本文档定义了文献辅助阅读系统的PostgreSQL数据库表结构，包括用户管理、文档管理、分类管理、笔记管理和AI对话等核心功能模块。

## 用户表

```sql
-- 用户表：存储用户基本信息和认证数据
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE COMMENT '用户唯一标识，格式：user_xxxxxxxxx',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，3-20字符',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱地址',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/super_admin',
    status VARCHAR(20) NOT NULL DEFAULT 'pending_verification' COMMENT '用户状态：pending_verification/active/suspended',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 用户表索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_user_id ON users(user_id);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 用户表注释
COMMENT ON TABLE users IS '用户表：存储系统用户的基本信息、认证数据和状态信息';

-- 用户测试数据
INSERT INTO users (user_id, username, email, password_hash, real_name, role, status, last_login_at) VALUES 
('user_123456789', 'admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig.1h8kNjKSi', '系统管理员', 'admin', 'active', '2024-01-21 10:30:00'),
('user_987654321', 'john_doe', 'john@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig.1h8kNjKSi', '张三', 'user', 'active', '2024-01-21 09:15:00'),
('user_456789123', 'jane_smith', 'jane@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig.1h8kNjKSi', '李四', 'user', 'active', '2024-01-20 16:45:00');
```

## 分类表

```sql
-- 分类表：存储文档分类信息，支持层级结构
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    category_id VARCHAR(50) NOT NULL UNIQUE COMMENT '分类唯一标识，格式：cat_xxxxxxxxx',
    user_id VARCHAR(50) NOT NULL COMMENT '所属用户ID',
    parent_id VARCHAR(50) COMMENT '父分类ID，NULL表示根分类',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description TEXT COMMENT '分类描述',
    sort_order INTEGER NOT NULL DEFAULT 0 COMMENT '排序顺序',
    document_count INTEGER NOT NULL DEFAULT 0 COMMENT '该分类下的文档数量',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 分类表索引
CREATE INDEX idx_categories_category_id ON categories(category_id);
CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_user_parent ON categories(user_id, parent_id);
CREATE INDEX idx_categories_sort_order ON categories(sort_order);

-- 分类表注释
COMMENT ON TABLE categories IS '分类表：存储文档分类信息，支持多级嵌套的层级结构';

-- 分类测试数据
INSERT INTO categories (category_id, user_id, parent_id, name, description, sort_order, document_count) VALUES 
('cat_001', 'user_123456789', NULL, '机器学习', '机器学习相关论文和资料', 1, 15),
('cat_002', 'user_123456789', 'cat_001', '深度学习', '深度学习算法和应用', 1, 8),
('cat_003', 'user_123456789', 'cat_001', '强化学习', '强化学习理论和实践', 2, 7),
('cat_004', 'user_123456789', NULL, '自然语言处理', 'NLP相关研究', 2, 12),
('cat_005', 'user_987654321', NULL, '计算机视觉', 'CV相关论文', 1, 5);
```

## 文档表

```sql
-- 文档表：存储PDF文档的基本信息和元数据
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    document_id VARCHAR(50) NOT NULL UNIQUE COMMENT '文档唯一标识，格式：doc_xxxxxxxxx',
    user_id VARCHAR(50) NOT NULL COMMENT '所属用户ID',
    category_id VARCHAR(50) NOT NULL COMMENT '所属分类ID',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    author VARCHAR(255) COMMENT '作者信息',
    abstract TEXT COMMENT '文档摘要',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_size_display VARCHAR(20) COMMENT '文件大小显示（如：2.3 MB）',
    pages INTEGER NOT NULL DEFAULT 0 COMMENT '页数',
    status VARCHAR(20) NOT NULL DEFAULT 'processing' COMMENT '处理状态：processing/ready/failed',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    tags TEXT[] COMMENT '标签数组',
    read_progress DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '阅读进度（0-1）',
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    processed_at TIMESTAMP COMMENT '处理完成时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
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

-- 文档测试数据
INSERT INTO documents (document_id, user_id, category_id, title, filename, author, abstract, file_path, file_size, file_size_display, pages, status, tags, read_progress) VALUES 
('doc_001', 'user_123456789', 'cat_002', 'Attention Is All You Need', 'attention_is_all_you_need.pdf', 'Vaswani et al.', 'The dominant sequence transduction models are based on complex recurrent or convolutional neural networks...', '/uploads/2024/01/attention_is_all_you_need.pdf', 2411520, '2.3 MB', 15, 'ready', ARRAY['transformer', 'attention', 'nlp'], 0.60),
('doc_002', 'user_123456789', 'cat_002', 'BERT: Pre-training of Deep Bidirectional Transformers', 'bert_paper.pdf', 'Devlin et al.', 'We introduce a new language representation model called BERT...', '/uploads/2024/01/bert_paper.pdf', 1843200, '1.8 MB', 16, 'ready', ARRAY['bert', 'transformer', 'pretraining'], 0.35),
('doc_003', 'user_987654321', 'cat_005', 'ResNet: Deep Residual Learning for Image Recognition', 'resnet_paper.pdf', 'He et al.', 'Deeper neural networks are more difficult to train...', '/uploads/2024/01/resnet_paper.pdf', 3145728, '3.0 MB', 12, 'ready', ARRAY['resnet', 'cnn', 'computer_vision'], 0.80),
('doc_004', 'user_123456789', 'cat_003', 'Playing Atari with Deep Reinforcement Learning', 'dqn_paper.pdf', 'Mnih et al.', 'We present the first deep learning model to successfully learn control policies...', '/uploads/2024/01/dqn_paper.pdf', 1572864, '1.5 MB', 9, 'processing', ARRAY['dqn', 'reinforcement_learning', 'atari'], 0.00);
```

## 文档分片表

```sql
-- 文档分片表：存储文档的文本片段，用于AI向量检索
CREATE TABLE document_chunks (
    id SERIAL PRIMARY KEY,
    chunk_id VARCHAR(50) NOT NULL UNIQUE COMMENT '分片唯一标识，格式：chunk_xxxxxxxxx',
    document_id VARCHAR(50) NOT NULL COMMENT '所属文档ID',
    page_number INTEGER NOT NULL COMMENT '所在页码',
    chunk_index INTEGER NOT NULL COMMENT '在页面中的分片索引',
    content TEXT NOT NULL COMMENT '分片文本内容',
    content_length INTEGER NOT NULL COMMENT '内容长度',
    embedding_vector VECTOR(1536) COMMENT '向量嵌入（OpenAI ada-002维度）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
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

-- 文档分片测试数据
INSERT INTO document_chunks (chunk_id, document_id, page_number, chunk_index, content, content_length) VALUES 
('chunk_001', 'doc_001', 1, 1, 'The Transformer, a model architecture eschewing recurrence and instead relying entirely on an attention mechanism to draw global dependencies between input and output.', 156, 156),
('chunk_002', 'doc_001', 1, 2, 'The Transformer allows for significantly more parallelization and can reach a new state of the art in translation quality after being trained for as little as twelve hours on eight P100 GPUs.', 178, 178),
('chunk_003', 'doc_002', 1, 1, 'BERT is designed to pre-train deep bidirectional representations from unlabeled text by jointly conditioning on both left and right context in all layers.', 148, 148);
```

## 笔记表

```sql
-- 笔记表：存储用户在阅读文档时创建的笔记
CREATE TABLE notes (
    id SERIAL PRIMARY KEY,
    note_id VARCHAR(50) NOT NULL UNIQUE COMMENT '笔记唯一标识，格式：note_xxxxxxxxx',
    user_id VARCHAR(50) NOT NULL COMMENT '所属用户ID',
    document_id VARCHAR(50) NOT NULL COMMENT '关联的文档ID',
    title VARCHAR(255) NOT NULL COMMENT '笔记标题',
    content TEXT NOT NULL COMMENT '笔记内容',
    page_number INTEGER COMMENT '关联的页码',
    selected_text TEXT COMMENT '选中的原文文本',
    position_info JSONB COMMENT '位置信息（坐标、高亮区域等）',
    tags TEXT[] COMMENT '标签数组',
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否收藏',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
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

-- 笔记测试数据
INSERT INTO notes (note_id, user_id, document_id, title, content, page_number, selected_text, tags, is_favorite) VALUES 
('note_001', 'user_123456789', 'doc_001', 'Transformer架构要点', 'Transformer完全基于注意力机制，摒弃了循环和卷积结构。这是一个重要的架构创新，使得模型可以并行化训练。', 3, 'The Transformer, a model architecture eschewing recurrence', ARRAY['transformer', 'architecture'], TRUE),
('note_002', 'user_123456789', 'doc_001', '多头注意力机制', '多头注意力允许模型在不同的表示子空间中关注不同的位置信息，这比单一注意力头更加强大。', 5, 'Multi-head attention allows the model to jointly attend to information', ARRAY['attention', 'multi-head'], FALSE),
('note_003', 'user_987654321', 'doc_003', 'ResNet残差连接', '残差连接解决了深度网络的梯度消失问题，使得训练更深的网络成为可能。', 2, 'Deep residual learning framework to ease the training', ARRAY['resnet', 'residual'], TRUE),
('note_004', 'user_123456789', 'doc_002', 'BERT预训练任务', 'BERT使用掩码语言模型和下一句预测两个任务进行预训练，这种设计很巧妙。', 4, 'BERT is pre-trained using two unsupervised tasks', ARRAY['bert', 'pretraining'], FALSE);
```

## AI对话表

```sql
-- AI对话表：存储AI对话会话信息
CREATE TABLE ai_conversations (
    id SERIAL PRIMARY KEY,
    conversation_id VARCHAR(50) NOT NULL UNIQUE COMMENT '对话唯一标识，格式：conv_xxxxxxxxx',
    user_id VARCHAR(50) NOT NULL COMMENT '所属用户ID',
    document_id VARCHAR(50) COMMENT '关联的文档ID（可选）',
    title VARCHAR(255) COMMENT '对话标题',
    model VARCHAR(50) NOT NULL COMMENT '使用的AI模型',
    total_messages INTEGER NOT NULL DEFAULT 0 COMMENT '消息总数',
    total_tokens INTEGER NOT NULL DEFAULT 0 COMMENT '总token消耗',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '对话状态：active/archived/deleted',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- AI对话表索引
CREATE INDEX idx_conversations_conversation_id ON ai_conversations(conversation_id);
CREATE INDEX idx_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_conversations_document_id ON ai_conversations(document_id);
CREATE INDEX idx_conversations_status ON ai_conversations(status);
CREATE INDEX idx_conversations_created_at ON ai_conversations(created_at);

-- AI对话表注释
COMMENT ON TABLE ai_conversations IS 'AI对话表：存储AI对话会话的基本信息和统计数据';

-- AI对话测试数据
INSERT INTO ai_conversations (conversation_id, user_id, document_id, title, model, total_messages, total_tokens, status) VALUES 
('conv_001', 'user_123456789', 'doc_001', 'Transformer论文讨论', 'gpt-3.5-turbo', 8, 1250, 'active'),
('conv_002', 'user_123456789', 'doc_002', 'BERT模型解析', 'gpt-4', 6, 2100, 'active'),
('conv_003', 'user_987654321', 'doc_003', 'ResNet架构分析', 'gpt-3.5-turbo', 4, 850, 'active'),
('conv_004', 'user_123456789', NULL, '通用AI助手', 'gpt-3.5-turbo', 12, 1800, 'active');
```

## AI消息表

```sql
-- AI消息表：存储AI对话中的具体消息内容
CREATE TABLE ai_messages (
    id SERIAL PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL UNIQUE COMMENT '消息唯一标识，格式：msg_xxxxxxxxx',
    conversation_id VARCHAR(50) NOT NULL COMMENT '所属对话ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色：user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    tokens_used INTEGER COMMENT '该消息使用的token数',
    model VARCHAR(50) COMMENT '使用的AI模型',
    temperature DECIMAL(3,2) COMMENT '温度参数',
    max_tokens INTEGER COMMENT '最大token限制',
    response_time INTEGER COMMENT '响应时间（毫秒）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- AI消息表索引
CREATE INDEX idx_messages_message_id ON ai_messages(message_id);
CREATE INDEX idx_messages_conversation_id ON ai_messages(conversation_id);
CREATE INDEX idx_messages_role ON ai_messages(role);
CREATE INDEX idx_messages_created_at ON ai_messages(created_at);
CREATE INDEX idx_messages_conversation_created ON ai_messages(conversation_id, created_at);

-- AI消息表注释
COMMENT ON TABLE ai_messages IS 'AI消息表：存储AI对话中的具体消息内容和元数据';

-- AI消息测试数据
INSERT INTO ai_messages (message_id, conversation_id, role, content, tokens_used, model, temperature, max_tokens, response_time) VALUES 
('msg_001', 'conv_001', 'user', '请解释一下Transformer中的自注意力机制是如何工作的？', 15, NULL, NULL, NULL, NULL),
('msg_002', 'conv_001', 'assistant', 'Transformer中的自注意力机制是一种让模型能够关注输入序列中不同位置信息的机制。具体工作原理如下：\n\n1. **查询、键、值矩阵**：首先将输入通过线性变换得到Query(Q)、Key(K)、Value(V)三个矩阵..', 150, 'gpt-3.5-turbo', 0.7, 2048, 1200),
('msg_003', 'conv_002', 'user', 'BERT的预训练任务有哪些？各自的作用是什么？', 12, NULL, NULL, NULL, NULL),
('msg_004', 'conv_002', 'assistant', 'BERT使用了两个主要的预训练任务：\n\n**1. 掩码语言模型（Masked Language Model, MLM）**\n- 随机掩盖输入中15%的token\n- 让模型预测被掩盖的token\n- 使模型学习双向的上下文表示..', 180, 'gpt-4', 0.7, 2048, 1500);
```

## 用户会话表（可选）

```sql
-- 用户会话表：存储用户登录会话信息
CREATE TABLE user_sessions (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE COMMENT '会话唯一标识',
    user_id VARCHAR(50) NOT NULL COMMENT '所属用户ID',
    token_hash VARCHAR(255) NOT NULL COMMENT 'JWT Token哈希值',
    device_info JSONB COMMENT '设备信息',
    ip_address INET COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理字符串',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否活跃',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间'
);

-- 用户会话表索引
CREATE INDEX idx_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX idx_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_sessions_is_active ON user_sessions(is_active);

-- 用户会话表注释
COMMENT ON TABLE user_sessions IS '用户会话表：存储用户登录会话信息，用于token管理和安全控制';
```

## 数据库扩展和函数

```sql
-- 安装必要的PostgreSQL扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- UUID生成
CREATE EXTENSION IF NOT EXISTS "vector";     -- 向量相似度搜索（需要pgvector）

-- 更新时间自动更新函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要的表添加更新时间触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_categories_updated_at BEFORE UPDATE ON categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_notes_updated_at BEFORE UPDATE ON notes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON ai_conversations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## 数据库视图

```sql
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
```

## 性能优化建议

### 1. 分区表（适用于大数据量）

```sql
-- 按月分区AI消息表（当消息量很大时）
CREATE TABLE ai_messages_partitioned (
    LIKE ai_messages INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- 创建月度分区
CREATE TABLE ai_messages_2024_01 PARTITION OF ai_messages_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### 2. 定期清理任务

```sql
-- 清理过期会话
DELETE FROM user_sessions WHERE expires_at < CURRENT_TIMESTAMP AND is_active = FALSE;

-- 清理已删除对话的消息
DELETE FROM ai_messages WHERE conversation_id IN (
    SELECT conversation_id FROM ai_conversations WHERE status = 'deleted'
);
```

### 3. 统计信息更新

```sql
-- 更新分类文档数量
UPDATE categories SET document_count = (
    SELECT COUNT(*) FROM documents WHERE category_id = categories.category_id
);

-- 更新对话统计
UPDATE ai_conversations SET 
    total_messages = (SELECT COUNT(*) FROM ai_messages WHERE conversation_id = ai_conversations.conversation_id),
    total_tokens = (SELECT COALESCE(SUM(tokens_used), 0) FROM ai_messages WHERE conversation_id = ai_conversations.conversation_id);
```

## 备份和恢复

```sql
-- 创建备份
pg_dump -h localhost -U postgres -d scholar_assist > backup_$(date +%Y%m%d_%H%M%S).sql

-- 恢复备份
psql -h localhost -U postgres -d scholar_assist < backup_20240121_103000.sql
```

## 安全配置

```sql
-- 创建应用专用用户
CREATE USER scholar_app WITH PASSWORD 'your_secure_password';

-- 授予必要权限
GRANT CONNECT ON DATABASE scholar_assist TO scholar_app;
GRANT USAGE ON SCHEMA public TO scholar_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO scholar_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO scholar_app;

-- 行级安全策略示例（确保用户只能访问自己的数据）
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY documents_user_policy ON documents FOR ALL TO scholar_app USING (user_id = current_setting('app.current_user_id'));
```

---

**注意事项：**

1. 所有表都使用了合适的索引来优化查询性能
2. 外键关系在应用层面维护，数据库层面不设置外键约束以提高性能
3. 使用了PostgreSQL的数组类型存储标签，提高查询效率
4. 向量搜索需要安装pgvector扩展
5. 建议在生产环境中根据实际数据量调整索引策略
6. 定期执行VACUUM和ANALYZE来维护数据库性能