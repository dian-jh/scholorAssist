package com.zd.scuserservice.controller;

import com.zd.scuserservice.model.dto.request.*;
import com.zd.scuserservice.model.dto.response.*;
import com.zd.scuserservice.test.BaseIntegrationTest;
import com.zd.scuserservice.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 需要认证的用户API接口测试
 * 测试需要JWT Token的接口功能
 * 
 * @author system
 * @since 2024-01-21
 */
@DisplayName("需要认证的用户API测试")
public class UserControllerAuthenticatedTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/users";
    private String jwtToken;
    private String userId;

    @BeforeEach
    void setUpAuthentication() throws Exception {
        // 注册并登录获取JWT Token
        UserRegisterRequest registerRequest = TestDataFactory.createValidRegisterRequest();
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)))
                .andExpect(status().isOk());

        UserLoginRequest loginRequest = TestDataFactory.createValidLoginRequest();
        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        UserLoginResponse response = fromJson(responseBody, UserLoginResponse.class);
        this.jwtToken = response.getAccessToken();
        this.userId = response.getUserInfo().getUserId();
    }

    @Nested
    @DisplayName("用户信息管理测试")
    class UserProfileTests {

        @Test
        @DisplayName("获取用户信息 - 成功")
        void testGetProfile_Success() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL + "/profile")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserInfoResponse response = fromJson(responseBody, UserInfoResponse.class);
            
            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals("testuser", response.getUsername());
            assertEquals("test@example.com", response.getEmail());
        }

        @Test
        @DisplayName("获取用户信息 - 未认证")
        void testGetProfile_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("获取用户信息 - Token无效")
        void testGetProfile_InvalidToken() throws Exception {
            mockMvc.perform(get(BASE_URL + "/profile")
                    .header("Authorization", "Bearer invalid_token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("更新用户信息 - 成功")
        void testUpdateProfile_Success() throws Exception {
            UserUpdateRequest request = TestDataFactory.createValidUpdateRequest();

            MvcResult result = mockMvc.perform(post(BASE_URL + "/profile")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserInfoResponse response = fromJson(responseBody, UserInfoResponse.class);
            
            assertNotNull(response);
            assertEquals(request.getRealName(), response.getRealName());
            assertEquals(request.getAvatarUrl(), response.getAvatarUrl());
        }

        @Test
        @DisplayName("更新用户信息 - 未认证")
        void testUpdateProfile_Unauthorized() throws Exception {
            UserUpdateRequest request = TestDataFactory.createValidUpdateRequest();

            mockMvc.perform(post(BASE_URL + "/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("密码管理测试")
    class PasswordManagementTests {

        @Test
        @DisplayName("修改密码 - 成功")
        void testChangePassword_Success() throws Exception {
            ChangePasswordRequest request = TestDataFactory.createValidChangePasswordRequest();

            mockMvc.perform(post(BASE_URL + "/change-password")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("修改密码 - 当前密码错误")
        void testChangePassword_WrongCurrentPassword() throws Exception {
            ChangePasswordRequest request = TestDataFactory.createValidChangePasswordRequest();
            request.setCurrentPassword("WrongPassword123!");

            mockMvc.perform(post(BASE_URL + "/change-password")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("修改密码 - 新密码确认不匹配")
        void testChangePassword_NewPasswordMismatch() throws Exception {
            ChangePasswordRequest request = TestDataFactory.createValidChangePasswordRequest();
            request.setConfirmPassword("DifferentPassword123!");

            mockMvc.perform(post(BASE_URL + "/change-password")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("修改密码 - 未认证")
        void testChangePassword_Unauthorized() throws Exception {
            ChangePasswordRequest request = TestDataFactory.createValidChangePasswordRequest();

            mockMvc.perform(post(BASE_URL + "/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("用户权限测试")
    class UserPermissionTests {

        @Test
        @DisplayName("获取用户权限 - 成功")
        void testGetPermissions_Success() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL + "/permissions")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserPermissionResponse response = fromJson(responseBody, UserPermissionResponse.class);
            
            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals("user", response.getRole());
            assertNotNull(response.getPermissions());
            assertNotNull(response.getUsageLimits());
        }

        @Test
        @DisplayName("获取用户权限 - 未认证")
        void testGetPermissions_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/permissions"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("用户登出测试")
    class LogoutTests {

        @Test
        @DisplayName("用户登出 - 成功")
        void testLogout_Success() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("用户登出 - 未认证")
        void testLogout_Unauthorized() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("登出后Token失效")
        void testTokenInvalidAfterLogout() throws Exception {
            // 先登出
            mockMvc.perform(post(BASE_URL + "/logout")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk());

            // 再次使用Token应该失败
            mockMvc.perform(get(BASE_URL + "/profile")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("管理员功能测试")
    class AdminFunctionTests {

        private String adminToken;

        @BeforeEach
        void setUpAdmin() throws Exception {
            // 创建管理员用户（这里需要根据实际实现调整）
            // 暂时跳过，因为需要先有管理员权限才能创建管理员
        }

        @Test
        @DisplayName("普通用户获取用户列表 - 权限不足")
        void testGetUserList_InsufficientPermission() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + jwtToken)
                    .param("page", "1")
                    .param("pageSize", "20"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("普通用户更新其他用户权限 - 权限不足")
        void testUpdateUserPermissions_InsufficientPermission() throws Exception {
            UserPermissionUpdateRequest request = TestDataFactory.createValidPermissionUpdateRequest();

            mockMvc.perform(post(BASE_URL + "/other_user_id/permissions")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("用户状态管理测试")
    class UserStatusManagementTests {

        @Test
        @DisplayName("普通用户激活其他用户 - 权限不足")
        void testActivateUser_InsufficientPermission() throws Exception {
            mockMvc.perform(post(BASE_URL + "/other_user_id/activate")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("普通用户禁用其他用户 - 权限不足")
        void testSuspendUser_InsufficientPermission() throws Exception {
            mockMvc.perform(post(BASE_URL + "/other_user_id/suspend")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("普通用户删除其他用户 - 权限不足")
        void testDeleteUser_InsufficientPermission() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/other_user_id")
                    .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isForbidden());
        }
    }
}