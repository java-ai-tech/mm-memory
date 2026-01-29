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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iflytek.artisan.memory.ArtisanMemory;
import com.iflytek.artisan.memory.compression.PromptConfig;
import com.iflytek.artisan.memory.compression.WorkingMemoryCompression;
import com.iflytek.artisan.memory.compression.events.MemoryEventPublisher;
import com.iflytek.artisan.memory.management.StorageClientManager;
import com.iflytek.artisan.memory.model.MessagePair;
import com.iflytek.artisan.memory.storage.OriginalStorage;
import com.iflytek.artisan.memory.storage.WorkingMemoryStorage;
import com.iflytek.artisan.memory.storage.mongodb.MongoOriginalStorage;
import com.iflytek.artisan.memory.storage.mongodb.OriginalMessageRepository;
import com.iflytek.artisan.memory.storage.redis.RedisWorkingMemoryStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Spring Boot auto-configuration for ArtisanMemory.
 *
 * <p>This configuration class automatically configures ArtisanMemory as a Spring Bean
 * when Spring Boot is on the classpath. It enables configuration property binding
 * from application.yml and creates all necessary beans.
 */
@Configuration
@EnableConfigurationProperties(ArtisanMemoryProperties.class)
public class ArtisanMemoryAutoConfiguration {

    /**
     * Creates a specialized RedisTemplate for MessagePair objects.
     *
     * <p>Spring Boot's default RedisTemplate is RedisTemplate<Object, Object>.
     * We need a RedisTemplate<String, MessagePair> for type-safe operations.
     *
     * @param connectionFactory Redis connection factory
     * @return configured RedisTemplate<String, MessagePair>
     */
    @Bean
    @ConditionalOnMissingBean(name = "messagePairRedisTemplate")
    public RedisTemplate<String, MessagePair> messagePairRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, MessagePair> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Configure ObjectMapper with proper modules and error handling
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configure deserialization to handle missing type information gracefully
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

        // Use Jackson2JsonRedisSerializer for type-safe MessagePair serialization
        // This ensures Redis always deserializes to MessagePair, not LinkedHashMap
        Jackson2JsonRedisSerializer<MessagePair> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, MessagePair.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates the OriginalStorage bean using Spring Data MongoDB.
     *
     * @param repository OriginalMessageRepository bean auto-configured by Spring Boot
     * @return MongoOriginalStorage bean
     */
    @Bean
    @ConditionalOnMissingBean(OriginalStorage.class)
    public OriginalStorage originalStorage(OriginalMessageRepository repository) {
        return new MongoOriginalStorage(repository);
    }


    /**
     * 创建 WorkingMemoryStorage bean。
     *
     * <p>使用 StringRedisTemplate 存储 JSON 序列化的对象，因为需要存储多种类型：
     * MessagePair、Msg、Pin。
     *
     * @param connectionFactory Redis 连接工厂
     * @param properties        配置属性
     * @return WorkingMemoryStorage bean
     */
    @Bean
    @ConditionalOnMissingBean(RedisWorkingMemoryStorage.class)
    public WorkingMemoryStorage workingMemoryStorage(RedisConnectionFactory connectionFactory, ArtisanMemoryProperties properties) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(connectionFactory);
        int expireDays = properties.getWorkingMemory().getExpireDays();
        return new RedisWorkingMemoryStorage(stringRedisTemplate, expireDays);
    }

    /**
     * Creates the StorageClientManager bean for managing storage clients.
     *
     * @param workingMemoryStorage working memory storage implementation
     * @param originalStorage      original storage implementation
     * @return StorageClientManager bean
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageClientManager storageClientManager(WorkingMemoryStorage workingMemoryStorage, OriginalStorage originalStorage) {
        return new StorageClientManager(workingMemoryStorage, originalStorage);
    }


    /**
     * Creates the PromptConfig bean for custom compression prompts.
     *
     * <p>Returns an empty PromptConfig that will use default prompts from {@link com.iflytek.artisan.memory.compression.Prompts}.
     * Users can override by providing their own PromptConfig bean with custom prompts.
     *
     * @return PromptConfig bean with default (empty) configuration
     */
    @Bean
    @ConditionalOnMissingBean
    public PromptConfig promptConfig() {
        // Return an empty PromptConfig - will use default prompts
        return PromptConfig.builder().build();
    }

    /**
     * Creates the MemoryEventPublisher bean for internal event management.
     *
     * <p>This is the central event publisher used by ArtisanMemory and all event handlers.
     *
     * @return MemoryEventPublisher bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MemoryEventPublisher memoryEventPublisher() {
        return new MemoryEventPublisher();
    }

    /**
     * Creates the ArtisanMemory service bean.
     *
     * <p>This bean is a singleton that manages all session contexts internally.
     * Users can inject it directly using @Resource or @Autowired.
     *
     * @param properties               configuration properties
     * @param clientManager            storage client manager
     * @param workingMemoryCompression working memory compression executor
     * @return ArtisanMemory service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ArtisanMemory artisanMemory(ArtisanMemoryProperties properties, StorageClientManager clientManager, WorkingMemoryCompression workingMemoryCompression) {
        return new ArtisanMemory(properties, clientManager, workingMemoryCompression);
    }
}
