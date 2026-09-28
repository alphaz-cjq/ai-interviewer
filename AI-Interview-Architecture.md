# AI 智能面试官系统 — 架构设计

## 一、系统概述

基于 RAG（检索增强生成）的 AI 面试平台，覆盖「简历解析 → 语义出题 → 流式对话 → 自动评分 → 报告生成」全流程。系统结合候选人简历与岗位 JD 生成个性化面试题，通过多轮对话完成面试，并自动评分、生成评估报告。

## 二、技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2 |
| 认证鉴权 | Spring Security + JWT |
| ORM | MyBatis-Plus |
| 存储 | MySQL、Redis |
| 消息队列 | RabbitMQ |
| AI | Spring AI、通义千问（qwen-plus / text-embedding-v3）、ChromaDB |
| 文档解析 | Apache Tika |
| 前端 | Vue 3 |
| 流式通信 | SSE（Server-Sent Events） |

## 三、系统架构

后端采用多模块 Maven 架构，按职责分层：

```
answer-web      控制器层（REST API、SSE 流式接口）
answer-service  业务逻辑层（面试引擎、RAG、并发控制）
answer-mapper   数据访问层（MyBatis-Plus）
answer-pojo     数据对象层（DTO / PO / VO / Enum）
answer-utils    工具层（JWT、统一异常、常量、切面）
```

核心业务层拆分为三个专职组件，实现职责分离（高内聚、低耦合）：

- **AIEngine**：统一封装所有 AI 调用（出题、评分、追问、流式输出），切换模型只需改这一个类
- **SessionManager**：管理面试会话（Redis 存储 + MySQL 降级重建）
- **StateMachine**：驱动面试状态流转（出题 → 评分 → 追问 / 下一题 → 结束）

## 四、核心流程

1. **简历入库**：上传 PDF/Word → Tika 解析 → 重叠分片（500 字 + 80 重叠）→ Embedding 向量化 → 存入 Chroma
2. **开始面试**：RAG 检索简历 + JD 片段 → LLM 生成第一题 → SSE 逐字推送给前端
3. **提交答案**：LLM 评分 → 状态机决策（追问 / 下一题 / 结束）
4. **面试结束**：发消息到 RabbitMQ → 消费者异步生成评估报告 → 前端轮询获取

## 五、数据库设计（核心表）

```
user                       interview
┌──────────────┐          ┌──────────────┐
│ id           │──┐       │ id           │
│ username     │  │       │ user_id      │
│ password     │  │       │ job_id       │
│ email        │  │       │ resume_id    │
│ create_time  │  │       │ status       │
└──────────────┘  │       │ total_score  │
                  │       │ start_time   │
resume            │       │ end_time     │
┌──────────────┐  │       └──────┬───────┘
│ id           │  │              │
│ user_id      │  │              │
│ name         │  │              │
│ parsed_text  │  │              │
│ create_time  │  │              │
└──────────────┘  │       ┌──────▼────────┐
                  │       │interview_question│
job_position      │       │ interview_id   │
┌──────────────┐  │       │ question_text  │
│ id           │──┘       │ answer_text    │
│ title        │          │ score          │
│ requirements │          │ comment        │
│ skills       │          │ question_num   │
└──────────────┘          │ question_type  │
                          └──────┬─────────┘
                                 │
                    interview_report
                    ┌──────────────┐
                    │ id           │
                    │ interview_id │
                    │ total_score  │
                    │ dimensions   │
                    │ strengths    │
                    │ weaknesses   │
                    │ suggestion   │
                    └──────────────┘
```

## 六、RAG 检索链路

### 6.1 数据入库（离线）

```
简历 PDF/Word → Tika 解析 → 纯文本 → 重叠分片 → Embedding 向量化 → 存入 Chroma
JD 岗位描述  → 同样处理 → 存入另一 collection
```

### 6.2 面试出题（在线）

1. 用「项目经验、技术栈、工作职责」检索简历片段（Top-K）
2. 用「技能要求、任职资格」检索 JD 片段（Top-K）
3. 把简历片段 + JD 片段拼进 Prompt → LLM 生成一道结合候选人真实背景的题目

### 6.3 评分（在线）

把「题目 + 候选人回答」交给 LLM，输出结构化评分（分数、评价、是否追问、追问原因）。

## 七、并发控制设计

| 场景 | 方案 | 说明 |
|---|---|---|
| 防重复提交 | 幂等 Token + Redis SETNX 原子占位 | 同一 token 只处理一次 |
| 状态流转串行化 | 分布式锁（SET NX PX + UUID + Lua 原子释放 + TTL） | 按面试 ID 加锁 |
| 岗位列表缓存 | Cache-Aside + 空值防穿透 + 随机 TTL 防雪崩 + 锁防击穿 + 延迟双删 | 保证最终一致 |

## 八、消息队列设计

面试结束时，主流程只负责「发消息」，不等待报告生成：

```
面试结束 → RabbitTemplate 发送消息 → 消费者 @RabbitListener 接收 → 调用 AI 生成报告
```

好处：主流程与报告生成解耦、削峰缓冲、消息持久化（服务重启不丢任务）。消息体用 JSON 序列化（Jackson2JsonMessageConverter），避免 Java 原生序列化的安全隐患。

## 九、API 接口设计

```
认证 /api/auth
  POST /api/auth/register        注册
  POST /api/auth/login           登录
  POST /api/auth/send-code       发送验证码

简历 /api/resume
  POST /api/resume/upload        上传简历（PDF/Word）
  GET  /api/resume/list          简历列表

岗位 /api/job
  POST /api/job                  创建岗位
  GET  /api/job                  岗位列表
  GET  /api/job/{id}             岗位详情
  PUT  /api/job/{id}             更新岗位
  DELETE /api/job/{id}           删除岗位

面试 /api/interview
  POST /api/interview/start/stream        流式开始面试（SSE 返回第一题）
  POST /api/interview/submit/stream       流式提交答案（SSE 返回追问/下一题/结束）
  GET  /api/interview/history             面试历史
  GET  /api/interview/{id}/status         面试状态
  PUT  /api/interview/{id}/terminate      终止面试

报告 /api/report
  GET  /api/report/{interviewId}          查询评估报告
```
