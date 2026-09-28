package com.cjq.pojo.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewAnswerRequest {
    @NotNull(message = "面试ID不能为空")
    private Long interviewId;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotBlank(message = "回答内容不能为空")
    private String answerText;

    @NotBlank(message ="幂等token不能为空")//保证点击提交多少次，都只做一次处理
    private String idempotencyToken;//前端必须传
}
