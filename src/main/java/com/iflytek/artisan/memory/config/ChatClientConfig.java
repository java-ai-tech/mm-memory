/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iflytek.artisan.memory.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI ChatClient.
 *
 * <p>This configuration class sets up the ChatClient bean for LLM integration
 * using Spring AI framework with DeepSeek model.
 */
@Configuration
@ConditionalOnClass(ChatClient.class)
public class ChatClientConfig {

    /**
     * Creates a ChatClient bean using the ChatClient.Builder.
     *
     * <p>The ChatClient.Builder is auto-configured by Spring Boot based on
     * the properties in application.yml (spring.ai.deepseek.*).
     *
     * @param chatModel the DeepSeekChatModel auto-configured by Spring Boot
     * @return configured ChatClient instance
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(DeepSeekChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
