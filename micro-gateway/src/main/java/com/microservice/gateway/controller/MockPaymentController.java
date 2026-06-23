package com.microservice.gateway.controller;

import com.microservice.common.dto.ThirdPartyPaymentRequest;
import com.microservice.common.dto.ThirdPartyPaymentResponse;
import com.microservice.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

/**
 * 本地模拟第三方支付平台，便于演示超时、重试和熔断降级。
 */
@RestController
@RequestMapping("/mock/payment")
public class MockPaymentController {

    @PostMapping("/charge")
    public ResponseEntity<Result<ThirdPartyPaymentResponse>> charge(@RequestBody ThirdPartyPaymentRequest request)
            throws InterruptedException {
        String scenario = request.getScenario() == null
                ? "SUCCESS"
                : request.getScenario().trim().toUpperCase(Locale.ROOT);

        if ("TIMEOUT".equals(scenario)) {
            Thread.sleep(2500L);
        }

        if ("FAIL".equals(scenario)) {
            ThirdPartyPaymentResponse failed = ThirdPartyPaymentResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("模拟第三方支付平台 500 异常")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(500, "第三方支付平台异常", failed));
        }

        if ("BIZ_FAIL".equals(scenario)) {
            ThirdPartyPaymentResponse failed = ThirdPartyPaymentResponse.builder()
                    .success(false)
                    .status("BIZ_FAIL")
                    .message("模拟第三方支付风控拒绝")
                    .build();
            return ResponseEntity.ok(Result.success("第三方支付平台返回业务失败", failed));
        }

        ThirdPartyPaymentResponse success = ThirdPartyPaymentResponse.builder()
                .success(true)
                .status("SUCCESS")
                .providerTradeNo("MOCK-" + UUID.randomUUID().toString().replace("-", ""))
                .message("模拟第三方支付成功")
                .build();
        return ResponseEntity.ok(Result.success("支付成功", success));
    }
}
