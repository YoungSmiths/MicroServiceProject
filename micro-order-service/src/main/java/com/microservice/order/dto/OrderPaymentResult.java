package com.microservice.order.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单支付结果。
 */
@Data
@Builder
public class OrderPaymentResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private String orderNo;

    private Integer orderStatus;

    private boolean paid;

    private boolean degraded;

    private String paymentStatus;

    private String providerTradeNo;

    private String message;
}
