package com.cjq.pojo.DTO;

public final class InterviewSessionKeys {

    private InterviewSessionKeys() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /** 当前题目编号 (Integer) */
    public static final String CURRENT_QUESTION_NUM = "currentQuestionNum";

    /** 总题目数 (Integer) */
    public static final String TOTAL_QUESTIONS = "totalQuestions";

    /** 岗位ID (Long) */
    public static final String JOB_ID = "jobId";

    /** 简历ID (Long) */
    public static final String RESUME_ID = "resumeId";

    /** 累积分数 (Integer) */
    public static final String SCORE_ACCUMULATED = "scoreAccumulated";

    /** 面试状态 — InterviewStatus.name() */
    public static final String STATUS = "status";

    /** 追问次数计数器 (Integer) */
    public static final String FOLLOW_UP_COUNT = "followUpCount";
}
