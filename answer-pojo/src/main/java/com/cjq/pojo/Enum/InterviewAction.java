package com.cjq.pojo.Enum;

/**
 * 面试动作枚举 — 状态机的输出
 *
 * 【设计思路】
 *  submitAnswer() 执行完毕后，必然走向三种结果之一：
 *    FOLLOW_UP     → 回答不够深入，追问
 *    NEXT_QUESTION → 当前题过关，进入下一题
 *    INTERVIEW_END → 所有题目完成，面试结束
 *
 *  用枚举显式表达，替代 if-else 中隐式的状态转换。
 */
public enum InterviewAction {
    FOLLOW_UP("追问"),
    NEXT_QUESTION("下一题"),
    INTERVIEW_END("面试结束");

    private final String description;

    InterviewAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
