package com.microservice.user.controller;

import com.microservice.common.dto.UserOrderSyncRequest;
import com.microservice.common.result.Result;
import com.microservice.user.entity.User;
import com.microservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户控制器测试
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setRealName("测试用户");
        testUser.setStatus(1);
    }

    @Test
    void testHello() {
        Result<String> result = userController.hello();
        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("User Service is OK!", result.getData());
    }

    @Test
    void testGetUserById_WhenUserExists() {
        when(userService.findByUserId(1L)).thenReturn(testUser);

        Result<User> result = userController.getUserById(1L);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertNotNull(result.getData());
        assertEquals("testuser", result.getData().getUsername());
        verify(userService, times(1)).findByUserId(1L);
    }

    @Test
    void testGetUserById_WhenUserNotExists() {
        when(userService.findByUserId(999L)).thenReturn(null);

        Result<User> result = userController.getUserById(999L);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void testGetUserByUsername_WhenUserExists() {
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        Result<User> result = userController.getUserByUsername("testuser");

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertNotNull(result.getData());
        assertEquals("testuser", result.getData().getUsername());
    }

    @Test
    void testGetUserByUsername_WhenUserNotExists() {
        when(userService.findByUsername("notexist")).thenReturn(null);

        Result<User> result = userController.getUserByUsername("notexist");

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getCode());
    }

    @Test
    void testRegisterUser() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("password");
        
        when(userService.registerUser(any(User.class))).thenReturn(newUser);

        Result<User> result = userController.register(newUser);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("注册成功", result.getMessage());
        assertNotNull(result.getData());
        verify(userService, times(1)).registerUser(any(User.class));
    }

    @Test
    void testUpdateUser_Success() {
        when(userService.updateUser(any(User.class))).thenReturn(true);

        Result<Boolean> result = userController.updateUser(testUser);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("更新成功", result.getMessage());
        assertTrue(result.getData());
    }

    @Test
    void testUpdateUser_Failure() {
        when(userService.updateUser(any(User.class))).thenReturn(false);

        Result<Boolean> result = userController.updateUser(testUser);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getCode());
        assertEquals("更新失败", result.getMessage());
    }

    @Test
    void testDeleteUser_Success() {
        when(userService.deleteUser(1L)).thenReturn(true);

        Result<Boolean> result = userController.deleteUser(1L);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("删除成功", result.getMessage());
        assertTrue(result.getData());
    }

    @Test
    void testDeleteUser_Failure() {
        when(userService.deleteUser(999L)).thenReturn(false);

        Result<Boolean> result = userController.deleteUser(999L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getCode());
        assertEquals("删除失败", result.getMessage());
    }

    @Test
    void testSyncOrderProfile_Success() {
        UserOrderSyncRequest request = new UserOrderSyncRequest();
        request.setUserId(1L);
        request.setOrderNo("ORD123456");

        when(userService.syncOrderProfile(any(UserOrderSyncRequest.class))).thenReturn(true);

        Result<Boolean> result = userController.syncOrderProfile(request);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("同步用户下单画像成功", result.getMessage());
        assertTrue(result.getData());
    }
}
