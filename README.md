# AI 智能面试官

一个基于 **大模型（LLM）+ RAG** 的 AI 应用，业务场景是模拟技术面试。项目同时覆盖两条线：**AI 能力**（文档解析、向量检索、Prompt 调用、SSE 流式对话）和**后端工程**（并发控制、消息队列、状态机编排、缓存一致性）。

## 业务链路

上传简历 → Tika 解析 → 分片向量化进 Chroma → RAG 检索简历 / JD → 大模型出题与评分 → 状态机决定追问 / 下一题 / 结束 → RabbitMQ 异步生成评估报告

## 技术栈

| 层级 | 使用 |
|------|------|
| 后端 | Spring Boot 3.2、Java 17、Maven 多模块 |
| Web / 安全 | Spring MVC、Spring Security、JWT |
| 持久化 | MySQL + MyBatis-Plus |
| 中间件 | Redis、RabbitMQ |
| AI | Spring AI 1.0.0-M6、通义千问 qwen-plus、Embedding text-embedding-v3 |
| 向量库 | ChromaDB |
| 文档解析 | Apache Tika |
| 前端 | Vue 3 + Vite |

## AI 能力

- **RAG 检索增强生成**：Tika 解析 PDF / Word 简历，按「500 字 + 80 字重叠」切分，Embedding 后写入 Chroma；出题时检索候选人简历与岗位 JD 的相关片段，拼进 Prompt 让大模型生成个性化题目。
- **多轮对话编排**：用状态机编排「出题 → 评分 → 追问 / 下一题 → 结束」，评分由大模型以 JSON 返回（分数、评价、是否追问）。
- **SSE 流式输出**：题目逐字推给前端（打字机效果），并解决异步线程下 SecurityContext 丢失的问题。

## 后端工程

- **并发控制**：按 interviewId 加 Redis 分布式锁（SET NX PX + Lua 原子释放）串行化面试流程；用幂等 Token（Redis SETNX）防重复提交。
- **消息队列**：面试结束发消息到 RabbitMQ，消费者异步生成评估报告，解耦 + 削峰 + 失败降级。
- **缓存一致性**：岗位列表 Cache-Aside，处理穿透 / 击穿 / 雪崩。
- **架构设计**：多模块 Maven，核心业务拆成 AIEngine / SessionManager / StateMachine 三个专职类，职责分离。

## 启动方式

### 环境变量

```
DASHSCOPE_API_KEY=你的通义千问 Key（必填）
DB_PASSWORD=MySQL 密码
RABBITMQ_PASSWORD=RabbitMQ 密码
JWT_SECRET=JWT 签名密钥（32 字节以上随机串）
```

在 IDEA 的 Run Configuration → Environment variables 里配置，或设为系统环境变量。

### 依赖

JDK 17、Maven、Node.js；MySQL（库名 `ai_interviewer`）、Redis、RabbitMQ、ChromaDB。执行 `sql/schema.sql` 建表。

### 启动

- 后端：IDEA 运行 `AnswerWebApplication`；或命令行 `mvn install -DskipTests` 后 `mvn -pl answer-web spring-boot:run`（端口 8080）。
- 前端：`cd frontend && npm install && npm run dev`。
