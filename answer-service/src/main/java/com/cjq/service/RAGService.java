package com.cjq.service;

import com.cjq.pojo.PO.SearchHit;

import java.util.List;

public interface RAGService {

    /**
     * 根据查询文本检索相似的简历片段
     *
     * @param queryText 查询文本（比如面试题目的关键词）
     * @param topK      返回前 K 个最相似的结果
     * @param resumeId  简历ID（用于过滤，只检索该简历的片段）
     * @return 相似简历片段列表
     */
    List<SearchHit> searchResumeChunks(String queryText, int topK, Long resumeId);

    /**
     * 根据查询文本检索相似的岗位描述（JD）片段
     *
     * @param queryText 查询文本
     * @param topK      返回前 K 个最相似的结果
     * @param jobId     岗位ID（用于过滤，只检索该岗位的 JD）
     * @return 相似 JD 片段列表
     */
    List<SearchHit> searchJobDescriptionChunks(String queryText, int topK, Long jobId);

    /**
     * 将简历文本向量化并存入向量库
     *
     * @param resumeId 简历ID
     * @param chunks   文本片段列表
     */
    void indexResume(Long resumeId, List<String> chunks);

    /**
     * 将 JD（岗位描述）向量化并存入向量库
     *
     * @param jobId 岗位ID
     * @param jdText JD文本
     */
    void indexJobDescription(Long jobId, String jdText);

    /**
     * 根据 jobId 删除对应的 JD 向量数据
     * @param jobId 岗位ID
     */
    void deleteJobDescriptionByJobId(Long jobId);

    /**
     * 根据 resumeId 删除对应的简历向量数据
     * @param resumeId 简历ID
     */
    void deleteResumeByResumeId(Long resumeId);

    /**
     * 从题库中检索相关题目
     *
     * @param skillKeyword 技能关键词，如 "Java多线程"
     * @param topK         返回前 K 题
     * @return 相关题目的文本列表
     */
    List<String> searchQuestions(String skillKeyword, int topK);
}
