package com.microservice.order.feign;

import com.microservice.common.dto.UserDTO;
import com.microservice.common.dto.UserOrderSyncRequest;
import com.microservice.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户服务 Feign 客户端降级处理
 */
@Slf4j
@Component
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public Result<UserDTO> getUserById(Long userId) {
        log.warn("调用用户服务失败，服务降级返回空用户, userId: {}", userId);
        return Result.error("用户服务暂时不可用");
    }

    @Override
    public Result<UserDTO> getUserByUsername(String username) {
        log.warn("调用用户服务失败，服务降级返回空用户, username: {}", username);
        return Result.error("用户服务暂时不可用");
    }

    @Override
    public Result<Boolean> syncOrderProfile(UserOrderSyncRequest request) {
        Long userId = request == null ? null : request.getUserId();
        String orderNo = request == null ? null : request.getOrderNo();
        log.warn("调用用户服务同步下单画像失败，服务降级返回, userId: {}, orderNo: {}", userId, orderNo);
        return Result.error("用户服务暂时不可用，无法同步下单画像");
    }
}
