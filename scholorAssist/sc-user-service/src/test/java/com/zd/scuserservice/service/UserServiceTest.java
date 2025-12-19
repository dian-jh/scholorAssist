package com.zd.scuserservice.service;

import com.zd.sccommon.common.BusinessException;
import com.zd.scuserservice.manager.UserManager;
import com.zd.scuserservice.model.domain.User;
import com.zd.scuserservice.model.dto.request.UserRegisterRequest;
import com.zd.scuserservice.model.dto.response.UserInfoResponse;
import com.zd.scuserservice.service.impl.UserServiceImpl;
import com.zd.scuserservice.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 
 * @author system
 * @since 2024-01-21
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserManager userManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegisterRequest registerRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        registerRequest = new UserRegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("TestPass123!");
        registerRequest.setConfirmPassword("TestPass123!");
        registerRequest.setRealName("测试用户");

        mockUser = User.builder()
                .id(1L)
                .userId("user_123456789")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .realName("测试用户")
                .role("user")
                .status("pending_verification")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testRegister_Success() {
        // Given
        when(userManager.existsByUsername(anyString())).thenReturn(false);
        when(userManager.existsByEmail(anyString())).thenReturn(false);
        when(userManager.createUser(any(User.class))).thenReturn(mockUser);

        // When
        UserInfoResponse response = userService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("user", response.getRole());
        assertEquals("pending_verification", response.getStatus());

        verify(userManager).existsByUsername("testuser");
        verify(userManager).existsByEmail("test@example.com");
        verify(userManager).createUser(any(User.class));
    }

    @Test
    void testRegister_UsernameExists() {
        // Given
        when(userManager.existsByUsername(anyString())).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.register(registerRequest));
        
        assertEquals(400, exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());

        verify(userManager).existsByUsername("testuser");
        verify(userManager, never()).existsByEmail(anyString());
        verify(userManager, never()).createUser(any(User.class));
    }

    @Test
    void testRegister_EmailExists() {
        // Given
        when(userManager.existsByUsername(anyString())).thenReturn(false);
        when(userManager.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.register(registerRequest));
        
        assertEquals(400, exception.getCode());
        assertEquals("邮箱已被注册", exception.getMessage());

        verify(userManager).existsByUsername("testuser");
        verify(userManager).existsByEmail("test@example.com");
        verify(userManager, never()).createUser(any(User.class));
    }

    @Test
    void testRegister_PasswordMismatch() {
        // Given
        registerRequest.setConfirmPassword("DifferentPassword123!");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.register(registerRequest));
        
        assertEquals(400, exception.getCode());
        assertEquals("密码和确认密码不一致", exception.getMessage());

        verify(userManager, never()).existsByUsername(anyString());
        verify(userManager, never()).existsByEmail(anyString());
        verify(userManager, never()).createUser(any(User.class));
    }

    @Test
    void testGetUserInfo_Success() {
        // Given
        String userId = "user_123456789";
        when(userManager.getUserByUserId(userId)).thenReturn(mockUser);

        // When
        UserInfoResponse response = userService.getUserInfo(userId);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());

        verify(userManager).getUserByUserId(userId);
    }

    @Test
    void testGetUserInfo_UserNotFound() {
        // Given
        String userId = "user_nonexistent";
        when(userManager.getUserByUserId(userId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.getUserInfo(userId));
        
        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());

        verify(userManager).getUserByUserId(userId);
    }

    @Test
    void testIsUsernameAvailable_Available() {
        // Given
        when(userManager.existsByUsername("newuser")).thenReturn(false);

        // When
        boolean available = userService.isUsernameAvailable("newuser");

        // Then
        assertTrue(available);
        verify(userManager).existsByUsername("newuser");
    }

    @Test
    void testIsUsernameAvailable_NotAvailable() {
        // Given
        when(userManager.existsByUsername("existinguser")).thenReturn(true);

        // When
        boolean available = userService.isUsernameAvailable("existinguser");

        // Then
        assertFalse(available);
        verify(userManager).existsByUsername("existinguser");
    }

    @Test
    void testIsEmailAvailable_Available() {
        // Given
        when(userManager.existsByEmail("new@example.com")).thenReturn(false);

        // When
        boolean available = userService.isEmailAvailable("new@example.com");

        // Then
        assertTrue(available);
        verify(userManager).existsByEmail("new@example.com");
    }

    @Test
    void testIsEmailAvailable_NotAvailable() {
        // Given
        when(userManager.existsByEmail("existing@example.com")).thenReturn(true);

        // When
        boolean available = userService.isEmailAvailable("existing@example.com");

        // Then
        assertFalse(available);
        verify(userManager).existsByEmail("existing@example.com");
    }

    @Test
    void testActivateUser_Success() {
        // Given
        String userId = "user_123456789";
        User pendingUser = User.builder()
                .userId(userId)
                .status("pending_verification")
                .build();
        
        when(userManager.getUserByUserId(userId)).thenReturn(pendingUser);
        when(userManager.updateStatus(userId, "active")).thenReturn(true);

        // When
        boolean result = userService.activateUser(userId);

        // Then
        assertTrue(result);
        verify(userManager).getUserByUserId(userId);
        verify(userManager).updateStatus(userId, "active");
    }

    @Test
    void testActivateUser_UserNotFound() {
        // Given
        String userId = "user_nonexistent";
        when(userManager.getUserByUserId(userId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.activateUser(userId));
        
        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());

        verify(userManager).getUserByUserId(userId);
        verify(userManager, never()).updateStatus(anyString(), anyString());
    }

    @Test
    void testActivateUser_AlreadyActive() {
        // Given
        String userId = "user_123456789";
        User activeUser = User.builder()
                .userId(userId)
                .status("active")
                .build();
        
        when(userManager.getUserByUserId(userId)).thenReturn(activeUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.activateUser(userId));
        
        assertEquals(400, exception.getCode());
        assertEquals("用户已经是激活状态", exception.getMessage());

        verify(userManager).getUserByUserId(userId);
        verify(userManager, never()).updateStatus(anyString(), anyString());
    }
}