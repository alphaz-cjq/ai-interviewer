package com.cjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cjq.Exceptions.BusinessException;
import com.cjq.constant.RedisKeyConstants;
import com.cjq.mapper.InterviewMapper;
import com.cjq.mapper.InterviewQuestionMapper;
import com.cjq.pojo.DTO.InterviewSessionKeys;
import com.cjq.pojo.Enum.InterviewStatus;
import com.cjq.pojo.PO.Interview;
import com.cjq.pojo.PO.InterviewQuestion;
import com.cjq.service.InterviewSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.cjq.pojo.DTO.InterviewSessionKeys.TOTAL_QUESTIONS;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewSessionManagerImpl implements InterviewSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;

    private static final long SESSION_TIMEOUT = 3600; // 会话过期时间（秒）

    // ==================== 创建会话 ====================
    @Override
    public void createSession(Long interviewId, Map<String, Object> session) {

        // 拼接 sessionKey = INTERVIEW_SESSION_PREFIX + interviewId
        String sessionKey = buildKey(interviewId);
        redisTemplate.opsForHash().putAll(sessionKey, session);
        redisTemplate.expire(sessionKey, SESSION_TIMEOUT, TimeUnit.SECONDS);
        log.info("创建面试会话：{}，过期时间 {} 秒", interviewId, SESSION_TIMEOUT); // 日志信息
    }

    // ==================== 获取会话 ====================

    @Override
    public Map<Object, Object> getSession(Long interviewId) {
        // 步骤：
        // 1. 拼接 sessionKey = INTERVIEW_SESSION_PREFIX + interviewId
        String sessionKey = buildKey(interviewId);
        Map<Object, Object> session = null;
        try {
            session = redisTemplate.opsForHash().entries(sessionKey);
        } catch (Exception e) {
            //获取会话异常
            //场景1：Redis连接超时、宕机、网络抖动等
            log.warn("Redis 连接异常，尝试从 MySQL 重建会话: {}", e.getMessage());
        }

        // 2. 如果 session 为 null 或 empty → 调用 rebuildFromMySQL()
        //场景2：如果Redis没数据（Key过期）或者刚才 catch 了异常，session 为 null 或空
        //重建会话
        if (session == null || session.isEmpty()) {
            log.info("会话不存在，尝试从 MySQL 重建会话: {}", interviewId);
            session = rebuildFromMySQL(interviewId);
        }
        // 5. 返回 session
        //
        // 注意：redisTemplate.opsForHash().entries() 返回的是 Map<Object, Object>
        return session;
    }
    // ==================== 从 MySQL 重建会话（降级方案） ====================

    /**
     * 无需 questionId 的重建：自动查找该面试最近一题作为当前题目
     * -------用于redis完全不可用的极端情况，不知道进行到哪一题，系统需要自动推断
     *
     */
    @Override
    public Map<Object, Object> rebuildFromMySQL(Long interviewId) {
        // 查找该面试最新的那道题（按 questionNum 降序，取第一条）
        LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQuestion::getInterviewId, interviewId)
                .orderByDesc(InterviewQuestion::getQuestionNum)
                .last("LIMIT 1");
        InterviewQuestion latest = questionMapper.selectOne(wrapper);
        if (latest == null) {
            throw new BusinessException("该面试没有任何题目记录，无法重建会话");
        }
        return rebuildFromMySQL(interviewId, latest.getId());//拿到 latest.getId() 后，它调用第二个重载方法完成真正的重建。
    }

    /*
    *第二个重载方法：负责重建会话
    * */
    @Override
    public Map<Object, Object> rebuildFromMySQL(Long interviewId, Long questionId) {

        //1.根据面试id查询面试主记录
        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BusinessException("面试记录不存在");
        }
        if (!InterviewStatus.IN_PROGRESS.equals(interview.getStatus())) {
            throw new BusinessException("面试已结束，无法继续答题");
        }

        //重建会话

        //代码进行到这里说明面试记录存在且状态进行中，但没有session，需要从MySQL中重建会话
        // 2. 根据当前提交的 questionId 获取题号
        InterviewQuestion currentQ = questionMapper.selectById(questionId);//根据id查询题目
        if (currentQ == null) {
            throw new BusinessException("题目不存在");
        }
        int currentNum = currentQ.getQuestionNum();//获取当前题目编号
        log.info("从 MySQL 重建会话，interviewId: {}, questionId: {}, questionNum: {}", interviewId, questionId, currentNum);

        //3.计算已累计的总分（从所有已经评分的题目累加）
        LambdaQueryWrapper<InterviewQuestion> scoreWrapper = new LambdaQueryWrapper<>(); // 创建一个查询条件构造器
        // （1）限定范围：只查询当前面试（ID=request.getInterviewId()）下的题目，避免累加其他面试的分数
        //（2）整段代码要做的：“我要从数据库里，找出所有属于这场面试（interview_id = 11）的，
        // 并且已经打过分（score IS NOT NULL） 的题目，然后把它们的分数累加起来。”

        // wrapper构造器只做查询条件构造，不执行查询
        scoreWrapper.eq(InterviewQuestion::getInterviewId,//指定数据库表中的interview_id 字段
                        interviewId)//前端传过来的具体面试ID值
                .isNotNull(InterviewQuestion::getScore);//判断分数是否为空，找出已评分的题目

        List<InterviewQuestion> scoredList = questionMapper.selectList(scoreWrapper);//根据这个规则查询题目列表

        int accumulated = scoredList.stream()
                .mapToInt(InterviewQuestion::getScore)
                .sum();// 计算总分

        //4.计算当前题目的追问次数（防止重建后追问次数清零）
        LambdaQueryWrapper<InterviewQuestion> followUpWrapper = new LambdaQueryWrapper<>();
        followUpWrapper.eq(InterviewQuestion::getInterviewId, interviewId)
                .eq(InterviewQuestion::getQuestionNum, currentNum)
                .eq(InterviewQuestion::getQuestionType, "FOLLOW_UP");

        int followUpCount = questionMapper.selectCount(followUpWrapper).intValue(); // 获取追问次数

        //5.组装新的会话数据
        Map<String, Object> newSession = new HashMap<>();
        newSession.put("currentQuestionNum", currentNum);
        newSession.put("totalQuestions", TOTAL_QUESTIONS);
        newSession.put("jobId", interview.getJobId());
        newSession.put("resumeId", interview.getResumeId());
        newSession.put("scoreAccumulated", accumulated);
        newSession.put("status", InterviewStatus.IN_PROGRESS.name());
        newSession.put("followUpCount", followUpCount);

        //6.将写好的新会话写回redis(重置过期时间)
        String key = buildKey(interviewId);
        redisTemplate.opsForHash().putAll(key, newSession);
        redisTemplate.expire(key, SESSION_TIMEOUT, TimeUnit.SECONDS);

        //7.将重建后的数据转为Map<Object,Object>供后续代码使用

        log.info("Redis 会话重建成功，面试ID: {}, 当前题号: {}, 累计分: {}, 追问次数: {}",
                interviewId, currentNum, accumulated, followUpCount);

        return new HashMap<>(newSession);
    }


    // ==================== 单字段操作 ====================
/*
* 封装 redisTemplate.opsForHash().put(sessionKey, field, value)
* */
    @Override
    public void updateField(Long interviewId, String field, Object value) {
        String key = buildKey(interviewId);
        redisTemplate.opsForHash().put(key, field, value);
    }

    /*
    * 从 session 中安全取出 currentQuestionNum
    * */
    @Override
    public int getCurrentQuestionNum(Map<Object, Object> session) {
        return extractInt(session, InterviewSessionKeys.CURRENT_QUESTION_NUM, 1);
    }

    /*
    * 从 session 中安全取出 scoreAccumulated
    * */
    @Override
    public int getScoreAccumulated(Map<Object, Object> session) {
        return extractInt(session, InterviewSessionKeys.SCORE_ACCUMULATED, 0);
    }

    /*
    * 从 session 中安全取出 followUpCount
    * */
    @Override
    public int getFollowUpCount(Map<Object, Object> session) {
        return extractInt(session, InterviewSessionKeys.FOLLOW_UP_COUNT, 0);
    }

    /*
    * 追问次数 +1 并写回 Redis
    * */
    @Override
    public int incrementFollowUp(Long interviewId) {
        String key = buildKey(interviewId);
        String field = InterviewSessionKeys.FOLLOW_UP_COUNT;

        // 使用 Redis 原子递增（避免并发问题）这条命令直接修改Redis中的数据
        //incrementFollowUp 已经更新了 Redis，不是“赋了一个值”那么简单
        Long newCount = redisTemplate.opsForHash().increment(key, field, 1);
        return newCount.intValue();
    }

/*
* 更新当前题号
* */
    @Override
    public void updateCurrentQuestionNum(Long interviewId, int questionNum) {
        updateField(interviewId, InterviewSessionKeys.CURRENT_QUESTION_NUM, questionNum);
    }

    /*
    * 累加分数（返回累加后的总分，避免本地 session 和 Redis 不一致）
    * */
    @Override
    public long addScore(Long interviewId, int score) {
        String key = buildKey(interviewId);
        return redisTemplate.opsForHash().increment(key, InterviewSessionKeys.SCORE_ACCUMULATED, score);
    }

/*
* 更新面试状态
* */
    @Override
    public void updateStatus(Long interviewId, String status) {
        updateField(interviewId, InterviewSessionKeys.STATUS, status);
    }

    /*
    * 检查 Redis Key 是否存在
    * */
    @Override
    public boolean sessionExists(Long interviewId) {
        String key = buildKey(interviewId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ==================== 私有工具方法 ====================

    /**
     * 拼接 Redis Key
     * return INTERVIEW_SESSION_PREFIX + interviewId;
     */
    private String buildKey(Long interviewId) {
        return RedisKeyConstants.INTERVIEW_SESSION_PREFIX + interviewId;
    }

    /**
     * 从 session Map 中安全提取整数值
     */
    private int extractInt(Map<Object, Object> session, String key, int defaultValue) {
        if (session == null) {
            return defaultValue;
        }
        Object value = session.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return ((Number) value).intValue();
        } catch (ClassCastException e) {
            log.warn("字段 {} 类型转换失败，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }
}
