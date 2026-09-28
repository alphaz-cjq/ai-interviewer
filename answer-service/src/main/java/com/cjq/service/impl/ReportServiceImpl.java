package com.cjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cjq.Exceptions.BusinessException;
import com.cjq.mapper.InterviewMapper;
import com.cjq.mapper.InterviewQuestionMapper;
import com.cjq.mapper.InterviewReportMapper;
import com.cjq.pojo.Enum.InterviewStatus;
import com.cjq.pojo.PO.Interview;
import com.cjq.pojo.PO.InterviewQuestion;
import com.cjq.pojo.PO.InterviewReport;
import com.cjq.pojo.VO.InterviewReportVO;
import com.cjq.service.ReportService;
import com.cjq.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final InterviewMapper interviewMapper;
    private final InterviewQuestionMapper questionMapper;
    private final InterviewReportMapper reportMapper;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
/*
* 生成报告
* */
    @Override

    //现在不使用@Async-->而是用MQ
    // ⚠️ 不使用 @Transactional：@Async 与 @Transactional 组合时事务可能不传播
    public void generateReport(Long interviewId) {
        log.info("开始异步生成面试报告：{}",interviewId);

        try {
            //1.获取面试基本信息
            //判断面试基本信息是否存在
            Interview interview = interviewMapper.selectById(interviewId);
            if (interview == null) {
                log.error("面试不存在：{}", interviewId);
                return;
            }

            //2.获取面试所有问答记录(按题号升序)
            List<InterviewQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                    .eq(InterviewQuestion::getInterviewId, interviewId)
                    .orderByAsc(InterviewQuestion::getQuestionNum));
            //判断
            if (questions.isEmpty()) {
                log.error("面试 {} 没有题目记录，跳过报告生成", interviewId);
                return;
            }

            //3.计算统计数据

            //总分
            int totalScore = questions.stream()
                    .mapToInt(q -> q.getScore() != null ? q.getScore() : 0)// 获取分数，如果为空则返回 0
                    .sum();// 计算总分

            //平均分
            double avgScore = questions.stream()
                    .filter(q -> q.getScore() != null)
                    .mapToInt(InterviewQuestion::getScore)
                    .average()
                    .orElse(0.0);// 计算平均分，如果为空则返回 0.0

            //4.构建对话日志（给AI看）
            String conversationLog = questions.stream()
                    .map(q -> String.format("【第%d题】%s\n候选人回答：%s\n评分：%d分\n评语：%s",
                            q.getQuestionNum(),
                            q.getQuestionText(),
                            q.getAnswerText() != null ? q.getAnswerText() : "（未回答）",
                            q.getScore() != null ? q.getScore() : 0,
                            q.getComment() != null ? q.getComment() : "无"))
                    .collect(Collectors.joining("\n\n"));

            //5.构建Prompt
            String prompt = buildReportPrompt(conversationLog, questions.size(), totalScore, avgScore);

            //6.调用大模型生成报告
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();

            log.info("AI 报告原始响应：{}", response);

            //7.清洗并解析JSON
            String cleanJson = JsonUtils.cleanJsonResponse(response);
            Map<String, Object> reportData = objectMapper.readValue(cleanJson, Map.class);

            //8.提取维度分数
            Map<String, Integer> dimensions = (Map<String, Integer>) reportData.get("dimensions");

            //9.保存到数据库
            InterviewReport report = new InterviewReport();
            report.setInterviewId(interviewId);
            report.setTotalScore(totalScore);
            report.setDimensions(objectMapper.writeValueAsString(dimensions));
            report.setStrengths((String) reportData.get("strengths"));
            report.setWeaknesses((String) reportData.get("weaknesses"));
            report.setSuggestion((String) reportData.get("suggestion"));
            report.setCreateTime(LocalDateTime.now());
            reportMapper.insert(report);
        }catch (Exception e){
            log.error("生成面试报告异常：{}", e.getMessage(), e);
            // 降级：生成一份基础报告
            createFallbackReport(interviewId);
        }
    }

/*
* 查询已生成的报告
* */
    @Override
    public InterviewReportVO getByInterviewId(Long interviewId) {
        //1.根据面试id获取报告
        InterviewReport report=reportMapper.selectOne(
                new LambdaQueryWrapper<InterviewReport>()
                        .eq(InterviewReport::getInterviewId,interviewId)
        );

        //2.检查面试是否已经完成
        if (report==null){//当前报告没有
            Interview interview = interviewMapper.selectById(interviewId);
            //面试完成
            if (interview!=null&& InterviewStatus.COMPLETED.name().equals(interview.getStatus())){
                throw new BusinessException("报告正在生成中，请稍后刷新");
            }
            //面试未完成
            throw new BusinessException("报告不存在或面试尚未完成");
        }

        //代码执行到这里说明有报告

        //3.解析AI给的 dimensions JSON格式
        Map<String,Integer>dimensions=new HashMap<>();
        //3.1进行判断 dimensions是否存在
        try {
            //dimensions存在数据
            if (report.getDimensions()!=null&&!report.getDimensions().isEmpty()){

               // ObjectMapper 解析 JSON 字符串为 dimensions
                dimensions=objectMapper.readValue(report.getDimensions(), Map.class);
        }

        }catch (Exception e){
            log.error("解析 dimensions JSON 失败", e);
        }
        //返回构建好的面试报告
        return InterviewReportVO.builder()
                .interviewId(report.getInterviewId())
                .totalScore(report.getTotalScore())
                .dimensions(dimensions)
                .strengths(report.getStrengths())
                .weaknesses(report.getWeaknesses())
                .suggestion(report.getSuggestion())
                .createTime(report.getCreateTime())
                .build();
    }
    // ========== 私有方法 ==========

    /**
     * 构建报告 Prompt
     */
    private String buildReportPrompt(String conversationLog,int questionCount,int totalScore,double avgScore) {
        return """
                你是一位资深技术面试官，请基于以下完整的面试记录，生成一份综合评估报告。

                【面试记录】
                %s

                【统计数据】
                总题目数：%d
                总分：%d
                平均分：%.1f

                请输出严格的 JSON 格式（不要加 markdown 代码块），包含以下字段：
                {
                    "dimensions": {
                        "技术能力": 整数（0-10）,
                        "沟通表达": 整数（0-10）,
                        "逻辑思维": 整数（0-10）,
                        "经验匹配度": 整数（0-10）
                    },
                    "strengths": "候选人的优势总结（150字以内）",
                    "weaknesses": "候选人的不足与待改进点（150字以内）",
                    "suggestion": "具体的学习或改进建议（150字以内）"
                }
                """.formatted(conversationLog, questionCount, totalScore, avgScore);
    }

    /*
    * 降级方案：AI解析失败时生成基础报告
    * */
    private void  createFallbackReport(Long interviewId) {
        try {
            InterviewReport report = new InterviewReport();
            report.setInterviewId(interviewId);
            report.setTotalScore(0);
            Map<String, Integer> dims = new HashMap<>();
            dims.put("技术能力", 5);
            dims.put("沟通表达", 5);
            dims.put("逻辑思维", 5);
            dims.put("经验匹配度", 5);
            report.setDimensions(objectMapper.writeValueAsString(dims));
            report.setStrengths("系统未能生成详细报告，请人工复核面试记录。");
            report.setWeaknesses("建议重新生成报告或手动评估。");
            report.setSuggestion("请参考面试问答记录进行综合判断。");
            report.setCreateTime(LocalDateTime.now());

            reportMapper.insert(report);//插入到数据库
        }
        catch (Exception e){
            log.error("生成降级报告失败：{}", e.getMessage());
        }
    }
}
