package com.zd.scliteraturemanage.controller;

import com.zd.sccommon.model.UserContext;
import com.zd.sccommon.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 
 * <p>用于测试用户上下文传递功能</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "测试接口", description = "用于测试用户上下文传递功能")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/user-context")
    @Operation(summary = "测试用户上下文", description = "获取当前用户上下文信息")
    public Map<String, Object> testUserContext() {
        log.info("测试用户上下文接口被调用");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取用户上下文
            UserContext userContext = UserContextUtil.getUserContext();
            
            if (userContext != null) {
                result.put("success", true);
                result.put("message", "用户上下文获取成功");
                result.put("userContext", Map.of(
                    "userId", userContext.getUserId(),
                    "username", userContext.getUsername(),
                    "role", userContext.getRole(),
                    "isLoggedIn", userContext.isLoggedIn(),
                    "isAdmin", userContext.isAdmin(),
                    "isSuperAdmin", userContext.isSuperAdmin()
                ));
                
                // 测试工具类方法
                result.put("utilMethods", Map.of(
                    "getCurrentUserId", UserContextUtil.getCurrentUserId(),
                    "getCurrentUsername", UserContextUtil.getCurrentUsername(),
                    "getCurrentUserRole", UserContextUtil.getCurrentUserRole(),
                    "isUserLoggedIn", UserContextUtil.isUserLoggedIn(),
                    "isAdmin", UserContextUtil.isAdmin(),
                    "isSuperAdmin", UserContextUtil.isSuperAdmin()
                ));
                
                log.info("用户上下文测试成功，userId: {}, username: {}", 
                        userContext.getUserId(), userContext.getUsername());
            } else {
                result.put("success", false);
                result.put("message", "用户上下文为空");
                log.warn("用户上下文为空");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户上下文时发生异常: " + e.getMessage());
            log.error("获取用户上下文时发生异常", e);
        }
        
        return result;
    }
}