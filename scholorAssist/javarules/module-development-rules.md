---
description: 微服务模块独立开发规范
globs: 
alwaysApply: true
---

# 微服务模块独立开发规范

## 概述

本文档定义了文献辅助阅读系统各个微服务模块的独立开发规范，确保每个模块都能独立开发、测试和部署，同时保持整体架构的一致性。

## 通用开发规范

### 强制规则 🔴
1. **模块独立性**: 每个微服务必须能够独立编译、测试和部署
2. **接口契约**: 服务间通信必须通过明确定义的API接口
3. **数据隔离**: 每个服务只能访问自己的数据库表
4. **全局响应处理**: 严格使用sc-common中的全局响应处理器
5. **日志规范**: 统一日志格式和级别定义

### 建议规则 🟡
1. 优先使用异步通信方式
2. 实现服务降级和熔断机制
3. 提供健康检查接口
4. 支持优雅关闭

## sc-user-service (用户服务) 开发规范

### 业务职责
- 用户注册、登录、信息管理
- 用户会话管理和token验证
- 用户权限控制和角色管理

### 核心接口定义
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public UserInfoResponse register(@Valid @RequestBody UserRegisterRequest request) {
        return userService.register(request);
    }
    
    @PostMapping("/login")
    public UserLoginResponse login(@Valid @RequestBody UserLoginRequest request) {
        return userService.login(request);
    }
    
    @GetMapping("/profile")
    public UserInfoResponse getUserProfile(@RequestHeader("Authorization") String token) {
        return userService.getUserProfile(token);
    }
}
```

### 数据表映射
- **users** - 用户基本信息表
- **user_sessions** - 用户会话表

### 开发约束
1. 密码必须使用BCrypt加密存储
2. JWT Token过期时间不超过24小时
3. 用户敏感信息不能出现在日志中
4. 登录失败超过5次需要锁定账户

## sc-categories-manage (分类管理服务) 开发规范

### 业务职责
- 文档分类的CRUD操作
- 支持多级嵌套的分类结构
- 分类文档数量统计

### 核心接口定义
```java
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    @GetMapping
    public List<CategoryTreeResponse> getCategoryTree() {
        return categoryService.getCategoryTree();
    }
    
    @PostMapping
    public CategoryResponse createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.createCategory(request);
    }
    
    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, 
                                         @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, request);
    }
}
```

### 数据表映射
- **categories** - 文档分类表

### 开发约束
1. 分类层级不能超过5级
2. 同级分类名称不能重复
3. 删除分类前必须检查是否有关联文档
4. 支持分类排序功能

## sc-literature-manage (文献管理服务) 开发规范

### 业务职责
- PDF文档上传和存储
- 文档元数据自动提取
- 文档内容解析和分片

### 核心接口定义
```java
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    @PostMapping("/upload")
    public DocumentUploadResponse uploadDocument(@RequestParam("file") MultipartFile file,
                                               @RequestParam("categoryId") Long categoryId) {
        return documentService.uploadDocument(file, categoryId);
    }
    
    @GetMapping
    public PageResult<DocumentListResponse> getDocuments(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return documentService.getDocuments(page, pageSize);
    }
    
    @GetMapping("/{id}")
    public DocumentDetailResponse getDocumentDetail(@PathVariable Long id) {
        return documentService.getDocumentDetail(id);
    }
}
```

### 数据表映射
- **documents** - 文档基本信息表
- **document_chunks** - 文档分片表

### 开发约束
1. 支持的文件格式：PDF
2. 单个文件大小不超过50MB
3. 文档上传后自动进行内容解析
4. 文档删除时需要同时删除文件存储

## sc-note-service (笔记服务) 开发规范

### 业务职责
- 富文本笔记编辑
- 笔记标签管理
- 笔记与文档关联

### 核心接口定义
```java
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {
    
    private final NoteService noteService;
    
    @GetMapping
    public PageResult<NoteListResponse> getNotes(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return noteService.getNotes(page, pageSize);
    }
    
    @PostMapping
    public NoteResponse createNote(@Valid @RequestBody NoteCreateRequest request) {
        return noteService.createNote(request);
    }
    
    @PutMapping("/{id}")
    public NoteResponse updateNote(@PathVariable Long id, 
                                 @Valid @RequestBody NoteUpdateRequest request) {
        return noteService.updateNote(id, request);
    }
}
```

### 数据表映射
- **notes** - 用户笔记表

### 开发约束
1. 笔记内容支持Markdown格式
2. 单个笔记大小不超过1MB
3. 支持标签功能，单个笔记最多10个标签
4. 笔记与文档关联为可选功能

## sc-ai-service (AI服务) 开发规范

### 业务职责
- AI对话和问答
- 文档内容向量化
- 语义搜索和检索

### 核心接口定义
```java
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    
    private final AiService aiService;
    
    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiService.chat(request);
    }
    
    @GetMapping("/conversations")
    public PageResult<ConversationListResponse> getConversations(@RequestParam(defaultValue = "1") Integer page,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return aiService.getConversations(page, pageSize);
    }
    
    @PostMapping("/search")
    public List<DocumentSearchResponse> semanticSearch(@Valid @RequestBody SemanticSearchRequest request) {
        return aiService.semanticSearch(request);
    }
}
```

### 数据表映射
- **ai_conversations** - AI对话会话表
- **ai_messages** - AI消息详情表

### 开发约束
1. AI调用需要实现超时控制（30秒）
2. 对话历史保留最近100条消息
3. 向量搜索结果最多返回20条
4. 实现AI服务降级机制

## 服务间通信规范

### 接口调用规范
```java
// 使用Feign客户端进行服务间调用
@FeignClient(name = "sc-user-service", path = "/api/users")
public interface UserServiceClient {
    
    @GetMapping("/{userId}")
    Result<UserInfoResponse> getUserInfo(@PathVariable("userId") String userId);
    
    @PostMapping("/validate-token")
    Result<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request);
}
```

### 错误处理
1. 服务不可用时返回统一错误码
2. 实现重试机制（最多3次）
3. 记录服务调用日志
4. 提供服务降级方案

## 配置管理规范

### 应用配置
```yaml
# application.yml
spring:
  application:
    name: ${SERVICE_NAME}
  profiles:
    active: ${ACTIVE_PROFILE:dev}
  
server:
  port: ${SERVER_PORT:8080}

# 数据库配置
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 环境变量
- `SERVICE_NAME`: 服务名称
- `ACTIVE_PROFILE`: 激活的配置文件
- `SERVER_PORT`: 服务端口
- `DB_HOST`: 数据库主机
- `DB_PORT`: 数据库端口
- `DB_NAME`: 数据库名称

## 测试规范

### 单元测试
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserManager userManager;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void testRegister_Success() {
        // Given
        UserRegisterRequest request = createRegisterRequest();
        when(userManager.existsByUsername(anyString())).thenReturn(false);
        
        // When
        UserInfoResponse response = userService.register(request);
        
        // Then
        assertThat(response.getUsername()).isEqualTo(request.getUsername());
    }
}
```

### 集成测试
1. 使用TestContainers进行数据库测试
2. Mock外部服务依赖
3. 测试覆盖率不低于80%
4. 包含异常场景测试

## 部署规范

### Docker配置
```dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 健康检查
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 检查数据库连接
        // 检查外部服务连接
        return Health.up().build();
    }
}
```

## 监控和日志

### 日志规范
```java
// 使用结构化日志
log.info("用户注册成功, userId={}, username={}", userId, username);

// 错误日志包含异常堆栈
log.error("用户注册失败, request={}", request, e);
```

### 监控指标
1. 接口响应时间
2. 错误率统计
3. 数据库连接池状态
4. JVM内存使用情况

## 注意事项

1. **严格遵循** 全局响应处理器使用规范
2. **每个服务** 只包含一个完整功能示例
3. **核心逻辑** 不超过50行实现
4. **优先级**: 规范说明 > 代码示例
5. **兼容性**: 确保与现有架构100%兼容