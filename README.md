# AI 智能面试官系统

一个以 **AI 面试为业务场景的 Java 后端系统**，重点展示并发控制、消息队列、状态机编排、流式推送等后端核心能力；AI 能力（RAG / 大模型）作为业务增强模块接入。

## 项目定位

- **后端为主体**：围绕 Spring Boot + Redis + MySQL + RabbitMQ，落地并发控制、缓存一致性、状态机、异步消息、SSE 流式推送等后端能力
- **AI 为业务增强**：通过 Spring AI + RAG + 向量库，让面试出题基于候选人真实简历与岗位 JD

## 技术栈

- **后端**：Spring Boot 3.2、Spring Security、JWT、MyBatis-Plus、Redis、MySQL、RabbitMQ
- **AI（业务增强）**：Spring AI、ChromaDB、Apache Tika、通义千问
- **前端**：Vue 3、SSE

## 后端核心能力

- **并发控制**：幂等（Token + Redis SETNX）、分布式锁（UUID + Lua 原子释放 + TTL）、缓存一致性（Cache-Aside + 穿透 / 击穿 / 雪崩防护）
- **消息队列**：RabbitMQ 异步生成评估报告，解耦 + 削峰 + 持久化
- **状态机编排**：面试流程抽象为状态机（出题 → 评分 → 追问 / 下一题 → 结束），决策与执行解耦
- **流式推送**：SSE 打字机式逐字输出，解决异步线程 SecurityContext 丢失问题
- **认证鉴权**：Spring Security + JWT 无状态认证

## 业务功能（AI 增强）

- **RAG 语义出题**：解析简历 / JD，向量化检索，AI 基于候选人真实背景生成题目
- **自动评分**：LLM 多维度评分 + 追问判断

## 启动方式

1. 准备依赖：MySQL、Redis、RabbitMQ、ChromaDB
2. 配置环境变量：`DB_PASSWORD`、`RABBITMQ_PASSWORD`、`DASHSCOPE_API_KEY`
3. 后端：IDEA 运行 `AnswerWebApplication`（或 `mvn -pl answer-web spring-boot:run`）
4. 前端：进入 `frontend/` 目录，`npm install` 后 `npm run dev`
