package com.cjq.service;

import com.cjq.pojo.DTO.ScoreResult;
import com.cjq.pojo.Enum.InterviewAction;

import java.util.Map;

public interface InterviewStateMachine {

    /**
     * 决定面试下一步动作
     */
    InterviewAction decide(Map<Object, Object> session,
                           ScoreResult scoreResult,
                           int totalQuestions,
                           int maxFollowUpCount);

    /**
     * 判断是否应该强制终止（当前题目数已到上限，是否最后一题）
     */
    boolean isLastQuestion(Map<Object, Object> session, int totalQuestions);

    /**
     * 判断是否允许继续追问（追问次数未达上限 + AI 认为需要追问）
     */
    boolean canFollowUp(Map<Object, Object> session, ScoreResult scoreResult, int maxFollowUpCount);
}
