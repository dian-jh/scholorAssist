package com.zd.scuserservice.service;

import com.zd.scuserservice.model.dto.request.*;
import com.zd.scuserservice.model.dto.response.*;

/**
 * 用户业务服务接口
 * 
 * @author system
 * @since 2024-01-21
 */
public interface UserService {

    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 用户信息响应
     */
    UserInfoResponse register(UserRegisterRequest request);

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 用户登出
     * 
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean logout(String userId);

    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息响应
     */
    UserInfoResponse getUserInfo(String userId);

    /**
     * 更新用户信息
     * 
     * @param userId 用户ID
     * @param request 更新请求
     * @return 用户信息响应
     */
    UserInfoResponse updateUserInfo(String userId, UserUpdateRequest request);

    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param request 修改密码请求
     * @return 是否成功
     */
    boolean changePassword(String userId, ChangePasswordRequest request);

    /**
     * 获取用户权限
     * 
     * @param userId 用户ID
     * @return 用户权限响应
     */
    UserPermissionResponse getUserPermissions(String userId);

    /**
     * 管理员获取用户列表
     * 
     * @param page 页码
     * @param pageSize 每页数量
     * @param search 搜索关键词
     * @param role 角色筛选
     * @param status 状态筛选
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 用户列表响应
     */
    UserListResponse getUserList(int page, int pageSize, String search, String role, 
                                String status, String sortBy, String sortOrder);

    /**
     * 管理员更新用户权限
     * 
     * @param targetUserId 目标用户ID
     * @param operatorUserId 操作者用户ID
     * @param request 权限更新请求
     * @return 用户信息响应
     */
    UserInfoResponse updateUserPermissions(String targetUserId, String operatorUserId, 
                                          UserPermissionUpdateRequest request);

    /**
     * 验证用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 是否验证通过
     */
    boolean validatePassword(String userId, String password);

    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return 是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱
     * @return 是否可用
     */
    boolean isEmailAvailable(String email);

    /**
     * 激活用户账号
     * 
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean activateUser(String userId);

    /**
     * 禁用用户账号
     * 
     * @param userId 用户ID
     * @param operatorUserId 操作者用户ID
     * @return 是否成功
     */
    boolean suspendUser(String userId, String operatorUserId);

    /**
     * 删除用户账号
     * 
     * @param userId 用户ID
     * @param operatorUserId 操作者用户ID
     * @return 是否成功
     */
    boolean deleteUser(String userId, String operatorUserId);
}