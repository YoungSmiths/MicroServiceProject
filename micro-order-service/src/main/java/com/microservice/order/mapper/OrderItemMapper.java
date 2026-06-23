package com.microservice.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microservice.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
