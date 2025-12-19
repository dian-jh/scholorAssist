---
description: 代码模板和最佳实践
globs: 
alwaysApply: true
---

# 代码模板和最佳实践

## 概述

本文档提供文献辅助阅读系统的标准代码模板，确保代码质量和一致性。

## 全局响应处理规范

### 强制要求
- **严格使用** sc-common 中的全局响应处理器
- **禁止** Controller 层手动封装 Result 返回
- **确保** 所有异常被全局处理器捕获

### 全局处理器使用示例
```java
// ✅ 正确：直接返回业务数据，由全局处理器自动封装
@PostMapping("/register")
public UserInfoResponse register(@Valid @RequestBody UserRegisterRequest request) {
    return userService.register(request);
}

// ❌ 错误：手动封装Result
@PostMapping("/register") 
public Result<UserInfoResponse> register(@Valid @RequestBody UserRegisterRequest request) {
    return Result.ok(userService.register(request));
}
```

## Controller层模板

### 标准Controller结构
```java
package com.zd.user.controller;

import com.zd.sccommon.common.BusinessException;
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
@Tag(name = "用户管理", description = "用户注册、登录、信息管理")
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

### 异常处理规范
```java
// ✅ 正确：抛出BusinessException，由全局处理器处理
public UserInfoResponse register(UserRegisterRequest request) {
    if (userManager.existsByUsername(request.getUsername())) {
        throw new BusinessException(400, "用户名已存在");
    }
    return createUser(request);
}

// ❌ 错误：手动处理异常并返回Result
public Result<UserInfoResponse> register(UserRegisterRequest request) {
    try {
        return Result.ok(createUser(request));
    } catch (Exception e) {
        return Result.error(500, "注册失败");
    }
}
```

## Service层模板

### 接口定义
```java
package com.zd.user.service;

import com.zd.user.model.dto.request.UserRegisterRequest;
import com.zd.user.model.dto.response.UserInfoResponse;

public interface UserService {
    UserInfoResponse register(UserRegisterRequest request);
    UserInfoResponse getUserInfo(String userId);
}
```

### 实现规范
```java
package com.zd.user.service.impl;

import com.zd.sccommon.common.BusinessException;
import com.zd.user.manager.UserManager;
import com.zd.user.model.domain.User;
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

## Manager层模板

### 接口定义
```java
package com.zd.user.manager;

import com.zd.user.model.domain.User;

public interface UserManager {
    User create(User user);
    User getByUserId(String userId);
    boolean existsByUsername(String username);
}
```

### 实现规范
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

## 实体类模板

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

### DTO模板
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

## Mapper层模板

### 接口定义
```java
package com.zd.user.mapper;

import com.zd.user.model.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    int insert(@Param("user") User user);
    User selectByUserId(@Param("userId") String userId);
    long countByUsername(@Param("username") String username);
}
```

## 核心开发规范

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

### 4. 日志规范
- 关键业务操作必须记录日志
- 使用结构化日志格式
- 敏感信息不得记录

### 5. 代码质量
- 必须通过 SonarQube 检测
- 单元测试覆盖率 > 80%
- 接口响应时间 < 500ms

## 注意事项

1. **严格遵循** 全局响应处理器使用规范
2. **每个文档** 只包含一个完整接口示例
3. **核心逻辑** 不超过50行实现
4. **优先级**: 规范说明 > 代码示例
5. **兼容性**: 确保与现有架构100%兼容