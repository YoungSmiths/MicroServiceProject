package com.microservice.order.service;

import com.microservice.common.dto.ThirdPartyPaymentResponse;
import com.microservice.order.entity.Order;
import com.microservice.order.integration.ThirdPartyPaymentClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 第三方支付治理编排服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyPaymentService {

    private final ThirdPartyPaymentClient thirdPartyPaymentClient;

    @CircuitBreaker(name = "thirdPartyPayment", fallbackMethod = "paymentFallback")
    @Bulkhead(name = "thirdPartyPaymentBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "paymentFallback")
    public ThirdPartyPaymentResponse charge(Order order, String scenario) {
        return thirdPartyPaymentClient.charge(order, scenario);
    }

    @SuppressWarnings("unused")
    private ThirdPartyPaymentResponse paymentFallback(Order order, String scenario, Throwable throwable) {
        log.warn("触发第三方支付降级, orderId: {}, scenario: {}, reason: {}",
                order == null ? null : order.getOrderId(),
                scenario,
                throwable == null ? "unknown" : throwable.getClass().getSimpleName());
        return ThirdPartyPaymentResponse.builder()
                .success(false)
                .status("DEGRADED")
                .message("第三方支付触发熔断/隔离降级，请稍后重试或转人工处理")
                .build();
    }
}
