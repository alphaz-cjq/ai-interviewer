package com.cjq.service;

import com.cjq.pojo.DTO.InterviewAnswerRequest;
import com.cjq.pojo.DTO.InterviewStartRequest;
import com.cjq.pojo.VO.InterviewVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface InterviewService {

    /**
     * 开始一场面试
     *
     * 返回值是 Map，因为前端需要同时拿到面试ID和第一道题
     *   {
     *     "interviewId": 1,
     *     "question": "请简单介绍一下你的项目经验",
     *     "questionNum": 1,
     *     "totalQuestions": 5
     *   }
     */
    Map<String, Object> startInterview(Long userId, InterviewStartRequest request);

    /**
     * 流式开始面试 — 逐字返回第一道面试题（SSE）
     *
     * 【与 startInterview 的区别】
     *  同样是创建面试 + 出第一题，但题目通过 SseEmitter 逐 token 推送给前端。
     *  DB 写入（创建 Interview 记录、保存 Question、创建 Redis 会话）在流完成回调中执行。
     */
    SseEmitter startInterviewStream(Long userId, InterviewStartRequest request);

    /**
     * 流式提交答案 — 先推送评分，再决定是否流式追问（SSE）
     */
    SseEmitter submitAnswerStream(Long userId, InterviewAnswerRequest request);


    /**
     *提交答案 — 评分 + 追问
     * 下一题
     *
     * 这是面试引擎最复杂的方法！
     *
     * 逻辑分支：
     *   1. 评分当前答案
     *   2. 判断是否需要追问（回答太浅 → 追问；回答充分 → 下一题）
     *   3. 如果追问 → 返回追问内容
     *   4. 如果下一题 → 返回新题目
     *   5. 如果是最后一题 → 返回"面试结束" + 总分
     *
     * @return {
     *   "type": "FOLLOW_UP" | "NEXT_QUESTION" | "INTERVIEW_END",
     *   "question": "...",
     *   "score": 8,
     *   "comment": "你的回答...",
     *   "questionNum": 2
     * }
     */
    Map<String, Object> submitAnswer(Long userId, InterviewAnswerRequest request);



    /**
     * 获取面试状态（用于前端断线重连）
     */
    Map<String, Object> getInterviewStatus(Long interviewId);


    /*
    * 面试历史列表
    * */
    List<InterviewVO> getHistory(Long userId);

    /*
    * 面试状态
    * */
    void terminate(Long id, Long userId);
}
