package com.cjq.controller;


import com.cjq.pojo.DTO.JobPositionRequest;
import com.cjq.pojo.PO.JobPosition;
import com.cjq.pojo.common.Result;
import com.cjq.service.JobPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@Tag(name = "岗位管理", description = "岗位的增删改查+JD向量化")
public class JobPositionController {

     private final JobPositionService jobPositionService;//注入Service层

    /*
    * 创建岗位
    * */
    @PostMapping
    @Operation(summary = "创建岗位",description = "创建新岗位，并自动将JD向量化存入Chroma")
    public Result<JobPosition> create(@Valid @RequestBody JobPositionRequest request) {
        //@Valid 表示请求参数需要进行验证
        log.info("创建岗位请求:{}",request.getTitle());
        //1. 构建岗位实体
        JobPosition job = jobPositionService.create(request);
        return Result.success(job);
    }

    /*
    * 岗位列表
    * */
    @GetMapping
    @Operation(summary = "岗位列表", description = "获取所有岗位，按创建时间倒序")
    public Result<List<JobPosition>> list() {
        log.info("获取岗位列表");
        List<JobPosition> list = jobPositionService.listAll();
        return Result.success(list);
    }

    /*
    * 查询岗位详情
    * */
    @GetMapping("/{id}")
    @Operation(summary = "岗位详情", description = "获取指定ID的岗位详情")
    public Result<JobPosition> getById(@PathVariable Long id) {
        log.info("获取岗位详情: {}", id);
        JobPosition job = jobPositionService.getById(id);
        return Result.success(job);
    }

    /*
    * 更新岗位
    * */
    @PutMapping("/{id}")
    @Operation(summary = "更新岗位", description = "更新指定ID的岗位信息")
    public Result<JobPosition> update(@PathVariable("id") Long id,
                                      @RequestBody JobPositionRequest request) {
        log.info("更新岗位请求：id={}, title={}", id, request.getTitle());
        JobPosition job = jobPositionService.update(id, request);
        return Result.success(job);
    }

    /*
    * 删除岗位
    * */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除岗位", description = "删除岗位（数据库记录删除，向量数据暂不删除，但会被过滤掉）")
    public Result<Void> delete(@PathVariable("id") Long id) {
        log.info("删除岗位请求：{}", id);
        jobPositionService.delete(id);
        return Result.success();
    }
}
