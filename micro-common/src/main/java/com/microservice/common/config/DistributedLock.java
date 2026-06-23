package com.microservice.common.config;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁工具类
 * 使用 Redisson 实现分布式锁
 */
@Component
public class DistributedLock {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取可重入锁
     * @param lockKey 锁的key
     * @return RLock
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 加锁（默认30秒自动释放）
     * @param lockKey 锁的key
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.tryLock();
    }

    /**
     * 加锁（自定义等待时间和自动释放时间）
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 自动释放时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 加锁（公平锁）
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 自动释放时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryFairLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getFairLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 解锁
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 解锁（传入锁对象）
     * @param lock RLock对象
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 判断是否被锁定
     * @param lockKey 锁的key
     * @return 是否被锁定
     */
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 读锁
     * @param lockKey 锁的key
     * @return RLock
     */
    public RLock getReadLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey).readLock();
    }

    /**
     * 写锁
     * @param lockKey 锁的key
     * @return RLock
     */
    public RLock getWriteLock(String lockKey) {
        return redissonClient.getReadWriteLock(lockKey).writeLock();
    }
}
