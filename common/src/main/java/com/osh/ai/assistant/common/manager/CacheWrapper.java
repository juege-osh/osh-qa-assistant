package com.osh.ai.assistant.common.manager;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaodaowen
 */
public class CacheWrapper {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    public <T> void set(String key, T t, long timeout,TimeUnit timeUnit){
        if (ClassUtils.isPrimitiveOrWrapper(t.getClass()) || String.class.equals(t.getClass())) {
            redisTemplate.opsForValue().set(key, t,timeout, timeUnit);
        }else {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(t),timeout, timeUnit);
        }
    }

    public <T> void set(String key, T t){
        if (ClassUtils.isPrimitiveOrWrapper(t.getClass()) || String.class.equals(t.getClass())) {
            redisTemplate.opsForValue().set(key, t);
        }else {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(t));
        }
    }


    public <T> T get(String key,Class<T> clazz){
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        if (ClassUtils.isPrimitiveOrWrapper(clazz) || String.class.equals(clazz)) {
            return (T) obj;
        }
        return JSONUtil.toBean(obj.toString(),clazz);
    }
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除
     */
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    public void increment(String key, Integer delta) {
        redisTemplate.opsForValue().increment(key,delta);
    }

    public Set<String> keys(String key) {
        return redisTemplate.keys(key + "*");
    }

    /**
     * 批量获取
     */
    public List<Object> multiGet(Collection<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    public Long execute(DefaultRedisScript<Long> redisScript, List<String> keys, Object[] args) {
        // redis执行lua脚本是原子性的
        return redisTemplate.execute(redisScript, keys, args);
    }

    public void multiSet(Map<String, Object> map) {
        redisTemplate.opsForValue().multiSet(map);
    }
}
