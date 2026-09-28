package com.cjq.consumer;

import com.cjq.config.RabbitMQConfig;
import com.cjq.pojo.DTO.ReportMessageDTO;
import com.cjq.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
// 消费者：处理报告生成消息（从消息队列中获取消息并处理，最终调用服务层方法生成报告）
public class ReportConsumer {
    private final ReportService reportService;
    @RabbitListener(queues = RabbitMQConfig.REPORT_QUEUE)
    //这个注解会在 Spring 启动时也生效，它会告诉 RabbitMQ：“如果 interview.report.queue 里有消息，
    // 就立刻推给我，我调 handleReport 方法去处理。”
    public void handReport(ReportMessageDTO message){
        log.info("📥 消费者收到报告生成消息：interviewId={}", message.getInterviewId());
        try {
            reportService.generateReport(message.getInterviewId());
            log.info("✅ 报告生成完成：interviewId={}", message.getInterviewId());
        }catch (Exception e){
            log.error("❌ 报告生成失败：interviewId={}, error={}",
                    message.getInterviewId(), e.getMessage(), e);
            // 抛出异常让 MQ 自动重试（默认会重试 3 次）
            throw e;
        }
    }
}
