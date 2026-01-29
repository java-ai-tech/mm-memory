package com.glmapper.memory.config;

import com.glmapper.memory.compression.CompressionStrategy;
import com.glmapper.memory.compression.CurrentRoundCompressionStrategy;
import com.glmapper.memory.compression.HistorySummarizationStrategy;
import com.glmapper.memory.compression.PinAggregationStrategy;
import com.glmapper.memory.compression.PinJudgmentStrategy;
import com.glmapper.memory.compression.PromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 压缩策略配置类。
 *
 * <p>按修改方案，压缩策略执行顺序：
 * <ol>
 *   <li>Pin 判定策略 - 提取 Pin 实体，存储到 pinnedFacts</li>
 *   <li>当前轮次摘要策略 - 将摘要/原文添加到 timingContextWindow</li>
 *   <li>历史对话摘要策略 - 仅对 timingContextWindow 进行摘要</li>
 * </ol>
 *
 * <p>默认不开启自动压缩。
 *
 * @author glmapper
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "artisan.memory.compression.auto-compression", havingValue = "true")
public class CompressionStrategyConfig {

    private final ArtisanMemoryProperties memoryProperties;

    public CompressionStrategyConfig(ArtisanMemoryProperties memoryProperties) {
        this.memoryProperties = memoryProperties;
    }

    @Bean
    @ConditionalOnStrategyEnabled("PIN_AGGREGATION")
    public PinAggregationStrategy pinAggregationStrategy(
            ChatClient chatClient,
            PromptConfig promptConfig, ArtisanMemoryProperties memoryProperties) {
        return new PinAggregationStrategy(chatClient, promptConfig, memoryProperties);
    }

    @Bean
    @ConditionalOnStrategyEnabled("PIN_JUDGMENT")
    public PinJudgmentStrategy pinJudgmentStrategy(
            ChatClient chatClient,
            PromptConfig promptConfig) {
        return new PinJudgmentStrategy(chatClient, promptConfig);
    }

    @Bean
    @ConditionalOnStrategyEnabled("CURRENT_ROUND_SUMMARIZATION")
    public CurrentRoundCompressionStrategy currentRoundCompressionStrategy(
            ChatClient chatClient,
            PromptConfig promptConfig) {
        return new CurrentRoundCompressionStrategy(chatClient, promptConfig, memoryProperties);
    }

    @Bean
    @ConditionalOnStrategyEnabled("HISTORY_SUMMARIZATION")
    public HistorySummarizationStrategy historySummarizationStrategy(
            ChatClient chatClient,
            PromptConfig promptConfig) {
        return new HistorySummarizationStrategy(chatClient, promptConfig, memoryProperties);
    }
}
