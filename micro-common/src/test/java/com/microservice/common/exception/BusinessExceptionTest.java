package com.microservice.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常测试
 */
class BusinessExceptionTest {

    @Test
    void testConstructorWithCodeAndMessage() {
        BusinessException ex = new BusinessException(400, "参数错误");
        assertEquals(400, ex.getCode());
        assertEquals("参数错误", ex.getMessage());
    }

    @Test
    void testConstructorWithMessage() {
        BusinessException ex = new BusinessException("服务器错误");
        assertEquals(500, ex.getCode());
        assertEquals("服务器错误", ex.getMessage());
    }

    @Test
    void testBadRequest() {
        BusinessException ex = BusinessException.badRequest("参数校验失败");
        assertEquals(400, ex.getCode());
        assertEquals("参数校验失败", ex.getMessage());
    }

    @Test
    void testUnauthorized() {
        BusinessException ex = BusinessException.unauthorized("未登录");
        assertEquals(401, ex.getCode());
    }

    @Test
    void testForbidden() {
        BusinessException ex = BusinessException.forbidden("无权限");
        assertEquals(403, ex.getCode());
    }

    @Test
    void testNotFound() {
        BusinessException ex = BusinessException.notFound("用户不存在");
        assertEquals(404, ex.getCode());
    }

    @Test
    void testServerError() {
        BusinessException ex = BusinessException.serverError("系统错误");
        assertEquals(500, ex.getCode());
    }
}
