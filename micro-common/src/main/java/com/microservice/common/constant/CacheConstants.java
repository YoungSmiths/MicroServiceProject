package com.microservice.common.constant;

/**
 * 缓存常量
 */
public class CacheConstants {

    /**
     * 用户缓存前缀
     */
    public static final String USER_CACHE_PREFIX = "user:cache:";

    /**
     * 订单缓存前缀
     */
    public static final String ORDER_CACHE_PREFIX = "order:cache:";

    /**
     * 分布式锁前缀
     */
    public static final String LOCK_PREFIX = "lock:";

    /**
     * 用户锁前缀
     */
    public static final String USER_LOCK_PREFIX = LOCK_PREFIX + "user:";

    /**
     * 订单锁前缀
     */
    public static final String ORDER_LOCK_PREFIX = LOCK_PREFIX + "order:";

    /**
     * 默认缓存时间（秒）
     */
    public static final long DEFAULT_CACHE_TTL = 3600;

    /**
     * 用户缓存时间（秒）
     */
    public static final long USER_CACHE_TTL = 1800;

    /**
     * 订单缓存时间（秒）
     */
    public static final long ORDER_CACHE_TTL = 900;
}
