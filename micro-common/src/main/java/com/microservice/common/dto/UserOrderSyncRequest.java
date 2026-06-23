package com.microservice.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户侧下单画像同步请求
 */
@Data
public class UserOrderSyncRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String orderNo;
}
