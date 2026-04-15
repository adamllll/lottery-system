package org.adam.lotterysystem.common.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class RedisUtilTest {

    @Test
    void testSetIfAbsentThrowsWhenRedisFails() {
        RedisUtil redisUtil = new RedisUtil();
        StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);

        ReflectionTestUtils.setField(redisUtil, "stringRedisTemplate", stringRedisTemplate);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new RuntimeException("redis unavailable"));

        assertThrows(IllegalStateException.class, () -> redisUtil.setIfAbsent("lock-key", "lock-value", 30L));
    }
}
