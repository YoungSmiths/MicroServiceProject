package com.microservice.order.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单实体测试
 */
class OrderTest {

    @Test
    void testOrderEntity() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(100L);
        order.setOrderNo("ORD20240101123456789");
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(0);
        order.setReceiverName("张三");
        order.setReceiverPhone("13800138000");
        order.setReceiverAddress("北京市朝阳区xxx");
        order.setRemark("请尽快发货");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);

        assertEquals(1L, order.getOrderId());
        assertEquals(100L, order.getUserId());
        assertEquals("ORD20240101123456789", order.getOrderNo());
        assertEquals(new BigDecimal("99.99"), order.getTotalAmount());
        assertEquals(0, order.getStatus());
        assertEquals("张三", order.getReceiverName());
        assertEquals("13800138000", order.getReceiverPhone());
        assertEquals(0, order.getDeleted());
    }

    @Test
    void testOrderBuilder() {
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .orderId(1L)
                .userId(100L)
                .orderNo("ORD123")
                .totalAmount(new BigDecimal("199.99"))
                .status(1)
                .receiverName("李四")
                .receiverPhone("13900139000")
                .receiverAddress("上海市浦东新区xxx")
                .createTime(now)
                .updateTime(now)
                .build();

        assertEquals(1L, order.getOrderId());
        assertEquals(100L, order.getUserId());
        assertEquals(new BigDecimal("199.99"), order.getTotalAmount());
        assertEquals(1, order.getStatus());
    }
}
