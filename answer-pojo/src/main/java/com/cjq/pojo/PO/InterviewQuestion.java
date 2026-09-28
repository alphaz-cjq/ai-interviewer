package com.cjq.pojo.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("interview_question")
@AllArgsConstructor
@NoArgsConstructor
public class InterviewQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long interviewId;      // 属于哪次面试
    private String questionText;   // 题目内容
    private String answerText;     // 候选人的回答（可能为空，如果还没答）
    private String questionType;   // TECHNICAL / BEHAVIORAL / CODING / FOLLOW_UP
    private Integer questionNum;   // 第几题
    private Integer score;         // 该题得分（0-10）
    private String comment;        // AI 对该题回答的评价
}
