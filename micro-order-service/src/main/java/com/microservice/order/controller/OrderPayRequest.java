package com.microservice.order.controller;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单支付请求。
 */
@Data
public class OrderPayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * SUCCESS / FAIL / TIMEOUT / BIZ_FAIL
     */
    private String scenario;
}
