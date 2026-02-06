package org.adam.lotterysystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void redisTest() {
        redisTemplate.opsForValue().set("testKey1", "testValue1");
        System.out.println("testKey1: " + redisTemplate.opsForValue().get("testKey1"));
    }

}
