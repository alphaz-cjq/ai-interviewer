package com.cjq.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建/更新岗位请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobPositionRequest {
    private String title;//岗位标题
    private String description;//岗位描述
    private String requirements;//岗位要求
    private String skills;//岗位技能
}
