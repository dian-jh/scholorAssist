package com.zd.scuserservice.manager;

import com.zd.scuserservice.model.domain.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据管理接口
 * 封装数据库访问与缓存操作，聚合多表读写，提供细粒度的领域方法
 * 
 * @author system
 * @since 2024-01-21
 */
public interface UserManager {

    /**
     * 创建用户
     * 
     * @param user 用户实体
     * @return 创建后的用户实体
     */
    User createUser(User user);

    /**
     * 根据用户ID获取用户
     * 
     * @param userId 用户ID
     * @return 用户实体
     */
    User getUserByUserId(String userId);

    /**
     * 根据用户名获取用户
     * 
     * @param username 用户名
     * @return 用户实体
     */
    User getUserByUsername(String username);

    /**
     * 根据邮箱获取用户
     * 
     * @param email 邮箱
     * @return 用户实体
     */
    User getUserByEmail(String email);

    /**
     * 根据用户名或邮箱获取用户（用于登录）
     * 
     * @param login 用户名或邮箱
     * @return 用户实体
     */
    User getUserByUsernameOrEmail(String login);

    /**
     * 更新用户信息
     * 
     * @param user 用户实体
     * @return 更新后的用户实体
     */
    User updateUser(User user);

    /**
     * 更新用户最后登录时间
     * 
     * @param userId 用户ID
     * @param lastLoginAt 最后登录时间
     * @return 是否更新成功
     */
    boolean updateLastLoginTime(String userId, LocalDateTime lastLoginAt);

    /**
     * 更新用户密码
     * 
     * @param userId 用户ID
     * @param passwordHash 新密码哈希值
     * @return 是否更新成功
     */
    boolean updatePassword(String userId, String passwordHash);

    /**
     * 更新用户状态
     * 
     * @param userId 用户ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(String userId, String status);

    /**
     * 更新用户角色
     * 
     * @param userId 用户ID
     * @param role 新角色
     * @return 是否更新成功
     */
    boolean updateRole(String userId, String role);

    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 删除用户
     * 
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(String userId);

    /**
     * 搜索用户列表
     * 
     * @param keyword 搜索关键词
     * @param role 角色筛选
     * @param status 状态筛选
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> searchUsers(String keyword, String role, String status, 
                          String sortBy, String sortOrder, int offset, int limit);

    /**
     * 统计用户总数
     * 
     * @param keyword 搜索关键词
     * @param role 角色筛选
     * @param status 状态筛选
     * @return 用户总数
     */
    long countUsers(String keyword, String role, String status);
}