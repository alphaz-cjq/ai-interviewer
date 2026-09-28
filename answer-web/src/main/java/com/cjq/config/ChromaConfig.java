package com.cjq.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置。
 * VectorStore 由 ChromaVectorStoreAutoConfiguration 自动创建，
 * 它会读取 yaml 中的 spring.ai.vectorstore.chroma.* 配置。
 * 这里只补充自动配置不会生成的 ChatClient Bean。
 */
@Configuration
public class ChromaConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
