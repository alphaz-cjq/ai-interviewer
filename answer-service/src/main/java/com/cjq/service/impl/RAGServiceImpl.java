package com.cjq.service.impl;


import com.cjq.pojo.PO.SearchHit;
import com.cjq.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {


    private final VectorStore vectorStore;             //负责和Chroma数据库通信

    /*
     *检索相似简历片段（按 resumeId 过滤）
     * */
    @Override
    public List<SearchHit> searchResumeChunks(String queryText, int topK, Long resumeId) {
        return doSearch(queryText, topK,
                "type == 'resume' AND resumeId == '" + resumeId + "'");
    }

    /*
     *检索相似 JD 片段（按 jobId 过滤）
     * */
    @Override
    public List<SearchHit> searchJobDescriptionChunks(String queryText, int topK, Long jobId) {
        return doSearch(queryText, topK,
                "type == 'job_description' AND jobId == '" + jobId + "'");
    }

    /*
     * 私有公共检索逻辑：构造 SearchRequest → similaritySearch → 转成 SearchHit 列表
     * */
    private List<SearchHit> doSearch(String queryText, int topK, String filterExpression) {
        //1.构造检索请求
        //SearchRequest：Spring AI 提供的检索参数封装类。
        SearchRequest request = SearchRequest.builder()
                .query(queryText)//用户输入的问题
                .topK(topK)//排序+截取（设置返回的最相似结果数量）
                .filterExpression(filterExpression)//过滤条件
                .build();//构建出一个不可变的请求对象。

        //2.执行相似度搜索(Spring AI 会自动调用Chroma的V1接口，帮生成query_embeddings)
        List<Document> results = vectorStore.similaritySearch(request);

        //3.提取匹配到的文本内容
        return results.stream()
                .map(doc -> {
                    Double score = doc.getScore();
                    return new SearchHit(doc.getText(), score);
                })
                .collect(Collectors.toList());
    }

    /*
     *L - Load（加载）：从内存到持久化向量库
     * 简历入库（向量化+存储）
     * */
    @Override
    public void indexResume(Long resumeId, List<String> chunks) {
        //1.判断:如果没有文本块，直接返回，不浪费API调用
        if (chunks == null || chunks.isEmpty()) {
            log.warn("简历{}没有文本块，跳过向量化", resumeId);
            return;
        }
        log.info("开始向量化简历{}，共{}个文本块", resumeId, chunks.size());

        // 2. 将文本块转换成 Spring AI 的 Document 对象
        //    并附上元数据（Metadata），方便以后按简历 ID 删除或过滤
        List<Document> documents = chunks.stream()
                .map(content -> {
                    Document doc = new Document(content);
                    doc.getMetadata().put("resumeId", resumeId.toString());
                    doc.getMetadata().put("type", "resume");
                    return doc;
                })
                .collect(Collectors.toList());

        // 3. 调用 VectorStore 的 add 方法
        //    这背后自动做了两件事：
        //    ① 循环调用 embeddingClient.embed(text) 把文字变成向量（调阿里云 API）
        //    ② 把生成的向量 + 原文 + 元数据 一起存入 Chroma 数据库

        // 3. 分批存入向量库（阿里云 Embedding API 限制每次最多 10 条文本）
        int batchSize = 10;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
            log.debug("已向量化批次: {}-{} / {}", i + 1, end, documents.size());
        }

        log.info("简历{}向量化完成，共{}个文本块", resumeId, chunks.size());
    }

/*
* 岗位描述向量化
* */
    @Override
    public void indexJobDescription(Long jobId, String jdText) {
        //1.健壮性检查
        if (jdText==null || jdText.isEmpty()) {
            log.warn("岗位描述{}没有文本块，跳过向量化", jobId);
            return;
        }
        log.info("开始向量化岗位描述{}，文本长度{}字", jobId, jdText.length());

        //2.将岗位描述转成Document对象（附带元数据）
        Document doc=new Document(jdText);
        doc.getMetadata().put("jobId", jobId.toString());
        doc.getMetadata().put("type", "job_description");//与简历区分开

        // 3. 存入 Chroma（Spring AI 会自动调用 EmbeddingClient 做向量化）
        vectorStore.add(List.of(doc));

        log.info("✅ 岗位 {} 的 JD 向量化完成", jobId);
    }

    /*
    * JD向量删除
    * */
    @Override
    public void deleteJobDescriptionByJobId(Long jobId) {
        log.info("开始删除岗位 {} 的 JD 向量数据", jobId);
        deleteByFilter("type == 'job_description' AND jobId == '" + jobId + "'",
                "岗位 " + jobId + " 的 JD 向量");
    }

    /*
    * 简历向量删除
    * */
    @Override
    public void deleteResumeByResumeId(Long resumeId) {
        log.info("开始删除简历 {} 的向量数据", resumeId);
        deleteByFilter("type == 'resume' AND resumeId == '" + resumeId + "'",
                "简历 " + resumeId + " 的向量");
    }

    /*
    * 私有公共删除逻辑：按元数据条件过滤 → 查询 → 按 ID 批量删除
    * */
    private void deleteByFilter(String filterExpression, String desc) {
        //1.构建过滤查询：只按元数据条件筛选（空查询，不按语义召回）
        SearchRequest request = SearchRequest.builder()
                .query("")// 空查询，表示不按语义过滤，只按元数据条件筛选
                .topK(Integer.MAX_VALUE)// 保险起见设大，确保查全匹配文档
                .filterExpression(filterExpression)
                .build();

        //2.执行查询，拿到所有匹配的文档
        List<Document> documents = vectorStore.similaritySearch(request);

        if (documents.isEmpty()){
            log.info("{} 没有对应的向量数据，无需删除", desc);
            return;
        }

        //3.提取文档ID列表
        List<String> ids = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        //4.按ID批量删除
        vectorStore.delete(ids);
        log.info("✅ {} 删除完成，共删除 {} 条", desc, ids.size());
    }

    @Override
    public List<String> searchQuestions(String skillKeyword, int topK) {
        // TODO: 对接题库向量库，按技能关键词搜索相似面试题
        log.warn("searchQuestions() 尚未实现，返回空列表。keyword={}", skillKeyword);
        return List.of();
    }
}