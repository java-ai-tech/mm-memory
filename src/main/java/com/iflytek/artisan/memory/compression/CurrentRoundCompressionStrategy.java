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
package com.iflytek.artisan.memory.compression;

import com.iflytek.artisan.memory.compression.events.EvictedMessageEventHandler;
import com.iflytek.artisan.memory.config.ArtisanMemoryProperties;
import com.iflytek.artisan.memory.model.MessagePair;
import com.iflytek.artisan.memory.model.Msg;
import com.iflytek.artisan.memory.model.MsgRole;
import com.iflytek.artisan.memory.model.TextBlock;
import com.iflytek.artisan.memory.model.WorkingMemory;
import com.iflytek.artisan.memory.util.TokenCounterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前轮次摘要策略 - 步骤 2：对从 Tail 移出的消息对进行摘要（仅生成，不存储）。
 *
 * <p>此策略处理从 Tail 移出的消息对（evictedPair），根据其 token 数量决定处理方式：
 * <ul>
 *   <li>如果 token 超限：使用 LLM 生成陈述句型摘要（去除修饰和过程性信息），返回摘要消息</li>
 *   <li>如果没有超限：返回 notCompressed，由调用方（EventHandler）直接添加原文到 timingContextWindow</li>
 * </ul>
 *
 * <p>注意：
 * <ul>
 *   <li>此策略只负责生成摘要，不负责存储到 timingContextWindow</li>
 *   <li>存储操作由 {@link EvictedMessageEventHandler} 统一处理</li>
 *   <li>传入的 currentPair 实际上是从 Tail 移出的消息对（evictedPair）</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
public class CurrentRoundCompressionStrategy implements CompressionStrategy {

    private final ChatClient chatClient;
    private final PromptConfig promptConfig;
    private final int currentRoundTokenThreshold;

    public CurrentRoundCompressionStrategy(ChatClient chatClient, PromptConfig promptConfig, ArtisanMemoryProperties memoryProperties) {
        this.chatClient = chatClient;
        this.promptConfig = promptConfig;
        this.currentRoundTokenThreshold = memoryProperties.getWorkingMemory().getCurrentRoundTokenThreshold();
    }

    /**
     * 对从 Tail 移出的消息对进行处理（仅生成摘要，不存储）。
     *
     * @param conversationId 会话上下文
     * @param workingMemory  工作记忆（未使用，保持接口兼容）
     * @param evictedPair    从 Tail 移出的消息对
     * @return 如果生成了摘要，返回包含摘要消息的 compressed 结果；否则返回 notCompressed
     */
    @Override
    public CompressionResult compress(String conversationId, WorkingMemory workingMemory, MessagePair evictedPair) {
        if (evictedPair == null || !evictedPair.isComplete()) {
            log.debug("No complete evicted pair, skipping current round summarization");
            return CompressionResult.notCompressed();
        }

        // 计算消息对的 token 数量
        int tokenCount = TokenCounterUtil.calculateToken(evictedPair.getAllMessages());

        if (tokenCount > currentRoundTokenThreshold) {
            // Token 超限，需要生成摘要
            log.info("{} triggered: tokenCount={}, threshold={}, sessionId={}", this.getName(), tokenCount, currentRoundTokenThreshold, conversationId);

            if (chatClient == null) {
                log.warn("ChatClient not available, skipping summarization");
                return CompressionResult.notCompressed();
            }

            // 使用 LLM 生成摘要
            Msg summaryMsg = generateSummary(evictedPair);
            if (summaryMsg != null) {
                // 返回生成的摘要消息，由 EventHandler 负责存储
                int compressedTokens = TokenCounterUtil.calculateToken(List.of(summaryMsg));
                log.info("{} completed: originalTokens={}, compressedTokens={}, sessionId={}", this.getName(), tokenCount, compressedTokens, conversationId);
                return CompressionResult.compressedWithSummary(summaryMsg, 1);
            } else {
                // 压缩失败
                log.warn("Summarization failed, returning notCompressed");
                return CompressionResult.notCompressed();
            }
        } else {
            // 没有超限，不进行压缩，由调用方（EventHandler）直接添加原文
            log.debug("{}: tokenCount={} <= threshold={}, no summarization needed, sessionId={}", this.getName(), tokenCount, currentRoundTokenThreshold, conversationId);
            return CompressionResult.notCompressed();
        }
    }

    /**
     * 使用 LLM 生成摘要消息
     *
     * @param pair 要摘要的消息对
     * @return 摘要消息，如果失败返回 null
     */
    private Msg generateSummary(MessagePair pair) {
        try {
            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(getPromptOrDefault(promptConfig != null ? promptConfig.getCurrentRoundCompressionPrompt() : null, Prompts.CURRENT_ROUND_COMPRESSION_PROMPT)));

            StringBuilder pairText = new StringBuilder();
            pairText.append("User: ").append(pair.getUserMessage().getTextContent()).append("\n\n");
            if (pair.getAssistantMessage() != null) {
                pairText.append("Assistant: ").append(pair.getAssistantMessage().getTextContent()).append("\n");
            }
            // 包含工具调用信息
            if (pair.getIntermediateMessages() != null && !pair.getIntermediateMessages().isEmpty()) {
                pairText.append("\n[包含 ").append(pair.getIntermediateMessages().size()).append(" 条工具调用消息]\n");
            }
            promptMessages.add(new UserMessage(pairText.toString()));

            // 调用 LLM
            String summaryText = chatClient.prompt().messages(promptMessages).call().content();

            // 创建摘要消息
            Msg summaryMsg = Msg.builder()
                    .role(MsgRole.ASSISTANT)
                    .content(TextBlock.of("[当前轮次摘要] " + summaryText))
                    .build();

            // 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("summary", true);
            metadata.put("summary_type", "current_round");
            metadata.put("original_user_msg_id", pair.getUserMessage().getId());
            if (pair.getAssistantMessage() != null) {
                metadata.put("original_assistant_msg_id", pair.getAssistantMessage().getId());
            }
            summaryMsg.setMetadata(metadata);

            return summaryMsg;
        } catch (Exception e) {
            log.error("Failed to generate current round summary", e);
            return null;
        }
    }

    private String getPromptOrDefault(String customPrompt, String defaultPrompt) {
        if (customPrompt != null && !customPrompt.isBlank()) {
            return customPrompt;
        }
        return defaultPrompt;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public String getName() {
        return "CURRENT_ROUND_SUMMARIZATION";
    }
}
