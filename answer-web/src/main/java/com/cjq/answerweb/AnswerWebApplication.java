package com.cjq.answerweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.cjq")
@MapperScan("com.cjq.mapper")
@EnableAsync
public class AnswerWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnswerWebApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════╗
                ║  🎯 AI 智能面试官系统 启动成功！         ║
                ║  Swagger文档: http://localhost:8080/doc.html ║
                ║  祝你学习顺利！                          ║
                ╚══════════════════════════════════════════╝
                """);
    }

}
