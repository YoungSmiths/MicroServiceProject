package com.microservice.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON 工具类测试
 */
class JsonUtilTest {

    @Test
    void testToJson() {
        TestBean bean = new TestBean(1L, "test", true);
        String json = JsonUtil.toJson(bean);
        
        assertNotNull(json);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"test\""));
    }

    @Test
    void testFromJson() {
        String json = "{\"id\":1,\"name\":\"test\",\"active\":true}";
        TestBean bean = JsonUtil.fromJson(json, TestBean.class);
        
        assertNotNull(bean);
        assertEquals(1L, bean.getId());
        assertEquals("test", bean.getName());
        assertTrue(bean.isActive());
    }

    @Test
    void testToJsonWithNull() {
        assertNull(JsonUtil.toJson(null));
    }

    @Test
    void testFromJsonWithNull() {
        assertNull(JsonUtil.fromJson(null, TestBean.class));
        assertNull(JsonUtil.fromJson("", TestBean.class));
    }

    @Test
    void testToBytes() {
        TestBean bean = new TestBean(1L, "test", true);
        byte[] bytes = JsonUtil.toBytes(bean);
        
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void testFromBytes() {
        TestBean original = new TestBean(1L, "test", true);
        byte[] bytes = JsonUtil.toBytes(original);
        
        TestBean result = JsonUtil.fromBytes(bytes, TestBean.class);
        assertNotNull(result);
        assertEquals(original.getId(), result.getId());
        assertEquals(original.getName(), result.getName());
    }

    static class TestBean {
        private Long id;
        private String name;
        private boolean active;

        public TestBean() {}

        public TestBean(Long id, String name, boolean active) {
            this.id = id;
            this.name = name;
            this.active = active;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
