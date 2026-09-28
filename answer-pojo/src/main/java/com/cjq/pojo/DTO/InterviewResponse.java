package com.cjq.pojo.DTO;

import com.cjq.pojo.Enum.InterviewAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试响应 DTO — 替代所有返回给前端的 Map<String, Object>
 *
 * 【设计思路】
 *  一个类承载所有可能的返回字段，通过 type 区分响应类型。
 *  前端根据 type 决定读取哪些字段：
 *
 *  FOLLOW_UP     → 关注 question、questionId、questionNum
 *  NEXT_QUESTION → 关注 question、questionId、questionNum、totalQuestions
 *  INTERVIEW_END → 关注 totalScore、comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponse {

    // ========== 通用字段（所有响应都有） ==========

    /** 响应类型：FOLLOW_UP / NEXT_QUESTION / INTERVIEW_END */
    private InterviewAction type;

    // ========== 开始面试 专用 ==========
    private Long interviewId;

    // ========== 追问 / 下一题 共用 ==========
    private String question;       // 题目文本
    private Long questionId;       // 题目ID
    private Integer questionNum;   // 第几题

    // ========== 下一题 专用 ==========
    private Integer totalQuestions; // 总题数

    // ========== 评分结果（可选用） ==========
    private Integer score;         // 当前题得分
    private String comment;        // 评价

    // ========== 面试结束 专用 ==========
    private Integer totalScore;    // 总分

    // ========== 静态工厂方法 ==========

    /** 开始面试 */
    public static InterviewResponse start(Long interviewId, String question,
                                           Long questionId, int questionNum, int totalQuestions) {
        return InterviewResponse.builder()
                .type(InterviewAction.NEXT_QUESTION)
                .interviewId(interviewId)
                .question(question)
                .questionId(questionId)
                .questionNum(questionNum)
                .totalQuestions(totalQuestions)
                .build();
    }

    /** 追问 */
    public static InterviewResponse followUp(String question, Long questionId, int questionNum) {
        return InterviewResponse.builder()
                .type(InterviewAction.FOLLOW_UP)
                .question(question)
                .questionId(questionId)
                .questionNum(questionNum)
                .build();
    }

    /** 下一题 */
    public static InterviewResponse nextQuestion(String question, Long questionId,
                                                  int questionNum, int totalQuestions) {
        return InterviewResponse.builder()
                .type(InterviewAction.NEXT_QUESTION)
                .question(question)
                .questionId(questionId)
                .questionNum(questionNum)
                .totalQuestions(totalQuestions)
                .build();
    }

    /** 面试结束 */
    public static InterviewResponse end(int totalScore, String comment) {
        return InterviewResponse.builder()
                .type(InterviewAction.INTERVIEW_END)
                .totalScore(totalScore)
                .comment(comment)
                .build();
    }
}
