package com.cjq.service;

import java.util.Map;

public interface InterviewSessionManager {

    /**
     * 从 Redis 获取面试会话，Redis 不可用时自动从 MySQL 重建
     *
     * 【降级策略】
     *  1. 尝试 redisTemplate.opsForHash().entries(sessionKey)
     *  2. 如果返回 null/empty 或抛异常 → 调用 rebuildFromMySQL()
     *  3. 重建成功后写回 Redis（重置过期时间）
     *
     * @param interviewId 面试ID
     * @return 会话 Map（key 用 InterviewSessionKeys 常量）
     */
    Map<Object, Object> getSession(Long interviewId);

    /**
     * 从 MySQL 重建 Redis 会话（Redis 完全不可用时的兜底）
     *
     * 【重建步骤】
     *  1. 查 Interview 主记录（校验存在 + 状态为 IN_PROGRESS）
     *  2. 查当前题目 InterviewQuestion（获取 currentNum）
     *  3. 查已评分题目累加总分（scoreAccumulated）
     *  4. 查当前题的追问次数（followUpCount）
     *  5. 组装 Map 写回 Redis + 设置过期时间
     */
    Map<Object, Object> rebuildFromMySQL(Long interviewId, Long questionId);

    /**
     * 从 MySQL 重建 Redis 会话（不需要 questionId，自动找最新题目）
     */
    Map<Object, Object> rebuildFromMySQL(Long interviewId);

    /**
     * 创建新会话（面试开始时调用）
     *
     * @param interviewId 面试ID
     * @param session 初始会话数据
     */
    void createSession(Long interviewId, Map<String, Object> session);

    /**
     * 更新单个字段（替代散落的 redisTemplate.opsForHash().put() 调用）
     */
    void updateField(Long interviewId, String field, Object value);

    /**
     * 从会话中安全获取当前题号（处理 Number 类型转换）
     */
    int getCurrentQuestionNum(Map<Object, Object> session);

    /**
     * 从会话中安全获取累计分数（处理 Number 类型转换）
     */
    int getScoreAccumulated(Map<Object, Object> session);

    /**
     * 从会话中安全获取追问次数（处理 Number 类型转换）
     */
    int getFollowUpCount(Map<Object, Object> session);

    /**
     * 追问次数 +1 并写回 Redis
     * @return 递增后的追问次数
     */
    int incrementFollowUp(Long interviewId);

    /**
     * 更新当前题号到 Redis
     */
    void updateCurrentQuestionNum(Long interviewId, int questionNum);

    /**
     * 累加分数到 Redis
     * @return 累加后的总分数
     */
    long addScore(Long interviewId, int score);

    /**
     * 更新面试状态
     */
    void updateStatus(Long interviewId, String status);

    /**
     * 检查会话是否存在且有效
     */
    boolean sessionExists(Long interviewId);
}
