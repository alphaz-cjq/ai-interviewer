package com.cjq.service.impl;

import com.cjq.pojo.DTO.InterviewSessionKeys;
import com.cjq.pojo.DTO.ScoreResult;
import com.cjq.pojo.Enum.InterviewAction;
import com.cjq.service.InterviewStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
 * 根据面试会话状态和评分结果，决定面试下一步动作（追问、结束、下一题）
 *
 * */

@Slf4j
@Component
public class InterviewStateMachineImpl implements InterviewStateMachine {


    @Override
    public InterviewAction decide(Map<Object, Object> session,// 面试会话状态
                                   ScoreResult scoreResult,//评分结果
                                   int totalQuestions,// 总题目数
                                   int maxFollowUpCount// 最大追问次数
                                  ) {
        //1.提取当前题号和追问次数
        int currentNum=extractInt(session, InterviewSessionKeys.CURRENT_QUESTION_NUM, 0);//当前题号
        int followUpCount=extractInt(session, InterviewSessionKeys.FOLLOW_UP_COUNT, 0);//追问次数

        log.debug("状态机决策：当前题号={}, 追问次数={}, shouldFollowUp={}",
                currentNum, followUpCount, scoreResult.isShouldFollowUp());

        // 2. 优先判断：是否应该追问
        //    条件：AI 认为需要追问 AND 追问次数未达上限
        if (scoreResult.isShouldFollowUp() && followUpCount < maxFollowUpCount){
            log.info("决定：追问（当前已追问 {} 次，上限 {} 次）", followUpCount, maxFollowUpCount);
            return InterviewAction.FOLLOW_UP;//追问
        }
        //3.其次判断是否已经达到最后一题
        if (currentNum>=totalQuestions){
            log.info("决定：结束面试（当前第 {} 题，共 {} 题）", currentNum, totalQuestions);
            return InterviewAction.INTERVIEW_END;
        }
        //4.默认：进入下一题
        log.info("决定：进入下一题（当前第 {} 题）", currentNum);
        return InterviewAction.NEXT_QUESTION;
    }

    /*
    * 判断当前题号是否为最后一题
    * */
    @Override
    public boolean isLastQuestion(Map<Object, Object> session, int totalQuestions) {

        int currentNum = extractInt(session, InterviewSessionKeys.CURRENT_QUESTION_NUM, 1);// 获取当前题号
        return currentNum >= totalQuestions;// 判断是否达到或超过总题目数
    }

    /*
    * 判断是否允许追问
    * */
    @Override
    public boolean canFollowUp(Map<Object, Object> session, ScoreResult scoreResult, int maxFollowUpCount) {

        int count = extractInt(session, InterviewSessionKeys.FOLLOW_UP_COUNT, 0);// 获取追问次数
        return scoreResult.isShouldFollowUp() && count < maxFollowUpCount;// 判断是否允许追问

    }

    // ==================== 私有工具方法 ====================

    /**
     * 从 session 中安全提取整数（处理 null 和类型转换）
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
            log.warn("会话字段 {} 类型转换失败，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }
}
