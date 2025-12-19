package com.zd.sccommon.utils;

import com.zd.sccommon.common.BusinessException;
import com.zd.sccommon.model.UserContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文工具类
 * 从ThreadLocal中获取当前请求的用户信息
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
public class UserContextUtil {

    /**
     * 获取用户上下文信息
     * 
     * @return 用户上下文信息，如果未设置返回null
     */
    public static UserContext getUserContext() {
        UserContext context = UserContextHolder.getContext();
        if (context != null) {
            log.debug("获取用户上下文信息，userId: {}, username: {}", context.getUserId(), context.getUsername());
        } else {
            log.debug("用户上下文信息为空");
        }
        return context;
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未设置返回null
     */
    public static String getCurrentUserId() {
        UserContext context = getUserContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     * 
     * @return 用户名，如果未设置返回null
     */
    public static String getCurrentUsername() {
        UserContext context = getUserContext();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 获取当前用户显示名称
     * 
     * @return 用户显示名称，如果未设置返回null
     */
    public static String getCurrentUserDisplayName() {
        UserContext context = getUserContext();
        return context != null ? context.getDisplayName() : null;
    }

    /**
     * 获取当前用户真实姓名
     * 
     * @return 用户真实姓名，如果未设置返回null
     */
    public static String getCurrentUserName() {
        UserContext context = getUserContext();
        return context != null ? context.getName() : null;
    }

    /**
     * 获取当前用户角色
     * 
     * @return 用户角色，如果未设置返回null
     */
    public static String getCurrentUserRole() {
        UserContext context = getUserContext();
        return context != null ? context.getRole() : null;
    }

    /**
     * 获取当前JWT token
     * 
     * @return JWT token，如果未设置返回null
     */
    public static String getCurrentToken() {
        UserContext context = getUserContext();
        return context != null ? context.getToken() : null;
    }

    /**
     * 获取当前用户头像URL
     * 
     * @return 用户头像URL，如果未设置返回null
     */
    public static String getCurrentUserAvatar() {
        UserContext context = getUserContext();
        return context != null ? context.getAvatar() : null;
    }

    /**
     * 获取当前用户邮箱
     * 
     * @return 用户邮箱，如果未设置返回null
     */
    public static String getCurrentUserEmail() {
        UserContext context = getUserContext();
        return context != null ? context.getEmail() : null;
    }

    /**
     * 获取当前用户状态
     * 
     * @return 用户状态，如果未设置返回null
     */
    public static String getCurrentUserStatus() {
        UserContext context = getUserContext();
        return context != null ? context.getStatus() : null;
    }

    /**
     * 检查当前用户是否已登录
     * 
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn() {
        UserContext context = getUserContext();
        boolean loggedIn = context != null && context.isLoggedIn();
        log.debug("检查用户登录状态: {}", loggedIn);
        return loggedIn;
    }

    /**
     * 检查当前用户是否具有指定角色
     * 
     * @param role 角色名称
     * @return 是否具有指定角色
     */
    public static boolean hasRole(String role) {
        UserContext context = getUserContext();
        boolean hasRole = context != null && context.hasRole(role);
        log.debug("检查用户角色权限，当前角色: {}, 需要角色: {}, 结果: {}", 
                context != null ? context.getRole() : null, role, hasRole);
        return hasRole;
    }

    /**
     * 检查当前用户是否为管理员
     * 
     * @return 是否为管理员
     */
    public static boolean isAdmin() {
        UserContext context = getUserContext();
        return context != null && context.isAdmin();
    }

    /**
     * 检查当前用户是否为超级管理员
     * 
     * @return 是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        UserContext context = getUserContext();
        return context != null && context.isSuperAdmin();
    }

    /**
     * 验证当前用户权限
     * 
     * @param requiredRole 需要的角色
     * @throws BusinessException 如果权限不足
     */
    public static void validateUserPermission(String requiredRole) {
        if (!isUserLoggedIn()) {
            log.warn("用户未登录，拒绝访问");
            throw new BusinessException(401, "用户未登录");
        }
        
        if (requiredRole != null && !hasRole(requiredRole)) {
            log.warn("用户权限不足，当前角色: {}, 需要角色: {}", getCurrentUserRole(), requiredRole);
            throw new BusinessException(403, "权限不足");
        }
        
        log.debug("用户权限验证通过，userId: {}, role: {}", getCurrentUserId(), getCurrentUserRole());
    }

    /**
     * 验证管理员权限
     * 
     * @throws BusinessException 如果不是管理员
     */
    public static void validateAdminPermission() {
        if (!isUserLoggedIn()) {
            log.warn("用户未登录，拒绝访问");
            throw new BusinessException(401, "用户未登录");
        }
        
        if (!isAdmin()) {
            log.warn("用户不是管理员，拒绝访问，当前角色: {}", getCurrentUserRole());
            throw new BusinessException(403, "需要管理员权限");
        }
        
        log.debug("管理员权限验证通过，userId: {}, role: {}", getCurrentUserId(), getCurrentUserRole());
    }

    /**
     * 检查当前用户是否为活跃状态
     * 
     * @return 是否为活跃状态
     */
    public static boolean isUserActive() {
        UserContext context = getUserContext();
        return context != null && context.isActive();
    }

    /**
     * 验证用户状态是否为活跃状态
     * 
     * @throws BusinessException 如果用户状态不是活跃状态
     */
    public static void validateUserActive() {
        if (!isUserLoggedIn()) {
            log.warn("用户未登录，拒绝访问");
            throw new BusinessException(401, "用户未登录");
        }
        
        if (!isUserActive()) {
            log.warn("用户状态异常，拒绝访问，当前状态: {}", getCurrentUserStatus());
            throw new BusinessException(403, "用户状态异常，请联系管理员");
        }
        
        log.debug("用户状态验证通过，userId: {}, status: {}", getCurrentUserId(), getCurrentUserStatus());
    }
}