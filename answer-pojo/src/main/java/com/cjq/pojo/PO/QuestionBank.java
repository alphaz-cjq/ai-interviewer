package com.cjq.pojo.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_bank")
public class QuestionBank {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;            // 关联的岗位（可为NULL表示通用题）
    private String questionText;   // 题目内容
    private String questionType;   // TECHNICAL / BEHAVIORAL / CODING
    private String difficulty;     // EASY / MEDIUM / HARD
    private String skillTag;       // 关联技能标签，如 "Java基础"
    private String refAnswer;      // 参考答案（评分用）
}
