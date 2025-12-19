package com.zd.scuserservice.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zd.sccommon.common.BusinessException;
import com.zd.scuserservice.manager.UserManager;
import com.zd.scuserservice.mapper.UserMapper;
import com.zd.scuserservice.model.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据管理实现类
 * 封装数据库访问与缓存操作，聚合多表读写，提供细粒度的领域方法
 * 
 * @author system
 * @since 2024-01-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserManagerImpl implements UserManager {

    private final UserMapper userMapper;

    @Override
    public User createUser(User user) {
        log.info("创建用户，userId: {}, username: {}", user.getUserId(), user.getUsername());
        
        try {
            // 设置创建和更新时间
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            
            int result = userMapper.insert(user);
            if (result <= 0) {
                throw new BusinessException(500, "用户创建失败");
            }
            
            log.info("用户创建成功，userId: {}", user.getUserId());
            return user;
        } catch (Exception e) {
            log.error("创建用户失败，userId: {}, error: {}", user.getUserId(), e.getMessage(), e);
            throw new BusinessException(500, "用户创建失败：" + e.getMessage());
        }
    }

    @Override
    public User getUserByUserId(String userId) {
        log.debug("根据用户ID获取用户，userId: {}", userId);
        
        try {
            User user = userMapper.selectByUserId(userId);
            if (user != null) {
                log.debug("用户查询成功，userId: {}, username: {}", userId, user.getUsername());
            } else {
                log.debug("用户不存在，userId: {}", userId);
            }
            return user;
        } catch (Exception e) {
            log.error("根据用户ID查询用户失败，userId: {}, error: {}", userId, e.getMessage(), e);
            throw new BusinessException(500, "用户查询失败：" + e.getMessage());
        }
    }

    @Override
    public User getUserByUsername(String username) {
        log.debug("根据用户名获取用户，username: {}", username);
        
        try {
            User user = userMapper.selectByUsername(username);
            if (user != null) {
                log.debug("用户查询成功，username: {}, userId: {}", username, user.getUserId());
            } else {
                log.debug("用户不存在，username: {}", username);
            }
            return user;
        } catch (Exception e) {
            log.error("根据用户名查询用户失败，username: {}, error: {}", username, e.getMessage(), e);
            throw new BusinessException(500, "用户查询失败：" + e.getMessage());
        }
    }

    @Override
    public User getUserByEmail(String email) {
        log.debug("根据邮箱获取用户，email: {}", email);
        
        try {
            User user = userMapper.selectByEmail(email);
            if (user != null) {
                log.debug("用户查询成功，email: {}, userId: {}", email, user.getUserId());
            } else {
                log.debug("用户不存在，email: {}", email);
            }
            return user;
        } catch (Exception e) {
            log.error("根据邮箱查询用户失败，email: {}, error: {}", email, e.getMessage(), e);
            throw new BusinessException(500, "用户查询失败：" + e.getMessage());
        }
    }

    @Override
    public User getUserByUsernameOrEmail(String login) {
        log.debug("根据用户名或邮箱获取用户，login: {}", login);
        
        try {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, login)
                       .or()
                       .eq(User::getEmail, login);
            
            User user = userMapper.selectOne(queryWrapper);
            if (user != null) {
                log.debug("用户查询成功，login: {}, userId: {}", login, user.getUserId());
            } else {
                log.debug("用户不存在，login: {}", login);
            }
            return user;
        } catch (Exception e) {
            log.error("根据用户名或邮箱查询用户失败，login: {}, error: {}", login, e.getMessage(), e);
            throw new BusinessException(500, "用户查询失败：" + e.getMessage());
        }
    }

    @Override
    public User updateUser(User user) {
        log.info("更新用户信息，userId: {}", user.getUserId());
        
        try {
            // 设置更新时间
            user.setUpdatedAt(LocalDateTime.now());
            
            int result = userMapper.updateById(user);
            if (result <= 0) {
                throw new BusinessException(500, "用户更新失败");
            }
            
            log.info("用户更新成功，userId: {}", user.getUserId());
            return user;
        } catch (Exception e) {
            log.error("更新用户失败，userId: {}, error: {}", user.getUserId(), e.getMessage(), e);
            throw new BusinessException(500, "用户更新失败：" + e.getMessage());
        }
    }

    @Override
    public boolean updateLastLoginTime(String userId, LocalDateTime lastLoginAt) {
        log.debug("更新用户最后登录时间，userId: {}", userId);
        
        try {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getUserId, userId)
                        .set(User::getLastLoginAt, lastLoginAt)
                        .set(User::getUpdatedAt, LocalDateTime.now());
            
            int result = userMapper.update(null, updateWrapper);
            boolean success = result > 0;
            
            if (success) {
                log.debug("用户最后登录时间更新成功，userId: {}", userId);
            } else {
                log.warn("用户最后登录时间更新失败，userId: {}", userId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("更新用户最后登录时间失败，userId: {}, error: {}", userId, e.getMessage(), e);
            throw new BusinessException(500, "更新登录时间失败：" + e.getMessage());
        }
    }

    @Override
    public boolean updatePassword(String userId, String passwordHash) {
        log.info("更新用户密码，userId: {}", userId);
        
        try {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getUserId, userId)
                        .set(User::getPasswordHash, passwordHash)
                        .set(User::getUpdatedAt, LocalDateTime.now());
            
            int result = userMapper.update(null, updateWrapper);
            boolean success = result > 0;
            
            if (success) {
                log.info("用户密码更新成功，userId: {}", userId);
            } else {
                log.warn("用户密码更新失败，userId: {}", userId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("更新用户密码失败，userId: {}, error: {}", userId, e.getMessage(), e);
            throw new BusinessException(500, "密码更新失败：" + e.getMessage());
        }
    }

    @Override
    public boolean updateStatus(String userId, String status) {
        log.info("更新用户状态，userId: {}, status: {}", userId, status);
        
        try {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getUserId, userId)
                        .set(User::getStatus, status)
                        .set(User::getUpdatedAt, LocalDateTime.now());
            
            int result = userMapper.update(null, updateWrapper);
            boolean success = result > 0;
            
            if (success) {
                log.info("用户状态更新成功，userId: {}, status: {}", userId, status);
            } else {
                log.warn("用户状态更新失败，userId: {}, status: {}", userId, status);
            }
            
            return success;
        } catch (Exception e) {
            log.error("更新用户状态失败，userId: {}, status: {}, error: {}", userId, status, e.getMessage(), e);
            throw new BusinessException(500, "状态更新失败：" + e.getMessage());
        }
    }

    @Override
    public boolean updateRole(String userId, String role) {
        log.info("更新用户角色，userId: {}, role: {}", userId, role);
        
        try {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getUserId, userId)
                        .set(User::getRole, role)
                        .set(User::getUpdatedAt, LocalDateTime.now());
            
            int result = userMapper.update(null, updateWrapper);
            boolean success = result > 0;
            
            if (success) {
                log.info("用户角色更新成功，userId: {}, role: {}", userId, role);
            } else {
                log.warn("用户角色更新失败，userId: {}, role: {}", userId, role);
            }
            
            return success;
        } catch (Exception e) {
            log.error("更新用户角色失败，userId: {}, role: {}, error: {}", userId, role, e.getMessage(), e);
            throw new BusinessException(500, "角色更新失败：" + e.getMessage());
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        log.debug("检查用户名是否存在，username: {}", username);
        
        try {
            long count = userMapper.countByUsername(username);
            boolean exists = count > 0;
            
            log.debug("用户名存在性检查完成，username: {}, exists: {}", username, exists);
            return exists;
        } catch (Exception e) {
            log.error("检查用户名存在性失败，username: {}, error: {}", username, e.getMessage(), e);
            throw new BusinessException(500, "用户名检查失败：" + e.getMessage());
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        log.debug("检查邮箱是否存在，email: {}", email);
        
        try {
            long count = userMapper.countByEmail(email);
            boolean exists = count > 0;
            
            log.debug("邮箱存在性检查完成，email: {}, exists: {}", email, exists);
            return exists;
        } catch (Exception e) {
            log.error("检查邮箱存在性失败，email: {}, error: {}", email, e.getMessage(), e);
            throw new BusinessException(500, "邮箱检查失败：" + e.getMessage());
        }
    }

    @Override
    public boolean deleteUser(String userId) {
        log.info("删除用户，userId: {}", userId);
        
        try {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUserId, userId);
            
            int result = userMapper.delete(queryWrapper);
            boolean success = result > 0;
            
            if (success) {
                log.info("用户删除成功，userId: {}", userId);
            } else {
                log.warn("用户删除失败，userId: {}", userId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("删除用户失败，userId: {}, error: {}", userId, e.getMessage(), e);
            throw new BusinessException(500, "用户删除失败：" + e.getMessage());
        }
    }

    @Override
    public List<User> searchUsers(String keyword, String role, String status, 
                                 String sortBy, String sortOrder, int offset, int limit) {
        log.debug("搜索用户列表，keyword: {}, role: {}, status: {}, sortBy: {}, sortOrder: {}, offset: {}, limit: {}", 
                 keyword, role, status, sortBy, sortOrder, offset, limit);
        
        try {
            List<User> users = userMapper.searchUsers(keyword, role, status, sortBy, sortOrder, offset, limit);
            log.debug("用户列表查询完成，返回 {} 条记录", users.size());
            
            return users;
        } catch (Exception e) {
            log.error("搜索用户列表失败，error: {}", e.getMessage(), e);
            throw new BusinessException(500, "用户列表查询失败：" + e.getMessage());
        }
    }

    @Override
    public long countUsers(String keyword, String role, String status) {
        log.debug("统计用户总数，keyword: {}, role: {}, status: {}", keyword, role, status);
        
        try {
            Long count = userMapper.countSearchUsers(keyword, role, status);
            log.debug("用户总数统计完成，count: {}", count);
            
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("统计用户总数失败，error: {}", e.getMessage(), e);
            throw new BusinessException(500, "用户统计失败：" + e.getMessage());
        }
    }
}