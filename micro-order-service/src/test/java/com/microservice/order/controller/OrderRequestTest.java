package com.microservice.order.controller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单请求测试
 */
class OrderRequestTest {

    @Test
    void testOrderItemRequestToOrderItem() {
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(1000L);
        itemRequest.setProductName("iPhone 15 Pro");
        itemRequest.setProductImage("https://example.com/iphone.jpg");
        itemRequest.setPrice(new BigDecimal("8999.00"));
        itemRequest.setQuantity(2);

        var orderItem = itemRequest.toOrderItem();

        assertEquals(1000L, orderItem.getProductId());
        assertEquals("iPhone 15 Pro", orderItem.getProductName());
        assertEquals("https://example.com/iphone.jpg", orderItem.getProductImage());
        assertEquals(new BigDecimal("8999.00"), orderItem.getPrice());
        assertEquals(2, orderItem.getQuantity());
    }

    @Test
    void testOrderRequest() {
        OrderRequest request = new OrderRequest();
        request.setUserId(1L);
        request.setReceiverName("张三");
        request.setReceiverPhone("13800138000");
        request.setReceiverAddress("北京市朝阳区");
        request.setRemark("测试备注");
        request.setSimulateDistributedTxFailure(true);

        assertEquals(1L, request.getUserId());
        assertEquals("张三", request.getReceiverName());
        assertEquals("13800138000", request.getReceiverPhone());
        assertEquals("北京市朝阳区", request.getReceiverAddress());
        assertEquals("测试备注", request.getRemark());
        assertTrue(request.isSimulateDistributedTxFailure());
    }

    @Test
    void testOrderItemRequestValidation() {
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        
        assertThrows(Exception.class, () -> {
            throw new IllegalArgumentException("商品ID不能为空");
        });
    }
}
