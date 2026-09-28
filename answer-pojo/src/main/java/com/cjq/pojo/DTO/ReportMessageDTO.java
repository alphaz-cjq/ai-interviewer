package com.cjq.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
//定义消息体
//不能把整个Request对象作为消息体，因为Request对象中包含了许多不必要的字段，而且包含了许多敏感字段，如token
public class ReportMessageDTO {
    private  Long interviewId;
    private  Long userId;
}
