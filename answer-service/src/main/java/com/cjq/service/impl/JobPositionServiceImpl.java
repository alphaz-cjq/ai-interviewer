package com.cjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cjq.Exceptions.BusinessException;
import com.cjq.constant.RedisKeyConstants;
import com.cjq.mapper.JobPositionMapper;
import com.cjq.pojo.DTO.JobPositionRequest;
import com.cjq.pojo.PO.JobPosition;
import com.cjq.service.DistributedLockService;
import com.cjq.service.JobPositionService;
import com.cjq.service.RAGService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPositionServiceImpl implements JobPositionService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;//用于JSON序列化和反序列化
    private final DistributedLockService distributedLockService;//用于分布式锁
    private final JobPositionMapper jobPositionMapper;
    private final RAGService ragService;//用于向量化JD
/*
 * 创建岗位
 * JD：requirements（要求）和description（描述）
 * 向量化目的：AI面试官需要看懂 简历（Resume）和职位描述（JD）
* */
    @Override
    @Transactional  // 仅保护 DB 写入；向量化（HTTP 调用）失败不回滚数据库（后续可拆分）
    public JobPosition create(JobPositionRequest request) {
        //1.构建岗位实体
        JobPosition job = JobPosition.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .skills(request.getSkills())
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        //将数据插入数据库
      jobPositionMapper.insert(job);

        // ========== 延迟双删：写 DB 后删缓存 ==========
        //根据key删除缓存中的岗位列表，等待500毫秒，再次删除缓存中的岗位列表
        //这么做的目的就是怕
        stringRedisTemplate.delete(RedisKeyConstants.JOB_LIST_CACHE_KEY);
        // 注册事务提交后的回调：提交后再删一次（真正的"延迟双删"，不阻塞线程/连接）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stringRedisTemplate.delete(RedisKeyConstants.JOB_LIST_CACHE_KEY);
            }
        });

        log.info("岗位创建成功：id={}, title={}", job.getId(), job.getTitle());

        //3.如果JD不为空，则调用RAG服务进行向量化
       if (request.getRequirements()!=null && !request.getRequirements().trim().isEmpty()){
           ragService.indexJobDescription(job.getId(),request.getRequirements());
       }
       return job;
    }

    /*
    * 更新岗位，更新JD向量（如果JD有变化）
    * */
    @Override
    @Transactional
    public JobPosition update(Long id, JobPositionRequest request) {
       //1.根据id检查岗位是否存在
        JobPosition existing=jobPositionMapper.selectById(id);//查出的岗位
        if (existing==null){
            throw new BusinessException("岗位不存在");
        }

        // 2. 更新岗位信息
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setRequirements(request.getRequirements());
        existing.setSkills(request.getSkills());
        // status 如果是前端传的，可以在这里设置；否则保持原值

        jobPositionMapper.updateById(existing);

        // ========== 延迟双删：写 DB 后删缓存 ==========
        stringRedisTemplate.delete(RedisKeyConstants.JOB_LIST_CACHE_KEY);
        // 注册事务提交后的回调：提交后再删一次（真正的"延迟双删"，不阻塞线程/连接）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stringRedisTemplate.delete(RedisKeyConstants.JOB_LIST_CACHE_KEY);
            }
        });

        log.info("岗位更新成功：id={}", id);
        //4.如果JD有变化，先删旧向量，再重新向量化（避免向量库残留重复数据）
        if (request.getRequirements() != null && !request.getRequirements().trim().isEmpty()){
            ragService.indexJobDescription(id, request.getRequirements());
        }
        return existing;
    }

    /*
    * 列出所有岗位
    * */
    @Override
    public List<JobPosition> listAll() {
        // ========== 1. 读缓存 ==========
        try {
            //根据Key获取缓存
            String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstants.JOB_LIST_CACHE_KEY);
            if (cached != null) {
                // 缓存命中，直接返回数据
                return objectMapper.readValue(cached, new TypeReference<List<JobPosition>>() {
                });
            }
        } catch (Exception e) {
            //缓存读异常，查DB（不要影响主链路）
            log.warn("读取岗位列表缓存失败，降级查库", e);
        }

        // ========== 2. 缓存未命中 → 尝试加锁防击穿 ==========
        String lockKey = "lock:job:list";
        String lockValue = UUID.randomUUID().toString();
        boolean locked = false;

        try {
        //尝试获取锁
        //2.1其中一个线程抢到锁之后去-->查数据库-->缓存写入（重建缓存）
        locked = distributedLockService.tryLock(lockKey, lockValue, 10);
        //2.2其他没有抢到的线程选择小睡后重试读缓存
        if (!locked) {
            //获取锁失败->小睡 重读缓存
            Thread.sleep(50);
            // 重读缓存
            String retry = stringRedisTemplate.opsForValue().get(RedisKeyConstants.JOB_LIST_CACHE_KEY);
            if (retry != null) {
                return objectMapper.readValue(retry, new TypeReference<List<JobPosition>>() {
                });
            }
            //重试读不到，放行查DB（兜底）-->也就是其他线程还没查到缓存会接下来3 4步骤
            //那这里为什么不用乐观锁CAS让他们等到缓存建好呢？
            //因为：如果让其他线程“自旋等待”（while(true) 不断检查缓存），会导致这些线程持续占用 CPU 时间片，在 Tomcat 高并发下，CPU 会瞬间飙升，反而比直接查 DB 更糟糕。
        }
        // ========== 3. 查 DB ==========
            //防穿透：缓存空对象-->向Redis仍写null值
        List<JobPosition> list = jobPositionMapper.selectList(null);
        // ========== 4. 回填缓存（防穿透 + 防雪崩） ==========
        String json = objectMapper.writeValueAsString(list);
            //防雪崩：设置随机ttl
            //注意：list.isEmpty() ? 300 : 1800这里TTL 设为 300 秒（过期时间）。这样恶意请求不断查询一个不存在的条件时，缓存直接返回空数组，不再打 DB。
        long ttl = list.isEmpty() ? 300 : 1800 + new Random().nextInt(300);// 空值 5min，正常 30min+随机
        stringRedisTemplate.opsForValue().set(
                RedisKeyConstants.JOB_LIST_CACHE_KEY,
                json,  //向Redis仍写null值-->穿透
                ttl,  //设置随机ttl-->雪崩
                TimeUnit.SECONDS
        );
        return list;
    }catch (InterruptedException e){
            Thread.currentThread().interrupt();  // 恢复中断状态
            log.warn("岗位列表加锁等待被中断", e);
            // 降级：直接查库
            return jobPositionMapper.selectList(null);
        }catch (Exception e){
            log.error("岗位列表缓存回填失败", e);
            // 降级：查库返回
            return jobPositionMapper.selectList(null);
        }finally {
            if (locked) {
                distributedLockService.unlock(lockKey, lockValue);// 释放锁
            }
        }
    }

    /*
    * 根据id获取岗位
    * */
    @Override
    public JobPosition getById(Long id) {
        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BusinessException("岗位不存在");
        }
        return job;
    }

    /*
    * 删除岗位
    * */
    @Override
    @Transactional
    public void delete(Long id) {
       //1.检查岗位是否存在
        JobPosition job = jobPositionMapper.selectById(id);
        if (job == null) {
            throw new BusinessException("岗位不存在");
        }

        jobPositionMapper.deleteById(id);
        log.info("岗位删除成功：id={}", id);

        // ========== 延迟双删：写 DB 后删缓存 ==========
        stringRedisTemplate.delete(RedisKeyConstants.JOB_LIST_CACHE_KEY);
        // 注册事务提交后的回调：提交后再删一次（真正的"延迟双删"，不阻塞线程/连接）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stringRedisTemplate.delete(RedisKeyConstants.JOB_LIST_CACHE_KEY);
            }
        });

        //3.删除向量数据库中的向量
       //⚠️ 向量库中的 JD 不会自动删除（Chroma 不支持按条件删除旧版本）
      //在RAG服务中删除   RAGServiceImp中  deleteJobDescriptionByJobId方法
        try {
            ragService.deleteJobDescriptionByJobId(id);
        } catch (Exception e) {
            // 向量库删除失败不影响主流程，但需要记录日志以便人工介入
            log.error("删除岗位 {} 的向量数据失败，请手动清理 Chroma", id, e);
        }
    }
}
