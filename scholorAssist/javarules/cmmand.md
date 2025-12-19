---
description: 文献辅助阅读系统开发指南
globs: 
alwaysApply: true
---

# 文献辅助阅读系统开发指南

## 项目概述

文献辅助阅读系统是基于微服务架构的智能文献管理平台，采用Spring Boot + MyBatis + PostgreSQL技术栈，支持PDF文档上传、AI智能问答、笔记管理等核心功能。

## 系统架构

### 微服务模块划分
```
文献辅助阅读系统
├── sc-gateway          # API网关服务
├── sc-common           # 通用组件模块
├── sc-user-service     # 用户管理服务
├── sc-categories-manage # 分类管理服务
├── sc-literature-manage # 文献管理服务
├── sc-note-service     # 笔记管理服务
├── sc-ai-service       # AI智能服务
└── sc-statistics-service # 统计分析服务
```

### 技术栈
- **后端框架**: Spring Boot 2.7+
- **数据库**: PostgreSQL 12+
- **缓存**: Redis 6+
- **消息队列**: RabbitMQ
- **文件存储**: MinIO/阿里云OSS
- **AI服务**: OpenAI API

## 核心功能模块

### 1. 用户管理模块 (sc-user-service)
**功能特性**:
- 用户注册、登录、信息管理
- JWT Token认证和会话管理
- 用户权限控制和角色管理

**核心接口**:
- `POST /api/users/register` - 用户注册
- `POST /api/users/login` - 用户登录
- `GET /api/users/profile` - 获取用户信息

### 2. 分类管理模块 (sc-categories-manage)
**功能特性**:
- 支持多级嵌套的分类结构
- 分类的CRUD操作和排序
- 分类文档数量统计

**核心接口**:
- `GET /api/categories` - 获取分类树
- `POST /api/categories` - 创建分类
- `PUT /api/categories/{id}` - 更新分类

### 3. 文献管理模块 (sc-literature-manage)
**功能特性**:
- PDF文档上传和存储
- 文档元数据自动提取
- 文档内容解析和分片

**核心接口**:
- `POST /api/documents/upload` - 上传文档
- `GET /api/documents` - 获取文档列表
- `GET /api/documents/{id}` - 获取文档详情

### 4. 笔记管理模块 (sc-note-service)
**功能特性**:
- 富文本笔记编辑
- 笔记标签管理
- 笔记与文档关联

**核心接口**:
- `GET /api/notes` - 获取笔记列表
- `POST /api/notes` - 创建笔记
- `PUT /api/notes/{id}` - 更新笔记

### 5. AI智能服务模块 (sc-ai-service)
**功能特性**:
- AI对话和问答
- 文档内容向量化
- 语义搜索和检索

**核心接口**:
- `POST /api/ai/chat` - AI对话
- `GET /api/ai/conversations` - 获取对话列表
- `POST /api/ai/search` - 语义搜索

## 开发规范

### 强制规范 🔴
1. **严格遵循微服务架构模式**
2. **网关相关代码必须放在sc-gateway模块**
3. **通用组件必须放在sc-common模块**
4. **严格禁止修改现有Java代码**（除非明确授权）
5. **禁止修改API和需求文档**
6. **所有SQL语句必须写在XML映射文件中**
7. **建表语句必须参考docs/tables.md和docs/tables.sql**

### 全局响应处理规范
- **严格使用** sc-common 中的全局响应处理器
- **禁止** Controller 层手动封装 Result 返回
- **确保** 所有异常被全局处理器捕获

### 建议规范 ⭐
1. 优先使用现有的通用组件和工具类
2. 遵循RESTful API设计规范
3. 统一异常处理和日志记录
4. 编写单元测试，确保代码质量

## 数据库设计

### 核心表结构
基于PostgreSQL数据库，主要包含以下核心表：

- **users** - 用户基本信息表
- **user_sessions** - 用户会话管理表
- **categories** - 文档分类表（支持层级结构）
- **documents** - 文档基本信息表
- **document_chunks** - 文档分片表（用于AI向量检索）
- **notes** - 用户笔记表
- **ai_conversations** - AI对话会话表
- **ai_messages** - AI消息详情表

### 数据库特性
- 使用PostgreSQL的JSONB类型存储复杂数据
- 支持全文搜索和向量相似度检索
- 使用数组类型存储标签信息
- 建立合适的索引优化查询性能

## 开发流程

### 1. 环境准备
```bash
# 克隆项目
git clone <repository-url>
cd scholorAssist

# 安装依赖
mvn clean install

# 启动数据库
docker-compose up -d postgresql redis

# 初始化数据库
psql -h localhost -U postgres -d scholar_assist -f docs/tables.sql
```

### 2. 开发步骤
1. **需求分析**: 仔细阅读API文档和需求文档
2. **设计评审**: 确认技术方案和架构设计
3. **编码实现**: 按照规范编写代码
4. **单元测试**: 编写并执行单元测试
5. **集成测试**: 进行接口和功能测试
6. **代码审查**: 提交Pull Request进行代码审查

### 3. 质量保证
- **测试覆盖率**: 不低于80%
- **代码规范**: 通过Spotless格式检查
- **安全扫描**: 通过OWASP依赖检查
- **性能测试**: API响应时间小于500ms

## 部署架构

### 开发环境
```yaml
# docker-compose.yml
version: '3.8'
services:
  postgresql:
    image: postgres:12
    environment:
      POSTGRES_DB: scholar_assist
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
  
  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"
```

### 生产环境
- **容器化部署**: 使用Docker和Kubernetes
- **负载均衡**: Nginx + Spring Cloud Gateway
- **数据库**: PostgreSQL主从复制
- **缓存**: Redis集群

## 常见问题解决

### 1. 数据库连接问题
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scholar_assist
    username: postgres
    password: password
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
```

### 2. 跨域问题
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

### 3. 文件上传限制
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

## 性能优化建议

### 1. 数据库优化
- 合理使用索引
- 避免N+1查询问题
- 使用连接池管理连接
- 定期执行VACUUM和ANALYZE

### 2. 缓存策略
- 热点数据使用Redis缓存
- 实现多级缓存架构
- 设置合理的过期时间
- 使用缓存预热策略

### 3. 异步处理
- 文档处理使用异步任务
- AI调用使用异步接口
- 统计计算使用定时任务
- 消息队列解耦服务

## 安全最佳实践

### 1. 认证授权
- 使用JWT Token进行认证
- 实现基于角色的权限控制
- 定期刷新Token
- 记录安全审计日志

### 2. 数据安全
- 敏感数据加密存储
- 使用HTTPS传输
- 防止SQL注入攻击
- 输入参数严格校验

### 3. 接口安全
- 实现接口限流
- 防止CSRF攻击
- 添加请求签名验证
- 敏感操作二次确认

## 核心开发原则

### 1. 分层职责
- **Controller**: 参数校验、接口文档、日志记录
- **Service**: 业务逻辑、事务管理、异常处理
- **Manager**: 数据操作、缓存管理
- **Mapper**: 数据库访问

### 2. 异常处理
- 使用 `BusinessException` 处理业务异常
- 全局处理器自动捕获并返回标准格式
- 禁止手动封装异常响应

### 3. 返回值规范
- Controller 直接返回业务对象
- 全局处理器自动封装为 Result 格式
- 统一注入 requestId

### 4. 代码质量
- 必须通过 SonarQube 检测
- 单元测试覆盖率 > 80%
- 接口响应时间 < 500ms

## 注意事项

1. **严格遵循** 全局响应处理器使用规范
2. **每个模块** 只包含一个完整功能示例
3. **核心逻辑** 不超过50行实现
4. **优先级**: 规范说明 > 代码示例
5. **兼容性**: 确保与现有架构100%兼容

---

**重要提醒**：
1. 开发前请仔细阅读所有规范文档
2. 遇到问题及时与团队沟通
3. 保持代码质量和文档同步更新
4. 关注系统性能和安全性