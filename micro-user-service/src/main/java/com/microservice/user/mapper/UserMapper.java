package com.microservice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microservice.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
