package com.microservice.order.feign;

import com.microservice.common.dto.UserDTO;
import com.microservice.common.dto.UserOrderSyncRequest;
import com.microservice.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "user-service", path = "/api/user", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

    @GetMapping("/{userId}")
    Result<UserDTO> getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/username/{username}")
    Result<UserDTO> getUserByUsername(@PathVariable("username") String username);

    @PostMapping("/order-sync")
    Result<Boolean> syncOrderProfile(@RequestBody UserOrderSyncRequest request);
}
