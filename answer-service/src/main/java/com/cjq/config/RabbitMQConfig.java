package com.cjq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//配置RabbitMQ交换机、队列、绑定关系
/**
 * RabbitMQ 配置类 —— 相当于“邮局提前布置好的分拣规则和信箱”
 *
 * 作用：在 Spring 启动时，自动去 RabbitMQ 服务器创建：
 *   1. 一个交换机（分拣中心）
 *   2. 一个队列（收件箱）
 *   3. 一条绑定规则（分拣路线）
 */
public class RabbitMQConfig {
    //交换机名字 —— 相当于“邮局分拣中心的名称” */
    public static final String REPORT_EXCHANGE = "interview.report.exchange";
    //队列名字 —— 相当于“你家的收件箱编号” */
    public static final String REPORT_QUEUE = "interview.report.queue";
    //路由键 —— 相当于“分拣路线的编号” */
    public static final String REPORT_ROUTING_KEY = "interview.report";

    /**
     * 创建一个 Topic 类型的交换机（分拣中心）
     *
     * TopicExchange 的特点：支持通配符，可以按规则模糊匹配路由键。
     * 比如 "interview.*" 可以匹配 "interview.start" 或 "interview.report"。
     *
     * 参数解释：
     *   - name: 交换机名字（interview.report.exchange）
     *   - durable: true（持久化）→ 即使 RabbitMQ 服务器重启，交换机也不消失
     *   - autoDelete: false（不自动删除）→ 没有消费者时也不删除
     */
    @Bean
    public TopicExchange reportExchange(){
        return new TopicExchange(REPORT_EXCHANGE, true, false);
    }

    /**
     * 创建一个队列（消息真正存储的地方）
     *
     * 参数解释：
     *   - name: 队列名字（interview.report.queue）
     *   - durable: true（持久化）→ 消息会存到磁盘，RabbitMQ 重启后消息不丢
     *
     * 补充说明：其他参数如 exclusive（是否独占）、autoDelete（是否自动删除）
     * 这里都没写，使用默认值（非独占、非自动删除）。
     */
    @Bean
    public Queue reportQueue(){
        return new Queue(REPORT_QUEUE, true);
    }

    /**
     * 将队列绑定到交换机上，并指定路由键（分拣规则）
     *
     * 意思是：凡是发到 "interview.report.exchange" 交换机，
     * 且路由键（标签）为 "interview.report" 的消息，
     * 都会投递到 "interview.report.queue" 队列里。
     */
    @Bean
    public Binding reportBinding(){
        return BindingBuilder
                .bind(reportQueue())
                .to(reportExchange())
                .with(REPORT_ROUTING_KEY);
    }

    /**
     * 消息转换器：改用 JSON 序列化（Jackson），替代默认的 Java 原生序列化。
     * 好处：
     *   1. 从根上绕开 Java 反序列化的"信任列表"限制（SecurityException 消失）
     *   2. 跨语言、跨版本更稳（Java 原生序列化对类结构敏感）
     * Spring Boot 会自动把这个 Bean 装配到 RabbitTemplate（生产者）和监听容器（消费者）。
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
