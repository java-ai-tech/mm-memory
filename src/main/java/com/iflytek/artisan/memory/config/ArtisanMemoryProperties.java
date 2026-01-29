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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot configuration properties for ArtisanMemory.
 *
 * <p>Configuration is bound from application.yml with prefix "artisan.memory":
 * <pre>
 * artisan:
 *   memory:
 *     storage:
 *       redis:
 *         host: localhost
 *         port: 6379
 *       mongo:
 *         host: localhost
 *         port: 27017
 *         database: artisan_memory
 *       key-prefix: "session:"
 *     memory:
 *       max-token: 131072
 *       token-ratio: 0.75
 *       auto-compression: true
 *     session:
 *       cleanup-interval-minutes: 10
 *       max-inactive-minutes: 60
 * </pre>
 */
@ConfigurationProperties(prefix = "artisan.memory")
@Data
public class ArtisanMemoryProperties {
    private boolean autoCompression = false;
    private WorkingMemory workingMemory = new WorkingMemory();
    private Session session = new Session();
    private Compression compression = new Compression();

    /**
     * Working Memory 配置。
     *
     * <p>定义了 WorkingMemory 的分区大小和压缩策略的触发条件：
     * <ul>
     *   <li>Head 区域大小 - 保留最旧的 N 轮对话</li>
     *   <li>Tail 区域大小 - 保留最新的 N 轮对话</li>
     *   <li>timingContextWindow 最大条数和 token 阈值</li>
     *   <li>当前轮次摘要的 token 阈值</li>
     * </ul>
     *
     * <p>WorkingMemory 结构：
     * <pre>
     * WorkingMemory (conversationId)
     * ├── Head (最旧 N 轮) - 永不压缩
     * ├── Tail (最新 N 轮) - 永不压缩
     * ├── timingContextWindow (历史摘要 + 当前轮次摘要 + 中间消息)
     * └── Pinned Facts (确认事实) - 永不压缩
     * </pre>
     */
    @Data
    public static class WorkingMemory {
        /**
         * Head 区域保留的轮次数
         * <p>保留最旧的 N 轮对话，永不压缩。
         * <p>默认值：1
         */
        private int headSize = 1;

        /**
         * Tail 区域保留的轮次数
         * <p>保留最新的 N 轮对话，永不压缩。
         * <p>默认值：2
         */
        private int tailSize = 2;

        /**
         * WorkingMemory 在 Redis 中的过期时间（天）
         * <p>默认值：7
         */
        private int expireDays = 7;

        /**
         * Pin 最大数量
         * <p>当 pinnedFacts 数量超过此值时，触发 Pin 聚合。
         * <p>默认值：10
         */
        private int maxPinCount = 10;

        /**
         * Pin 总 token 最大值
         * <p>当 pinnedFacts 总 token 数超过此值时，触发 Pin 聚合。
         * <p>默认值：300
         */
        private int maxPinTokens = 300;

        /**
         * timingContextWindow 最大条数
         * <p>当 timingContextWindow 中的消息数超过此值时，触发历史摘要压缩。
         * <p>默认值：5
         */
        private int timingContextWindowMaxSize = 5;

        /**
         * timingContextWindow 的 token 阈值
         * <p>当 timingContextWindow 中的总 token 数超过此值时，触发历史摘要压缩。
         * <p>默认值：3000
         */
        private int timingContextWindowTokenThreshold = 3000;

        /**
         * 当前对话对压缩的 token 阈值
         * <p>当单个对话轮次（User + Assistant）的 token 数超过此值时，触发当前轮次摘要。
         * <p>默认值：1000
         */
        private int currentRoundTokenThreshold = 1000;

        /**
         * 历史消息总 token 阈值（已废弃）
         * @deprecated 请使用 {@link #timingContextWindowTokenThreshold} 代替
         */
        @Deprecated
        private int historyTokenThreshold = 5000;

        /**
         * 历史消息轮次阈值（已废弃）
         * @deprecated 请使用 {@link #timingContextWindowMaxSize} 代替
         */
        @Deprecated
        private int historyRoundThreshold = 10;

        /**
         * Pin 聚合的数量阈值（已废弃，Pin 永不压缩）
         * @deprecated Pin 永不压缩，此配置不再使用
         */
        @Deprecated
        private int pinCountThreshold = 5;

        /**
         * 工具调用响应压缩的 token 阈值（已废弃，合并到当前轮次摘要）
         * @deprecated 请使用 {@link #currentRoundTokenThreshold} 代替
         */
        @Deprecated
        private int toolCallTokenThreshold = 2000;
    }

    /**
     * Session management configuration (cleanup, timeout).
     */
    @Data
    public static class Session {
        private int cleanupIntervalMinutes = 10;
        private int maxInactiveMinutes = 60;
    }

    /**
     * Compression configuration.
     */
    @Data
    public static class Compression {
        private boolean autoCompression = false;
        private List<String> strategies = new ArrayList<>();
    }
}
