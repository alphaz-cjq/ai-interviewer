package com.cjq.service;

import com.cjq.pojo.VO.InterviewReportVO;

public interface ReportService {
    /**
     * 生成面试报告（异步任务）
     */
    void generateReport(Long interviewId);

    /**
     * 获取已生成的面试报告
     */
    InterviewReportVO getByInterviewId(Long interviewId);
}
