---
description: 微服务架构项目结构规范
globs: 
alwaysApply: true
---

# 微服务架构项目结构规范

## 概述

本文档定义了文献辅助阅读系统微服务架构的标准项目结构，确保所有微服务模块遵循统一的组织方式和开发规范。

## 强制规则 🔴

### 1. 严格遵循微服务架构模式
- 每个微服务必须独立部署和运行
- 禁止跨服务直接访问数据库
- 服务间通信必须通过API接口
- **严格使用** sc-common 中的全局响应处理器

### 2. 标准目录结构
每个微服务模块必须包含以下标准目录结构：

```
sc-{service-name}/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/zd/{service}/
│   │   │       ├── controller/          # 接口定义层
│   │   │       ├── service/             # 业务逻辑层
│   │   │       │   └── impl/            # Service实现类
│   │   │       ├── manager/             # 数据管理层
│   │   │       │   └── impl/            # Manager实现类
│   │   │       ├── mapper/              # 数据库操作接口
│   │   │       ├── model/               # 数据模型
│   │   │       │   ├── domain/          # 数据库实体
│   │   │       │   └── dto/             # 数据传输对象
│   │   │       │       ├── request/     # 请求体
│   │   │       │       └── response/    # 响应实体
│   │   │       ├── config/              # 配置类
│   │   │       └── {ServiceName}Application.java
│   │   └── resources/
│   │       ├── mapper/                  # MyBatis XML映射文件
│   │       └── application.yml          # 应用配置
│   └── test/
│       └── java/
│           └── com/zd/{service}/
│               ├── controller/          # Controller测试
│               ├── service/             # Service测试
│               └── manager/             # Manager测试
└── pom.xml                              # Maven配置
```

## 各层职责定义

### Controller层
- **职责**: HTTP请求处理、参数校验、接口文档
- **命名规范**: `{Entity}Controller`
- **注解要求**: `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`
- **返回类型**: 直接返回业务对象，由全局处理器自动封装

### Service层
- **职责**: 业务逻辑实现、事务管理、异常处理
- **命名规范**: 接口 `{Entity}Service`，实现类 `{Entity}ServiceImpl`
- **注解要求**: `@Service`, `@Transactional`
- **调用规范**: 只能调用Manager层和其他Service

### Manager层
- **职责**: 数据访问封装、缓存管理、复杂查询聚合
- **命名规范**: 接口 `{Entity}Manager`，实现类 `{Entity}ManagerImpl`
- **注解要求**: `@Component`
- **调用规范**: 只能调用Mapper层

### Mapper层
- **职责**: 数据库操作接口定义
- **命名规范**: `{Entity}Mapper`
- **注解要求**: `@Mapper`
- **实现方式**: XML映射文件

## 标准代码模板

### Controller层模板
```java
package com.zd.user.controller;

import com.zd.user.model.dto.request.UserRegisterRequest;
import com.zd.user.model.dto.response.UserInfoResponse;
import com.zd.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public UserInfoResponse register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("用户注册请求，用户名：{}", request.getUsername());
        return userService.register(request);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取用户信息")
    public UserInfoResponse getUserInfo(@PathVariable String userId) {
        return userService.getUserInfo(userId);
    }
}
```

### Service层模板
```java
package com.zd.user.service.impl;

import com.zd.sccommon.common.BusinessException;
import com.zd.user.manager.UserManager;
import com.zd.user.model.domain.User;
import com.zd.user.model.dto.request.UserRegisterRequest;
import com.zd.user.model.dto.response.UserInfoResponse;
import com.zd.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    private final UserManager userManager;

    @Override
    public UserInfoResponse register(UserRegisterRequest request) {
        // 业务校验
        if (userManager.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }
        
        // 创建用户
        User user = buildUser(request);
        userManager.create(user);
        
        return convertToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(String userId) {
        User user = userManager.getByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return convertToResponse(user);
    }

    private User buildUser(UserRegisterRequest request) {
        return User.builder()
                .userId(generateUserId())
                .username(request.getUsername())
                .email(request.getEmail())
                .build();
    }

    private UserInfoResponse convertToResponse(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
```

### Manager层模板
```java
package com.zd.user.manager.impl;

import com.zd.user.manager.UserManager;
import com.zd.user.mapper.UserMapper;
import com.zd.user.model.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserManagerImpl implements UserManager {

    private final UserMapper userMapper;

    @Override
    public User create(User user) {
        userMapper.insert(user);
        return user;
    }

    @Override
    public User getByUserId(String userId) {
        return userMapper.selectByUserId(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.countByUsername(username) > 0;
    }
}
```

## 数据模型规范

### Domain实体
```java
package com.zd.user.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String userId;
    private String username;
    private String email;
    private String passwordHash;
    private String status;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

### DTO规范
```java
// 请求DTO
@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码不能少于8位")
    private String password;
}

// 响应DTO
@Data
@Builder
@Schema(description = "用户信息响应")
public class UserInfoResponse {
    private String userId;
    private String username;
    private String email;
    private String status;
    private LocalDateTime createdAt;
}
```

## 配置文件规范

### application.yml
```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: ${SERVICE_NAME}
  profiles:
    active: ${ACTIVE_PROFILE:dev}
  
  # 数据库配置
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

# MyBatis配置
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.zd.${SERVICE_NAME}.model.domain
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true

# 日志配置
logging:
  level:
    com.zd.${SERVICE_NAME}: INFO
    com.zd.${SERVICE_NAME}.mapper: DEBUG
```

## 依赖管理规范

### pom.xml模板
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.zd</groupId>
        <artifactId>scholorAssist</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    
    <artifactId>sc-user-service</artifactId>
    <name>sc-user-service</name>
    <description>用户管理服务</description>

    <dependencies>
        <!-- 通用组件 -->
        <dependency>
            <groupId>com.zd</groupId>
            <artifactId>sc-common</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
        
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- 数据库相关 -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
    </dependencies>
</project>
```

## 测试规范

### 单元测试模板
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

## 部署配置

### Dockerfile
```dockerfile
FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 注意事项

### 1. 开发规范
- **严格遵循** 全局响应处理器使用规范
- **每个服务** 只包含一个完整功能示例
- **核心逻辑** 不超过50行实现
- **优先级**: 规范说明 > 代码示例

### 2. 架构规范
- 服务间通信使用HTTP REST API
- 数据库表按服务划分，禁止跨服务访问
- 配置信息通过环境变量管理
- 日志格式统一，便于集中收集

### 3. 兼容性要求
- 确保与现有架构100%兼容
- 新增服务必须遵循统一规范
- API接口必须向后兼容

---

**重要提醒**：
1. 严格按照目录结构组织代码
2. 每层职责清晰，禁止跨层调用
3. 统一使用全局响应处理器
4. 保持代码风格和命名规范一致