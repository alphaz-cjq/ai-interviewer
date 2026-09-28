package com.cjq.pojo.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@TableName("interview")
@AllArgsConstructor
@NoArgsConstructor
public class Interview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;           // 候选人
    private Long jobId;            // 面试的岗位
    private Long resumeId;         // 使用的简历
    private String status;         // IN_PROGRESS / COMPLETED / ABANDONED
    private Integer totalScore;    // 总分（面试结束后计算）
    private String summary;        // AI 生成的面试总结
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
