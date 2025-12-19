package com.zd.scuserservice.controller;

import com.zd.scuserservice.model.dto.request.*;
import com.zd.scuserservice.model.dto.response.*;
import com.zd.scuserservice.test.BaseIntegrationTest;
import com.zd.scuserservice.test.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户控制器集成测试
 * 覆盖所有API接口的正常和异常场景测试
 * 
 * @author system
 * @since 2024-01-21
 */
@DisplayName("用户管理API集成测试")
public class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/users";

    @Nested
    @DisplayName("用户注册接口测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册 - 应该返回201和用户信息")
        void testRegister_Success() throws Exception {
            // 准备测试数据
            UserRegisterRequest request = TestDataFactory.createValidRegisterRequest();

            // 执行请求
            MvcResult result = mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // 验证响应
            String responseBody = result.getResponse().getContentAsString();
            UserInfoResponse response = fromJson(responseBody, UserInfoResponse.class);
            
            assertNotNull(response);
            assertNotNull(response.getUserId());
            assertEquals(request.getUsername(), response.getUsername());
            assertEquals(request.getEmail(), response.getEmail());
            assertEquals(request.getRealName(), response.getRealName());
            assertEquals("user", response.getRole());
            assertEquals("pending_verification", response.getStatus());
        }

        @Test
        @DisplayName("注册失败 - 用户名为空")
        void testRegister_EmptyUsername() throws Exception {
            UserRegisterRequest request = TestDataFactory.createInvalidRegisterRequest_EmptyUsername();

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("注册失败 - 邮箱格式错误")
        void testRegister_InvalidEmail() throws Exception {
            UserRegisterRequest request = TestDataFactory.createInvalidRegisterRequest_InvalidEmail();

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("注册失败 - 密码不符合要求")
        void testRegister_WeakPassword() throws Exception {
            UserRegisterRequest request = TestDataFactory.createInvalidRegisterRequest_WeakPassword();

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("注册失败 - 密码确认不匹配")
        void testRegister_PasswordMismatch() throws Exception {
            UserRegisterRequest request = TestDataFactory.createInvalidRegisterRequest_PasswordMismatch();

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void testRegister_UsernameExists() throws Exception {
            UserRegisterRequest request = TestDataFactory.createValidRegisterRequest();
            
            // 第一次注册成功
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk());

            // 第二次注册相同用户名应该失败
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("用户登录接口测试")
    class LoginTests {

        @Test
        @DisplayName("正常登录 - 应该返回200和JWT Token")
        void testLogin_Success() throws Exception {
            // 先注册用户
            UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isOk());

            // 登录
            UserLoginRequest loginRequest = TestDataFactory.createValidLoginRequest();
            MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            // 验证响应
            String responseBody = result.getResponse().getContentAsString();
            UserLoginResponse response = fromJson(responseBody, UserLoginResponse.class);
            
            assertNotNull(response);
            assertNotNull(response.getAccessToken());
            assertNotNull(response.getUserInfo());
            assertEquals(registerRequest.getUsername(), response.getUserInfo().getUsername());
            assertEquals(registerRequest.getEmail(), response.getUserInfo().getEmail());
        }

        @Test
        @DisplayName("登录失败 - 用户名为空")
        void testLogin_EmptyLogin() throws Exception {
            UserLoginRequest request = TestDataFactory.createInvalidLoginRequest_EmptyLogin();

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("登录失败 - 密码为空")
        void testLogin_EmptyPassword() throws Exception {
            UserLoginRequest request = TestDataFactory.createInvalidLoginRequest_EmptyPassword();

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void testLogin_UserNotFound() throws Exception {
            UserLoginRequest request = TestDataFactory.createValidLoginRequest();
            request.setLogin("nonexistent");

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void testLogin_WrongPassword() throws Exception {
            // 先注册用户
            UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isOk());

            // 使用错误密码登录
            UserLoginRequest loginRequest = TestDataFactory.createValidLoginRequest();
            loginRequest.setPassword("WrongPassword123!");

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("用户名和邮箱可用性检查测试")
    class AvailabilityCheckTests {

        @Test
        @DisplayName("检查用户名可用性 - 可用")
        void testCheckUsername_Available() throws Exception {
            mockMvc.perform(get(BASE_URL + "/check-username")
                    .param("username", "availableuser"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("检查用户名可用性 - 不可用")
        void testCheckUsername_NotAvailable() throws Exception {
            // 先注册用户
            UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isOk());

            // 检查已存在的用户名
            mockMvc.perform(get(BASE_URL + "/check-username")
                    .param("username", registerRequest.getUsername()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @Test
        @DisplayName("检查邮箱可用性 - 可用")
        void testCheckEmail_Available() throws Exception {
            mockMvc.perform(get(BASE_URL + "/check-email")
                    .param("email", "available@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("检查邮箱可用性 - 不可用")
        void testCheckEmail_NotAvailable() throws Exception {
            // 先注册用户
            UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(registerRequest)))
                    .andExpect(status().isOk());

            // 检查已存在的邮箱
            mockMvc.perform(get(BASE_URL + "/check-email")
                    .param("email", registerRequest.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }
    }

    /**
     * 注册用户并获取JWT Token
     */
    private String registerAndLogin() throws Exception {
        // 注册用户
        UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
                .andExpect(status().isOk());

        // 登录获取Token
        UserLoginRequest loginRequest = TestDataFactory.createValidLoginRequest();
        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        UserLoginResponse response = fromJson(responseBody, UserLoginResponse.class);
        return response.getAccessToken();
    }
}