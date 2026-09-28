package com.cjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cjq.Exceptions.BusinessException;
import com.cjq.config.RabbitMQConfig;
import com.cjq.constant.RedisKeyConstants;
import com.cjq.mapper.InterviewMapper;
import com.cjq.mapper.InterviewQuestionMapper;
import com.cjq.mapper.JobPositionMapper;
import com.cjq.mapper.ResumeMapper;
import com.cjq.pojo.DTO.*;
import com.cjq.pojo.Enum.InterviewAction;
import com.cjq.pojo.Enum.InterviewStatus;
import com.cjq.pojo.PO.*;
import com.cjq.pojo.VO.InterviewVO;
import com.cjq.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    // ========== Mapper 层依赖 ==========
    private final InterviewMapper interviewMapper;// 面试Mapper
    private final InterviewQuestionMapper questionMapper;// 面试问题Mapper
    private final ResumeMapper resumeMapper;// 简历Mapper
    private final JobPositionMapper jobPositionMapper;// 岗位Mapper

    // ========== 三个专职服务（重构后新增） ==========

    private final InterviewStateMachine stateMachine;// 注入面试状态机
    private final InterviewAIEngine aiEngine;// 注入面试AI引擎
    private final InterviewSessionManager sessionManager;// 注入面试会话管理器
    private final IdempotencyService idempotencyService;//幂等服务
    private final DistributedLockService distributedLockService;// 分布式锁服务
    private final RabbitTemplate rabbitTemplate;// 注入RabbitTemplate用于发送消息
    // ========== 常量配置 ==========
    private static final int TOTAL_QUESTIONS = 5;
    private static final int MAX_FOLLOW_UP = 2;

    /*
     * 生成面试题功能
     * */
    // ⚠️ 不在方法级别使用 @Transactional：AI 调用耗时 5-30 秒，不应占用 DB 连接
    @Override
    public Map<String, Object> startInterview(Long userId, InterviewStartRequest request) {
        //1.获取简历和  JD信息
        Resume resume = resumeMapper.selectById(request.getResumeId());
        if (resume == null) {
            throw new BusinessException("简历不存在");
        }
        JobPosition job = jobPositionMapper.selectById(request.getJobId());
        if (job == null) {
            throw new BusinessException("岗位不存在");
        }

        //2.创建面试记录
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setJobId(request.getJobId());
        interview.setResumeId(request.getResumeId());
        interview.setStatus(InterviewStatus.IN_PROGRESS.name());
        interview.setStartTime(LocalDateTime.now());
        interviewMapper.insert(interview);// 添加面试记录到数据库

        // 3. 调用AI生成第一题（非流式，因为需要同步获得题目文本并保存到数据库）
        String firstQuestion = aiEngine.generateQuestion(interview.getId(), job, resume, 1);

        //4.保存第一题
        InterviewQuestion question = new InterviewQuestion();
        question.setInterviewId(interview.getId());// 设置面试ID
        question.setQuestionText(firstQuestion);// 设置问题文本
        question.setQuestionNum(1);// 设置问题编号
        question.setQuestionType("TECHNICAL");// 设置问题类型

        questionMapper.insert(question);// 添加问题记录到数据库

        // 5. redis 存储面试会话信息
        // 创建一个 Map 集合，用于存储面试会话记录，键为面试ID，值为会话记录
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put(InterviewSessionKeys.CURRENT_QUESTION_NUM, 1);// 当前问题编号
        sessionData.put(InterviewSessionKeys.TOTAL_QUESTIONS, TOTAL_QUESTIONS);// 总问题数
        sessionData.put(InterviewSessionKeys.JOB_ID, request.getJobId());// 岗位ID
        sessionData.put(InterviewSessionKeys.RESUME_ID, request.getResumeId());// 简历ID
        sessionData.put(InterviewSessionKeys.SCORE_ACCUMULATED, 0);// 累计分数
        sessionData.put(InterviewSessionKeys.STATUS, InterviewStatus.IN_PROGRESS.name());// 状态
        sessionData.put(InterviewSessionKeys.FOLLOW_UP_COUNT, 0);// 设置追问次数计数器

        sessionManager.createSession(interview.getId(), sessionData);// 创建会话


        //6.返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("interviewId", interview.getId());// 面试ID
        result.put("questionId", question.getId()); // 题目ID
        result.put("question", firstQuestion);// 题目
        result.put("questionNum", 1);// 题目编号
        result.put("totalQuestions", TOTAL_QUESTIONS);// 总题目数
        return result;
    }

    /*
     * 流式开始面试 — 先非流式生成题目并入库，再将文本逐字流式推给前端
     * */
    // ⚠️ 不使用 @Transactional：DB 写入在异步 onComplete 回调中执行，不在本方法的事务范围内
    @Override
    public SseEmitter startInterviewStream(Long userId, InterviewStartRequest request) {
        // 1. 获取简历和 JD 信息
        Resume resume = resumeMapper.selectById(request.getResumeId());
        if (resume == null) {
            throw new BusinessException("简历不存在");
        }
        JobPosition job = jobPositionMapper.selectById(request.getJobId());
        if (job == null) {
            throw new BusinessException("岗位不存在");
        }

        // 2. 创建面试记录
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setJobId(request.getJobId());
        interview.setResumeId(request.getResumeId());
        interview.setStatus(InterviewStatus.IN_PROGRESS.name());
        interview.setStartTime(LocalDateTime.now());
        interviewMapper.insert(interview);

        // 3. 调用非流式 AI 生成第一题（只生成一次！）
        String firstQuestion = aiEngine.generateQuestion(interview.getId(), job, resume, 1);

        // 4. 保存第一题到 DB
        InterviewQuestion question = new InterviewQuestion();
        question.setInterviewId(interview.getId());
        question.setQuestionText(firstQuestion);
        question.setQuestionNum(1);
        question.setQuestionType("TECHNICAL");
        questionMapper.insert(question);

        // 5. 创建 Redis 会话
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put(InterviewSessionKeys.CURRENT_QUESTION_NUM, 1);
        sessionData.put(InterviewSessionKeys.TOTAL_QUESTIONS, TOTAL_QUESTIONS);
        sessionData.put(InterviewSessionKeys.JOB_ID, request.getJobId());
        sessionData.put(InterviewSessionKeys.RESUME_ID, request.getResumeId());
        sessionData.put(InterviewSessionKeys.SCORE_ACCUMULATED, 0);
        sessionData.put(InterviewSessionKeys.STATUS, InterviewStatus.IN_PROGRESS.name());
        sessionData.put(InterviewSessionKeys.FOLLOW_UP_COUNT, 0);
        sessionManager.createSession(interview.getId(), sessionData);

        //  ✅ 准备 done 事件数据（一定要把 interviewId 和 questionId 传回去！）
        //保证流式推送
        Map<String, Object> doneMeta = new HashMap<>();
        doneMeta.put("interviewId", interview.getId());
        doneMeta.put("questionId", question.getId());
        doneMeta.put("questionNum", 1);
        doneMeta.put("totalQuestions", TOTAL_QUESTIONS);
        log.info("流式开始面试（复用非流式题目）：interviewId={}, questionId={}", interview.getId(), question.getId());

        // 6. 把已入库的题目文本逐字流式推给前端
        return aiEngine.streamText((firstQuestion), doneMeta);
    }

    /*
     * 提交答案 — 评分 + 追问
     * 下一题
     * */
    // ⚠️ 不使用 @Transactional：AI 评分和追问调用耗时 5-30 秒，不应长时间占用 DB 连接
    @Override
    public Map<String, Object> submitAnswer(Long userId, InterviewAnswerRequest request) {
        // ==================== 0. 分布式锁（先锁，串行化同一场面试的并发提交） ====================
        //拼接Key，按面试ID进行分
        String lockKey = RedisKeyConstants.buildKey(RedisKeyConstants.DISTRIBUTED_LOCK_PREFIX,
                String.valueOf(request.getInterviewId()));
        //生成唯一随机字符串，确认锁
        String lockValue = UUID.randomUUID().toString();
        //尝试获取锁
        if (!distributedLockService.tryLock(lockKey, lockValue, 60)) {
            throw new BusinessException("操作太频繁，请稍后重试");
        }

        try {
        // ==================== 1. 幂等性检查（锁内，防止 token 被孤儿化） ====================
        //30秒防止重复提交，防止key永远留在redis里
        if (!idempotencyService.tryAcquire(request.getIdempotencyToken(), 30)) {
            throw new BusinessException("请勿重复提交");
        }

        // ==================== 2. 获取会话（SessionManager 自动处理降级） ====================
        //从redis当中读取数据到内存
        // session只是当前JAVA对象，内存数据，不是redis中的数据
        Map<Object, Object> session = sessionManager.getSession(request.getInterviewId());

        // ==================== 2. 保存答案 ====================
        //获取当前题目后再获取答案
        InterviewQuestion currentQuestion = questionMapper.selectById(request.getQuestionId());//获取题目
        if (currentQuestion == null) {
            throw new BusinessException("题目不存在");
        }
        //校验题号一致性
        int currentNumFromRedis = sessionManager.getCurrentQuestionNum(session);//安全获取当前题目编号（自动处理 Long→int）
        int currentNumFromDB = currentQuestion.getQuestionNum();//获取当前题目编号（从数据库）
        if (currentNumFromRedis != currentNumFromDB) {
            log.warn("Redis 题号 ({}) 与数据库题号 ({}) 不一致，强制修正 Redis",
                    currentNumFromRedis, currentNumFromDB);
            //修正Redis里的题号
            sessionManager.updateCurrentQuestionNum(request.getInterviewId(), currentNumFromDB);
            //同时修正currentNum变量，让后续逻辑使用正确的值
            session.put(InterviewSessionKeys.CURRENT_QUESTION_NUM, currentNumFromDB);
        }
        currentQuestion.setAnswerText(request.getAnswerText());// 设置答案文本
        questionMapper.updateById(currentQuestion);// 更新问题记录到数据库

        // ==================== 3. AI 评分 ====================
        ScoreResult scoreResult = aiEngine.evaluateAnswer(currentQuestion.getQuestionText()
                , request.getAnswerText());
        int score = scoreResult.getScore(); // 获取评分
        String comment = scoreResult.getComment();//获取评价
        boolean shouldFollowUp = scoreResult.isShouldFollowUp();
        //追问次数限制
        int followUpCount = sessionManager.getFollowUpCount(session);
        if (shouldFollowUp && followUpCount >= 2) {
            log.info("追问次数达到上限，不再追问{}", request.getInterviewId());
            shouldFollowUp = false;//强制打断
        }
        //4.更新面试记录（评分，评论）
        currentQuestion.setScore(score);
        currentQuestion.setComment(comment);
        questionMapper.updateById(currentQuestion);

        // 5.更新 Redis 中的累计分数，并刷新本地 session
        long newAccumulated = sessionManager.addScore(request.getInterviewId(), score);
        session.put(InterviewSessionKeys.SCORE_ACCUMULATED, (int) newAccumulated);

        //======================4.状态机决策=======================
        //6.判断是否应该追问
        InterviewAction action = stateMachine.decide(
                session,
                scoreResult,
                TOTAL_QUESTIONS,
                MAX_FOLLOW_UP  // 你需要定义这个常量，比如 2
        );
        // ==================== 5. 根据决策执行动作（用 Switch 替代混乱的 if-else） ====================

        switch (action) {
            // ---------- 情况 A：追问 ----------
            case FOLLOW_UP -> {
                //追问
                String followUp = aiEngine.generateFollowUp(currentQuestion.getQuestionText()
                        , currentQuestion.getAnswerText());

                //6.1 存追问为独立题目到数据库
                InterviewQuestion followUpQuestion = new InterviewQuestion();
                followUpQuestion.setInterviewId(request.getInterviewId());
                followUpQuestion.setQuestionText(followUp);
                followUpQuestion.setQuestionNum(sessionManager.getCurrentQuestionNum(session));//保持当前题目编号
                followUpQuestion.setQuestionType("FOLLOW_UP");
                questionMapper.insert(followUpQuestion);

                //更新追问次数计数器（Redis）
                int newCount = sessionManager.incrementFollowUp(request.getInterviewId());

                // 更新 session 中的追问次数计数器
                //也就是更新本地JAVA内存当中存的redis数据，方便后续逻辑使用
                session.put(InterviewSessionKeys.FOLLOW_UP_COUNT, newCount);

                //6.2返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("type", "FOLLOW_UP");
                result.put("question", followUp);
                result.put("questionId", followUpQuestion.getId());
                result.put("questionNum", currentQuestion.getQuestionNum());

                return result;
            }
            //7.进入下一题
            case NEXT_QUESTION -> {

                //获取当前题号并进行计算下一题编号
                int nextNum = sessionManager.getCurrentQuestionNum(session) + 1;
                //下一题
                //7.1  从会话当中获得jobId和resumeId
                Long jobId = ((Number) session.get(InterviewSessionKeys.JOB_ID)).longValue();
                Long resumeId = ((Number) session.get(InterviewSessionKeys.RESUME_ID)).longValue();

                //7.2  查询对应的岗位和简历
                JobPosition job = jobPositionMapper.selectById(jobId);
                Resume resume = resumeMapper.selectById(resumeId);
                //调用AI生成下一题
                String nextQuestion = aiEngine.generateQuestion(request.getInterviewId(), job, resume, nextNum);

                //7.3  保存下一题到数据库
                InterviewQuestion newQuestion = new InterviewQuestion();
                newQuestion.setInterviewId(request.getInterviewId());
                newQuestion.setQuestionText(nextQuestion);
                newQuestion.setQuestionNum(nextNum);
                newQuestion.setQuestionType("TECHNICAL");
                questionMapper.insert(newQuestion);

                //更新 Redis 中的当前题号
                sessionManager.updateCurrentQuestionNum(request.getInterviewId(), nextNum);

                //7.4  构造返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("type", "NEXT_QUESTION");
                result.put("question", nextQuestion);              // ← 这里是字符串，不是 ID
                result.put("questionId", newQuestion.getId());     // ← ID 单独用 questionId 返回
                result.put("questionNum", nextNum);
                result.put("totalQuestions", TOTAL_QUESTIONS);
                return result;
            }


            //面试结束
            case INTERVIEW_END -> {
                //超过（没有下一题），面试结束，更新面试记录的信息

                //1.获取最终总分
                int finalScore = sessionManager.getScoreAccumulated(session);
                //2.获取面试记录
                Interview interview = interviewMapper.selectById(request.getInterviewId());
                //3.更新面试记录
                interview.setStatus(InterviewStatus.COMPLETED.name());//更新面试状态
                interview.setTotalScore(finalScore);//更新总分
                interview.setEndTime(LocalDateTime.now());//更新结束时间
                interviewMapper.updateById(interview);

                //4.更新面试状态
                sessionManager.updateStatus(request.getInterviewId(), InterviewStatus.COMPLETED.name());

                // 🔥 通过 MQ 异步生成报告
                // 生产者：发送消息到消息队列
                //1. 创建报告消息DTO
                ReportMessageDTO message = new ReportMessageDTO(request.getInterviewId(), userId);
                //2. 发送消息到 MQ
                //convertAndSend方法：把消息体 message 发给 RabbitMQConfig.REPORT_EXCHANGE 里指定的交换机（interview.report.exchange），并贴上一个标签（interview.report）
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.REPORT_EXCHANGE,//连接RabbitMQ服务器的交换机
                        RabbitMQConfig.REPORT_ROUTING_KEY,// 路由键（贴上了标签为 "interview.report" 的消息）
                        message,
                        new MessagePostProcessor() {
                            @Override
                            public Message postProcessMessage(Message message) throws AmqpException {
                                message.getMessageProperties().setContentType("application/json");
                                return message;
                            }
                        }
                );
                log.info("📨 报告生成消息已发送至 MQ，interviewId={}", request.getInterviewId());

                //返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("type", "INTERVIEW_END");//面试结束
                result.put("totalScore", finalScore);//总分
                result.put("comment", "面试结束，报告正在后台生成中...");//评论
                return result;
            }
            default -> throw new BusinessException("未知的面试动作");
        }
    }finally {
            //释放锁
            distributedLockService.unlock(lockKey,lockValue);
        }
    }

    /*
     * 流式提交答案 — 完整业务逻辑 + 流式输出（与 submitAnswer 同等的状态机驱动）
     * */
    @Override
    public SseEmitter submitAnswerStream(Long userId, InterviewAnswerRequest request) {
        // ===== 0. 分布式锁（按面试ID串行化，保护状态机的读改写） =====
        String lockKey = RedisKeyConstants.buildKey(RedisKeyConstants.DISTRIBUTED_LOCK_PREFIX,
                String.valueOf(request.getInterviewId()));
        String lockValue = UUID.randomUUID().toString();
        if (!distributedLockService.tryLock(lockKey, lockValue, 60)) {
            throw new BusinessException("操作太频繁，请稍后重试");
        }

        try {
        // ===== 1. 幂等性检查（锁内，防止 token 被孤儿化） =====
        if(!idempotencyService.tryAcquire(request.getIdempotencyToken(), 30)) {
            throw new BusinessException("请勿重复提交");
        }

        // ===== 2. 获取会话 =====
        Map<Object, Object> session = sessionManager.getSession(request.getInterviewId());

        // ===== 2. 查题目，保存答案 =====
        InterviewQuestion currentQuestion = questionMapper.selectById(request.getQuestionId());
        if (currentQuestion == null) {
            throw new BusinessException("题目不存在");
        }
        // 校验题号一致性
        int currentNumFromRedis = sessionManager.getCurrentQuestionNum(session);
        int currentNumFromDB = currentQuestion.getQuestionNum();
        if (currentNumFromRedis != currentNumFromDB) {
            log.warn("Redis 题号 ({}) 与数据库题号 ({}) 不一致，强制修正", currentNumFromRedis, currentNumFromDB);
            sessionManager.updateCurrentQuestionNum(request.getInterviewId(), currentNumFromDB);
            session.put(InterviewSessionKeys.CURRENT_QUESTION_NUM, currentNumFromDB);
        }
        currentQuestion.setAnswerText(request.getAnswerText());
        questionMapper.updateById(currentQuestion);

        // ===== 3. AI 评分（必须同步完成，因为后续状态机依赖评分结果） =====
        ScoreResult scoreResult = aiEngine.evaluateAnswer(
                currentQuestion.getQuestionText(), request.getAnswerText());
        int score = scoreResult.getScore();
        boolean shouldFollowUp = scoreResult.isShouldFollowUp();

        // 追问次数限制
        int followUpCount = sessionManager.getFollowUpCount(session);
        if (shouldFollowUp && followUpCount >= MAX_FOLLOW_UP) {
            log.info("追问次数达到上限，强制打断");
            shouldFollowUp = false;
        }

        // 保存评分到 DB
        currentQuestion.setScore(score);
        currentQuestion.setComment(scoreResult.getComment());
        questionMapper.updateById(currentQuestion);

        // 更新 Redis 累计分数
        long newAccumulated = sessionManager.addScore(request.getInterviewId(), score);
        session.put(InterviewSessionKeys.SCORE_ACCUMULATED, (int) newAccumulated);

        // ===== 4. 状态机决策 =====
        // 构建一个"修正后"的 ScoreResult 给状态机
        ScoreResult effectiveResult = ScoreResult.builder()
                .score(score)
                .comment(scoreResult.getComment())
                .shouldFollowUp(shouldFollowUp)
                .followUpReason(scoreResult.getFollowUpReason())
                .build();
        InterviewAction action = stateMachine.decide(session, effectiveResult, TOTAL_QUESTIONS, MAX_FOLLOW_UP);

        // ===== 5. 根据决策构建 SSE 流 =====
        switch (action) {
            case FOLLOW_UP -> {
                // 先非流式生成追问文本，保存到 DB，再流式推给前端
                String followUpText = aiEngine.generateFollowUp(
                        currentQuestion.getQuestionText(), currentQuestion.getAnswerText());

                // 存追问为独立题目到数据库
                InterviewQuestion followUpQuestion = new InterviewQuestion();
                followUpQuestion.setInterviewId(request.getInterviewId());
                followUpQuestion.setQuestionText(followUpText);
                followUpQuestion.setQuestionNum(currentQuestion.getQuestionNum());
                followUpQuestion.setQuestionType("FOLLOW_UP");
                questionMapper.insert(followUpQuestion);

                sessionManager.incrementFollowUp(request.getInterviewId());

                Map<String, Object> followUpMeta = new HashMap<>();
                followUpMeta.put("questionId", followUpQuestion.getId());
                followUpMeta.put("questionNum", currentQuestion.getQuestionNum());
                return aiEngine.streamText(followUpText, followUpMeta);
            }
            case NEXT_QUESTION -> {

                // 重置追问次数（新题目重新计数）
                session.put(InterviewSessionKeys.FOLLOW_UP_COUNT, 0);
                sessionManager.updateField(request.getInterviewId(), InterviewSessionKeys.FOLLOW_UP_COUNT, 0);

                // 先非流式生成下一题，保存到 DB，再流式推给前端
                Long jobId = ((Number) session.get(InterviewSessionKeys.JOB_ID)).longValue();
                Long resumeId = ((Number) session.get(InterviewSessionKeys.RESUME_ID)).longValue();
                JobPosition job = jobPositionMapper.selectById(jobId);
                Resume resume = resumeMapper.selectById(resumeId);
                int nextNum = currentNumFromDB + 1;

                String nextQuestionText = aiEngine.generateQuestion(request.getInterviewId(), job, resume, nextNum);

                InterviewQuestion newQuestion = new InterviewQuestion();
                newQuestion.setInterviewId(request.getInterviewId());
                newQuestion.setQuestionText(nextQuestionText);
                newQuestion.setQuestionNum(nextNum);
                newQuestion.setQuestionType("TECHNICAL");
                questionMapper.insert(newQuestion);

                sessionManager.updateCurrentQuestionNum(request.getInterviewId(), nextNum);

                Map<String, Object> nextMeta = new HashMap<>();
                nextMeta.put("questionId", newQuestion.getId());
                nextMeta.put("questionNum", nextNum);
                nextMeta.put("totalQuestions", TOTAL_QUESTIONS);
                return aiEngine.streamText(nextQuestionText, nextMeta);
            }
            case INTERVIEW_END -> {
                // 面试结束
                Interview interview = interviewMapper.selectById(request.getInterviewId());
                interview.setStatus(InterviewStatus.COMPLETED.name());
                interview.setTotalScore((int) newAccumulated);
                interview.setEndTime(LocalDateTime.now());
                interviewMapper.updateById(interview);

                sessionManager.updateStatus(request.getInterviewId(), InterviewStatus.COMPLETED.name());

                // 🔥 通过 MQ 异步生成报告（避免阻塞"面试结束"响应）
                ReportMessageDTO message = new ReportMessageDTO(request.getInterviewId(), userId);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.REPORT_EXCHANGE,
                        RabbitMQConfig.REPORT_ROUTING_KEY,
                        message
                );
                log.info("📨 报告生成消息已发送至 MQ，interviewId={}", request.getInterviewId());

                // 返回简单的结束 SSE 事件
                SseEmitter emitter = new SseEmitter(30_000L);
                try {
                    Map<String, Object> endData = new HashMap<>();
                    endData.put("type", "INTERVIEW_END");
                    endData.put("totalScore", (int) newAccumulated);
                    endData.put("comment", "面试结束，感谢参与！");
                    emitter.send(SseEmitter.event().name("done").data(endData));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
                return emitter;
            }
            default -> throw new BusinessException("未知的面试动作");
        }
        } finally {
            //释放锁
            distributedLockService.unlock(lockKey, lockValue);
        }
    }


    /*
     * 获取面试状态
     * */
    @Override
    public Map<String, Object> getInterviewStatus(Long interviewId) {
        //1.从redis中获取面试会话记录
        Map<Object, Object> session = sessionManager.getSession(interviewId);
        //2.判断
        if (session == null || session.isEmpty()) {
            throw new BusinessException("面试会话不存在或已过期");
        }
        //3.返回记录
        Map<String, Object> result = new HashMap<>();
        result.put("interviewId", interviewId);// 面试ID
        result.put("currentQuestionNum", session.get(InterviewSessionKeys.CURRENT_QUESTION_NUM));// 当前题目编号
        result.put("totalQuestions", session.get(InterviewSessionKeys.TOTAL_QUESTIONS));// 总题目数
        result.put("scoreAccumulated", session.get(InterviewSessionKeys.SCORE_ACCUMULATED));// 累计分数
        result.put("status", session.get(InterviewSessionKeys.STATUS));// 状态
        return result;
    }

/*
* 面试历史列表
* */
    @Override
    public List<InterviewVO> getHistory(Long userId) {
        //1.查询该用户的所有面试记录
        List<Interview> interviews = interviewMapper.selectList(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getUserId, userId)
                .orderByDesc(Interview::getStartTime)
        );
        if (interviews.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 收集所有 jobId，批量查询岗位名称（避免 N+1 查询）
        //防止多次查询数据库
        List<Long> jobIds = interviews.stream()
                .map(Interview::getJobId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询岗位(组装到一个集合里)
        List<JobPosition> jobs = jobPositionMapper.selectList(
                new LambdaQueryWrapper<JobPosition>()
                        .in(JobPosition::getId, jobIds)//一次就可以查出来
        );
        // 将集合当中的每个title和id一一对应
        Map<Long, String> jobTitleMap = jobs.stream()
                .collect(Collectors.toMap(JobPosition::getId, JobPosition::getTitle));

        //3.批量查询每个面试的题目数量（避免循环中多次查询数据库）
        List<Long>interviewIds=interviews.stream()
                .map(Interview::getId)
                .collect(Collectors.toList());//将每个面试id装进集合当中

        List<InterviewQuestion> allQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .in(InterviewQuestion::getInterviewId, interviewIds)
        );//查出对应面试问题id的所有问题

        //按 interviewId 分组，数出每组有多少条记录，最终得到一个 Map<面试ID, 题目总数>
        Map<Long, Long> questionCountMap = allQuestions.stream()
                .collect(Collectors.groupingBy(InterviewQuestion::getInterviewId, Collectors.counting()));

        // 4. 组装 VO
        return interviews.stream()
                .map(interview -> {
                    String jobTitle = jobTitleMap.getOrDefault(interview.getJobId(), "未知岗位");
                    Long questionCount = questionCountMap.getOrDefault(interview.getId(), 0L);
                    return InterviewVO.builder()
                            .id(interview.getId())
                            .jobTitle(jobTitle)
                            .status(interview.getStatus())
                            .totalScore(interview.getTotalScore())
                            .questionCount(questionCount.intValue())
                            .startTime(interview.getStartTime())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /*
    * 面试状态
    * */
    @Override
    public void terminate(Long id, Long userId) {
        Interview interview = interviewMapper.selectOne(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getId, id)
                        .eq(Interview::getUserId, userId)
        );
        if (interview == null) throw new BusinessException("面试不存在或无权限");

        interview.setStatus("TERMINATED");  // 从 IN_PROGRESS → TERMINATED
        interviewMapper.updateById(interview);
    }

}

