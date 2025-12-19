package com.zd.scuserservice.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.zd.sccommon.common.BusinessException;
import com.zd.scuserservice.manager.UserManager;
import com.zd.scuserservice.model.domain.User;
import com.zd.scuserservice.model.dto.request.*;
import com.zd.scuserservice.model.dto.response.*;
import com.zd.scuserservice.service.UserService;
import com.zd.scuserservice.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 用户业务服务实现类
 * 
 * @author system
 * @since 2024-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    private final UserManager userManager;
    private final JwtUtil jwtUtil;

    @Override
    public UserInfoResponse register(UserRegisterRequest request) {
        log.info("用户注册，username: {}, email: {}", request.getUsername(), request.getEmail());

        // 业务校验
        validateRegisterRequest(request);

        // 检查用户名和邮箱唯一性
        if (userManager.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }

        if (userManager.existsByEmail(request.getEmail())) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 创建用户
        User user = buildUserFromRegisterRequest(request);
        User createdUser = userManager.createUser(user);

        log.info("用户注册成功，userId: {}, username: {}", createdUser.getUserId(), createdUser.getUsername());
        return convertToUserInfoResponse(createdUser);
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        log.info("用户登录，login: {}", request.getLogin());

        // 查找用户
        User user = userManager.getUserByUsernameOrEmail(request.getLogin());
        if (user == null) {
            throw new BusinessException(401, "用户名或邮箱不存在");
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "密码错误");
        }

        // 检查用户状态
        if ("suspended".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 更新最后登录时间
        LocalDateTime loginTime = LocalDateTime.now();
        userManager.updateLastLoginTime(user.getUserId(), loginTime);

        // 生成JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), 
                                           user.getRole(), request.getRememberMe());

        log.info("用户登录成功，userId: {}, username: {}", user.getUserId(), user.getUsername());

        return UserLoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(request.getRememberMe() ? 7 * 24 * 3600L : 24 * 3600L)
                .userInfo(convertToUserInfoResponse(user))
                .loginTime(loginTime)
                .build();
    }

    @Override
    public boolean logout(String userId) {
        log.info("用户登出，userId: {}", userId);
        
        // 这里可以实现token黑名单机制
        // 目前只记录日志，实际的token失效由JWT过期时间控制
        
        log.info("用户登出成功，userId: {}", userId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(String userId) {
        log.debug("获取用户信息，userId: {}", userId);

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return convertToUserInfoResponse(user);
    }

    @Override
    public UserInfoResponse updateUserInfo(String userId, UserUpdateRequest request) {
        log.info("更新用户信息，userId: {}", userId);

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 检查邮箱唯一性（如果邮箱有变更）
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userManager.existsByEmail(request.getEmail())) {
                throw new BusinessException(400, "邮箱已被其他用户使用");
            }
            user.setEmail(request.getEmail());
        }

        // 更新其他字段
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updatedUser = userManager.updateUser(user);
        log.info("用户信息更新成功，userId: {}", userId);

        return convertToUserInfoResponse(updatedUser);
    }

    @Override
    public boolean changePassword(String userId, ChangePasswordRequest request) {
        log.info("修改密码，userId: {}", userId);

        // 验证新密码和确认密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "新密码和确认密码不一致");
        }

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证当前密码
        if (!BCrypt.checkpw(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "当前密码错误");
        }

        // 加密新密码
        String newPasswordHash = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());

        // 更新密码
        boolean success = userManager.updatePassword(userId, newPasswordHash);
        if (success) {
            log.info("密码修改成功，userId: {}", userId);
        } else {
            log.error("密码修改失败，userId: {}", userId);
        }

        return success;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPermissionResponse getUserPermissions(String userId) {
        log.debug("获取用户权限，userId: {}", userId);

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return buildUserPermissionResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResponse getUserList(int page, int pageSize, String search, String role, 
                                       String status, String sortBy, String sortOrder) {
        log.info("获取用户列表，page: {}, pageSize: {}, search: {}, role: {}, status: {}", 
                page, pageSize, search, role, status);

        int offset = (page - 1) * pageSize;
        
        // 查询用户列表
        List<User> users = userManager.searchUsers(search, role, status, sortBy, sortOrder, offset, pageSize);
        
        // 统计总数
        long totalCount = userManager.countUsers(search, role, status);
        
        // 转换为响应DTO
        List<UserInfoResponse> userInfoList = users.stream()
                .map(this::convertToUserInfoResponse)
                .toList();

        // 构建分页信息
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        UserListResponse.PageInfo pageInfo = UserListResponse.PageInfo.builder()
                .currentPage(page)
                .pageSize(pageSize)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();

        return UserListResponse.builder()
                .users(userInfoList)
                .pageInfo(pageInfo)
                .build();
    }

    @Override
    public UserInfoResponse updateUserPermissions(String targetUserId, String operatorUserId, 
                                                 UserPermissionUpdateRequest request) {
        log.info("更新用户权限，targetUserId: {}, operatorUserId: {}, role: {}, status: {}", 
                targetUserId, operatorUserId, request.getRole(), request.getStatus());

        User targetUser = userManager.getUserByUserId(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(404, "目标用户不存在");
        }

        User operatorUser = userManager.getUserByUserId(operatorUserId);
        if (operatorUser == null) {
            throw new BusinessException(404, "操作者用户不存在");
        }

        // 权限检查：只有管理员可以修改权限
        if (!"admin".equals(operatorUser.getRole()) && !"super_admin".equals(operatorUser.getRole())) {
            throw new BusinessException(403, "权限不足，只有管理员可以修改用户权限");
        }

        // 超级管理员权限检查：只有超级管理员可以设置超级管理员角色
        if ("super_admin".equals(request.getRole()) && !"super_admin".equals(operatorUser.getRole())) {
            throw new BusinessException(403, "权限不足，只有超级管理员可以设置超级管理员角色");
        }

        // 更新角色
        if (!request.getRole().equals(targetUser.getRole())) {
            userManager.updateRole(targetUserId, request.getRole());
        }

        // 更新状态
        if (!request.getStatus().equals(targetUser.getStatus())) {
            userManager.updateStatus(targetUserId, request.getStatus());
        }

        // 获取更新后的用户信息
        User updatedUser = userManager.getUserByUserId(targetUserId);
        log.info("用户权限更新成功，targetUserId: {}, newRole: {}, newStatus: {}", 
                targetUserId, updatedUser.getRole(), updatedUser.getStatus());

        return convertToUserInfoResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validatePassword(String userId, String password) {
        log.debug("验证用户密码，userId: {}", userId);

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            return false;
        }

        return BCrypt.checkpw(password, user.getPasswordHash());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        log.debug("检查用户名可用性，username: {}", username);
        return !userManager.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        log.debug("检查邮箱可用性，email: {}", email);
        return !userManager.existsByEmail(email);
    }

    @Override
    public boolean activateUser(String userId) {
        log.info("激活用户，userId: {}", userId);

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if ("active".equals(user.getStatus())) {
            throw new BusinessException(400, "用户已经是激活状态");
        }

        boolean success = userManager.updateStatus(userId, "active");
        if (success) {
            log.info("用户激活成功，userId: {}", userId);
        } else {
            log.error("用户激活失败，userId: {}", userId);
        }

        return success;
    }

    @Override
    public boolean suspendUser(String userId, String operatorUserId) {
        log.info("禁用用户，userId: {}, operatorUserId: {}", userId, operatorUserId);

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if ("suspended".equals(user.getStatus())) {
            throw new BusinessException(400, "用户已经是禁用状态");
        }

        // 不能禁用超级管理员
        if ("super_admin".equals(user.getRole())) {
            throw new BusinessException(400, "不能禁用超级管理员");
        }

        boolean success = userManager.updateStatus(userId, "suspended");
        if (success) {
            log.info("用户禁用成功，userId: {}", userId);
        } else {
            log.error("用户禁用失败，userId: {}", userId);
        }

        return success;
    }

    @Override
    public boolean deleteUser(String userId, String operatorUserId) {
        log.info("删除用户，userId: {}, operatorUserId: {}", userId, operatorUserId);

        User user = userManager.getUserByUserId(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 不能删除超级管理员
        if ("super_admin".equals(user.getRole())) {
            throw new BusinessException(400, "不能删除超级管理员");
        }

        // 不能删除自己
        if (userId.equals(operatorUserId)) {
            throw new BusinessException(400, "不能删除自己的账号");
        }

        boolean success = userManager.deleteUser(userId);
        if (success) {
            log.info("用户删除成功，userId: {}", userId);
        } else {
            log.error("用户删除失败，userId: {}", userId);
        }

        return success;
    }

    /**
     * 验证注册请求
     */
    private void validateRegisterRequest(UserRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "密码和确认密码不一致");
        }
    }

    /**
     * 从注册请求构建用户实体
     */
    private User buildUserFromRegisterRequest(UserRegisterRequest request) {
        String userId = "user_" + IdUtil.fastSimpleUUID();
        String passwordHash = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());

        return User.builder()
                .userId(userId)
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .realName(request.getRealName())
                .role("user")  // 默认角色
                .status("pending_verification")  // 默认状态
                .build();
    }

    /**
     * 转换为用户信息响应DTO
     */
    private UserInfoResponse convertToUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .realName(user.getRealName())
                .role(user.getRole())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 构建用户权限响应
     */
    private UserPermissionResponse buildUserPermissionResponse(User user) {
        List<String> permissions = getPermissionsByRole(user.getRole());
        UserPermissionResponse.UsageLimits usageLimits = getUsageLimitsByRole(user.getRole());

        return UserPermissionResponse.builder()
                .userId(user.getUserId())
                .role(user.getRole())
                .permissions(permissions)
                .usageLimits(usageLimits)
                .build();
    }

    /**
     * 根据角色获取权限列表
     */
    private List<String> getPermissionsByRole(String role) {
        return switch (role) {
            case "super_admin" -> Arrays.asList(
                "user:read", "user:write", "user:delete",
                "document:read", "document:write", "document:delete",
                "note:read", "note:write", "note:delete",
                "ai:chat", "ai:unlimited",
                "system:admin", "system:config"
            );
            case "admin" -> Arrays.asList(
                "user:read", "user:write",
                "document:read", "document:write",
                "note:read", "note:write",
                "ai:chat", "ai:unlimited"
            );
            default -> Arrays.asList(
                "document:read", "document:write",
                "note:read", "note:write",
                "ai:chat"
            );
        };
    }

    /**
     * 根据角色获取使用限制
     */
    private UserPermissionResponse.UsageLimits getUsageLimitsByRole(String role) {
        return switch (role) {
            case "super_admin", "admin" -> UserPermissionResponse.UsageLimits.builder()
                    .maxDocuments(-1)  // 无限制
                    .maxStorageSize(-1L)  // 无限制
                    .dailyAiConversations(-1)  // 无限制
                    .allowExport(true)
                    .allowShare(true)
                    .build();
            default -> UserPermissionResponse.UsageLimits.builder()
                    .maxDocuments(100)
                    .maxStorageSize(1024L)  // 1GB
                    .dailyAiConversations(50)
                    .allowExport(true)
                    .allowShare(true)
                    .build();
        };
    }
}