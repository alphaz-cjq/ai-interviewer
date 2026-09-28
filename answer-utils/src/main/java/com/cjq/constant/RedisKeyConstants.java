package com.cjq.constant;

/*
* 专门用来存放Redis缓存Key前缀常量
* */
public final class RedisKeyConstants {
    private RedisKeyConstants() {
        // 构造函数私有，防止实例化（工具类）
        throw new UnsupportedOperationException("不允许实例化");
    }

    /**
     * 邮箱验证码前缀
     * 完整 Key 示例：email:code:123@qq.com
     */
    public static final String EMAIL_CODE_PREFIX = "email:code:";

    /**
     * 面试会话上下文前缀
     * 完整 Key 示例：interview:session:1001
     */
    public static final String INTERVIEW_SESSION_PREFIX = "interview:session:";

    /**
     * JWT 黑名单前缀（阶段7扩展）
     */
    public static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * 构建完整的 Redis Key
     * @param prefix 前缀常量（如 EMAIL_CODE_PREFIX）
     * @param identifier 唯一标识（如邮箱、用户ID）
     * @return 完整的 Key 字符串
     */
    public static String buildKey(String prefix, String identifier) {
        return prefix + identifier;
    }

    //设置幂等性
    public static final String IDEMPOTENCY_PREFIX = "idempotency:answer:";

    //分布式锁
    public static final String DISTRIBUTED_LOCK_PREFIX="lock:interview:";

    //缓存一致性
    //缓存对象：岗位列表
    public static final String JOB_LIST_CACHE_KEY="cache:job:list";
}
