package com.microservice.user.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户实体测试
 */
class UserTest {

    @Test
    void testUserEntity() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setRealName("张三");
        user.setPhone("13800138000");
        user.setEmail("test@example.com");
        user.setGender(1);
        user.setStatus(1);
        user.setOrderCount(2);
        user.setLastOrderNo("ORD123456");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);

        assertEquals(1L, user.getUserId());
        assertEquals("testuser", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertEquals("张三", user.getRealName());
        assertEquals("13800138000", user.getPhone());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(1, user.getGender());
        assertEquals(1, user.getStatus());
        assertEquals(2, user.getOrderCount());
        assertEquals("ORD123456", user.getLastOrderNo());
        assertEquals(0, user.getDeleted());
    }

    @Test
    void testUserBuilder() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .userId(1L)
                .username("testuser")
                .password("password")
                .realName("李四")
                .phone("13900139000")
                .email("li@example.com")
                .gender(0)
                .status(1)
                .orderCount(5)
                .lastOrderNo("ORD999")
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();

        assertEquals(1L, user.getUserId());
        assertEquals("testuser", user.getUsername());
        assertEquals("李四", user.getRealName());
        assertEquals(0, user.getGender());
        assertEquals(5, user.getOrderCount());
    }
}
