package com.zd.scuserservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zd.scuserservice.model.dto.request.UserRegisterRequest;
import com.zd.scuserservice.model.dto.request.UserLoginRequest;
import com.zd.scuserservice.model.dto.response.UserInfoResponse;
import com.zd.scuserservice.model.dto.response.UserLoginResponse;
import com.zd.scuserservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户控制器简化测试
 * 使用Mock对象测试控制器逻辑，不依赖完整的Spring Boot上下文
 * 
 * @author system
 * @since 2024-01-21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户控制器简化测试")
public class UserControllerSimpleTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("用户注册接口测试")
    void testRegister() throws Exception {
        // 准备测试数据
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("TestPass123!");
        request.setConfirmPassword("TestPass123!");
        request.setRealName("测试用户");

        UserInfoResponse mockResponse = UserInfoResponse.builder()
                .userId("user_123456789")
                .username("testuser")
                .email("test@example.com")
                .realName("测试用户")
                .role("user")
                .status("pending_verification")
                .build();

        // Mock服务层方法
        when(userService.register(any(UserRegisterRequest.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value("user_123456789"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.status").value("pending_verification"));
    }

    @Test
    @DisplayName("用户登录接口测试")
    void testLogin() throws Exception {
        // 准备测试数据
        UserLoginRequest request = new UserLoginRequest();
        request.setLogin("testuser");
        request.setPassword("TestPass123!");
        request.setRememberMe(false);

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .userId("user_123456789")
                .username("testuser")
                .email("test@example.com")
                .role("user")
                .status("active")
                .build();

        UserLoginResponse mockResponse = UserLoginResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userInfo(userInfo)
                .build();

        // Mock服务层方法
        when(userService.login(any(UserLoginRequest.class))).thenReturn(mockResponse);

        // 执行测试
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 修正：期望标准的Result格式
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.accessToken").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.userInfo.userId").value("user_123456789"))
                .andExpect(jsonPath("$.data.userInfo.username").value("testuser"));
    }

    @Test
    @DisplayName("检查用户名可用性测试")
    void testCheckUsername() throws Exception {
        // Mock服务层方法
        when(userService.isUsernameAvailable("availableuser")).thenReturn(true);
        when(userService.isUsernameAvailable("existinguser")).thenReturn(false);

        // 测试可用的用户名
        mockMvc.perform(get("/api/users/check-username")
                .param("username", "availableuser"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 测试不可用的用户名
        mockMvc.perform(get("/api/users/check-username")
                .param("username", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("检查邮箱可用性测试")
    void testCheckEmail() throws Exception {
        // Mock服务层方法
        when(userService.isEmailAvailable("available@example.com")).thenReturn(true);
        when(userService.isEmailAvailable("existing@example.com")).thenReturn(false);

        // 测试可用的邮箱
        mockMvc.perform(get("/api/users/check-email")
                .param("email", "available@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 测试不可用的邮箱
        mockMvc.perform(get("/api/users/check-email")
                .param("email", "existing@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("参数验证测试 - 注册请求参数为空")
    void testRegisterValidation() throws Exception {
        // 准备无效的测试数据
        UserRegisterRequest request = new UserRegisterRequest();
        // 不设置任何字段，触发验证错误

        // 执行测试，期望返回400错误
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("参数验证测试 - 登录请求参数为空")
    void testLoginValidation() throws Exception {
        // 准备无效的测试数据
        UserLoginRequest request = new UserLoginRequest();
        // 不设置任何字段，触发验证错误

        // 执行测试，期望返回400错误
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}