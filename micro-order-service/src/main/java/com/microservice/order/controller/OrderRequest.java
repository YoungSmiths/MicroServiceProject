package com.microservice.order.controller;

import com.microservice.order.entity.OrderItem;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建订单请求
 */
@Data
public class OrderRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "收货人姓名不能为空")
    @Length(max = 50, message = "收货人姓名不能超过50个字符")
    private String receiverName;

    @NotBlank(message = "收货人电话不能为空")
    @Length(max = 20, message = "收货人电话不能超过20个字符")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Length(max = 200, message = "收货地址不能超过200个字符")
    private String receiverAddress;

    @Length(max = 500, message = "备注不能超过500个字符")
    private String remark;

    /**
     * 是否在用户侧同步成功后继续制造异常，用于演示 Seata 全局回滚。
     */
    private boolean simulateDistributedTxFailure;

    @NotEmpty(message = "订单项不能为空")
    @Valid
    private List<OrderItemRequest> items;

    /**
     * 转换为 OrderItem 列表
     */
    public List<OrderItem> toOrderItems() {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemRequest : items) {
            orderItems.add(itemRequest.toOrderItem());
        }
        return orderItems;
    }

    /**
     * 订单项请求
     */
    @Data
    public static class OrderItemRequest {
        
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotBlank(message = "商品名称不能为空")
        @Length(max = 100, message = "商品名称不能超过100个字符")
        private String productName;

        private String productImage;

        @NotNull(message = "商品单价不能为空")
        private BigDecimal price;

        @NotNull(message = "购买数量不能为空")
        private Integer quantity;

        /**
         * 转换为 OrderItem 实体
         */
        public OrderItem toOrderItem() {
            OrderItem item = new OrderItem();
            item.setProductId(this.productId);
            item.setProductName(this.productName);
            item.setProductImage(this.productImage);
            item.setPrice(this.price);
            item.setQuantity(this.quantity);
            return item;
        }
    }
}
