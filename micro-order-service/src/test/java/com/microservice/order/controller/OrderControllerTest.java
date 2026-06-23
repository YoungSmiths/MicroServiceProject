package com.microservice.order.controller;

import com.microservice.common.result.Result;
import com.microservice.order.dto.OrderPaymentResult;
import com.microservice.order.entity.Order;
import com.microservice.order.entity.OrderItem;
import com.microservice.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单控制器测试
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private Order testOrder;
    private OrderItem testItem;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setUserId(100L);
        testOrder.setOrderNo("ORD123456");
        testOrder.setTotalAmount(new BigDecimal("199.99"));
        testOrder.setStatus(0);
        testOrder.setReceiverName("张三");
        testOrder.setReceiverPhone("13800138000");
        testOrder.setReceiverAddress("北京市朝阳区");

        testItem = new OrderItem();
        testItem.setItemId(1L);
        testItem.setOrderId(1L);
        testItem.setProductId(1000L);
        testItem.setProductName("iPhone 15");
        testItem.setPrice(new BigDecimal("199.99"));
        testItem.setQuantity(1);
        testItem.setSubtotal(new BigDecimal("199.99"));
    }

    @Test
    void testHello() {
        Result<String> result = orderController.hello();
        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("Order Service is OK!", result.getData());
    }

    @Test
    void testCreateOrder() {
        OrderRequest request = new OrderRequest();
        request.setUserId(100L);
        request.setReceiverName("张三");
        request.setReceiverPhone("13800138000");
        request.setReceiverAddress("北京市朝阳区");
        
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(1000L);
        itemRequest.setProductName("iPhone 15");
        itemRequest.setPrice(new BigDecimal("199.99"));
        itemRequest.setQuantity(1);
        request.setItems(Arrays.asList(itemRequest));

        when(orderService.createOrder(any(Order.class), anyList(), eq(false))).thenReturn(testOrder);

        Result<Order> result = orderController.createOrder(request);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("订单创建成功", result.getMessage());
        assertNotNull(result.getData());
        verify(orderService, times(1)).createOrder(any(Order.class), anyList(), eq(false));
    }

    @Test
    void testGetOrderById_WhenOrderExists() {
        when(orderService.findByOrderId(1L)).thenReturn(testOrder);

        Result<Order> result = orderController.getOrderById(1L);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertNotNull(result.getData());
        assertEquals("ORD123456", result.getData().getOrderNo());
    }

    @Test
    void testGetOrderById_WhenOrderNotExists() {
        when(orderService.findByOrderId(999L)).thenReturn(null);

        Result<Order> result = orderController.getOrderById(999L);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getCode());
        assertEquals("订单不存在", result.getMessage());
    }

    @Test
    void testGetOrdersByUserId() {
        List<Order> orders = Arrays.asList(testOrder);
        when(orderService.findByUserId(100L)).thenReturn(orders);

        Result<List<Order>> result = orderController.getOrdersByUserId(100L);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void testGetOrderByOrderNo() {
        when(orderService.findByOrderNo("ORD123456")).thenReturn(testOrder);

        Result<Order> result = orderController.getOrderByOrderNo("ORD123456");

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void testCancelOrder_Success() {
        when(orderService.cancelOrder(1L)).thenReturn(true);

        Result<Boolean> result = orderController.cancelOrder(1L);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("订单取消成功", result.getMessage());
        assertTrue(result.getData());
    }

    @Test
    void testCancelOrder_Failure() {
        when(orderService.cancelOrder(999L)).thenReturn(false);

        Result<Boolean> result = orderController.cancelOrder(999L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getCode());
        assertEquals("订单取消失败", result.getMessage());
    }

    @Test
    void testPayOrder_Success() {
        OrderPaymentResult paymentResult = OrderPaymentResult.builder()
                .orderId(1L)
                .orderNo("ORD123456")
                .orderStatus(1)
                .paid(true)
                .degraded(false)
                .paymentStatus("SUCCESS")
                .providerTradeNo("MOCK-123")
                .message("支付成功")
                .build();
        when(orderService.payOrder(1L, null)).thenReturn(paymentResult);

        Result<OrderPaymentResult> result = orderController.payOrder(1L, null);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("订单支付成功", result.getMessage());
        assertTrue(result.getData().isPaid());
    }

    @Test
    void testPayOrder_Degraded() {
        OrderPayRequest request = new OrderPayRequest();
        request.setScenario("TIMEOUT");

        OrderPaymentResult paymentResult = OrderPaymentResult.builder()
                .orderId(1L)
                .orderNo("ORD123456")
                .orderStatus(0)
                .paid(false)
                .degraded(true)
                .paymentStatus("DEGRADED")
                .message("第三方支付已降级")
                .build();
        when(orderService.payOrder(1L, "TIMEOUT")).thenReturn(paymentResult);

        Result<OrderPaymentResult> result = orderController.payOrder(1L, request);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertTrue(result.getData().isDegraded());
    }

    @Test
    void testRefundOrder_Success() {
        when(orderService.refundOrder(1L)).thenReturn(true);

        Result<Boolean> result = orderController.refundOrder(1L);

        assertEquals(HttpStatus.OK.value(), result.getCode());
        assertEquals("订单退款成功", result.getMessage());
        assertTrue(result.getData());
    }
}
