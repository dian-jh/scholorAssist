package com.zd.sccommon.utils;

import com.zd.sccommon.model.UserContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文持有者
 * 
 * <p>使用ThreadLocal存储当前线程的用户上下文信息</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Slf4j
public class UserContextHolder {

    /**
     * ThreadLocal存储用户上下文
     */
    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置用户上下文
     * 
     * @param userContext 用户上下文信息
     */
    public static void setContext(UserContext userContext) {
        if (userContext != null) {
            CONTEXT_HOLDER.set(userContext);
            log.debug("用户上下文已设置到ThreadLocal，userId: {}", userContext.getUserId());
        } else {
            log.debug("用户上下文为null，清空ThreadLocal");
            CONTEXT_HOLDER.remove();
        }
    }

    /**
     * 获取用户上下文
     * 
     * @return 用户上下文信息，如果未设置返回null
     */
    public static UserContext getContext() {
        UserContext context = CONTEXT_HOLDER.get();
        if (context != null) {
            log.debug("从ThreadLocal获取用户上下文，userId: {}", context.getUserId());
        } else {
            log.debug("ThreadLocal中未找到用户上下文");
        }
        return context;
    }

    /**
     * 清空用户上下文
     */
    public static void clear() {
        UserContext context = CONTEXT_HOLDER.get();
        if (context != null) {
            log.debug("清空ThreadLocal中的用户上下文，userId: {}", context.getUserId());
        }
        CONTEXT_HOLDER.remove();
    }

    /**
     * 检查是否有用户上下文
     * 
     * @return 是否有用户上下文
     */
    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }
}