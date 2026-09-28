package com.cjq.controller;

import com.cjq.Exceptions.BusinessException;
import com.cjq.pojo.VO.InterviewVO;
import com.cjq.pojo.common.Result;
import com.cjq.pojo.DTO.InterviewAnswerRequest;
import com.cjq.pojo.DTO.InterviewStartRequest;
import com.cjq.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.lettuce.core.GeoArgs.Unit.m;

@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Tag(name = "面试控制器", description = "智能面试的核心接口")
public class InterviewController {

    private final InterviewService interviewService;

    // ==================== 非流式接口 ====================

    /**
     * 开始面试 — 返回第一道面试题（一次性返回）
     */
    @PostMapping("/start")
    @Operation(summary = "开始面试", description = "返回第一道面试题")
    public Result<Map<String, Object>> startInterview(
            @Valid @RequestBody InterviewStartRequest request
    ) {
        Long userId = getCurrentUserId();
        log.info("开始面试：userId={}, jobId={}", userId, request.getJobId());
        Map<String, Object> result = interviewService.startInterview(userId, request);
        return Result.success(result);
    }

    /**
     * 提交答案 — 返回追问或下一题（一次性返回）
     */
    @PostMapping("/submit")
    @Operation(summary = "提交答案", description = "返回追问或下一题")
    public Result<?> submitAnswer(
            @Valid @RequestBody InterviewAnswerRequest request
    ) {
        Long userId = getCurrentUserId();
        log.info("提交答案：userId={}, interviewId={}", userId, request.getInterviewId());
        Map<String, Object> result = interviewService.submitAnswer(userId, request);
        return Result.success(result);
    }

    // ==================== 流式接口（SSE） ====================

    /**
     * 流式开始面试 — 逐字返回第一道面试题
     *
     * SSE 事件序列：
     *   event:token   data: "请"
     *   event:token   data: "简"
     *   event:token   data: "述"
     *   event:done    data: {"questionNum": 1}
     */
    @PostMapping(value = "/start/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式开始面试", description = "SSE 逐字返回第一道面试题")
    public SseEmitter startInterviewStream(
//            @RequestParam("userId") Long userId, 不从url当中获取id,容易被篡改
            @Valid @RequestBody InterviewStartRequest request) {
        Long userId = getCurrentUserId(); // 从 SecurityContext 获取
        log.info("流式开始面试：userId={}, jobId={}", userId, request.getJobId());
        // Service 层负责：创建 Interview 记录 → AI 流式出题 → 回调中保存 Question + Redis 会话
        return interviewService.startInterviewStream(userId, request);
    }
/*
* 获取当前userId
* */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("用户未登录");
        }
        // 因为在 JwtAuthenticationFilter 中存的 principal 是 Long userId
        return (Long) authentication.getPrincipal();
    }

    /**
     * 流式提交答案 — 先推送评分，再决定是否流式追问
     *
     * SSE 事件序列：
     *   event:score   data: {"score": 8, "comment": "...", "shouldFollowUp": true}
     *   event:token   data: "请"        ← 仅当 shouldFollowUp=true
     *   event:token   data: "详细"      ← 仅当 shouldFollowUp=true
     *   event:done    data: {}
     */
    @PostMapping(value = "/submit/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式提交答案", description = "SSE 先返回评分，再流式返回追问")
    public SseEmitter submitAnswerStream(
            @Valid @RequestBody InterviewAnswerRequest request) {
        Long userId = getCurrentUserId();
        log.info("流式提交答案：userId={}, interviewId={}", userId, request.getInterviewId());
        // Service 层负责：查题目 → 保存答案 → AI 评分+流式追问
        return interviewService.submitAnswerStream(userId, request);
    }

    /**
     * 获取面试状态
     */
    @Operation(summary = "获取面试状态", description = "返回面试状态")
    @GetMapping("/{id}/status")
    public Result<Map<String,Object>> getStatus(@PathVariable("id") Long id) {
        log.info("获取面试状态: {}", id);
        Map<String,Object>result=interviewService.getInterviewStatus(id);
        return Result.success(result);
    }

    /**
     * 面试历史列表
     */
    @Operation(summary = "面试历史列表", description = "返回面试历史列表")
    @GetMapping("/history")
    public Result<?> history() {
        Long userId = getCurrentUserId();
        log.info("查询面试历史列表：userId={}", userId);
        List<InterviewVO> list = interviewService.getHistory(userId);
        return Result.success(list);
    }

/*
* 面试状态
* */
    @PutMapping("/{id}/terminate")
    @Operation(summary = "终止面试", description = "将面试状态改为已终止")
    public Result<Void> terminate(@PathVariable("id") Long id) {
        Long userId = getCurrentUserId();
        log.info("终止面试：id={}, userId={}", id, userId);
        interviewService.terminate(id, userId);
        return Result.success();
    }
}
