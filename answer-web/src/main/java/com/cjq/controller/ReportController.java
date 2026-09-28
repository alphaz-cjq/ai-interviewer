package com.cjq.controller;

import com.cjq.pojo.VO.InterviewReportVO;
import com.cjq.pojo.common.Result;
import com.cjq.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/report")
 @RequiredArgsConstructor
@Tag(name = "面试报告管理", description = "面试报告管理相关接口")
public class ReportController {

     private final ReportService reportService;

    /**
     * 获取面试报告
     */
    @GetMapping("/{interviewId}")
    @Operation(summary = "获取面试报告", description = "如果报告已生成则直接返回，否则提示正在生成")
    public Result<InterviewReportVO> getReport(@PathVariable("interviewId")Long interviewId) {
        log.info("获取面试报告: {}", interviewId);
        //根据面试ID获取报告
        InterviewReportVO report = reportService.getByInterviewId(interviewId);
        return Result.success(report);
    }

    /**
     * 手动触发报告生成
     */
    @PostMapping("/generate/{interviewId}")
    @Operation(summary = "手动触发报告生成", description = "用于重新生成或首次生成")
    public Result<Void> generateReport(@PathVariable Long interviewId) {
        log.info("手动触发报告生成：{}", interviewId);
        reportService.generateReport(interviewId);
        return Result.success(null);
    }
}
