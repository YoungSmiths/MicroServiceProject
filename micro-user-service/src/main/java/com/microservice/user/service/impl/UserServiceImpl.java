package com.microservice.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microservice.common.config.DistributedLock;
import com.microservice.common.constant.CacheConstants;
import com.microservice.common.dto.UserOrderSyncRequest;
import com.microservice.common.exception.BusinessException;
import com.microservice.common.utils.JsonUtil;
import com.microservice.user.entity.User;
import com.microservice.user.mapper.UserMapper;
import com.microservice.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public User findByUsername(String username) {
        String cacheKey = CacheConstants.USER_CACHE_PREFIX + username;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取用户: {}", username);
            return JsonUtil.fromJson(cached, User.class);
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(user), 
                    CacheConstants.USER_CACHE_TTL, TimeUnit.SECONDS);
        }
        return user;
    }

    @Override
    public User findByUserId(Long userId) {
        String cacheKey = CacheConstants.USER_CACHE_PREFIX + "id:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取用户ID: {}", userId);
            return JsonUtil.fromJson(cached, User.class);
        }

        User user = userMapper.selectById(userId);
        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJson(user),
                    CacheConstants.USER_CACHE_TTL, TimeUnit.SECONDS);
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User registerUser(User user) {
        String lockKey = CacheConstants.USER_LOCK_PREFIX + "register:" + user.getUsername();
        
        boolean lockAcquired = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("系统繁忙，请稍后重试");
        }

        try {
            // 校验用户名唯一
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername());
            if (userMapper.selectCount(wrapper) > 0) {
                throw BusinessException.badRequest("用户名已存在");
            }

            // 初始化用户信息
            user.setStatus(1);
            userMapper.insert(user);
            
            log.info("用户注册成功: {}", user.getUsername());
            return user;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        String lockKey = CacheConstants.USER_LOCK_PREFIX + "update:" + user.getUserId();
        
        boolean lockAcquired = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("系统繁忙，请稍后重试");
        }
        
        try {
            int result = userMapper.updateById(user);
            if (result > 0) {
                // 清除缓存
                clearUserCache(user.getUserId(), user.getUsername());
                log.info("用户更新成功: {}", user.getUserId());
            }
            return result > 0;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        String lockKey = CacheConstants.USER_LOCK_PREFIX + "delete:" + userId;
        
        boolean lockAcquired = distributedLock.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("系统繁忙，请稍后重试");
        }
        
        try {
            int result = userMapper.deleteById(userId);
            if (result > 0) {
                clearUserCache(userId, user.getUsername());
                log.info("用户删除成功: {}", userId);
            }
            return result > 0;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncOrderProfile(UserOrderSyncRequest request) {
        if (request == null || request.getUserId() == null || request.getOrderNo() == null
                || request.getOrderNo().isBlank()) {
            throw BusinessException.badRequest("同步用户下单画像参数不完整");
        }

        String lockKey = CacheConstants.USER_LOCK_PREFIX + "order-sync:" + request.getUserId();
        boolean lockAcquired = distributedLock.tryLock(lockKey, 5, 20, TimeUnit.SECONDS);
        if (!lockAcquired) {
            throw BusinessException.badRequest("用户画像同步中，请稍后重试");
        }

        try {
            User user = userMapper.selectById(request.getUserId());
            if (user == null) {
                throw BusinessException.notFound("用户不存在");
            }

            Integer currentCount = user.getOrderCount() == null ? 0 : user.getOrderCount();
            user.setOrderCount(currentCount + 1);
            user.setLastOrderNo(request.getOrderNo());
            int result = userMapper.updateById(user);
            if (result <= 0) {
                throw BusinessException.serverError("同步用户下单画像失败");
            }

            clearUserCache(user.getUserId(), user.getUsername());
            log.info("同步用户下单画像成功, userId: {}, orderNo: {}, orderCount: {}",
                    user.getUserId(), request.getOrderNo(), user.getOrderCount());
            return true;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    /**
     * 清除用户缓存
     */
    private void clearUserCache(Long userId, String username) {
        String idKey = CacheConstants.USER_CACHE_PREFIX + "id:" + userId;
        String nameKey = CacheConstants.USER_CACHE_PREFIX + username;
        redisTemplate.delete(idKey);
        redisTemplate.delete(nameKey);
    }
}
