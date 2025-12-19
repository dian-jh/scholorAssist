package com.zd.scuserservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zd.scuserservice.model.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据访问接口
 * 
 * <p>提供用户表的基础CRUD操作</p>
 * <p>继承MyBatis-Plus的BaseMapper，获得基础的增删改查功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 批量插入用户
     * 
     * <p>使用MyBatis-Plus的批量操作优化性能</p>
     * 
     * @param entities 用户实体列表
     * @return 影响行数
     */
    int batchInsert(@Param("entities") List<User> entities);

    /**
     * 根据用户ID查询用户
     * 
     * @param userId 用户唯一标识
     * @return 用户实体
     */
    User selectByUserId(@Param("userId") String userId);

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户实体
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     * 
     * @param email 邮箱地址
     * @return 用户实体
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据用户名或邮箱查询用户信息（用于登录）
     * 
     * @param login 用户名或邮箱地址
     * @return 用户实体
     */
    User selectByUsernameOrEmail(@Param("login") String login);

    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 存在数量
     */
    long countByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     * 
     * @param email 邮箱地址
     * @return 存在数量
     */
    long countByEmail(@Param("email") String email);

    /**
     * 更新用户最后登录时间
     * 
     * @param userId 用户ID
     * @param lastLoginAt 最后登录时间
     * @return 影响行数
     */
    int updateLastLoginTime(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * 更新用户密码
     * 
     * @param userId 用户ID
     * @param passwordHash 密码哈希值
     * @return 影响行数
     */
    int updatePassword(@Param("userId") String userId, @Param("passwordHash") String passwordHash);

    /**
     * 更新用户状态
     * 
     * @param userId 用户ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("userId") String userId, @Param("status") String status);

    /**
     * 搜索用户（支持用户名、邮箱、真实姓名模糊查询）
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
    List<User> searchUsers(@Param("keyword") String keyword, 
                          @Param("role") String role,
                          @Param("status") String status,
                          @Param("sortBy") String sortBy,
                          @Param("sortOrder") String sortOrder,
                          @Param("offset") int offset, 
                          @Param("limit") int limit);

    /**
     * 统计用户数量
     * 
     * @param keyword 搜索关键词
     * @param role 角色筛选
     * @param status 状态筛选
     * @return 用户数量
     */
    long countSearchUsers(@Param("keyword") String keyword,
                         @Param("role") String role,
                         @Param("status") String status);
}