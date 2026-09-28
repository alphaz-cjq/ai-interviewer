package com.cjq.service.impl;

import com.cjq.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImp implements DistributedLockService {
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> unlockScript;

    //尝试获取锁
    //用setIfAbsent
    @Override
    public boolean tryLock(String key, String value, long expireSeconds) {
       Boolean success=stringRedisTemplate.opsForValue()
               // setIfAbsent 等同于 Redis 的 SETNX 命令
               //其核心作用就是原子性地判断并设置键值。
               //当键 (Key) 不存在时：会将键的值设置为指定的值，并返回成功标识（通常为 1 或 true）。
               //当键 (Key) 已经存在时：则不做任何操作，并返回失败标识（通常为 0 或 false）。
               .setIfAbsent(key,value,expireSeconds, TimeUnit.SECONDS);
       return Boolean.TRUE.equals(success);
    }

    //释放锁（把门锁打开也就是删掉Redis里的Key），必须验证身份
    //用lua脚本原子释放
    @Override
    public boolean unlock(String key, String value) {
        //execute方法 为开发者提供一个底层、灵活的入口，用来执行几乎所有类型的Redis操作
      Long result=stringRedisTemplate.execute(unlockScript,
              Collections.singletonList(key),value);
      return Long.valueOf(1).equals(result);
    }
}
