package com.cjq.pojo.VO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class InterviewReportVO {
    private Long interviewId;
    private Integer totalScore;
    private Map<String, Integer> dimensions;  // 各维度得分
    private String strengths;
    private String weaknesses;
    private String suggestion;
    private LocalDateTime createTime;
}
