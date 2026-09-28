package com.cjq.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/*
*Spring AOP 异常日志输出
* */
@Slf4j
@Aspect // 切面类
@Component // bean
public class LogAspect {
    // 切点
    @Pointcut("execution(* com.cjq.controller..*.*(..))")  // 【已修复】表达式中间缺了空格，且用 .. 匹配所有子包
    public void pointcut() {
    }

    // 通知类型
    @Around("pointcut()")  // 【已修复】之前写成了 controllerPointcut()，切点方法名不匹配导致 AOP 永远不会触发
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();// 开始时间
        String method = joinPoint.getSignature().toShortString();// 获取被拦截方法的简短签名

        //请求进入
        log.info("请求进入：{}", method);

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;// 耗时
            log.info("请求结束：{}，耗时：{}ms", method, cost);
            return result;
        } catch (Throwable e) {
            log.error("接口抛出异常：{}-{}", method, e.getMessage());
            throw e;
        }
    }
}
