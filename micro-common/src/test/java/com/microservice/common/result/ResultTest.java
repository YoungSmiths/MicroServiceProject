package com.microservice.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一返回结果测试
 */
class ResultTest {

    @Test
    void testSuccess() {
        Result<String> result = Result.success();
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSuccessWithData() {
        Result<String> result = Result.success("test data");
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("test data", result.getData());
    }

    @Test
    void testSuccessWithMessageAndData() {
        Result<String> result = Result.success("custom message", "data");
        assertEquals(200, result.getCode());
        assertEquals("custom message", result.getMessage());
        assertEquals("data", result.getData());
    }

    @Test
    void testError() {
        Result<String> result = Result.error();
        assertEquals(500, result.getCode());
        assertEquals("操作失败", result.getMessage());
    }

    @Test
    void testErrorWithMessage() {
        Result<String> result = Result.error("自定义错误");
        assertEquals(500, result.getCode());
        assertEquals("自定义错误", result.getMessage());
    }

    @Test
    void testErrorWithCodeAndMessage() {
        Result<String> result = Result.error(400, "参数错误");
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
    }

    @Test
    void testBadRequest() {
        Result<String> result = Result.badRequest("参数校验失败");
        assertEquals(400, result.getCode());
        assertEquals("参数校验失败", result.getMessage());
    }

    @Test
    void testUnauthorized() {
        Result<String> result = Result.unauthorized("未授权");
        assertEquals(401, result.getCode());
    }

    @Test
    void testForbidden() {
        Result<String> result = Result.forbidden("禁止访问");
        assertEquals(403, result.getCode());
    }

    @Test
    void testNotFound() {
        Result<String> result = Result.notFound("资源不存在");
        assertEquals(404, result.getCode());
    }
}
