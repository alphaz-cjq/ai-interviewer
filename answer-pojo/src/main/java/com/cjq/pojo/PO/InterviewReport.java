package com.cjq.pojo.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_report")
public class InterviewReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long interviewId;      // 关联的面试
    private Integer totalScore;    // 总分
    private String dimensions;     // 各维度得分（JSON 字符串）
    private String strengths;      // 优势总结
    private String weaknesses;     // 不足总结
    private String suggestion;     // 学习和改进建议
    private LocalDateTime createTime;
}
