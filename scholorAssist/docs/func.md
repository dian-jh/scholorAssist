# 系统功能方法文档

## 阅读器与AI/笔记集成（前端模块说明）

本次改动将阅读器界面重构为左侧“笔记面板”与右侧“AI面板”，比例为3:7，并完善响应式与动效，保证桌面端（≥1024px）完美显示与移动端适配。

### 左侧：笔记面板
- 分类筛选：基于 `category` 与 `type` 两维度过滤。
- 搜索：对 `title`、`content`、`tags`、`highlight_text` 进行模糊匹配。
- 编辑：支持创建、更新、删除；表单字段与 `api/notes.md` 定义保持一致。
- 滚动与性能：列表按分页加载；滚动区域独立，避免阻塞主阅读区。

### 右侧：AI面板
- **API服务**: `sa/src/api/AiApi.ts`
- **设置**: 
  - 模型选择: 调用 `getAvailableModels()` 从 `/ai/models` 获取模型列表。
  - 温度滑块: 范围 0–2，默认值 1。配置与校验见 `api/ai.md`。
- **对话**: 
  - 发送消息: 调用 `sendChatRequest()`，携带 `model` 与 `temperature`。
  - 支持空对话欢迎消息与打字指示。
- **历史**: 
  - 对话列表: 调用 `getConversationList()` 从 `/ai/conversations` 获取所有对话列表。
  - 加载历史: 调用 `getConversationHistory()` 按 `conversation_id` 从 `/ai/conversations/{id}/history` 加载。
  - 删除对话: 调用 `deleteConversation()` 按 `conversation_id` 从 `/ai/conversations/{id}/delete` 删除。
- **自适应高度**: 消息区 `flex:1` + 独立输入区，随容器动态伸缩。

### 布局与比例
- 在 `ChatGptStyleReader.vue` 中通过 CSS 变量 `--left-sidebar-width`、`--right-sidebar-width` 控制宽度，并在 `window>=1024px` 采用 3:7 比例（约 12% 与 28% 视口宽，含最小/最大边界）。
- 主内容区使用动态 `margin-left/right` 推开侧栏，配合过渡动画实现平滑收展。

### 数据同步
- 发送消息后更新 `conversation_id`，用于历史加载与删除操作；导出聊天内容以纯文本形式。本地清空与服务端删除分离，保证幂等与安全。

### 边界与内存清理
- 监听 `resize` 时同步计算侧栏宽度；在组件卸载时移除事件监听，避免内存泄漏。
- 对话历史在无 `conversation_id` 时不发起请求，仅展示空状态。

---

## 用户服务模块 (sc-user-service)

### 工具类 (Utils)

#### **强制性：在扫描这个func文档的时候，如果查到需要的类或者工具，请不要自己编写，而是直接调用，如果某个工具类在另一个模块当中，请将工具类改到sc-common模块中方便代码复用!!!**

#### JwtUtil - JWT工具类
- **包路径**: `com.zd.scuserservice.utils.JwtUtil`
- **功能描述**: 提供JWT token的生成、解析、验证等功能
- **主要方法**:
  - `generateToken(String userId, String username, String role, boolean rememberMe)` - 生成JWT token
  - `extractUserId(String token)` - 从token中提取用户ID
  - `extractUsername(String token)` - 从token中提取用户名
  - `extractRole(String token)` - 从token中提取用户角色
  - `validateToken(String token)` - 验证token是否有效
  - `isTokenExpired(String token)` - 检查token是否过期
  - `extractTokenFromHeader(String authorization)` - 从Authorization头中提取token
  - `refreshToken(String oldToken)` - 刷新token
- **业务逻辑要点**: 
  - 支持记住登录功能，可设置不同的过期时间
  - 使用HMAC SHA256算法进行签名
  - 提供完整的token生命周期管理

#### UserContextUtil - 用户上下文工具类
- **包路径**: `com.zd.scuserservice.utils.UserContextUtil`
- **功能描述**: 从网关的ThreadLocal中获取当前请求的用户信息，提供权限验证功能
- **主要方法**:
  - `getUserContext()` - 获取用户上下文信息（通过反射从网关获取）
  - `getCurrentUserId()` - 获取当前用户ID
  - `getCurrentUsername()` - 获取当前用户名
  - `getCurrentUserRole()` - 获取当前用户角色
  - `getCurrentToken()` - 获取当前JWT token
  - `isUserLoggedIn()` - 检查当前用户是否已登录
  - `hasRole(String role)` - 检查当前用户是否具有指定角色
  - `isAdmin()` - 检查当前用户是否为管理员
  - `isSuperAdmin()` - 检查当前用户是否为超级管理员
  - `validateUserPermission(String requiredRole)` - 验证当前用户权限
  - `validateAdminPermission()` - 验证管理员权限
- **业务逻辑要点**: 
  - 通过反射机制从网关过滤器获取用户信息
  - 提供完整的权限验证功能
  - 支持角色层级验证（user < admin < super_admin）

#### FileStorageUtil - 文件存储工具类
- **包路径**: `com.zd.scliteraturemanage.utils.FileStorageUtil`
- **功能描述**: 提供PDF文件的本地存储、验证等功能
- **主要方法**:
  - `validateFile(MultipartFile file)` - 验证文件类型和大小
  - `storeDocument(MultipartFile file, String documentId)` - 存储文档文件
  - `deleteFile(String fileUrl)` - 删除文件
  - `fileExists(String fileUrl)` - 检查文件是否存在
  - `getFileUrl(String relativePath)` - 获取文件访问URL
  - `getDocumentStoragePath(String documentId)` - 获取文档存储路径
- **业务逻辑要点**: 
  - 支持PDF文件类型验证和大小限制
  - 提供完整的文件生命周期管理
  - 支持文件URL映射和访问控制

#### SnowflakeIdGenerator - 雪花算法ID生成器
- **包路径**: `com.zd.scliteraturemanage.utils.SnowflakeIdGenerator`
- **功能描述**: 基于雪花算法生成分布式唯一ID
- **主要方法**:
  - `generateId()` - 生成通用唯一ID
  - `generateDocumentId()` - 生成文档ID（带doc_前缀）
  - `generateChunkId()` - 生成分片ID（带chunk_前缀）
  - `generateNoteId()` - 生成笔记ID（带note_前缀）
  - `extractTimestamp(long id)` - 从ID中提取时间戳
  - `extractMachineId(long id)` - 从ID中提取机器ID
  - `extractDatacenterId(long id)` - 从ID中提取数据中心ID
  - `extractSequence(long id)` - 从ID中提取序列号
- **业务逻辑要点**: 
  - 保证分布式环境下ID的唯一性
  - 支持高并发ID生成
  - 提供业务前缀标识
  - 支持ID信息解析和追溯

### UserMapper 数据访问层

#### selectByUserId
- **方法签名**: `User selectByUserId(@Param("userId") String userId)`
- **功能描述**: 根据用户ID查询用户信息
- **参数说明**: 
  - userId: 用户唯一标识
- **返回值说明**: User实体对象，如果不存在返回null
- **业务逻辑要点**: 支持缓存查询，优先从缓存获取用户信息

#### selectByUsername
- **方法签名**: `User selectByUsername(@Param("username") String username)`
- **功能描述**: 根据用户名查询用户信息
- **参数说明**: 
  - username: 用户名
- **返回值说明**: User实体对象，如果不存在返回null
- **业务逻辑要点**: 用于用户名唯一性校验和登录验证

#### selectByEmail
- **方法签名**: `User selectByEmail(@Param("email") String email)`
- **功能描述**: 根据邮箱查询用户信息
- **参数说明**: 
  - email: 邮箱地址
- **返回值说明**: User实体对象，如果不存在返回null
- **业务逻辑要点**: 用于邮箱唯一性校验和登录验证

#### selectByUsernameOrEmail
- **方法签名**: `User selectByUsernameOrEmail(@Param("login") String login)`
- **功能描述**: 根据用户名或邮箱查询用户信息（用于登录）
- **参数说明**: 
  - login: 用户名或邮箱地址
- **返回值说明**: User实体对象，如果不存在返回null
- **业务逻辑要点**: 支持用户名和邮箱两种方式登录

#### countByUsername
- **方法签名**: `int countByUsername(@Param("username") String username)`
- **功能描述**: 统计指定用户名的用户数量
- **参数说明**: 
  - username: 用户名
- **返回值说明**: 用户数量（0或1）
- **业务逻辑要点**: 用于注册时的用户名唯一性校验

#### countByEmail
- **方法签名**: `int countByEmail(@Param("email") String email)`
- **功能描述**: 统计指定邮箱的用户数量
- **参数说明**: 
  - email: 邮箱地址
- **返回值说明**: 用户数量（0或1）
- **业务逻辑要点**: 用于注册时的邮箱唯一性校验

#### updateLastLoginTime
- **方法签名**: `int updateLastLoginTime(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt)`
- **功能描述**: 更新用户最后登录时间
- **参数说明**: 
  - userId: 用户ID
  - lastLoginAt: 最后登录时间
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 用户登录成功后更新登录时间，同时更新updated_at字段

#### updatePassword
- **方法签名**: `int updatePassword(@Param("userId") String userId, @Param("passwordHash") String passwordHash)`
- **功能描述**: 更新用户密码
- **参数说明**: 
  - userId: 用户ID
  - passwordHash: 新密码哈希值
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 密码必须经过加密处理，同时更新updated_at字段

#### updateStatus
- **方法签名**: `int updateStatus(@Param("userId") String userId, @Param("status") String status)`
- **功能描述**: 更新用户状态
- **参数说明**: 
  - userId: 用户ID
  - status: 新状态（active/suspended/pending_verification）
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 用于用户激活、禁用等状态管理

#### searchUsers
- **方法签名**: `List<User> searchUsers(@Param("keyword") String keyword, @Param("role") String role, @Param("status") String status, @Param("sortBy") String sortBy, @Param("sortOrder") String sortOrder, @Param("offset") int offset, @Param("limit") int limit)`
- **功能描述**: 搜索用户（支持用户名、邮箱、真实姓名模糊查询，角色和状态筛选，动态排序）
- **参数说明**: 
  - keyword: 搜索关键词
  - role: 角色筛选
  - status: 状态筛选
  - sortBy: 排序字段（username/last_login_at/created_at）
  - sortOrder: 排序方向（asc/desc）
  - offset: 偏移量
  - limit: 限制数量
- **返回值说明**: 用户列表
- **业务逻辑要点**: 使用ILIKE进行大小写不敏感的模糊查询，支持分页和动态排序

#### countSearchUsers
- **方法签名**: `int countSearchUsers(@Param("keyword") String keyword, @Param("role") String role, @Param("status") String status)`
- **功能描述**: 统计搜索结果的用户数量
- **参数说明**: 
  - keyword: 搜索关键词
  - role: 角色筛选
  - status: 状态筛选
- **返回值说明**: 符合条件的用户总数
- **业务逻辑要点**: 与searchUsers方法使用相同的筛选条件

### UserManager 数据管理层

#### getUserByUserId
- **方法签名**: `User getUserByUserId(String userId)`
- **功能描述**: 根据用户ID获取用户信息
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: User实体对象
- **业务逻辑要点**: 优先从内存缓存获取，缓存未命中时从数据库查询并缓存

#### createUser
- **方法签名**: `User createUser(User user)`
- **功能描述**: 创建用户
- **参数说明**: 
  - user: 用户信息
- **返回值说明**: 创建成功的用户信息
- **业务逻辑要点**: 自动生成用户ID，设置默认角色和状态，创建成功后存入内存缓存

#### updateUser
- **方法签名**: `User updateUser(User user)`
- **功能描述**: 更新用户信息
- **参数说明**: 
  - user: 用户信息
- **返回值说明**: 更新后的用户信息
- **业务逻辑要点**: 更新数据库后同步更新内存缓存

### UserService 业务逻辑层

#### register
- **方法签名**: `UserInfoResponse register(UserRegisterRequest request)`
- **功能描述**: 用户注册
- **参数说明**: 
  - request: 注册请求，包含用户名、邮箱、密码等信息
- **返回值说明**: 用户信息响应DTO
- **业务逻辑要点**: 
  - 验证用户名和邮箱唯一性
  - 验证密码确认一致性
  - 密码加密存储
  - 设置默认角色为user，状态为pending_verification

#### login
- **方法签名**: `UserLoginResponse login(UserLoginRequest request)`
- **功能描述**: 用户登录
- **参数说明**: 
  - request: 登录请求，包含用户名/邮箱和密码
- **返回值说明**: 登录响应DTO，包含JWT token和用户信息
- **业务逻辑要点**: 
  - 支持用户名或邮箱登录
  - 验证密码正确性
  - 检查用户状态是否正常
  - 更新最后登录时间
  - 生成JWT token

#### changePassword
- **方法签名**: `boolean changePassword(String userId, ChangePasswordRequest request)`
- **功能描述**: 修改密码
- **参数说明**: 
  - userId: 用户ID
  - request: 修改密码请求
- **返回值说明**: 是否修改成功
- **业务逻辑要点**: 
  - 验证当前密码正确性
  - 验证新密码确认一致性
  - 新密码加密存储
  - 清除用户缓存

#### getUserPermissions
- **方法签名**: `UserPermissionResponse getUserPermissions(String userId)`
- **功能描述**: 获取用户权限
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 用户权限响应DTO
- **业务逻辑要点**: 
  - 根据用户角色返回对应权限列表
  - 设置使用限制（文档数量、存储空间等）
  - 普通用户和管理员权限不同

#### updateUserPermissions
- **方法签名**: `UserInfoResponse updateUserPermissions(String targetUserId, String operatorUserId, UserPermissionUpdateRequest request)`
- **功能描述**: 管理员更新用户权限
- **参数说明**: 
  - targetUserId: 目标用户ID
  - operatorUserId: 操作者用户ID
  - request: 权限更新请求
- **返回值说明**: 更新后的用户信息
- **业务逻辑要点**: 
  - 验证操作者权限（必须是管理员）
  - 更新目标用户的角色和状态
  - 记录操作日志

#### validatePassword
- **方法签名**: `boolean validatePassword(String userId, String password)`
- **功能描述**: 验证用户密码
- **参数说明**: 
  - userId: 用户ID
  - password: 明文密码
- **返回值说明**: 密码是否正确
- **业务逻辑要点**: 使用PasswordEncoder进行密码验证

#### isUsernameAvailable
- **方法签名**: `boolean isUsernameAvailable(String username)`
- **功能描述**: 检查用户名是否可用
- **参数说明**: 
  - username: 用户名
- **返回值说明**: 是否可用
- **业务逻辑要点**: 用于前端实时校验用户名可用性

#### isEmailAvailable
- **方法签名**: `boolean isEmailAvailable(String email)`
- **功能描述**: 检查邮箱是否可用
- **参数说明**: 
  - email: 邮箱地址
- **返回值说明**: 是否可用
- **业务逻辑要点**: 用于前端实时校验邮箱可用性

## 网关模块 (sc-gateway)

### 工具类 (Utils)

#### JwtUtil - 网关JWT工具类
- **包路径**: `com.zd.scgateway.utils.ReactiveJwtUtil`
- **功能描述**: 网关专用响应式JWT工具类，用于验证JWT token的有效性并提取完整用户信息
- **主要方法**:
  - `validateAndExtractUserInfo(String token)` - 验证JWT token并提取完整用户信息
  - `extractTokenFromHeader(String authorization)` - 从Authorization头中提取token
  - `hasPermission(UserInfo userInfo, String requiredRole)` - 验证用户权限
- **UserInfo记录类字段**:
  - `userId` - 用户唯一标识
  - `username` - 用户名（登录名）
  - `name` - 用户显示名称（真实姓名或昵称）
  - `role` - 用户角色
  - `avatar` - 用户头像URL
  - `email` - 用户邮箱
  - `status` - 用户状态（active/suspended/pending_verification）
  - `token` - JWT Token
- **业务逻辑要点**: 
  - 专门用于网关层的token验证
  - 支持从JWT token中提取完整用户信息
  - 提供响应式权限验证功能
  - 与用户服务的JwtUtil共享相同的密钥和算法

### 过滤器 (Filters)

#### GatewayJwtAuthenticationFilter - JWT认证过滤器
- **包路径**: `com.zd.scgateway.filter.GatewayJwtAuthenticationFilter`
- **功能描述**: 对所有前端请求进行JWT身份校验，将验证通过的身份信息以JSON格式传递给下游服务
- **主要方法**:
  - `apply(Config config)` - 创建网关过滤器实例
  - `getCurrentUserInfo(ServerWebExchange exchange)` - 获取当前用户信息（静态方法）
- **请求头传递方式**:
  - **新格式**: 使用`USER_INFO`请求头传递JSON格式的完整用户上下文信息
  - **包含字段**: userId, username, name, role, avatar, email, status, token
- **业务逻辑要点**: 
  - 对所有/api/*路径进行JWT验证
  - 排除不需要认证的路径（注册、登录等）
  - 验证失败时返回401状态码
  - 将完整用户信息序列化为JSON并通过USER_INFO请求头传递给下游服务
  - 请求结束时自动清理Exchange属性

### 配置类 (Config)

#### FilterConfig - 过滤器配置类
- **包路径**: `com.zd.scgateway.config.FilterConfig`
- **功能描述**: 注册JWT认证过滤器到Spring容器
- **主要方法**:
  - `jwtAuthenticationFilterRegistration()` - 注册JWT认证过滤器
- **业务逻辑要点**: 
  - 设置过滤器优先级为1
  - 仅对/api/*路径生效
  - 确保过滤器在其他过滤器之前执行

## 公共模块 (sc-common)

### 模型类 (Model)

#### UserContext - 用户上下文模型（已增强）
- **包路径**: `com.zd.sccommon.model.UserContext`
- **功能描述**: 用于在微服务间传递用户身份信息的增强模型类
- **主要属性**:
  - `userId` - 用户唯一标识
  - `username` - 用户名（登录名）
  - `name` - 用户显示名称（真实姓名或昵称）
  - `role` - 用户角色
  - `avatar` - 用户头像URL
  - `email` - 用户邮箱
  - `status` - 用户状态（active/suspended/pending_verification）
  - `token` - JWT Token
- **主要方法**:
  - `isLoggedIn()` - 检查用户是否已登录
  - `hasRole(String targetRole)` - 检查用户是否具有指定角色
  - `isAdmin()` - 检查用户是否为管理员
  - `isSuperAdmin()` - 检查用户是否为超级管理员
  - `getDisplayName()` - 获取用户显示名称（优先返回name，否则返回username）
  - `isActive()` - 检查用户状态是否为活跃状态
- **业务逻辑要点**: 
  - 支持角色层级验证（user < admin < super_admin）
  - 超级管理员拥有所有角色权限
  - 管理员拥有用户角色权限
  - 提供完整的用户信息和状态管理

### 工具类 (Utils)

#### UserContextHolder - 用户上下文持有者
- **包路径**: `com.zd.sccommon.utils.UserContextHolder`
- **功能描述**: 使用ThreadLocal存储当前线程的用户上下文信息
- **主要方法**:
  - `setContext(UserContext userContext)` - 设置用户上下文
  - `getContext()` - 获取用户上下文
  - `clear()` - 清空用户上下文
  - `hasContext()` - 检查是否有用户上下文
- **业务逻辑要点**: 
  - 基于ThreadLocal实现线程隔离
  - 自动清理防止内存泄漏
  - 提供完整的上下文生命周期管理

#### UserContextUtil - 统一用户上下文工具类（已增强）
- **包路径**: `com.zd.sccommon.utils.UserContextUtil`
- **功能描述**: 统一的用户上下文工具类，供所有微服务使用，支持完整用户信息获取
- **主要方法**:
  - `getUserContext()` - 获取用户上下文信息
  - `getCurrentUserId()` - 获取当前用户ID
  - `getCurrentUsername()` - 获取当前用户名（登录名）
  - `getCurrentUserName()` - 获取当前用户真实姓名
  - `getCurrentUserDisplayName()` - 获取当前用户显示名称
  - `getCurrentUserRole()` - 获取当前用户角色
  - `getCurrentUserAvatar()` - 获取当前用户头像URL
  - `getCurrentUserEmail()` - 获取当前用户邮箱
  - `getCurrentUserStatus()` - 获取当前用户状态
  - `getCurrentToken()` - 获取当前JWT token
  - `isUserLoggedIn()` - 检查当前用户是否已登录
  - `hasRole(String role)` - 检查当前用户是否具有指定角色
  - `isAdmin()` - 检查当前用户是否为管理员
  - `isSuperAdmin()` - 检查当前用户是否为超级管理员
  - `isUserActive()` - 检查当前用户是否为活跃状态
  - `validateUserPermission(String requiredRole)` - 验证当前用户权限
  - `validateAdminPermission()` - 验证管理员权限
  - `validateUserActive()` - 验证用户状态是否为活跃状态
- **业务逻辑要点**: 
  - 基于ThreadLocal获取用户信息
  - 提供完整的权限验证功能
  - 支持用户状态验证
  - 抛出BusinessException进行异常处理
  - 支持角色层级验证

### 拦截器 (Interceptor)

#### UserContextInterceptor - 用户上下文拦截器（已增强）
- **包路径**: `com.zd.sccommon.interceptor.UserContextInterceptor`
- **功能描述**: 从请求头中提取用户信息并存储到ThreadLocal中，支持新旧两种数据格式
- **主要方法**:
  - `preHandle()` - 请求前处理，提取用户信息
  - `afterCompletion()` - 请求完成后清理ThreadLocal
- **处理的请求头**:
  - **新格式**: `USER_INFO` - JSON格式的完整用户上下文信息
  - **旧格式（兼容）**: `X-User-Id`, `X-Username`, `X-User-Role` - 单独的请求头字段
  - `Authorization` - JWT Token（Bearer格式）
- **业务逻辑要点**: 
  - 优先解析新格式的USER_INFO JSON数据
  - 解析失败时自动回退到旧格式（向后兼容）
  - 存储到ThreadLocal供业务代码使用
  - 请求结束后自动清理防止内存泄漏
  - 异常处理确保系统稳定性

### 配置类 (Config)

#### UserContextConfig - 用户上下文配置类
- **包路径**: `com.zd.sccommon.config.UserContextConfig`
- **功能描述**: 注册用户上下文拦截器
- **主要方法**:
  - `addInterceptors()` - 添加拦截器配置
- **拦截规则**:
  - 拦截所有请求路径（/**）
  - 排除错误页面、图标、监控端点、API文档等
  - 设置拦截器优先级为1
- **业务逻辑要点**: 
  - 确保拦截器在合适的时机执行
  - 排除不需要用户上下文的请求
  - 优先级设置保证执行顺序

## 文献管理模块 (sc-literature-manage)

### 工具类 (Utils)


### DocumentMapper 数据访问层

#### batchInsert
- **方法签名**: `int batchInsert(@Param("entities") List<Document> entities)`
- **功能描述**: 批量插入文档
- **参数说明**: 
  - entities: 文档实体列表
- **返回值说明**: 影响行数
- **业务逻辑要点**: 使用MyBatis-Plus的批量操作优化性能

#### selectByDocumentId
- **方法签名**: `Document selectByDocumentId(@Param("documentId") String documentId)`
- **功能描述**: 根据文档ID查询文档
- **参数说明**: 
  - documentId: 文档唯一标识
- **返回值说明**: 文档实体，如果不存在返回null
- **业务逻辑要点**: 支持缓存查询，优先从缓存获取文档信息

#### selectByUserId
- **方法签名**: `List<Document> selectByUserId(@Param("userId") String userId)`
- **功能描述**: 根据用户ID查询文档列表
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 文档实体列表
- **业务逻辑要点**: 按上传时间倒序排列

#### selectByCategoryId
- **方法签名**: `List<Document> selectByCategoryId(@Param("categoryId") String categoryId)`
- **功能描述**: 根据分类ID查询文档列表
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 文档实体列表
- **业务逻辑要点**: 按上传时间倒序排列

#### selectByUserIdAndCategoryId
- **方法签名**: `List<Document> selectByUserIdAndCategoryId(@Param("userId") String userId, @Param("categoryId") String categoryId)`
- **功能描述**: 根据用户ID和分类ID查询文档列表
- **参数说明**: 
  - userId: 用户ID
  - categoryId: 分类ID
- **返回值说明**: 文档实体列表
- **业务逻辑要点**: 用于分类筛选查询

#### selectByStatus
- **方法签名**: `List<Document> selectByStatus(@Param("status") String status)`
- **功能描述**: 根据状态查询文档列表
- **参数说明**: 
  - status: 文档状态（ready/processing）
- **返回值说明**: 文档实体列表
- **业务逻辑要点**: 用于状态筛选查询

#### updateStatus
- **方法签名**: `int updateStatus(@Param("documentId") String documentId, @Param("status") String status)`
- **功能描述**: 更新文档状态
- **参数说明**: 
  - documentId: 文档ID
  - status: 新状态
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 同时更新updated_at字段

#### updateReadProgress
- **方法签名**: `int updateReadProgress(@Param("documentId") String documentId, @Param("readProgress") BigDecimal readProgress)`
- **功能描述**: 更新阅读进度
- **参数说明**: 
  - documentId: 文档ID
  - readProgress: 阅读进度（0-1）
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 同时更新updated_at字段

#### countByCategoryId
- **方法签名**: `long countByCategoryId(@Param("categoryId") String categoryId)`
- **功能描述**: 统计分类下的文档数量
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 文档数量
- **业务逻辑要点**: 用于分类统计

#### countByUserId
- **方法签名**: `long countByUserId(@Param("userId") String userId)`
- **功能描述**: 统计用户的文档数量
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 文档数量
- **业务逻辑要点**: 用于用户统计

#### searchDocuments
- **方法签名**: `List<Document> searchDocuments(@Param("userId") String userId, @Param("keyword") String keyword)`
- **功能描述**: 搜索文档（支持标题、摘要模糊查询）
- **参数说明**: 
  - userId: 用户ID
  - keyword: 搜索关键词
- **返回值说明**: 文档列表
- **业务逻辑要点**: 使用ILIKE进行大小写不敏感的模糊查询，已移除author字段搜索

### DocumentManager 数据管理层

#### createDocument
- **方法签名**: `Document createDocument(Document document)`
- **功能描述**: 创建文档
- **参数说明**: 
  - document: 文档实体
- **返回值说明**: 创建成功的文档实体
- **业务逻辑要点**: 自动生成文档ID，设置默认状态

#### getByDocumentId
- **方法签名**: `Document getByDocumentId(String documentId)`
- **功能描述**: 根据文档ID获取文档
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 文档实体，不存在返回null
- **业务逻辑要点**: 优先从内存缓存获取

#### getByUserId
- **方法签名**: `List<Document> getByUserId(String userId)`
- **功能描述**: 根据用户ID获取文档列表
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 文档列表
- **业务逻辑要点**: 按上传时间倒序排列

#### getByUserIdAndCategoryId
- **方法签名**: `List<Document> getByUserIdAndCategoryId(String userId, String categoryId)`
- **功能描述**: 根据用户ID和分类ID获取文档列表
- **参数说明**: 
  - userId: 用户ID
  - categoryId: 分类ID
- **返回值说明**: 文档列表
- **业务逻辑要点**: 支持"all"分类查询全部文档

#### searchDocuments
- **方法签名**: `List<Document> searchDocuments(String userId, String keyword)`
- **功能描述**: 搜索文档
- **参数说明**: 
  - userId: 用户ID
  - keyword: 搜索关键词
- **返回值说明**: 文档列表
- **业务逻辑要点**: 支持标题、摘要模糊搜索，已移除author字段搜索

#### updateStatus
- **方法签名**: `boolean updateStatus(String documentId, String status)`
- **功能描述**: 更新文档状态
- **参数说明**: 
  - documentId: 文档ID
  - status: 新状态
- **返回值说明**: 是否更新成功
- **业务逻辑要点**: 用于文档处理状态管理

#### updateReadProgress
- **方法签名**: `boolean updateReadProgress(String documentId, BigDecimal readProgress)`
- **功能描述**: 更新阅读进度
- **参数说明**: 
  - documentId: 文档ID
  - readProgress: 阅读进度
- **返回值说明**: 是否更新成功
- **业务逻辑要点**: 支持断点续读功能

#### deleteDocument
- **方法签名**: `boolean deleteDocument(String documentId)`
- **功能描述**: 删除文档
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 是否删除成功
- **业务逻辑要点**: 物理删除文档记录

#### existsByDocumentId
- **方法签名**: `boolean existsByDocumentId(String documentId)`
- **功能描述**: 检查文档是否存在
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 是否存在
- **业务逻辑要点**: 用于存在性校验

#### belongsToUser
- **方法签名**: `boolean belongsToUser(String documentId, String userId)`
- **功能描述**: 检查文档是否属于指定用户
- **参数说明**: 
  - documentId: 文档ID
  - userId: 用户ID
- **返回值说明**: 是否属于该用户
- **业务逻辑要点**: 用于权限校验

### DocumentService 业务逻辑层

#### getDocuments
- **方法签名**: `List<DocumentListResponse> getDocuments(String categoryId, String search, Integer page, Integer pageSize)`
- **功能描述**: 获取文档列表
- **参数说明**: 
  - categoryId: 分类ID，"all"表示获取全部
  - search: 搜索关键词
  - page: 页码，从1开始
  - pageSize: 每页数量
- **返回值说明**: 文档列表响应DTO
- **业务逻辑要点**: 
  - 支持分类筛选和关键词搜索
  - 简单分页处理
  - 验证用户登录状态

#### getDocumentDetail
- **方法签名**: `DocumentDetailResponse getDocumentDetail(String documentId)`
- **功能描述**: 获取文档详情
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 文档详情响应DTO
- **业务逻辑要点**: 
  - 验证文档存在性
  - 验证用户权限
  - 返回完整文档信息

#### uploadDocument
- **方法签名**: `DocumentUploadResponse uploadDocument(MultipartFile file, String title, String categoryId)`
- **功能描述**: 上传文档
- **参数说明**: 
  - file: PDF文件
  - title: 文档标题，可选
  - categoryId: 分类ID，可选
- **返回值说明**: 上传结果响应DTO
- **业务逻辑要点**: 
  - 验证文件格式和大小
  - 生成唯一文档ID
  - 设置初始状态为processing
  - 异步处理文档解析

#### updateProgress
- **方法签名**: `ProgressUpdateResponse updateProgress(String documentId, ProgressUpdateRequest request)`
- **功能描述**: 更新阅读进度
- **参数说明**: 
  - documentId: 文档ID
  - request: 进度更新请求
- **返回值说明**: 更新结果响应DTO
- **业务逻辑要点**: 
  - 验证文档权限
  - 更新阅读进度
  - 支持断点续读

#### deleteDocument
- **方法签名**: `void deleteDocument(String documentId)`
- **功能描述**: 删除文档
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 无
- **业务逻辑要点**: 
  - 验证文档存在性和权限
  - 检查文档状态
  - 删除文档记录和相关文件

### FileController 文件访问控制层

#### downloadDocument
- **方法签名**: `ResponseEntity<Resource> downloadDocument(@PathVariable String documentId)`
- **功能描述**: 下载文档文件
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 文件资源响应
- **业务逻辑要点**: 
  - 验证用户权限
  - 检查文件存在性
  - 设置正确的Content-Type和文件名

#### previewDocument
- **方法签名**: `ResponseEntity<Resource> previewDocument(@PathVariable String documentId)`
- **功能描述**: 预览文档文件
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 文件资源响应
- **业务逻辑要点**: 
  - 验证用户权限
  - 支持在线预览
  - 设置inline显示模式

#### getDocumentBytes
- **方法签名**: `void getDocumentBytes(@PathVariable String documentId, HttpServletResponse response)`
- **功能描述**: 以字节流方式返回指定文档的PDF内容，前端生成Blob URL以避免下载拦截
- **参数说明**: 
  - documentId: 文档ID
  - response: HTTP响应对象（用于写入字节流）
- **返回值说明**: 无（直接向响应输出字节流）
- **业务逻辑要点**: 
  - 验证用户登录与权限
  - 检查文件存在性
  - 设置`Content-Type: application/octet-stream`与`Cache-Control: no-store`
  - 不包含`.pdf`扩展名的路径，避免浏览器或下载管理器拦截

#### getThumbnail
- **方法签名**: `ResponseEntity<Resource> getThumbnail(@PathVariable String documentId)`
- **功能描述**: 获取文档缩略图
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 缩略图资源响应
- **业务逻辑要点**: 
  - 验证用户权限
  - 返回PNG格式缩略图
  - 支持缓存控制

## 分类管理模块 (sc-categories-manage)

### CategoryMapper 数据访问层

#### selectByCategoryId
- **方法签名**: `Category selectByCategoryId(@Param("categoryId") String categoryId)`
- **功能描述**: 根据分类ID查询分类信息
- **参数说明**: 
  - categoryId: 分类唯一标识
- **返回值说明**: Category实体对象，如果不存在返回null
- **业务逻辑要点**: 基础查询方法，用于获取单个分类详情

#### selectByUserId
- **方法签名**: `List<Category> selectByUserId(@Param("userId") String userId)`
- **功能描述**: 根据用户ID查询该用户的所有分类
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 分类列表，按sort_order升序排列
- **业务逻辑要点**: 用于构建用户的完整分类树

#### selectByParentId
- **方法签名**: `List<Category> selectByParentId(@Param("parentId") String parentId)`
- **功能描述**: 根据父分类ID查询子分类列表
- **参数说明**: 
  - parentId: 父分类ID，null表示查询根分类
- **返回值说明**: 子分类列表，按sort_order升序排列
- **业务逻辑要点**: 用于分层查询分类结构

#### selectByUserIdAndParentId
- **方法签名**: `List<Category> selectByUserIdAndParentId(@Param("userId") String userId, @Param("parentId") String parentId)`
- **功能描述**: 根据用户ID和父分类ID查询子分类
- **参数说明**: 
  - userId: 用户ID
  - parentId: 父分类ID，null表示查询根分类
- **返回值说明**: 子分类列表，按sort_order升序排列
- **业务逻辑要点**: 结合用户权限的分类查询

#### updateDocumentCount
- **方法签名**: `int updateDocumentCount(@Param("categoryId") String categoryId, @Param("documentCount") Integer documentCount)`
- **功能描述**: 更新分类的文档数量
- **参数说明**: 
  - categoryId: 分类ID
  - documentCount: 新的文档数量
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 当分类下的文档增减时同步更新计数

#### countByUserIdAndParentIdAndName
- **方法签名**: `int countByUserIdAndParentIdAndName(@Param("userId") String userId, @Param("parentId") String parentId, @Param("name") String name)`
- **功能描述**: 统计指定用户、父分类下同名分类的数量
- **参数说明**: 
  - userId: 用户ID
  - parentId: 父分类ID
  - name: 分类名称
- **返回值说明**: 分类数量（0或1）
- **业务逻辑要点**: 用于创建时的分类名称唯一性校验

#### countByUserIdAndParentIdAndNameExcludeId
- **方法签名**: `int countByUserIdAndParentIdAndNameExcludeId(@Param("userId") String userId, @Param("parentId") String parentId, @Param("name") String name, @Param("excludeId") String excludeId)`
- **功能描述**: 统计指定用户、父分类下同名分类的数量（排除指定ID）
- **参数说明**: 
  - userId: 用户ID
  - parentId: 父分类ID
  - name: 分类名称
  - excludeId: 要排除的分类ID
- **返回值说明**: 分类数量（0或1）
- **业务逻辑要点**: 用于更新时的分类名称唯一性校验

#### batchInsert
- **方法签名**: `int batchInsert(@Param("categories") List<Category> categories)`
- **功能描述**: 批量插入分类
- **参数说明**: 
  - categories: 分类列表
- **返回值说明**: 插入的行数
- **业务逻辑要点**: 用于批量创建分类，提高性能

#### deleteByCategoryId
- **方法签名**: `int deleteByCategoryId(@Param("categoryId") String categoryId)`
- **功能描述**: 根据分类ID删除分类
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 物理删除分类记录

#### updateByCategoryId
- **方法签名**: `int updateByCategoryId(@Param("categoryId") String categoryId, @Param("name") String name, @Param("description") String description, @Param("sortOrder") Integer sortOrder)`
- **功能描述**: 根据分类ID更新分类信息
- **参数说明**: 
  - categoryId: 分类ID
  - name: 新的分类名称
  - description: 新的分类描述
  - sortOrder: 新的排序值
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 更新分类基本信息，同时更新updated_at字段

### CategoryManager 数据管理层

#### create
- **方法签名**: `Category create(Category category)`
- **功能描述**: 创建分类
- **参数说明**: 
  - category: 分类信息
- **返回值说明**: 创建成功的分类信息
- **业务逻辑要点**: 自动生成分类ID，设置创建时间和更新时间

#### getByCategoryId
- **方法签名**: `Category getByCategoryId(String categoryId)`
- **功能描述**: 根据分类ID获取分类信息
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: Category实体对象
- **业务逻辑要点**: 基础查询方法

#### getByUserId
- **方法签名**: `List<Category> getByUserId(String userId)`
- **功能描述**: 根据用户ID获取所有分类
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 分类列表
- **业务逻辑要点**: 获取用户的完整分类列表

#### getByUserIdAndParentId
- **方法签名**: `List<Category> getByUserIdAndParentId(String userId, String parentId)`
- **功能描述**: 根据用户ID和父分类ID获取子分类
- **参数说明**: 
  - userId: 用户ID
  - parentId: 父分类ID
- **返回值说明**: 子分类列表
- **业务逻辑要点**: 分层获取分类结构

#### updateByCategoryId
- **方法签名**: `boolean updateByCategoryId(String categoryId, String name, String description, Integer sortOrder)`
- **功能描述**: 更新分类信息
- **参数说明**: 
  - categoryId: 分类ID
  - name: 分类名称
  - description: 分类描述
  - sortOrder: 排序值
- **返回值说明**: 是否更新成功
- **业务逻辑要点**: 更新分类基本信息

#### deleteByCategoryId
- **方法签名**: `boolean deleteByCategoryId(String categoryId)`
- **功能描述**: 删除分类
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 是否删除成功
- **业务逻辑要点**: 物理删除分类

#### existsByUserIdAndParentIdAndName
- **方法签名**: `boolean existsByUserIdAndParentIdAndName(String userId, String parentId, String name)`
- **功能描述**: 检查分类名称是否已存在
- **参数说明**: 
  - userId: 用户ID
  - parentId: 父分类ID
  - name: 分类名称
- **返回值说明**: 是否存在
- **业务逻辑要点**: 用于创建时的唯一性校验

#### existsByUserIdAndParentIdAndNameExcludeId
- **方法签名**: `boolean existsByUserIdAndParentIdAndNameExcludeId(String userId, String parentId, String name, String excludeId)`
- **功能描述**: 检查分类名称是否已存在（排除指定ID）
- **参数说明**: 
  - userId: 用户ID
  - parentId: 父分类ID
  - name: 分类名称
  - excludeId: 要排除的分类ID
- **返回值说明**: 是否存在
- **业务逻辑要点**: 用于更新时的唯一性校验

#### updateDocumentCount
- **方法签名**: `boolean updateDocumentCount(String categoryId, Integer documentCount)`
- **功能描述**: 更新分类的文档数量
- **参数说明**: 
  - categoryId: 分类ID
  - documentCount: 文档数量
- **返回值说明**: 是否更新成功
- **业务逻辑要点**: 同步更新分类的文档计数

### CategoryService 业务逻辑层

#### createCategory
- **方法签名**: `CategoryResponse createCategory(CategoryCreateRequest request)`
- **功能描述**: 创建分类
- **参数说明**: 
  - request: 分类创建请求DTO
- **返回值说明**: 分类响应DTO
- **业务逻辑要点**: 
  - 验证用户登录状态
  - 验证父分类存在性和权限
  - 检查同级分类名称唯一性
  - 设置默认排序值
  - 创建分类记录

#### updateCategory
- **方法签名**: `CategoryResponse updateCategory(String categoryId, CategoryUpdateRequest request)`
- **功能描述**: 更新分类信息
- **参数说明**: 
  - categoryId: 分类ID
  - request: 分类更新请求DTO
- **返回值说明**: 更新后的分类响应DTO
- **业务逻辑要点**: 
  - 验证分类存在性和权限
  - 检查名称唯一性（排除当前分类）
  - 更新分类信息
  - 返回更新后的分类

#### deleteCategory
- **方法签名**: `void deleteCategory(String categoryId)`
- **功能描述**: 删除分类
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 无
- **业务逻辑要点**: 
  - 验证分类存在性和权限
  - 检查是否有子分类
  - 检查是否有关联文档
  - 执行删除操作

#### getCategoryTree
- **方法签名**: `List<CategoryTreeResponse> getCategoryTree()`
- **功能描述**: 获取用户的分类树
- **参数说明**: 无
- **返回值说明**: 分类树响应DTO列表
- **业务逻辑要点**: 
  - 验证用户登录状态
  - 获取用户所有分类
  - 构建递归树形结构

### 新增API方法 (2024年更新)

#### CategoryMapper 新增方法

##### selectAll
- **方法签名**: `List<Category> selectAll()`
- **功能描述**: 查询所有分类信息
- **参数说明**: 无
- **返回值说明**: 所有分类列表，按sort_order升序排列
- **业务逻辑要点**: 用于管理员查看全局分类或API接口

##### countByParentIdAndName
- **方法签名**: `int countByParentIdAndName(@Param("parentId") String parentId, @Param("name") String name)`
- **功能描述**: 统计指定父分类下同名分类的数量
- **参数说明**: 
  - parentId: 父分类ID，null表示根分类
  - name: 分类名称
- **返回值说明**: 分类数量（0或1）
- **业务逻辑要点**: 用于API接口的分类名称唯一性校验

##### countByParentIdAndNameExcludeId
- **方法签名**: `int countByParentIdAndNameExcludeId(@Param("parentId") String parentId, @Param("name") String name, @Param("excludeId") String excludeId)`
- **功能描述**: 统计指定父分类下同名分类的数量（排除指定ID）
- **参数说明**: 
  - parentId: 父分类ID，null表示根分类
  - name: 分类名称
  - excludeId: 要排除的分类ID
- **返回值说明**: 分类数量（0或1）
- **业务逻辑要点**: 用于API接口更新时的分类名称唯一性校验

##### countByParentId
- **方法签名**: `int countByParentId(@Param("parentId") String parentId)`
- **功能描述**: 统计指定父分类下的子分类数量
- **参数说明**: 
  - parentId: 父分类ID
- **返回值说明**: 子分类数量
- **业务逻辑要点**: 用于检查分类是否有子分类，删除前校验

#### CategoryManager 新增方法

##### getAllCategories
- **方法签名**: `List<Category> getAllCategories()`
- **功能描述**: 获取所有分类信息
- **参数说明**: 无
- **返回值说明**: 所有分类列表
- **业务逻辑要点**: 用于API接口获取全局分类列表

##### existsByParentIdAndName
- **方法签名**: `boolean existsByParentIdAndName(String parentId, String name)`
- **功能描述**: 检查指定父分类下分类名称是否已存在
- **参数说明**: 
  - parentId: 父分类ID
  - name: 分类名称
- **返回值说明**: 是否存在
- **业务逻辑要点**: 用于API接口的唯一性校验

##### existsByParentIdAndNameExcludeId
- **方法签名**: `boolean existsByParentIdAndNameExcludeId(String parentId, String name, String excludeId)`
- **功能描述**: 检查指定父分类下分类名称是否已存在（排除指定ID）
- **参数说明**: 
  - parentId: 父分类ID
  - name: 分类名称
  - excludeId: 要排除的分类ID
- **返回值说明**: 是否存在
- **业务逻辑要点**: 用于API接口更新时的唯一性校验

##### update
- **方法签名**: `boolean update(Category category)`
- **功能描述**: 更新分类信息（支持父分类变更）
- **参数说明**: 
  - category: 包含更新信息的分类对象
- **返回值说明**: 是否更新成功
- **业务逻辑要点**: 支持更新包括父分类在内的所有字段

##### delete
- **方法签名**: `boolean delete(String categoryId)`
- **功能描述**: 删除分类
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 是否删除成功
- **业务逻辑要点**: 物理删除分类记录

##### hasChildren
- **方法签名**: `boolean hasChildren(String categoryId)`
- **功能描述**: 检查分类是否有子分类
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 是否有子分类
- **业务逻辑要点**: 用于删除前的校验

##### hasDocuments
- **方法签名**: `boolean hasDocuments(String categoryId)`
- **功能描述**: 检查分类是否有关联文档
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 是否有关联文档
- **业务逻辑要点**: 用于删除前的校验

#### CategoryService 新增API方法

##### getCategoriesApi
- **方法签名**: `List<CategoryTreeApiResponse> getCategoriesApi()`
- **功能描述**: 获取分类树（API接口）
- **参数说明**: 无
- **返回值说明**: API格式的分类树响应DTO列表
- **业务逻辑要点**: 
  - 获取所有分类信息
  - 构建递归树形结构
  - 返回符合API文档格式的响应

##### createCategoryApi
- **方法签名**: `CategoryApiResponse createCategoryApi(CategoryCreateRequest request)`
- **功能描述**: 创建分类（API接口）
- **参数说明**: 
  - request: 分类创建请求DTO
- **返回值说明**: API格式的分类响应DTO
- **业务逻辑要点**: 
  - 验证父分类存在性（如果指定）
  - 检查同级分类名称唯一性
  - 设置默认排序值和文档数量
  - 创建分类记录
  - 返回符合API文档格式的响应

##### updateCategoryApi
- **方法签名**: `CategoryApiResponse updateCategoryApi(String categoryId, CategoryUpdateRequest request)`
- **功能描述**: 更新分类信息（API接口）
- **参数说明**: 
  - categoryId: 分类ID
  - request: 分类更新请求DTO
- **返回值说明**: API格式的更新后分类响应DTO
- **业务逻辑要点**: 
  - 验证分类存在性
  - 验证父分类存在性（如果变更）
  - 检查名称唯一性（排除当前分类）
  - 防止循环引用（不能将分类移动到自己的子分类下）
  - 更新分类信息
  - 返回符合API文档格式的响应

##### deleteCategoryApi
- **方法签名**: `void deleteCategoryApi(String categoryId)`
- **功能描述**: 删除分类（API接口）
- **参数说明**: 
  - categoryId: 分类ID
- **返回值说明**: 无
- **业务逻辑要点**: 
  - 验证分类存在性
  - 检查是否有子分类
  - 检查是否有关联文档
  - 执行删除操作
  - 按排序值排序

## 笔记服务模块 (sc-note-service)

### NoteMapper 数据访问层

#### batchInsert
- **方法签名**: `int batchInsert(@Param("notes") List<Note> notes)`
- **功能描述**: 批量插入笔记
- **参数说明**: 
  - notes: 笔记列表
- **返回值说明**: 插入的记录数
- **业务逻辑要点**: 使用批量插入提高性能，支持事务回滚

#### selectByNoteId
- **方法签名**: `Note selectByNoteId(@Param("noteId") String noteId)`
- **功能描述**: 根据笔记ID查询笔记
- **参数说明**: 
  - noteId: 笔记唯一标识
- **返回值说明**: Note实体对象，如果不存在返回null
- **业务逻辑要点**: 主键查询，性能最优

#### selectByUserId
- **方法签名**: `List<Note> selectByUserId(@Param("userId") String userId)`
- **功能描述**: 根据用户ID查询所有笔记
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 笔记列表
- **业务逻辑要点**: 按创建时间倒序排列

#### selectByUserIdWithPage
- **方法签名**: `Page<Note> selectByUserIdWithPage(@Param("userId") String userId, Page<Note> page)`
- **功能描述**: 分页查询用户笔记
- **参数说明**: 
  - userId: 用户ID
  - page: 分页参数
- **返回值说明**: 分页笔记列表
- **业务逻辑要点**: 支持分页查询，按创建时间倒序

#### selectByDocumentId
- **方法签名**: `List<Note> selectByDocumentId(@Param("documentId") String documentId)`
- **功能描述**: 根据文档ID查询笔记
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 笔记列表
- **业务逻辑要点**: 按页码和创建时间排序

#### selectByUserIdAndDocumentId
- **方法签名**: `List<Note> selectByUserIdAndDocumentId(@Param("userId") String userId, @Param("documentId") String documentId)`
- **功能描述**: 根据用户ID和文档ID查询笔记
- **参数说明**: 
  - userId: 用户ID
  - documentId: 文档ID
- **返回值说明**: 笔记列表
- **业务逻辑要点**: 组合查询，支持权限控制

#### selectByUserIdAndDocumentIdWithPage
- **方法签名**: `Page<Note> selectByUserIdAndDocumentIdWithPage(@Param("userId") String userId, @Param("documentId") String documentId, Page<Note> page)`
- **功能描述**: 分页查询用户在指定文档下的笔记
- **参数说明**: 
  - userId: 用户ID
  - documentId: 文档ID
  - page: 分页参数
- **返回值说明**: 分页笔记列表
- **业务逻辑要点**: 支持文档内笔记的分页浏览

#### selectByPageNumber
- **方法签名**: `List<Note> selectByPageNumber(@Param("userId") String userId, @Param("documentId") String documentId, @Param("pageNumber") Integer pageNumber)`
- **功能描述**: 根据页码查询笔记
- **参数说明**: 
  - userId: 用户ID
  - documentId: 文档ID
  - pageNumber: 页码
- **返回值说明**: 笔记列表
- **业务逻辑要点**: 查询特定页面的所有笔记

#### countByUserId
- **方法签名**: `int countByUserId(@Param("userId") String userId)`
- **功能描述**: 统计用户笔记总数
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 笔记数量
- **业务逻辑要点**: 用于统计和分页计算

#### countByUserIdAndDocumentId
- **方法签名**: `int countByUserIdAndDocumentId(@Param("userId") String userId, @Param("documentId") String documentId)`
- **功能描述**: 统计用户在指定文档下的笔记数量
- **参数说明**: 
  - userId: 用户ID
  - documentId: 文档ID
- **返回值说明**: 笔记数量
- **业务逻辑要点**: 用于文档内笔记统计

#### selectFavoritesByUserId
- **方法签名**: `List<Note> selectFavoritesByUserId(@Param("userId") String userId)`
- **功能描述**: 查询用户收藏的笔记
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 收藏笔记列表
- **业务逻辑要点**: 只返回is_favorite为true的笔记

#### selectByUserIdAndTag
- **方法签名**: `List<Note> selectByUserIdAndTag(@Param("userId") String userId, @Param("tag") String tag)`
- **功能描述**: 根据标签查询用户笔记
- **参数说明**: 
  - userId: 用户ID
  - tag: 标签名称
- **返回值说明**: 笔记列表
- **业务逻辑要点**: 使用JSON数组查询，支持标签筛选

#### updateByNoteId
- **方法签名**: `int updateByNoteId(@Param("noteId") String noteId, @Param("title") String title, @Param("content") String content, @Param("pageNumber") Integer pageNumber, @Param("selectedText") String selectedText, @Param("positionInfo") String positionInfo, @Param("tags") String[] tags, @Param("isFavorite") Boolean isFavorite)`
- **功能描述**: 根据笔记ID更新笔记信息
- **参数说明**: 
  - noteId: 笔记ID
  - title: 标题
  - content: 内容
  - pageNumber: 页码
  - selectedText: 选中文本
  - positionInfo: 位置信息
  - tags: 标签数组
  - isFavorite: 是否收藏
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 动态更新，只更新非空字段，自动更新updated_at

#### deleteByNoteId
- **方法签名**: `int deleteByNoteId(@Param("noteId") String noteId)`
- **功能描述**: 根据笔记ID删除笔记
- **参数说明**: 
  - noteId: 笔记ID
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 物理删除，不可恢复

#### deleteByDocumentId
- **方法签名**: `int deleteByDocumentId(@Param("documentId") String documentId)`
- **功能描述**: 根据文档ID删除所有相关笔记
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 批量删除，用于文档删除时的级联操作

#### deleteByUserId
- **方法签名**: `int deleteByUserId(@Param("userId") String userId)`
- **功能描述**: 根据用户ID删除所有笔记
- **参数说明**: 
  - userId: 用户ID
- **返回值说明**: 影响的行数
- **业务逻辑要点**: 批量删除，用于用户注销时的数据清理

### NoteManager 数据管理层

#### createNote
- **方法签名**: `Note createNote(Note note)`
- **功能描述**: 创建笔记
- **参数说明**: 
  - note: 笔记实体对象
- **返回值说明**: 创建成功的笔记对象
- **业务逻辑要点**: 
  - 自动生成noteId
  - 设置创建和更新时间
  - 支持事务回滚

#### getNoteByNoteId
- **方法签名**: `Note getNoteByNoteId(String noteId)`
- **功能描述**: 根据笔记ID获取笔记
- **参数说明**: 
  - noteId: 笔记ID
- **返回值说明**: Note实体对象
- **业务逻辑要点**: 直接数据库查询，不使用缓存

#### getNotesByUserId
- **方法签名**: `Page<Note> getNotesByUserId(String userId, Page<Note> page, String documentId, Boolean onlyFavorites, String tag)`
- **功能描述**: 分页查询用户笔记（支持多种筛选条件）
- **参数说明**: 
  - userId: 用户ID
  - page: 分页参数
  - documentId: 文档ID（可选）
  - onlyFavorites: 是否只查询收藏（可选）
  - tag: 标签筛选（可选）
- **返回值说明**: 分页笔记列表
- **业务逻辑要点**: 
  - 支持多条件组合查询
  - 动态构建查询条件
  - 按创建时间倒序排列

#### updateNote
- **方法签名**: `boolean updateNote(Note note)`
- **功能描述**: 更新笔记信息
- **参数说明**: 
  - note: 笔记实体对象
- **返回值说明**: 是否更新成功
- **业务逻辑要点**: 
  - 只更新非空字段
  - 自动更新updated_at时间
  - 支持部分字段更新

#### deleteByNoteId
- **方法签名**: `boolean deleteByNoteId(String noteId)`
- **功能描述**: 根据笔记ID删除笔记
- **参数说明**: 
  - noteId: 笔记ID
- **返回值说明**: 是否删除成功
- **业务逻辑要点**: 物理删除，操作不可逆

#### deleteByDocumentId
- **方法签名**: `boolean deleteByDocumentId(String documentId)`
- **功能描述**: 根据文档ID删除所有相关笔记
- **参数说明**: 
  - documentId: 文档ID
- **返回值说明**: 是否删除成功
- **业务逻辑要点**: 批量删除，用于文档删除的级联操作

#### isNoteOwnedByUser
- **方法签名**: `boolean isNoteOwnedByUser(String noteId, String userId)`
- **功能描述**: 检查笔记是否属于指定用户
- **参数说明**: 
  - noteId: 笔记ID
  - userId: 用户ID
- **返回值说明**: 是否拥有权限
- **业务逻辑要点**: 用于权限验证，确保用户只能操作自己的笔记

#### generateNoteId
- **方法签名**: `String generateNoteId()`
- **功能描述**: 生成唯一的笔记ID
- **参数说明**: 无
- **返回值说明**: 笔记ID字符串
- **业务逻辑要点**: 使用UUID生成，确保全局唯一性

### NoteService 业务逻辑层

#### createNote
- **方法签名**: `NoteResponse createNote(NoteCreateRequest request, String userId)`
- **功能描述**: 创建笔记
- **参数说明**: 
  - request: 笔记创建请求DTO
  - userId: 用户ID
- **返回值说明**: 笔记响应DTO
- **业务逻辑要点**: 
  - 验证请求参数完整性
  - 检查文档访问权限
  - 自动生成笔记ID
  - 设置默认值和时间戳

#### getNotes
- **方法签名**: `Page<NoteResponse> getNotes(NoteQueryRequest request, String userId)`
- **功能描述**: 分页查询用户笔记
- **参数说明**: 
  - request: 查询请求DTO
  - userId: 用户ID
- **返回值说明**: 分页笔记响应DTO
- **业务逻辑要点**: 
  - 支持多种筛选条件
  - 权限控制，只能查询自己的笔记
  - 分页参数验证
  - DTO转换

#### getNoteById
- **方法签名**: `NoteResponse getNoteById(String noteId, String userId)`
- **功能描述**: 根据ID获取笔记详情
- **参数说明**: 
  - noteId: 笔记ID
  - userId: 用户ID
- **返回值说明**: 笔记响应DTO
- **业务逻辑要点**: 
  - 验证笔记存在性
  - 权限验证，确保用户只能查看自己的笔记
  - 异常处理

#### updateNote
- **方法签名**: `NoteResponse updateNote(String noteId, NoteUpdateRequest request, String userId)`
- **功能描述**: 更新笔记信息
- **参数说明**: 
  - noteId: 笔记ID
  - request: 更新请求DTO
  - userId: 用户ID
- **返回值说明**: 更新后的笔记响应DTO
- **业务逻辑要点**: 
  - 验证笔记存在性和权限
  - 参数校验
  - 部分字段更新
  - 返回更新后的完整信息

#### deleteNote
- **方法签名**: `void deleteNote(String noteId, String userId)`
- **功能描述**: 删除笔记
- **参数说明**: 
  - noteId: 笔记ID
  - userId: 用户ID
- **返回值说明**: 无
- **业务逻辑要点**: 
  - 验证笔记存在性和权限
  - 物理删除操作
  - 异常处理
