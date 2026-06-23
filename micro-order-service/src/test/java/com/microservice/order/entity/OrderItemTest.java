package com.microservice.order.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单项实体测试
 */
class OrderItemTest {

    @Test
    void testOrderItemEntity() {
        OrderItem item = new OrderItem();
        item.setItemId(1L);
        item.setOrderId(100L);
        item.setProductId(1000L);
        item.setProductName("iPhone 15 Pro");
        item.setProductImage("https://example.com/iphone.jpg");
        item.setPrice(new BigDecimal("8999.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("8999.00"));
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setDeleted(0);

        assertEquals(1L, item.getItemId());
        assertEquals(100L, item.getOrderId());
        assertEquals(1000L, item.getProductId());
        assertEquals("iPhone 15 Pro", item.getProductName());
        assertEquals(new BigDecimal("8999.00"), item.getPrice());
        assertEquals(1, item.getQuantity());
        assertEquals(new BigDecimal("8999.00"), item.getSubtotal());
    }

    @Test
    void testOrderItemBuilder() {
        LocalDateTime now = LocalDateTime.now();
        OrderItem item = OrderItem.builder()
                .itemId(1L)
                .orderId(100L)
                .productId(1000L)
                .productName("MacBook Pro")
                .price(new BigDecimal("14999.00"))
                .quantity(1)
                .subtotal(new BigDecimal("14999.00"))
                .createTime(now)
                .updateTime(now)
                .build();

        assertEquals(1L, item.getItemId());
        assertEquals(100L, item.getOrderId());
        assertEquals("MacBook Pro", item.getProductName());
        assertEquals(new BigDecimal("14999.00"), item.getSubtotal());
    }

    @Test
    void testCalculateSubtotal() {
        BigDecimal price = new BigDecimal("99.99");
        int quantity = 3;
        BigDecimal expectedSubtotal = price.multiply(BigDecimal.valueOf(quantity));
        
        assertEquals(new BigDecimal("299.97"), expectedSubtotal);
    }
}
