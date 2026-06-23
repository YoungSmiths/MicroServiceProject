package com.microservice.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模拟第三方支付请求
 */
@Data
public class ThirdPartyPaymentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    /**
     * SUCCESS / FAIL / TIMEOUT / BIZ_FAIL
     */
    private String scenario;
}
