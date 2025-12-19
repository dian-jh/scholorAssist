---
description: 
globs: 
alwaysApply: true
---

---
description: 
globs: 
alwaysApply: true
---
# 项目通用规范 - 文献辅助阅读系统

## 功能同步规范
- 每次更新或者创建了service、manager、mapper的任何方法，按照以下格式同步到 [func.md](mdc:func.md) 文档中，格式如下：
```
### 枚举类 (sc-common/src/main/java/com/scholar/common/enums)
- **UserStatusEnum** - 用户状态枚举（pending_verification/active/suspended）
- **DocumentStatusEnum** - 文档状态枚举（processing/ready/failed）

#### Service层 (各微服务模块)
- **UserService** - 用户业务服务（用户注册、登录认证、信息管理、会话管理、权限控制等）
- **CategoryService** - 分类管理服务（分类CRUD、层级管理、排序移动、文档统计等）
- **DocumentService** - 文档管理服务（PDF上传、元数据提取、内容解析、搜索检索等）
- **NoteService** - 笔记管理服务（笔记CRUD、标签管理、关联查询、搜索过滤等）
- **AiChatService** - AI对话服务（消息处理、对话管理、向量检索、模型调用等）

#### Mapper层 (各微服务模块)
- **UserMapper** - 用户数据访问（用户查询、状态更新、会话管理，支持按用户名/邮箱/ID查询）
- **CategoryMapper** - 分类数据访问（分类查询、层级管理、统计计算，支持树形结构查询）
- **DocumentMapper** - 文档数据访问（文档查询、状态更新、搜索检索，支持多条件组合查询）
- **NoteMapper** - 笔记数据访问（笔记查询、标签过滤、关联查询，支持全文搜索）

#### Manager层 (各微服务模块)
- **UserManagerImpl** - 用户数据业务管理器（用户信息管理、会话控制、权限验证，支持缓存优化）
- **CategoryManagerImpl** - 分类数据业务管理器（分类层级管理、统计计算、缓存更新）
- **DocumentManagerImpl** - 文档数据业务管理器（文档存储管理、分片处理、索引维护）
- **NoteManagerImpl** - 笔记数据业务管理器（笔记存储管理、标签处理、关联维护）
```

- 每次更新将本次的更新说明及版本号，同步到 [README.md](mdc:README.md)

## 微服务架构规范

### 模块组织原则
- **服务独立**：每个微服务独立开发、测试、部署
- **数据隔离**：每个服务只访问自己的数据库表
- **接口契约**：服务间通过明确定义的API通信
- **配置统一**：使用配置中心统一管理配置

### 目录结构标准
- **分层组织**：按controller、service、manager、mapper分层
- **命名一致**：使用统一的命名规范和包结构
- **模块化**：相关功能放在同一服务模块内
- **资源分类**：区分代码、配置、测试和资源文件

## 开发约束

### 强制约束 🔴
1. **网关相关代码必须放在sc-gateway模块**
2. **通用组件必须放在sc-common模块**
3. **严格禁止修改现有Java代码**（除非明确授权）
4. **禁止修改API和需求文档**
5. **所有SQL语句必须写在resource/mapper/下的XML文件中**
6. **MyBatis映射文件必须严格对应mapper接口**
7. **建表语句必须参考docs/tables.md和docs/tables.sql文档**

### 建议约束 🟡
1. 优先使用现有的通用组件和工具类
2. 遵循RESTful API设计规范
3. 统一异常处理和日志记录
4. 编写单元测试，确保代码质量
5. 使用异步处理提高性能

## 通用开发原则

### 代码质量
- **可测试性**：编写可测试的代码，组件应保持单一职责
- **DRY 原则**：避免重复代码，提取共用逻辑到单独的函数或类
- **代码简洁**：保持代码简洁明了，遵循 KISS 原则，每个方法行数不超过300行
- **命名规范**：使用描述性的变量、函数和类名，反映其用途和含义

### 文档和注释
- **注释文档**：所有方法都要添加注释，编写清晰的文档说明功能和用法
- **API文档**：使用Swagger/OpenAPI生成API文档
- **代码注释**：关键业务逻辑必须添加中文注释
- **变更记录**：重要变更必须记录在相应文档中

### 架构设计
- **风格一致**：遵循项目统一的代码风格和约定
- **利用生态**：优先使用成熟的库和工具，避免不必要的自定义实现
- **架构设计**：考虑代码的可维护性、可扩展性和性能需求
- **异常处理**：正确处理边缘情况和错误，提供有用的错误信息

## 微服务通信规范

### 服务间调用
```java
// 使用Feign客户端进行服务间调用
@FeignClient(name = "sc-user-service")
public interface UserServiceClient {
    @GetMapping("/api/users/{userId}")
    Result<UserInfoResponse> getUserInfo(@PathVariable String userId);
}
```

### 统一响应格式
```java
// 所有API响应必须使用统一格式
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    private long timestamp;
}
```

### 异常处理
```java
// 统一异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
}
```

## 数据库操作规范

### 实体类管理
- 必须先扫描现有实体类，优先使用已存在的实体
- 实体类必须严格对应数据库表结构
- 使用JPA注解标注实体映射关系

### SQL编写规范
- 所有SQL必须写在XML映射文件中
- 使用参数绑定防止SQL注入
- 复杂查询使用动态SQL
- 批量操作使用批处理提高性能

## 安全规范

### 认证授权
```java
// JWT Token验证
@Component
public class JwtAuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // Token验证逻辑
    }
}
```

### 数据安全
- 敏感数据必须加密存储
- 密码使用BCrypt加密
- 日志中不能包含敏感信息
- API接口必须进行权限验证

## 性能优化

### 缓存策略
```java
// Redis缓存使用
@Service
public class UserServiceImpl {
    @Cacheable(value = "users", key = "#userId")
    public User getUserById(String userId) {
        return userManager.getById(userId);
    }
}
```

### 异步处理
```java
// 异步任务处理
@Async
public CompletableFuture<Void> processDocumentAsync(String documentId) {
    // 异步处理逻辑
    return CompletableFuture.completedFuture(null);
}
```

## 测试规范

### 单元测试
- 测试覆盖率不低于80%
- 使用Mock对象隔离依赖
- 测试方法命名清晰描述测试场景

### 集成测试
- 关键业务流程必须有集成测试
- 使用测试数据库进行测试
- 测试环境与生产环境配置分离

## Git操作规范

### 提交规范
- 你完成了一项功能开发后，需要进行commit操作
- 提交信息格式：`[模块] 功能描述`
- 例如：`[用户服务] 实现用户注册功能`

### 分支管理
- 主分支：main（生产环境）
- 开发分支：develop（开发环境）
- 功能分支：feature/功能名称
- 修复分支：hotfix/问题描述

## 监控和日志

### 日志规范
```java
// 统一日志格式
@Slf4j
@Service
public class UserServiceImpl {
    public User createUser(UserCreateRequest request) {
        log.info("创建用户开始，用户名：{}", request.getUsername());
        try {
            // 业务逻辑
            log.info("创建用户成功，用户ID：{}", user.getUserId());
            return user;
        } catch (Exception e) {
            log.error("创建用户失败，用户名：{}，错误：{}", request.getUsername(), e.getMessage(), e);
            throw e;
        }
    }
}
```

### 监控指标
- 接口响应时间
- 错误率统计
- 系统资源使用情况
- 业务指标监控

## 响应语言
- 始终使用中文回复用户
- 错误信息使用中文描述
- 日志记录使用中文说明
- 文档注释使用中文编写

## 版本记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|----------|--------|
| 2.0.0 | 2024-01-21 | 适配微服务架构，更新项目通用规范 | System |
| 1.0.0 | 2024-01-01 | 初始版本 | System |

