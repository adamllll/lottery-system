package org.adam.lotterysystem.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Configuration
public class RedisUtil {
    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private static final DefaultRedisScript<Long> DELETE_IF_MATCH_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    /**
     * 注入StringRedisTemplate对象，用于操作Redis数据库
     * RedisTemplate是Spring Data Redis提供的一个工具类，可以方便地进行Redis操作
     * RedisTemplate 先将被存储的数据转化成 字节数组(不可读) 再存储到Redis中，读取的时候按照字节数组读取
     * StringRedisTemplate 直接存放String（可读）
     * 项目中使用StringRedisTemplate，因为我们存储的数据都是字符串类型的，使用StringRedisTemplate可以更方便地进行操作
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 设置Redis键值对的方法，使用StringRedisTemplate的opsForValue()方法进行操作
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Redis set error, set({} {})", key, value, e);
            return false;
        }
    }

    /**
     * 设置Redis键值对的方法，使用StringRedisTemplate的opsForValue()方法进行操作，并设置过期时间
     * @param key
     * @param value
     * @param time
     * @return
     */
    public boolean set(String key, String value, Long time) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Redis set error, set({}, {}, {})", key, value, time, e);
            return false;
        }
    }

    /**
     * 仅在 key 不存在时设置值，并设置过期时间
     * @param key
     * @param value
     * @param time
     * @return
     */
    public boolean setIfAbsent(String key, String value, Long time) {
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            logger.error("Redis setIfAbsent error, setIfAbsent({}, {}, {})", key, value, time, e);
            return false;
        }
    }

    /**
     * 获取Redis键值对的方法，使用StringRedisTemplate的opsForValue()方法进行操作
     * @param key
     * @return
     */
    public String get(String key) {
        try {
            return StringUtils.hasText(key)
                    ? stringRedisTemplate.opsForValue().get(key)
                    : null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Redis get error, get({})", key, e);
            return null;
        }
    }
    /**
     * 删除Redis键值对的方法，使用StringRedisTemplate的delete()方法进行操作
     * @param key
     * @return
     */
    public boolean del(String... key) { // 可变参数，可以传入一个或多个键
        try {
            if (null != key && key.length > 0) {
                if (key.length == 1) {
                    stringRedisTemplate.delete(key[0]);
                } else {
                    stringRedisTemplate.delete(
                            (Collection<String>) CollectionUtils.arrayToList(key) // 将数组转换成集合
                    );
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Redis del error, del({})", key, e);
            return false;
        }
    }

    /**
     * 当 value 匹配时删除 key，用于安全释放分布式锁
     * @param key
     * @param value
     * @return
     */
    public boolean delIfMatch(String key, String value) {
        try {
            if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
                return false;
            }
            Long deleted = stringRedisTemplate.execute(
                    DELETE_IF_MATCH_SCRIPT,
                    Collections.singletonList(key),
                    value);
            return Long.valueOf(1L).equals(deleted);
        } catch (Exception e) {
            logger.error("Redis delIfMatch error, delIfMatch({}, {})", key, value, e);
            return false;
        }
    }

    /**
     * 判断Redis键是否存在的方法，使用StringRedisTemplate的hasKey()方法进行操作
     * @param key
     * @return
     */
    public boolean haskey(String key) {
        try {
            return StringUtils.hasText(key) ? stringRedisTemplate.hasKey(key) : false;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Redis hasKey error, hasKey({})", key, e);
            return false;
        }
    }
}
