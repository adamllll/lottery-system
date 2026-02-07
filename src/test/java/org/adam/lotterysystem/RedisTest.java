package org.adam.lotterysystem;

import org.adam.lotterysystem.common.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisUtil redisUtil;

    @Test
    void redisTest() {
        redisTemplate.opsForValue().set("testKey1", "testValue1");
        System.out.println("testKey1: " + redisTemplate.opsForValue().get("testKey1"));
    }

    @Test
    void setRedisUtil() {
        redisUtil.set("testKey2", "testValue2");
        redisUtil.set("testKey3", "testValue3", 60L);

        System.out.println("testKey2: " + redisUtil.haskey("testKey2"));
        System.out.println("testKey3: " + redisUtil.haskey("testKey3"));

        System.out.println("key2: " + redisUtil.get("testKey2"));
        System.out.println("key3: " + redisUtil.get("testKey3"));

        redisUtil.del("testKey2");
        System.out.println("testKey2 after deletion: " + redisUtil.haskey("testKey2"));
        System.out.println("testKey3 after deletion: " + redisUtil.haskey("testKey3"));
    }

}
