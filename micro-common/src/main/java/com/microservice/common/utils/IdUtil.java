package com.microservice.common.utils;

import java.util.UUID;

/**
 * ID 生成工具类
 */
public class IdUtil {

    /**
     * 生成 UUID (不带横杠)
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成带横杠的 UUID
     */
    public static String uuidWithDash() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成雪花 ID
     */
    public static long snowflakeId() {
        return SnowflakeIdWorker.getInstance().nextId();
    }

    /**
     * 生成雪花 ID 字符串
     */
    public static String snowflakeIdStr() {
        return String.valueOf(SnowflakeIdWorker.getInstance().nextId());
    }

    /**
     * 雪花 ID 生成器
     */
    private static class SnowflakeIdWorker {
        
        private static final long START_TIMESTAMP = 1609459200000L; // 2021-01-01
        private static final long WORKER_ID_BITS = 5L;
        private static final long DATACENTER_ID_BITS = 5L;
        private static final long SEQUENCE_BITS = 12L;
        
        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
        
        private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
        private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
        private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
        private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
        
        private final long workerId;
        private final long datacenterId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;
        
        private static final SnowflakeIdWorker INSTANCE = new SnowflakeIdWorker(1, 1);
        
        private SnowflakeIdWorker() {
            this(1, 1);
        }
        
        private SnowflakeIdWorker(long workerId, long datacenterId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("workerId 必须在 0-" + MAX_WORKER_ID + " 之间");
            }
            if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
                throw new IllegalArgumentException("datacenterId 必须在 0-" + MAX_DATACENTER_ID + " 之间");
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }
        
        public static SnowflakeIdWorker getInstance() {
            return INSTANCE;
        }
        
        public synchronized long nextId() {
            long timestamp = timeGen();
            
            if (timestamp < lastTimestamp) {
                throw new RuntimeException("时钟回拨，拒绝生成 ID");
            }
            
            if (lastTimestamp == timestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }
            
            lastTimestamp = timestamp;
            
            return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                    | (datacenterId << DATACENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }
        
        private long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen();
            }
            return timestamp;
        }
        
        private long timeGen() {
            return System.currentTimeMillis();
        }
    }
}
