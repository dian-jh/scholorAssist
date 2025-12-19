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
 * 管理员功能测试类
 * 测试管理员权限相关的API接口
 * 
 * @author system
 * @since 2024-01-21
 */
@DisplayName("管理员功能API测试")
public class UserControllerAdminTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/users";
    private String adminToken;
    private String adminUserId;
    private String normalUserToken;
    private String normalUserId;

    @BeforeEach
    void setUpUsers() throws Exception {
        // 创建普通用户
        createNormalUser();
        
        // 创建管理员用户（通过直接设置数据库或其他方式）
        // 注意：在实际测试中，可能需要通过数据库直接插入管理员用户
        // 或者通过系统初始化的超级管理员来创建
        createAdminUser();
    }

    private void createNormalUser() throws Exception {
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
        this.normalUserToken = response.getAccessToken();
        this.normalUserId = response.getUserInfo().getUserId();
    }

    private void createAdminUser() throws Exception {
        // 创建管理员用户的注册请求
        UserRegisterRequest adminRegisterRequest = TestDataFactory.createValidRegisterRequest();
        adminRegisterRequest.setUsername("admin");
        adminRegisterRequest.setEmail("admin@example.com");
        
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(adminRegisterRequest)))
                .andExpect(status().isOk());

        // 登录获取Token
        UserLoginRequest adminLoginRequest = TestDataFactory.createValidLoginRequest();
        adminLoginRequest.setLogin("admin");
        
        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        UserLoginResponse response = fromJson(responseBody, UserLoginResponse.class);
        this.adminToken = response.getAccessToken();
        this.adminUserId = response.getUserInfo().getUserId();

        // 注意：这里需要通过其他方式将用户提升为管理员
        // 在实际测试中，可能需要：
        // 1. 直接操作数据库
        // 2. 使用系统初始化的超级管理员账号
        // 3. 通过配置文件设置测试管理员
    }

    @Nested
    @DisplayName("用户列表管理测试")
    class UserListManagementTests {

        @Test
        @DisplayName("管理员获取用户列表 - 成功")
        void testGetUserList_AdminSuccess() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page", "1")
                    .param("pageSize", "20"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserListResponse response = fromJson(responseBody, UserListResponse.class);
            
            assertNotNull(response);
            assertNotNull(response.getUsers());
            assertNotNull(response.getPageInfo());
            assertTrue(response.getUsers().size() >= 2); // 至少有管理员和普通用户
        }

        @Test
        @DisplayName("管理员搜索用户 - 成功")
        void testSearchUsers_AdminSuccess() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page", "1")
                    .param("pageSize", "20")
                    .param("search", "test")
                    .param("role", "user")
                    .param("status", "active"))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserListResponse response = fromJson(responseBody, UserListResponse.class);
            
            assertNotNull(response);
            assertNotNull(response.getUsers());
        }

        @Test
        @DisplayName("普通用户获取用户列表 - 权限不足")
        void testGetUserList_NormalUserForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + normalUserToken)
                    .param("page", "1")
                    .param("pageSize", "20"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未认证用户获取用户列表 - 未授权")
        void testGetUserList_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .param("page", "1")
                    .param("pageSize", "20"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("用户权限管理测试")
    class UserPermissionManagementTests {

        @Test
        @DisplayName("管理员更新用户权限 - 成功")
        void testUpdateUserPermissions_AdminSuccess() throws Exception {
            UserPermissionUpdateRequest request = TestDataFactory.createValidPermissionUpdateRequest();
            request.setRole("admin");
            request.setStatus("active");

            MvcResult result = mockMvc.perform(post(BASE_URL + "/" + normalUserId + "/permissions")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserInfoResponse response = fromJson(responseBody, UserInfoResponse.class);
            
            assertNotNull(response);
            assertEquals(request.getRole(), response.getRole());
            assertEquals(request.getStatus(), response.getStatus());
        }

        @Test
        @DisplayName("普通用户更新其他用户权限 - 权限不足")
        void testUpdateUserPermissions_NormalUserForbidden() throws Exception {
            UserPermissionUpdateRequest request = TestDataFactory.createValidPermissionUpdateRequest();

            mockMvc.perform(post(BASE_URL + "/" + adminUserId + "/permissions")
                    .header("Authorization", "Bearer " + normalUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("管理员更新不存在用户的权限 - 用户不存在")
        void testUpdateUserPermissions_UserNotFound() throws Exception {
            UserPermissionUpdateRequest request = TestDataFactory.createValidPermissionUpdateRequest();

            mockMvc.perform(post(BASE_URL + "/nonexistent_user_id/permissions")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("用户状态管理测试")
    class UserStatusManagementTests {

        @Test
        @DisplayName("管理员激活用户 - 成功")
        void testActivateUser_AdminSuccess() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + normalUserId + "/activate")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("管理员禁用用户 - 成功")
        void testSuspendUser_AdminSuccess() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + normalUserId + "/suspend")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("管理员删除用户 - 成功")
        void testDeleteUser_AdminSuccess() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + normalUserId)
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("普通用户激活其他用户 - 权限不足")
        void testActivateUser_NormalUserForbidden() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + adminUserId + "/activate")
                    .header("Authorization", "Bearer " + normalUserToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("普通用户禁用其他用户 - 权限不足")
        void testSuspendUser_NormalUserForbidden() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + adminUserId + "/suspend")
                    .header("Authorization", "Bearer " + normalUserToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("普通用户删除其他用户 - 权限不足")
        void testDeleteUser_NormalUserForbidden() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + adminUserId)
                    .header("Authorization", "Bearer " + normalUserToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("管理员操作不存在的用户 - 用户不存在")
        void testManageNonexistentUser_NotFound() throws Exception {
            String nonexistentUserId = "nonexistent_user_id";

            // 测试激活
            mockMvc.perform(post(BASE_URL + "/" + nonexistentUserId + "/activate")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());

            // 测试禁用
            mockMvc.perform(post(BASE_URL + "/" + nonexistentUserId + "/suspend")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());

            // 测试删除
            mockMvc.perform(delete(BASE_URL + "/" + nonexistentUserId)
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("分页和排序测试")
    class PaginationAndSortingTests {

        @Test
        @DisplayName("测试分页参数 - 有效参数")
        void testPagination_ValidParameters() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page", "1")
                    .param("pageSize", "10")
                    .param("sortBy", "created_at")
                    .param("sortOrder", "desc"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("测试分页参数 - 无效页码")
        void testPagination_InvalidPage() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page", "0")
                    .param("pageSize", "20"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("测试分页参数 - 无效页面大小")
        void testPagination_InvalidPageSize() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page", "1")
                    .param("pageSize", "101")) // 超过最大限制100
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("测试排序参数 - 有效排序字段")
        void testSorting_ValidFields() throws Exception {
            String[] validSortFields = {"created_at", "last_login_at", "username"};
            String[] validSortOrders = {"asc", "desc"};

            for (String sortBy : validSortFields) {
                for (String sortOrder : validSortOrders) {
                    mockMvc.perform(get(BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "1")
                            .param("pageSize", "20")
                            .param("sortBy", sortBy)
                            .param("sortOrder", sortOrder))
                            .andExpect(status().isOk());
                }
            }
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("管理员不能删除自己")
        void testAdminCannotDeleteSelf() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + adminUserId)
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("管理员不能禁用自己")
        void testAdminCannotSuspendSelf() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + adminUserId + "/suspend")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("测试大量用户列表查询性能")
        void testLargeUserListPerformance() throws Exception {
            // 这个测试可能需要预先创建大量用户数据
            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page", "1")
                    .param("pageSize", "100"))
                    .andExpect(status().isOk());
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // 确保查询在合理时间内完成（例如2秒）
            assertTrue(duration < 2000, "查询时间过长: " + duration + "ms");
        }
    }
}