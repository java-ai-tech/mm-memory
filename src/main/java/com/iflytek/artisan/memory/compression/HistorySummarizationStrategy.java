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

import com.iflytek.artisan.memory.compression.events.HistorySummaryEventHandler;
import com.iflytek.artisan.memory.config.ArtisanMemoryProperties;
import com.iflytek.artisan.memory.model.MessagePair;
import com.iflytek.artisan.memory.model.Msg;
import com.iflytek.artisan.memory.model.MsgRole;
import com.iflytek.artisan.memory.model.TextBlock;
import com.iflytek.artisan.memory.model.WorkingMemory;
import com.iflytek.artisan.memory.util.TokenCounterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史对话摘要策略 - 步骤 3：对 timingContextWindow 中的消息进行摘要（仅生成，不存储）。
 *
 * <p>此策略检查 WorkingMemory 中 timingContextWindow 区的消息条数或总 token 数是否超过限制：
 * <ul>
 *   <li>如果超过：基于 timingContextWindow 生成历史摘要并返回</li>
 *   <li>如果没有超过：不做任何操作</li>
 * </ul>
 *
 * <p>注意：
 * <ul>
 *   <li>此策略只负责生成摘要，不负责存储到 timingContextWindow</li>
 *   <li>清空和添加操作由 {@link HistorySummaryEventHandler} 统一处理</li>
 *   <li>此策略不影响 Head/Tail/Pin</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
public class HistorySummarizationStrategy implements CompressionStrategy {

    private static final Logger log = LoggerFactory.getLogger(HistorySummarizationStrategy.class);

    private final ChatClient chatClient;
    private final PromptConfig promptConfig;
    private final int timingContextWindowMaxSize;
    private final int timingContextWindowTokenThreshold;

    public HistorySummarizationStrategy(ChatClient chatClient, PromptConfig promptConfig, ArtisanMemoryProperties memoryProperties) {
        this.chatClient = chatClient;
        this.promptConfig = promptConfig;
        this.timingContextWindowMaxSize = memoryProperties.getWorkingMemory().getTimingContextWindowMaxSize();
        this.timingContextWindowTokenThreshold = memoryProperties.getWorkingMemory()
                .getTimingContextWindowTokenThreshold();
    }

    @Override
    public CompressionResult compress(String conversationId, WorkingMemory workingMemory, MessagePair currentPair) {
        if (chatClient == null) {
            log.debug("ChatClient not available, skipping history summarization");
            return CompressionResult.notCompressed();
        }

        List<Msg> timingContextWindow = workingMemory.getTimingContextWindow();
        if (timingContextWindow == null || timingContextWindow.isEmpty()) {
            log.debug("timingContextWindow is empty, skipping history summarization");
            return CompressionResult.notCompressed();
        }

        int windowSize = timingContextWindow.size();
        int totalTokens = TokenCounterUtil.calculateToken(timingContextWindow);

        // 检查是否需要压缩
        boolean needsCompression = windowSize > timingContextWindowMaxSize || totalTokens > timingContextWindowTokenThreshold;

        if (!needsCompression) {
            log.debug("{}: windowSize={}, maxSize={}, tokens={}, threshold={}, no compression needed, sessionId={}", this.getName(), windowSize, timingContextWindowMaxSize, totalTokens, timingContextWindowTokenThreshold, conversationId);
            return CompressionResult.notCompressed();
        }

        log.info("{} triggered: windowSize={}, maxSize={}, tokens={}, threshold={}, sessionId={}", this.getName(), windowSize, timingContextWindowMaxSize, totalTokens, timingContextWindowTokenThreshold, conversationId);

        // 生成历史摘要
        Msg summaryMsg = generateHistorySummary(timingContextWindow);
        if (summaryMsg == null) {
            log.warn("Failed to generate history summary");
            return CompressionResult.notCompressed();
        }

        // 返回生成的摘要消息，由 EventHandler 负责清空和存储
        int compressedTokens = TokenCounterUtil.calculateToken(List.of(summaryMsg));
        log.info("{} completed: originalMessages={}, originalTokens={}, compressedTokens={}, sessionId={}", this.getName(), windowSize, totalTokens, compressedTokens, conversationId);

        return CompressionResult.compressedWithSummary(summaryMsg, windowSize - 1);
    }

    /**
     * 生成历史摘要消息
     *
     * @param messages timingContextWindow 中的消息列表
     * @return 历史摘要消息，如果失败返回 null
     */
    private Msg generateHistorySummary(List<Msg> messages) {
        try {
            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(getPromptOrDefault(promptConfig != null ? promptConfig.getHistorySummarizationPrompt() : null, Prompts.HISTORY_SUMMARIZATION_PROMPT)));

            StringBuilder messagesText = new StringBuilder();
            messagesText.append("Historical Messages:\n\n");
            for (int i = 0; i < messages.size(); i++) {
                Msg msg = messages.get(i);
                String roleStr = msg.getRole() != null ? msg.getRole().name() : "UNKNOWN";
                messagesText.append("[")
                        .append(i + 1)
                        .append("] ")
                        .append(roleStr)
                        .append(": ")
                        .append(msg.getTextContent())
                        .append("\n\n");
            }
            promptMessages.add(new UserMessage(messagesText.toString()));

            // 调用 LLM
            String summaryText = chatClient.prompt().messages(promptMessages).call().content();

            // 创建摘要消息
            Msg summaryMsg = Msg.builder()
                    .role(MsgRole.ASSISTANT)
                    .content(TextBlock.of("[历史对话摘要] " + summaryText))
                    .build();

            // 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("summary", true);
            metadata.put("summary_type", "history");
            metadata.put("original_message_count", messages.size());
            summaryMsg.setMetadata(metadata);

            return summaryMsg;
        } catch (Exception e) {
            log.error("Failed to generate history summary", e);
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
        return 3;
    }

    @Override
    public String getName() {
        return "HISTORY_SUMMARIZATION";
    }
}
