package com.microservice.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microservice.order.dto.OrderPaymentResult;
import com.microservice.order.entity.Order;
import com.microservice.order.entity.OrderItem;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单（分布式事务）
     */
    Order createOrder(Order order, List<OrderItem> items, boolean simulateDistributedTxFailure);

    /**
     * 根据订单ID查询订单
     */
    Order findByOrderId(Long orderId);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> findByUserId(Long userId);

    /**
     * 根据订单编号查询订单
     */
    Order findByOrderNo(String orderNo);

    /**
     * 取消订单
     */
    boolean cancelOrder(Long orderId);

    /**
     * 支付订单
     */
    OrderPaymentResult payOrder(Long orderId, String scenario);

    /**
     * 退款订单
     */
    boolean refundOrder(Long orderId);
}
