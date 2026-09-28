package com.cjq.pojo.VO;

import lombok.Builder;
import lombok.Data;
/**
 * 文档（简历）视图对象
 * 用于返回给前端的简历摘要信息
 */
@Data
@Builder
public class DocumentVO {
    private Long id;
    private String name;        // 简历名称
    private String fileType;    // pdf / docx
    private String preview;     // 解析后的文本预览（前200字）
    private String createTime;  // 上传时间
}
