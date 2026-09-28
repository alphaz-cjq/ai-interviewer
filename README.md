# AI 智能面试官系统

基于 RAG（检索增强生成）的 AI 面试平台，覆盖「简历解析 → 语义出题 → 流式对话 → 自动评分」全流程。

## 技术栈

- **后端**：Spring Boot 3.2、Spring Security、JWT、MyBatis-Plus、Redis、MySQL、RabbitMQ
- **AI**：Spring AI、ChromaDB、Apache Tika
- **前端**：Vue 3、SSE

## 核心功能

- **RAG 语义出题**：解析简历 / JD，向量化检索，AI 基于候选人真实背景出题
- **SSE 流式对话**：打字机式逐字输出
- **状态机驱动的多轮面试**：出题 → 评分 → 追问 / 下一题 → 结束
- **并发控制**：幂等、分布式锁、缓存一致性（穿透 / 击穿 / 雪崩）
- **消息队列**：RabbitMQ 异步生成评估报告

## 启动方式

1. 准备依赖：MySQL、Redis、RabbitMQ、ChromaDB
2. 配置环境变量：`DB_PASSWORD`、`RABBITMQ_PASSWORD`、`DASHSCOPE_API_KEY`
3. 后端：IDEA 打开运行 `AnswerWebApplication`（或 `mvn -pl answer-web spring-boot:run`）
4. 前端：进入前端目录 `npm install` 后 `npm run dev`
