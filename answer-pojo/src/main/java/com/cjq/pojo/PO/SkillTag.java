package com.cjq.pojo.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("skill_tag")
public class SkillTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;        // 标签名，如 "Spring Boot", "JVM调优"
    private String category;       // 分类：BACKEND / FRONTEND / DATABASE / DEVOPS / SOFT_SKILL
}
