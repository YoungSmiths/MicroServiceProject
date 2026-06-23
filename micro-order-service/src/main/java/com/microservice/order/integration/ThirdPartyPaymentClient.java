package com.microservice.order.integration;

import com.microservice.common.dto.ThirdPartyPaymentRequest;
import com.microservice.common.dto.ThirdPartyPaymentResponse;
import com.microservice.common.exception.BusinessException;
import com.microservice.common.result.Result;
import com.microservice.order.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 第三方支付 HTTP 客户端。
 */
@Slf4j
@Component
public class ThirdPartyPaymentClient {

    private final RestTemplate paymentRestTemplate;

    @Value("${business.third-party.payment-base-url:http://127.0.0.1:8080/mock/payment}")
    private String paymentBaseUrl;

    public ThirdPartyPaymentClient(@Qualifier("paymentRestTemplate") RestTemplate paymentRestTemplate) {
        this.paymentRestTemplate = paymentRestTemplate;
    }

    @Retryable(
            include = Exception.class,
            maxAttemptsExpression = "${business.third-party.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${business.third-party.retry-delay:200}",
                    multiplier = 2.0
            )
    )
    public ThirdPartyPaymentResponse charge(Order order, String scenario) {
        int attempt = RetrySynchronizationManager.getContext() == null
                ? 1
                : RetrySynchronizationManager.getContext().getRetryCount() + 1;
        log.info("调用第三方支付平台, orderId: {}, orderNo: {}, scenario: {}, attempt: {}",
                order.getOrderId(), order.getOrderNo(), scenario, attempt);

        ThirdPartyPaymentRequest request = new ThirdPartyPaymentRequest();
        request.setOrderId(order.getOrderId());
        request.setOrderNo(order.getOrderNo());
        request.setUserId(order.getUserId());
        request.setAmount(order.getTotalAmount());
        request.setScenario(scenario);

        ResponseEntity<Result<ThirdPartyPaymentResponse>> response = paymentRestTemplate.exchange(
                paymentBaseUrl + "/charge",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<Result<ThirdPartyPaymentResponse>>() {
                }
        );

        Result<ThirdPartyPaymentResponse> result = response.getBody();
        if (result == null || result.getData() == null) {
            throw BusinessException.serverError("第三方支付平台返回空响应");
        }
        return result.getData();
    }
}
