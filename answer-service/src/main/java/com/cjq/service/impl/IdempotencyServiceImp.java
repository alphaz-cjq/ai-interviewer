package com.cjq.service.impl;

import com.cjq.constant.RedisKeyConstants;
import com.cjq.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImp implements IdempotencyService {
    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean tryAcquire(String token, long expireSeconds) {
        //1.拼接RedisKey(统一用常量管理，避免魔法值)
        //写入redis的 Key：idempotency:answer: + IdempotencyToken。
        String key= RedisKeyConstants.buildKey(RedisKeyConstants.IDEMPOTENCY_PREFIX,token);
        //而value通常是一个固定值，比如 "1" 或当前时间戳（只要非空就行）。
        //TODO Value 的值本身不重要，Key 的存在性才是“已经被占位”的唯一证据。

        //2.如果key不存在则设置成功，否则失败
        // setIfAbsent 等同于 Redis 的 SETNX 命令
        //给token设置只有第一个才可以并且设置有效期
       Boolean success= stringRedisTemplate.opsForValue()
                .setIfAbsent(key,"1",expireSeconds, TimeUnit.SECONDS);

       //3.处理返回值（防止null）
        boolean acquired = Boolean.TRUE.equals(success);//用 Boolean.TRUE.equals(success) 可以安全地把任何情况转换成 boolean，避免空指针异常。
        if (acquired){
            log.debug("幂等 token 占位成功: {}", token);
        } else {
            log.debug("幂等 token 已被占用（重复请求）: {}", token);
        }
        return acquired;
    }
}
