package com.microservice.common.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 模拟第三方支付响应
 */
@Data
@Builder
public class ThirdPartyPaymentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;

    /**
     * SUCCESS / DEGRADED / FAILED / BIZ_FAIL
     */
    private String status;

    private String providerTradeNo;

    private String message;
}
