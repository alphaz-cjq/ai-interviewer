package com.cjq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 *
 * 替代默认的 SimpleAsyncTaskExecutor（每次新建线程，无上限），
 * 使用有界线程池 + 队列，防止线程耗尽。
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);                      // 核心线程数
        executor.setMaxPoolSize(10);                      // 最大线程数
        executor.setQueueCapacity(50);                    // 有界队列
        executor.setKeepAliveSeconds(60);                 // 空闲线程存活时间
        executor.setThreadNamePrefix("async-interview-"); // 线程名前缀（便于排查）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略：交回调用线程执行
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
