package com.zd.scuserservice.controller;

import com.zd.sccommon.utils.UserContextUtil;
import com.zd.scuserservice.model.dto.request.*;
import com.zd.scuserservice.model.dto.response.*;
import com.zd.scuserservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 用户管理控制器
 * 仅负责接口设计和请求响应处理，不包含业务逻辑
 * 
 * @author system
 * @since 2024-01-21
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账号，创建用户基本信息")
    public UserInfoResponse register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("用户注册请求，用户名：{}", request.getUsername());
        return userService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户通过用户名/邮箱和密码登录系统")
    public UserLoginResponse login(@Valid @RequestBody UserLoginRequest request) {
        log.info("用户登录请求，login：{}", request.getLogin());
        return userService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户退出登录，使当前Token失效")
    public void logout() {
        String userId = UserContextUtil.getCurrentUserId();
        log.info("用户登出请求，userId：{}", userId);
        userService.logout(userId);
    }

    @GetMapping("/profile")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public UserInfoResponse getProfile() {
        String userId = UserContextUtil.getCurrentUserId();
        log.debug("获取用户信息请求，userId：{}", userId);
        return userService.getUserInfo(userId);
    }

    @PostMapping("/profile")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的个人信息")
    public UserInfoResponse updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        String userId = UserContextUtil.getCurrentUserId();
        log.info("更新用户信息请求，userId：{}", userId);
        return userService.updateUserInfo(userId, request);
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "用户修改登录密码")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String userId = UserContextUtil.getCurrentUserId();
        log.info("修改密码请求，userId：{}", userId);
        
        boolean success = userService.changePassword(userId, request);
        if (!success) {
            throw new RuntimeException("密码修改失败");
        }
    }

    @GetMapping("/permissions")
    @Operation(summary = "获取用户权限", description = "获取当前用户的权限列表和角色信息")
    public UserPermissionResponse getPermissions() {
        String userId = UserContextUtil.getCurrentUserId();
        log.debug("获取用户权限请求，userId：{}", userId);
        return userService.getUserPermissions(userId);
    }

    @GetMapping
    @Operation(summary = "获取用户列表", description = "管理员获取系统中所有用户的列表信息")
    public UserListResponse getUserList(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量，默认20，最大100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "搜索关键词，支持用户名、邮箱、真实姓名") @RequestParam(required = false) String search,
            @Parameter(description = "角色筛选：user/admin/super_admin") @RequestParam(required = false) String role,
            @Parameter(description = "状态筛选：active/suspended/pending_verification") @RequestParam(required = false) String status,
            @Parameter(description = "排序字段：created_at/last_login_at/username") @RequestParam(defaultValue = "created_at") String sortBy,
            @Parameter(description = "排序方向：asc/desc，默认desc") @RequestParam(defaultValue = "desc") String sortOrder) {
        
        // 验证管理员权限
        UserContextUtil.validateAdminPermission();
        
        log.info("获取用户列表请求，page：{}, pageSize：{}", page, pageSize);
        return userService.getUserList(page, pageSize, search, role, status, sortBy, sortOrder);
    }

    @PostMapping("/{userId}/permissions")
    @Operation(summary = "更新用户权限", description = "管理员修改指定用户的角色和权限")
    public UserInfoResponse updateUserPermissions(
            @Parameter(description = "目标用户ID") @PathVariable String userId,
            @Valid @RequestBody UserPermissionUpdateRequest request) {
        
        // 验证管理员权限
        UserContextUtil.validateAdminPermission();
        String operatorUserId = UserContextUtil.getCurrentUserId();
        
        log.info("更新用户权限请求，targetUserId：{}, operatorUserId：{}", userId, operatorUserId);
        return userService.updateUserPermissions(userId, operatorUserId, request);
    }

    @GetMapping("/check-username")
    @Operation(summary = "检查用户名可用性", description = "检查用户名是否已被使用")
    public boolean checkUsername(@Parameter(description = "用户名") @RequestParam String username) {
        log.debug("检查用户名可用性，username：{}", username);
        return userService.isUsernameAvailable(username);
    }

    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱可用性", description = "检查邮箱是否已被使用")
    public boolean checkEmail(@Parameter(description = "邮箱") @RequestParam String email) {
        log.debug("检查邮箱可用性，email：{}", email);
        return userService.isEmailAvailable(email);
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "激活用户", description = "激活用户账号")
    public void activateUser(@Parameter(description = "用户ID") @PathVariable String userId) {
        // 验证管理员权限
        UserContextUtil.validateAdminPermission();
        String operatorUserId = UserContextUtil.getCurrentUserId();
        
        log.info("激活用户请求，userId：{}, operatorUserId：{}", userId, operatorUserId);
        
        boolean success = userService.activateUser(userId);
        if (!success) {
            throw new RuntimeException("用户激活失败");
        }
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "禁用用户", description = "禁用用户账号")
    public void suspendUser(@Parameter(description = "用户ID") @PathVariable String userId) {
        // 验证管理员权限
        UserContextUtil.validateAdminPermission();
        String operatorUserId = UserContextUtil.getCurrentUserId();
        
        log.info("禁用用户请求，userId：{}, operatorUserId：{}", userId, operatorUserId);
        
        boolean success = userService.suspendUser(userId, operatorUserId);
        if (!success) {
            throw new RuntimeException("用户禁用失败");
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户", description = "删除用户账号")
    public void deleteUser(@Parameter(description = "用户ID") @PathVariable String userId) {
        // 验证超级管理员权限
        UserContextUtil.validateUserPermission("super_admin");
        String operatorUserId = UserContextUtil.getCurrentUserId();
        
        log.info("删除用户请求，userId：{}, operatorUserId：{}", userId, operatorUserId);
        
        boolean success = userService.deleteUser(userId, operatorUserId);
        if (!success) {
            throw new RuntimeException("用户删除失败");
        }
    }
}