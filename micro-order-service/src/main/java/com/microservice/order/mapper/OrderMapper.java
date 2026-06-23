package com.microservice.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microservice.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
