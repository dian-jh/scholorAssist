package com.zd.scuserservice;

import com.zd.scuserservice.controller.UserControllerAdminTest;
import com.zd.scuserservice.controller.UserControllerAuthenticatedTest;
import com.zd.scuserservice.controller.UserControllerIntegrationTest;
import com.zd.scuserservice.performance.UserServicePerformanceTest;
import com.zd.scuserservice.service.UserServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 用户服务测试套件
 * 组织和管理所有用户服务相关的测试
 * 
 * @author system
 * @since 2024-01-21
 */
@Suite
@SuiteDisplayName("用户服务完整测试套件")
@SelectClasses({
    // 单元测试
    UserServiceTest.class,
    
    // 集成测试 - 基础功能
    UserControllerIntegrationTest.class,
    
    // 集成测试 - 认证功能
    UserControllerAuthenticatedTest.class,
    
    // 集成测试 - 管理员功能
    UserControllerAdminTest.class,
    
    // 性能测试
    UserServicePerformanceTest.class
})
@DisplayName("用户服务测试套件")
public class UserServiceTestSuite {
    
    /**
     * 测试套件说明：
     * 
     * 1. UserServiceTest - 用户服务单元测试
     *    - 测试业务逻辑层的各个方法
     *    - 使用Mock对象隔离依赖
     *    - 覆盖正常和异常场景
     * 
     * 2. UserControllerIntegrationTest - 基础API集成测试
     *    - 测试用户注册、登录等公开接口
     *    - 测试参数验证和错误处理
     *    - 测试用户名和邮箱可用性检查
     * 
     * 3. UserControllerAuthenticatedTest - 认证API集成测试
     *    - 测试需要JWT认证的接口
     *    - 测试用户信息管理、密码修改等功能
     *    - 测试权限验证和Token失效处理
     * 
     * 4. UserControllerAdminTest - 管理员功能集成测试
     *    - 测试管理员权限相关的接口
     *    - 测试用户列表查询、权限管理等功能
     *    - 测试权限控制和边界条件
     * 
     * 5. UserServicePerformanceTest - 性能测试
     *    - 测试API接口的响应时间
     *    - 测试并发处理能力
     *    - 测试内存使用和数据库连接池
     * 
     * 测试覆盖范围：
     * - 功能测试：100%覆盖所有API接口
     * - 异常测试：覆盖各种错误场景和边界条件
     * - 安全测试：验证认证、授权和权限控制
     * - 性能测试：验证响应时间和并发处理能力
     * - 集成测试：验证各组件之间的协作
     */
}