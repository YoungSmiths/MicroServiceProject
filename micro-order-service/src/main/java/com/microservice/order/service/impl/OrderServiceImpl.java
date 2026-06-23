package com.microservice.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microservice.common.config.DistributedLock;
import com.microservice.common.constant.CacheConstants;
import com.microservice.common.dto.ThirdPartyPaymentResponse;
import com.microservice.common.dto.UserDTO;
import com.microservice.common.dto.UserOrderSyncRequest;
import com.microservice.common.exception.BusinessException;
import com.microservice.common.result.Result;
import com.microservice.order.dto.OrderPaymentResult;
import com.microservice.order.entity.Order;
import com.microservice.order.entity.OrderItem;
import com.microservice.order.feign.UserFeignClient;
import com.microservice.order.mapper.OrderMapper;
import com.microservice.order.mapper.OrderItemMapper;
import com.microservice.order.service.OrderService;
import com.microservice.order.service.ThirdPartyPaymentService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ThirdPartyPaymentService thirdPartyPaymentService;

    @Override
    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order, List<OrderItem> items, boolean simulateDistributedTxFailure) {
        log.info("开始创建订单, XID: {}", RootContext.getXID());

        String lockKey = CacheConstants.ORDER_LOCK_PREFIX + "create:" + order.getUserId();

        boolean lockAcquired = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("系统繁忙，请稍后重试");
        }

        try {
            validateUserExists(order.getUserId());
            String orderNo = generateOrderNo();
            order.setOrderNo(orderNo);
            order.setStatus(0);

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderItem item : items) {
                item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                totalAmount = totalAmount.add(item.getSubtotal());
            }
            order.setTotalAmount(totalAmount);

            orderMapper.insert(order);
            log.info("订单保存成功, orderId: {}, orderNo: {}", order.getOrderId(), orderNo);

            for (OrderItem item : items) {
                item.setOrderId(order.getOrderId());
                orderItemMapper.insert(item);
                log.info("订单项保存成功, itemId: {}", item.getItemId());
            }

            syncUserOrderProfile(order);
            if (simulateDistributedTxFailure) {
                throw BusinessException.serverError("模拟用户侧同步成功后订单服务异常，验证 Seata 全局回滚");
            }

            cacheOrder(order);
            return order;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Override
    public Order findByOrderId(Long orderId) {
        String cacheKey = CacheConstants.ORDER_CACHE_PREFIX + "id:" + orderId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取订单: {}", orderId);
            return orderMapper.selectById(orderId);
        }

        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            redisTemplate.opsForValue().set(cacheKey, orderId.toString(),
                    CacheConstants.ORDER_CACHE_TTL, TimeUnit.SECONDS);
        }
        return order;
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Order findByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId) {
        log.info("取消订单, orderId: {}, XID: {}", orderId, RootContext.getXID());
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        if (order.getStatus() != 0) {
            throw BusinessException.badRequest("只有待支付的订单才能取消");
        }
        
        String lockKey = CacheConstants.ORDER_LOCK_PREFIX + "cancel:" + orderId;
        
        boolean lockAcquired = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("系统繁忙，请稍后重试");
        }
        
        try {
            order.setStatus(2); // 已取消
            int result = orderMapper.updateById(order);
            log.info("订单取消成功, orderId: {}", orderId);
            return result > 0;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentResult payOrder(Long orderId, String scenario) {
        log.info("支付订单, orderId: {}, XID: {}", orderId, RootContext.getXID());

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        if (order.getStatus() != 0) {
            throw BusinessException.badRequest("订单状态不正确");
        }

        String lockKey = CacheConstants.ORDER_LOCK_PREFIX + "pay:" + orderId;

        boolean lockAcquired = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("系统繁忙，请稍后重试");
        }

        try {
            ThirdPartyPaymentResponse response = thirdPartyPaymentService.charge(order, normalizeScenario(scenario));
            if (response.isSuccess()) {
                order.setStatus(1);
                order.setRemark(appendRemark(order.getRemark(),
                        "providerTradeNo=" + response.getProviderTradeNo()));
                orderMapper.updateById(order);
                cacheOrder(order);
                log.info("订单支付成功, orderId: {}, providerTradeNo: {}",
                        orderId, response.getProviderTradeNo());
                return buildPaymentResult(order, response, true, false);
            }

            order.setRemark(appendRemark(order.getRemark(), response.getMessage()));
            orderMapper.updateById(order);
            cacheOrder(order);
            log.warn("订单支付未完成, orderId: {}, paymentStatus: {}, message: {}",
                    orderId, response.getStatus(), response.getMessage());
            return buildPaymentResult(order, response, false,
                    "DEGRADED".equalsIgnoreCase(response.getStatus()));
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundOrder(Long orderId) {
        log.info("退款订单, orderId: {}, XID: {}", orderId, RootContext.getXID());
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        if (order.getStatus() != 1) {
            throw BusinessException.badRequest("只有已支付的订单才能退款");
        }
        
        String lockKey = CacheConstants.ORDER_LOCK_PREFIX + "refund:" + orderId;
        
        boolean lockAcquired = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("系统繁忙，请稍后重试");
        }
        
        try {
            order.setStatus(4);
            int result = orderMapper.updateById(order);
            cacheOrder(order);
            log.info("订单退款成功, orderId: {}", orderId);
            return result > 0;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private void validateUserExists(Long userId) {
        Result<UserDTO> userResult = userFeignClient.getUserById(userId);
        if (userResult == null || userResult.getCode() != 200 || userResult.getData() == null) {
            throw BusinessException.badRequest("用户不存在或用户服务不可用，无法创建订单");
        }
    }

    private void syncUserOrderProfile(Order order) {
        UserOrderSyncRequest request = new UserOrderSyncRequest();
        request.setUserId(order.getUserId());
        request.setOrderNo(order.getOrderNo());

        Result<Boolean> syncResult = userFeignClient.syncOrderProfile(request);
        if (syncResult == null || syncResult.getCode() != 200 || !Boolean.TRUE.equals(syncResult.getData())) {
            throw BusinessException.serverError("同步用户下单画像失败，订单创建回滚");
        }
    }

    private void cacheOrder(Order order) {
        String cacheKey = CacheConstants.ORDER_CACHE_PREFIX + "id:" + order.getOrderId();
        redisTemplate.opsForValue().set(cacheKey, order.getOrderId().toString(),
                CacheConstants.ORDER_CACHE_TTL, TimeUnit.SECONDS);
    }

    private String normalizeScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            return "SUCCESS";
        }
        return scenario.trim().toUpperCase(Locale.ROOT);
    }

    private OrderPaymentResult buildPaymentResult(
            Order order,
            ThirdPartyPaymentResponse response,
            boolean paid,
            boolean degraded) {
        return OrderPaymentResult.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .orderStatus(order.getStatus())
                .paid(paid)
                .degraded(degraded)
                .paymentStatus(response.getStatus())
                .providerTradeNo(response.getProviderTradeNo())
                .message(response.getMessage())
                .build();
    }

    private String appendRemark(String originalRemark, String extraRemark) {
        if (extraRemark == null || extraRemark.isBlank()) {
            return originalRemark;
        }
        if (originalRemark == null || originalRemark.isBlank()) {
            return extraRemark;
        }
        return originalRemark + " | " + extraRemark;
    }

    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        // 格式: 时间戳 + 随机数
        return System.currentTimeMillis() + "" + (int) ((Math.random() * 9000) + 1000);
    }
}
