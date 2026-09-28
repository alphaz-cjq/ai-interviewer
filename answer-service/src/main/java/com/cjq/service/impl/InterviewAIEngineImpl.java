package com.cjq.service.impl;

import com.cjq.pojo.DTO.ScoreResult;
import com.cjq.pojo.PO.JobPosition;
import com.cjq.pojo.PO.Resume;
import com.cjq.pojo.PO.SearchHit;
import com.cjq.service.InterviewAIEngine;
import com.cjq.service.RAGService;
import com.cjq.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewAIEngineImpl implements InterviewAIEngine {

    private final ChatClient chatClient;
    private final RAGService ragService;
    private final ObjectMapper objectMapper;

    // ==================== 非流式：生成题目 ====================
//！！！注意：非流式方法不能删除
// 非流式：供业务层同步调用
    //流式：供前端展示调用
    @Override
    public String generateQuestion(Long interviewId, JobPosition job, Resume resume, int questionNum) {
        //1.检索简历片段（用"项目经验 技术栈 工作职责"作为查询向量）
        List<SearchHit> resumeHits = ragService.searchResumeChunks("项目经验    技术栈   工作职责",//查询文本
                3,//  TOP_K：取最相似的 3 个片段
                resume.getId()// 简历ID
        );
        String resumeContext = resumeHits.stream()// 流式处理
                .map(SearchHit::getContent)// 获取片段内容
                .collect(Collectors.joining("\n---\n"));// 拼接成字符串

        // 2. 检索 JD 片段（用"技能要求 任职资格"作为查询向量）
        List<SearchHit> jdHits = ragService.searchJobDescriptionChunks(
                "技能要求 任职资格",
                3,
                job.getId()                 // 岗位 ID（用于过滤）
        );
        String jdContext = jdHits.stream()
                .map(SearchHit::getContent)//提取每个片段的纯文本内容
                .collect(Collectors.joining("\n---\n"));//收集片段内容

        //3.构建Prompt（复用公共方法，与流式版本保持一致的 Prompt）
        String prompt = buildQuestionPrompt(questionNum, resumeContext, jdContext);

        //4.调用大模型生成面试题
        String question = chatClient.prompt()
                .user(prompt)//设置用户信息
                .call()//执行调用
                .content() // 获取生成的面试题
                .trim();

        log.info("生成的第{}道面试题：{}", questionNum,question);
        return question;

    }

    // ==================== 流式：逐字推送已生成文本 ====================

    @Override
    public SseEmitter streamText(String text, Map<String, Object> doneMeta) {

        log.info("🔥 streamText 被调用，文本长度：{}", text.length());
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);//设置5分钟超时

        //SecurityContextHolder底层是ThreadLocal。ThreadLocal线程局部变量
        //解决方案：
        // 第一种：1.在异步执行前，捕获当前 SecurityContext
        SecurityContext securityContext = SecurityContextHolder.getContext();
        log.info("🔥 主线程 SecurityContext: {}", securityContext.getAuthentication());

        //异步执行进行 流式输出问题（去线程池当中借了一个线程）
        //线程切换，ThreadLocal 丢失
        CompletableFuture.runAsync(() -> {

            log.info("🔥 异步线程开始执行！");
            // 2.将 SecurityContext 绑定到当前异步线程 获取userId
            SecurityContextHolder.setContext(securityContext);//这里是解决的线程B的问题
            try {
                // 逐字推送（模拟打字效果，每字间隔 ~20ms）
                for (int i = 0; i < text.length(); i++) {
                    String ch = String.valueOf(text.charAt(i));
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(ch));
                    // 每 10 个字打印一次，避免刷屏
                    if (i % 10 == 0) {
                        log.info("🔥 已发送第 {} 个字", i);
                    }
                    Thread.sleep(20);
                }
                log.info("🔥 所有字已发送完毕，共 {} 个字", text.length());
                // 推送完成事件
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(doneMeta != null ? doneMeta : Map.of()));

                emitter.complete();//进行到这里异步线程完成工作，通过 AsyncContext 告诉 Tomcat发起ASYNC来写回响应
                //Tomcat安排新的线程 （exec-7） 去执行“将响应写回客户端”这个动作。
                //但是！！Servlet 规范规定：每一次进入 DispatcherServlet（Spring MVC 的核心入口），都必须经过完整的过滤器链。）
                //所以新的线程（exec-7）再经过一次过滤器链
                //exec-7 刚被分配，它的 ThreadLocal（SecurityContextHolder）里完全是空的。
                //exec-7 重新进入过滤器链，走到 AuthorizationFilter 时，法官往 SecurityContextHolder 里一看，发现是空的，于是他会判定为“匿名用户”，直接抛 AccessDeniedException。
                log.info("🔥 SseEmitter 已完成");
            } catch (IOException e) {
                log.warn("流式推送失败，可能客户端已断开", e);
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            }finally {
                // ⚠️ 绝对核心：清除当前异步线程的 ThreadLocal 绑定！
                SecurityContextHolder.clearContext();
                log.info("🧹 已安全清除异步线程的 SecurityContext");
            }
        });
        log.info("🔥 streamText 返回 SseEmitter");  // ✅ 加这行
        return emitter;
    }

    // ==================== 非流式：评分 ====================

    @Override
    public ScoreResult evaluateAnswer(String question, String answer) {
        //1.构建评分Prompt
        String prompt = buildScorePrompt(question, answer);

        //2.调用大模型返回（纯文本字符串（String），只不过这个字符串的内容恰好是 JSON 格式。）
            String response = chatClient.prompt()//开始构建提示词
                    .user(prompt)//设置用户信息
                    .call()//执行调用
                    .content();//提取文本内容
            log.info("AI评分结果：{}", response);

            try {
                // 3. 清洗 Markdown 格式（AI 有时会加 ```json）
                // 清洗后的 JSON 字符串
                String cleanJson = JsonUtils.cleanJsonResponse(response);


                //4.解析JSON
                // 使用 ObjectMapper 解析 JSON 字符串为 JsonNode（Java对象）
                JsonNode jsonNode = objectMapper.readTree(cleanJson);

                //5.构建返回结果
                ScoreResult result = ScoreResult.builder()
                        .score(jsonNode.path("score").asInt(5))
                        .comment(jsonNode.path("comment").asText("暂无评价"))
                        .shouldFollowUp(jsonNode.path("shouldFollowUp").asBoolean(false))
                        .followUpReason(jsonNode.path("followUpReason").asText(""))
                        .build();

                //6. 钳制分数范围（防止 AI 乱给分）
                log.info("评分结果：score={}, shouldFollowUp={}", result.getScore(), result.isShouldFollowUp());
                return result;
            } catch (Exception e) {
                log.error("AI评分失败，原返回内容：{}", response, e);
                return ScoreResult.fallback();
            }
    }

    // ==================== 非流式：追问 ====================

    @Override
    public String generateFollowUp(String question, String answer) {
        //1.构建追问Prompt
        String prompt = buildFollowUpPrompt(question, answer);
        //2.调用大模型生成追问
        String followUp = chatClient.prompt()
                .user(prompt)
                .call()
                .content()
                .trim();

        log.info("生成追问：{}", followUp);
        return followUp;
    }

    // ==================== 以下私有 Prompt 构建方法 ====================

    /**
     * 构建出题 Prompt
     */
    private String buildQuestionPrompt(int questionNum, String resumeContext, String jdContext) {
        return """
                你是一位资深技术面试官，正在为候选人出第 %d 道面试题。

                【候选人简历片段】
                %s

                【岗位要求片段】
                %s

                请生成一道技术面试题，要求：
                1. 必须结合简历中的项目经验或技术栈
                2. 必须结合岗位要求中的关键技术
                3. 题目要有实际场景，避免八股文
                4. 不要重复之前已问过的知识点
                5. 输出格式：直接输出题目内容，不需要额外说明

                题目：
                """.formatted(questionNum, resumeContext, jdContext);
    }

    /**
     * 构建评分 Prompt
     */
    private String buildScorePrompt(String question, String answer) {
        return """
                作为面试官，请对以下回答进行评分，并判断是否需要追问。

                题目：%s
                回答：%s

                请输出 JSON 格式（不要加 markdown 代码块）：
                {
                    "score": 整数（0-10）,
                    "comment": "一句话评价",
                    "shouldFollowUp": true/false,
                    "followUpReason": "追问原因（如果 shouldFollowUp 为 true）"
                }
                如果回答字数少于 20 字，或回答明显敷衍（如仅输入数字、标点符号），请直接将 shouldFollowUp 设为 false，不触发追问。
                """.formatted(question, answer);
    }
    /**
     * 构建追问 Prompt
     */
    private String buildFollowUpPrompt(String question, String answer) {
        return """
                你是面试官，刚才问了候选人这个问题：
                %s

                候选人回答：
                %s

                请生成一个追问，引导候选人深入回答。
                直接输出追问内容，不要额外说明。
                """.formatted(question, answer);
    }
}
