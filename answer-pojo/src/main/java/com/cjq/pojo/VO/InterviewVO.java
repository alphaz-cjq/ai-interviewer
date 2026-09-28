package com.cjq.pojo.VO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewVO {
    private Long id;
    private String jobTitle;       // 岗位名称（来自 job_position 表）
    private String status;         // 面试状态
    private Integer totalScore;    // 总分
    private Integer questionCount; // 题目数量
    private LocalDateTime startTime;
}
