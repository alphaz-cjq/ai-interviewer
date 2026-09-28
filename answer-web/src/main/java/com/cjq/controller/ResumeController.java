package com.cjq.controller;


import com.cjq.Exceptions.BusinessException;
import com.cjq.pojo.PO.SearchHit;
import com.cjq.pojo.VO.DocumentVO;
import com.cjq.pojo.common.Result;
import com.cjq.service.RAGService;
import com.cjq.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@Tag(name = "简历管理")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final RAGService ragService;

    /**
     * 从 SecurityContext 获取当前登录用户的 userId
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("用户未登录");
        }
        return (Long) authentication.getPrincipal();
    }

    /**
     * 上传简历
     */
    @PostMapping("/upload")
    @Operation(summary = "上传简历", description = "支持 PDF / Word / txt，解析后返回预览文本")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        log.info("上传简历请求：userId={}, fileName={}", userId, file.getOriginalFilename());
        String preview = resumeService.upload(file, userId);
        return Result.success("上传并解析成功：", preview);
    }

    /**
     * 简历列表 — 只返回当前用户的简历
     */
    @GetMapping("/list")
    @Operation(summary = "简历列表", description = "获取当前用户的所有简历")
    public Result<?> list() {
        Long userId = getCurrentUserId();
        log.info("获取简历列表：userId={}", userId);
        List<DocumentVO> list = resumeService.listByUserId(userId);
        return Result.success(list);
    }

    /**
     * 删除简历 — 只能删除自己的简历
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除简历", description = "删除指定简历（物理删除磁盘文件 + 数据库记录）")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long userId = getCurrentUserId();
        log.info("删除简历：id={}, userId={}", id, userId);
        resumeService.delete(id, userId);
        return Result.success();
    }

    @PostMapping("/search")
    @Operation(summary = "语义搜索简历片段")
    public Result<List<SearchHit>> searchResume(@RequestParam("query") String query,
                                                @RequestParam(value = "topK", defaultValue = "3") int topK,
                                                @RequestParam("resumeId") Long resumeId) {
        List<SearchHit> chunks = ragService.searchResumeChunks(query, topK, resumeId);
        return Result.success(chunks);
    }

    /**
     * 简历重命名 — 只能重命名自己的简历
     */
    @PutMapping("/{id}/rename")
    public Result<String> renameResume(@PathVariable("id") Long id,
                                     @RequestParam("fileName") String fileName) {
        Long userId = getCurrentUserId();
        log.info("重命名请求：id={}, userId={}, newName={}", id, userId, fileName);
        resumeService.renameResume(id, userId, fileName);
        return Result.success("重命名成功");
    }

}
