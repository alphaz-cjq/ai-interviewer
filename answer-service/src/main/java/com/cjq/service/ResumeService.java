package com.cjq.service;


import com.cjq.pojo.VO.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    /**
     * 上传并解析简历
     *
     * @param file   PDF/Word 文件
     * @param userId 当前用户ID
     * @return 解析结果
     */
    String upload(MultipartFile file, Long userId);

    /**
     * 获取用户的简历列表
     */
    List<DocumentVO> listByUserId(Long userId);

    /**
     * 获取简历解析后的完整文本
     */
    String getParsedText(Long resumeId);

    /**
     * 删除简历
     */
    void delete(Long resumeId, Long userId);

    /*
    * 简历重命名
    * */
    void renameResume(Long id, Long userId, String fileName);
}
