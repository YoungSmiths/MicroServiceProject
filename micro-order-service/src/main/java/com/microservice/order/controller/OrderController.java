package com.microservice.order.controller;

import com.microservice.common.result.Result;
import com.microservice.order.dto.OrderPaymentResult;
import com.microservice.order.entity.Order;
import com.microservice.order.entity.OrderItem;
import com.microservice.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Order Service is OK!");
    }

    @PostMapping("/create")
    public Result<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setRemark(request.getRemark());
        
        List<OrderItem> items = request.toOrderItems();
        
        Order created = orderService.createOrder(order, items, request.isSimulateDistributedTxFailure());
        return Result.success("订单创建成功", created);
    }

    @GetMapping("/{orderId}")
    public Result<Order> getOrderById(@PathVariable Long orderId) {
        Order order = orderService.findByOrderId(orderId);
        if (order == null) {
            return Result.notFound("订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        List<Order> orders = orderService.findByUserId(userId);
        return Result.success(orders);
    }

    @GetMapping("/no/{orderNo}")
    public Result<Order> getOrderByOrderNo(@PathVariable String orderNo) {
        Order order = orderService.findByOrderNo(orderNo);
        if (order == null) {
            return Result.notFound("订单不存在");
        }
        return Result.success(order);
    }

    @PutMapping("/cancel/{orderId}")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        boolean success = orderService.cancelOrder(orderId);
        return success ? Result.success("订单取消成功", true) : Result.error("订单取消失败");
    }

    @PutMapping("/pay/{orderId}")
    public Result<OrderPaymentResult> payOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) OrderPayRequest request) {
        String scenario = request == null ? null : request.getScenario();
        OrderPaymentResult result = orderService.payOrder(orderId, scenario);
        if (result.isPaid()) {
            return Result.success("订单支付成功", result);
        }
        if (result.isDegraded()) {
            return Result.success("第三方支付已降级，订单保持待支付", result);
        }
        return Result.error(400, "订单支付失败", result);
    }

    @PutMapping("/refund/{orderId}")
    public Result<Boolean> refundOrder(@PathVariable Long orderId) {
        boolean success = orderService.refundOrder(orderId);
        return success ? Result.success("订单退款成功", true) : Result.error("订单退款失败");
    }
}
