package com.microservice.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microservice.common.dto.UserOrderSyncRequest;
import com.microservice.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 根据用户ID查询用户
     */
    User findByUserId(Long userId);

    /**
     * 注册用户
     */
    User registerUser(User user);

    /**
     * 更新用户信息
     */
    boolean updateUser(User user);

    /**
     * 删除用户
     */
    boolean deleteUser(Long userId);

    /**
     * 同步用户侧下单统计
     */
    boolean syncOrderProfile(UserOrderSyncRequest request);
}
