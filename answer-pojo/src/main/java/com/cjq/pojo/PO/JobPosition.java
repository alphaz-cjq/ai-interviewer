package com.cjq.pojo.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("job_position")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobPosition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;          // 岗位名称，如 "Java后端开发工程师"
    private String description;    // 岗位描述
    private String requirements;   // 任职要求（用于RAG匹配）
    private String skills;         // 技能要求，逗号分隔，如 "Java,Spring,MySQL,Redis"
    private Integer status;        // 1=开放 0=关闭
    private LocalDateTime createTime;
}
