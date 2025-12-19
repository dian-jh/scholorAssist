package com.zd.scuserservice.test;

import com.zd.scuserservice.model.domain.User;
import com.zd.scuserservice.model.dto.request.*;
import com.zd.scuserservice.model.dto.response.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 测试数据工厂类
 * 提供标准化的测试数据生成
 * 
 * @author system
 * @since 2024-01-21
 */
public class TestDataFactory {

    /**
     * 创建有效的用户注册请求
     */
    public static UserRegisterRequest createValidRegisterRequest() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("TestPass123!");
        request.setConfirmPassword("TestPass123!");
        request.setRealName("测试用户");
        return request;
    }

    /**
     * 创建有效的用户登录请求
     */
    public static UserLoginRequest createValidLoginRequest() {
        UserLoginRequest request = new UserLoginRequest();
        request.setLogin("testuser");
        request.setPassword("TestPass123!");
        request.setRememberMe(false);
        return request;
    }

    /**
     * 创建有效的密码修改请求
     */
    public static ChangePasswordRequest createValidChangePasswordRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("TestPass123!");
        request.setNewPassword("NewPass456!");
        request.setConfirmPassword("NewPass456!");
        return request;
    }

    /**
     * 创建有效的用户更新请求
     */
    public static UserUpdateRequest createValidUpdateRequest() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRealName("更新后的姓名");
        request.setAvatarUrl("https://example.com/avatar.jpg");
        return request;
    }

    /**
     * 创建有效的权限更新请求
     */
    public static UserPermissionUpdateRequest createValidPermissionUpdateRequest() {
        UserPermissionUpdateRequest request = new UserPermissionUpdateRequest();
        request.setRole("admin");
        request.setStatus("active");
        return request;
    }

    /**
     * 创建测试用户实体
     */
    public static User createTestUser() {
        User user = new User();
        user.setUserId("user_" + UUID.randomUUID().toString().replace("-", ""));
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$10$encrypted_password_hash");
        user.setRealName("测试用户");
        user.setRole("user");
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        return user;
    }

    /**
     * 创建管理员用户实体
     */
    public static User createAdminUser() {
        User user = createTestUser();
        user.setUserId("admin_" + UUID.randomUUID().toString().replace("-", ""));
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setRole("admin");
        return user;
    }

    /**
     * 创建超级管理员用户实体
     */
    public static User createSuperAdminUser() {
        User user = createTestUser();
        user.setUserId("super_admin_" + UUID.randomUUID().toString().replace("-", ""));
        user.setUsername("superadmin");
        user.setEmail("superadmin@example.com");
        user.setRole("super_admin");
        return user;
    }

    /**
     * 创建无效的注册请求 - 用户名为空
     */
    public static UserRegisterRequest createInvalidRegisterRequest_EmptyUsername() {
        UserRegisterRequest request = createValidRegisterRequest();
        request.setUsername("");
        return request;
    }

    /**
     * 创建无效的注册请求 - 邮箱格式错误
     */
    public static UserRegisterRequest createInvalidRegisterRequest_InvalidEmail() {
        UserRegisterRequest request = createValidRegisterRequest();
        request.setEmail("invalid-email");
        return request;
    }

    /**
     * 创建无效的注册请求 - 密码不符合要求
     */
    public static UserRegisterRequest createInvalidRegisterRequest_WeakPassword() {
        UserRegisterRequest request = createValidRegisterRequest();
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        return request;
    }

    /**
     * 创建无效的注册请求 - 密码确认不匹配
     */
    public static UserRegisterRequest createInvalidRegisterRequest_PasswordMismatch() {
        UserRegisterRequest request = createValidRegisterRequest();
        request.setConfirmPassword("DifferentPass123!");
        return request;
    }

    /**
     * 创建无效的登录请求 - 用户名为空
     */
    public static UserLoginRequest createInvalidLoginRequest_EmptyLogin() {
        UserLoginRequest request = createValidLoginRequest();
        request.setLogin("");
        return request;
    }

    /**
     * 创建无效的登录请求 - 密码为空
     */
    public static UserLoginRequest createInvalidLoginRequest_EmptyPassword() {
        UserLoginRequest request = createValidLoginRequest();
        request.setPassword("");
        return request;
    }
}