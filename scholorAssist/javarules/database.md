---
description: 数据库操作规范 - PostgreSQL + MyBatis
globs: 
alwaysApply: true
---

# 数据库操作规范

## 概述

本文档定义了文献辅助阅读系统的数据库操作规范，基于PostgreSQL数据库和MyBatis框架。所有数据库操作必须严格遵循本规范。

## 强制规则 🔴

### 1. 数据库表结构规范
**必须严格参考以下文档进行建表操作：**
- 建表语句参考：`docs/tables.sql`
- 表结构文档：`docs/tables.md`
- **禁止随意修改表结构**，如需修改必须先更新文档

### 2. 全局响应处理规范
- **严格使用** sc-common 中的全局响应处理器
- **禁止** 在数据访问层手动封装 Result 返回
- **确保** 所有数据库异常被全局处理器捕获

### 3. 实体类使用规范
**必须先扫描现有实体类，优先使用已存在的实体：**

#### 核心实体类
- **User** - 用户表（用户基本信息、认证数据、状态信息）
- **UserSession** - 用户会话表（登录会话信息、token管理）
- **Category** - 分类表（文档分类信息，支持层级结构）
- **Document** - 文档表（PDF文档基本信息和元数据）
- **DocumentChunk** - 文档分片表（文档文本片段，用于AI向量检索）
- **Note** - 笔记表（用户阅读文档时创建的笔记和标注）
- **AiConversation** - AI对话表（AI对话会话信息）
- **AiMessage** - AI消息表（AI对话中的具体消息内容）

## MyBatis配置规范

### Mapper接口规范
```java
@Mapper
public interface UserMapper {
    // 查询方法
    User selectById(@Param("id") Long id);
    User selectByUserId(@Param("userId") String userId);
    List<User> selectByCondition(@Param("condition") UserQueryCondition condition);
    
    // 插入方法
    int insert(@Param("user") User user);
    
    // 更新方法
    int updateById(@Param("user") User user);
    
    // 删除方法
    int deleteById(@Param("id") Long id);
    
    // 统计方法
    long countByCondition(@Param("condition") UserQueryCondition condition);
}
```

### XML映射文件规范
**文件位置**: `src/main/resources/mapper/{Entity}Mapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zd.user.mapper.UserMapper">

    <!-- 结果映射 -->
    <resultMap id="BaseResultMap" type="com.zd.user.model.domain.User">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="user_id" property="userId" jdbcType="VARCHAR"/>
        <result column="username" property="username" jdbcType="VARCHAR"/>
        <result column="email" property="email" jdbcType="VARCHAR"/>
        <result column="password_hash" property="passwordHash" jdbcType="VARCHAR"/>
        <result column="status" property="status" jdbcType="VARCHAR"/>
        <result column="created_at" property="createdAt" jdbcType="TIMESTAMP"/>
        <result column="updated_at" property="updatedAt" jdbcType="TIMESTAMP"/>
    </resultMap>

    <!-- 基础字段 -->
    <sql id="Base_Column_List">
        id, user_id, username, email, password_hash, status, created_at, updated_at
    </sql>

    <!-- 查询方法 -->
    <select id="selectById" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM users
        WHERE id = #{id}
    </select>

    <select id="selectByUserId" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM users
        WHERE user_id = #{userId}
    </select>

    <!-- 插入方法 -->
    <insert id="insert" parameterType="com.zd.user.model.domain.User">
        INSERT INTO users (user_id, username, email, password_hash, status, created_at, updated_at)
        VALUES (#{user.userId}, #{user.username}, #{user.email}, #{user.passwordHash}, 
                #{user.status}, #{user.createdAt}, #{user.updatedAt})
    </insert>

    <!-- 更新方法 -->
    <update id="updateById" parameterType="com.zd.user.model.domain.User">
        UPDATE users
        SET username = #{user.username},
            email = #{user.email},
            status = #{user.status},
            updated_at = #{user.updatedAt}
        WHERE id = #{user.id}
    </update>

    <!-- 删除方法 -->
    <delete id="deleteById">
        DELETE FROM users WHERE id = #{id}
    </delete>

    <!-- 统计方法 -->
    <select id="countByCondition" resultType="long">
        SELECT COUNT(1) FROM users
        <where>
            <if test="condition.username != null and condition.username != ''">
                AND username LIKE CONCAT('%', #{condition.username}, '%')
            </if>
            <if test="condition.status != null and condition.status != ''">
                AND status = #{condition.status}
            </if>
        </where>
    </select>
</mapper>
```

## 数据库连接配置

### 连接池配置
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scholar_assist
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    
    # HikariCP连接池配置
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

# MyBatis配置
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.zd.*.model.domain
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
    lazy-loading-enabled: true
    multiple-result-sets-enabled: true
```

## 数据库操作最佳实践

### 1. 查询优化
```java
// ✅ 正确：使用索引字段查询
User user = userMapper.selectByUserId(userId);

// ❌ 错误：全表扫描
List<User> users = userMapper.selectAll();
```

### 2. 批量操作
```java
// ✅ 正确：批量插入
userMapper.insertBatch(userList);

// ❌ 错误：循环单条插入
for (User user : userList) {
    userMapper.insert(user);
}
```

### 3. 分页查询
```java
// ✅ 正确：使用LIMIT和OFFSET
<select id="selectByPage" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM users
    ORDER BY created_at DESC
    LIMIT #{pageSize} OFFSET #{offset}
</select>
```

### 4. 条件查询
```java
// ✅ 正确：使用动态SQL
<select id="selectByCondition" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM users
    <where>
        <if test="condition.username != null">
            AND username = #{condition.username}
        </if>
        <if test="condition.status != null">
            AND status = #{condition.status}
        </if>
    </where>
</select>
```

## 事务管理规范

### 1. 事务注解使用
```java
@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {
    
    @Override
    public void createUser(User user) {
        // 事务方法实现
        userMapper.insert(user);
        userSessionMapper.insert(createSession(user));
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        // 只读事务
        return userMapper.selectById(id);
    }
}
```

### 2. 事务传播行为
- **REQUIRED**: 默认传播行为，支持当前事务
- **REQUIRES_NEW**: 创建新事务，挂起当前事务
- **SUPPORTS**: 支持当前事务，如果没有事务则以非事务方式执行

## 数据库安全规范

### 1. SQL注入防护
```java
// ✅ 正确：使用参数化查询
<select id="selectByUsername" resultMap="BaseResultMap">
    SELECT * FROM users WHERE username = #{username}
</select>

// ❌ 错误：字符串拼接
<select id="selectByUsername" resultMap="BaseResultMap">
    SELECT * FROM users WHERE username = '${username}'
</select>
```

### 2. 敏感数据处理
```java
// ✅ 正确：密码加密存储
user.setPasswordHash(passwordEncoder.encode(password));

// ❌ 错误：明文存储密码
user.setPassword(password);
```

### 3. 数据权限控制
```java
// ✅ 正确：添加用户权限检查
<select id="selectUserDocuments" resultMap="DocumentResultMap">
    SELECT * FROM documents 
    WHERE user_id = #{userId}
</select>
```

## 性能优化建议

### 1. 索引使用
- 在经常查询的字段上建立索引
- 复合索引的字段顺序要合理
- 避免在小表上建立过多索引

### 2. 查询优化
- 避免SELECT *，只查询需要的字段
- 使用LIMIT限制返回结果数量
- 合理使用JOIN，避免N+1查询问题

### 3. 缓存策略
```java
// 使用MyBatis二级缓存
<cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>

// 或使用Redis缓存
@Cacheable(value = "users", key = "#userId")
public User getUserById(String userId) {
    return userMapper.selectByUserId(userId);
}
```

## 数据库监控

### 1. 连接池监控
```java
// 监控连接池状态
@Component
public class DataSourceHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try {
            Connection connection = dataSource.getConnection();
            connection.close();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

### 2. 慢查询监控
```yaml
# 开启慢查询日志
logging:
  level:
    com.zd.*.mapper: DEBUG
```

## 注意事项

### 1. 开发规范
- **严格遵循** 全局响应处理器使用规范
- **每个Mapper** 只包含一个完整功能示例
- **SQL语句** 不超过50行实现
- **优先级**: 规范说明 > 代码示例

### 2. 数据库规范
- 表名使用下划线命名法
- 字段名使用下划线命名法
- 主键统一使用id字段
- 创建时间和更新时间字段必须包含

### 3. 兼容性要求
- 确保与现有架构100%兼容
- 新增表结构必须向后兼容
- 数据迁移脚本必须可重复执行

---

**重要提醒**：
1. 所有数据库操作必须通过MyBatis进行
2. 禁止直接使用JDBC或其他ORM框架
3. 数据库连接信息通过配置文件管理
4. 生产环境数据库操作需要审核