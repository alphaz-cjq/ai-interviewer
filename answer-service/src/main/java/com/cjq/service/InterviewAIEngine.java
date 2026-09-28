package com.cjq.service;

import com.cjq.pojo.DTO.ScoreResult;
import com.cjq.pojo.PO.JobPosition;
import com.cjq.pojo.PO.Resume;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface InterviewAIEngine {

    /**
     * 生成一道面试题（非流式）
     *
     * 【RAG 流程】
     *  1. 从简历向量库检索相关片段（query: "项目经验 技术栈 工作职责"）
     *  2. 从 JD 向量库检索相关片段（query: "技能要求 任职资格"）
     *  3. 拼接 Prompt 模板
     *  4. 调用 ChatClient 生成题目
     */
    String generateQuestion(Long interviewId, JobPosition job, Resume resume, int questionNum);

    /**
     * 将已生成的文本以 SSE 流式逐字推送给前端（模拟打字效果）
     *
     * 【与 generateQuestion 的关系】
     *  先调用 generateQuestion() 拿到题目文本并保存到 DB，再调用本方法把同一段文本流式输出。
     *  这样不会生成两道不同的题。
     *
     * 【SseEmitter 事件约定】
     *  event:token     data: "文"              → 逐字推送
     *  event:done      data: {"questionNum": 1} → 全部推送完毕（可附带业务元数据）
     *  event:error     data: "错误信息"          → 推送失败
     *
     * @param text     已生成好的完整文本
     * @param doneMeta 推送到 done 事件时附带的元数据（如 questionNum）
     * @return SseEmitter
     */
    SseEmitter streamText(String text, Map<String, Object> doneMeta);

    /**
     * AI 评分 + 判断是否追问（非流式）
     *
     * 【返回 ScoreResult，不再返回 Map】
     *
     * 【JSON 解析容错】
     *  1. 清洗 Markdown 代码块（```json ... ```）
     *  2. 提取第一个 { 到最后一个 }（防止 AI 在 JSON 外说废话）
     *  3. ObjectMapper 解析
     *  4. 解析失败 → 返回 ScoreResult.fallback()
     */
    ScoreResult evaluateAnswer(String question, String answer);

    /**
     * 生成追问（非流式）
     */
    String generateFollowUp(String question, String answer);
}
