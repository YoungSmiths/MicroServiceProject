package com.microservice.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ID 生成工具类测试
 */
class IdUtilTest {

    @Test
    void testUuid() {
        String uuid = IdUtil.uuid();
        assertNotNull(uuid);
        assertEquals(32, uuid.length());
        
        String uuid2 = IdUtil.uuid();
        assertNotEquals(uuid, uuid2);
    }

    @Test
    void testUuidWithDash() {
        String uuid = IdUtil.uuidWithDash();
        assertNotNull(uuid);
        assertTrue(uuid.contains("-"));
        assertEquals(36, uuid.length());
    }

    @Test
    void testSnowflakeId() {
        long id1 = IdUtil.snowflakeId();
        long id2 = IdUtil.snowflakeId();
        
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertNotEquals(id1, id2);
    }

    @Test
    void testSnowflakeIdStr() {
        String idStr = IdUtil.snowflakeIdStr();
        assertNotNull(idStr);
        assertTrue(Long.parseLong(idStr) > 0);
    }

    @Test
    void testSnowflakeIdUniqueness() {
        int count = 10000;
        long[] ids = new long[count];
        
        for (int i = 0; i < count; i++) {
            ids[i] = IdUtil.snowflakeId();
        }
        
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                assertNotEquals(ids[i], ids[j], "ID 不应重复");
            }
        }
    }
}
