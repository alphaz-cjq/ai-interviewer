package com.cjq.pojo.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;           // 属于哪个用户
    private String name;           // 简历名称（用户自己起的）
    private String filePath;       // 原始文件存储路径
    private String fileType;       // pdf / docx / txt
    private String parsedText;     // Tika 解析后的纯文本（用于向量化）
    private LocalDateTime createTime;
}
