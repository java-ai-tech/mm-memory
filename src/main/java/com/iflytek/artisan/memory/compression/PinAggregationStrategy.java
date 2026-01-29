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

import com.iflytek.artisan.memory.config.ArtisanMemoryProperties;
import com.iflytek.artisan.memory.model.MessagePair;
import com.iflytek.artisan.memory.model.Msg;
import com.iflytek.artisan.memory.model.Pin;
import com.iflytek.artisan.memory.model.TextBlock;
import com.iflytek.artisan.memory.model.WorkingMemory;
import com.iflytek.artisan.memory.util.TokenCounterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Pin 聚合压缩策略。
 *
 * <p>当 WorkingMemory 中的 pinnedFacts 数量或 token 总数超过阈值时，
 * 将现有的 Pins 通过 LLM 聚合为更精简的摘要。
 *
 * <p>触发条件：
 * <ul>
 *   <li>Pin 数量超过配置的最大值（默认 10 条）</li>
 *   <li>Pin 总 token 数超过配置的最大值（默认 300）</li>
 * </ul>
 *
 * <p>聚合策略：
 * <ul>
 *   <li>将所有有效的 Pins 发送给 LLM 进行整合</li>
 *   <li>LLM 返回精简的聚合摘要</li>
 *   <li>清空现有 Pins，添加新的聚合 Pin</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
public class PinAggregationStrategy implements CompressionStrategy {

    private final ChatClient chatClient;
    private final PromptConfig promptConfig;
    private final int maxPinCount;
    private final int maxPinTokens;

    public PinAggregationStrategy(ChatClient chatClient, PromptConfig promptConfig, ArtisanMemoryProperties memoryProperties) {
        this.chatClient = chatClient;
        this.promptConfig = promptConfig;
        this.maxPinCount = memoryProperties.getWorkingMemory().getMaxPinCount();
        this.maxPinTokens = memoryProperties.getWorkingMemory().getMaxPinTokens();
    }

    @Override
    public String getName() {
        return "PIN_AGGREGATION";
    }

    /**
     * 执行 Pin 聚合压缩。
     *
     * @param conversationId 会话标识符
     * @param workingMemory  工作记忆
     * @param currentPair    当前对话对（未使用）
     * @return 压缩结果
     */
    @Override
    public CompressionResult compress(String conversationId, WorkingMemory workingMemory, MessagePair currentPair) {
        if (chatClient == null) {
            log.warn("[MEMORY]-[{}] ChatClient 不可用，跳过 Pin 聚合", conversationId);
            return CompressionResult.notCompressed();
        }

        List<Pin> activePins = workingMemory.getActivePins();
        if (activePins == null || activePins.isEmpty()) {
            log.debug("[MEMORY]-[{}] 没有有效的 Pin，跳过聚合", conversationId);
            return CompressionResult.notCompressed();
        }

        // 计算当前 Pin 的统计信息
        int pinCount = activePins.size();
        int totalTokens = calculatePinTokens(activePins);

        // 检查是否需要聚合
        boolean needsAggregation = pinCount > maxPinCount || totalTokens > maxPinTokens;

        if (!needsAggregation) {
            log.info("[MEMORY]-[{}] Pin 数量({})和 Token({})未超限，无需聚合", conversationId, pinCount, totalTokens);
            return CompressionResult.notCompressed();
        }

        log.info("[MEMORY]-[{}] 触发 Pin 聚合: pinCount={}, maxCount={}, tokens={}, maxTokens={}", conversationId, pinCount, maxPinCount, totalTokens, maxPinTokens);

        try {
            // 调用 LLM 聚合 Pins
            String aggregatedContent = aggregatePins(activePins);

            if (aggregatedContent == null || aggregatedContent.isEmpty()) {
                log.warn("[MEMORY]-[{}] LLM 返回空聚合内容，跳过", conversationId);
                return CompressionResult.notCompressed();
            }

            // 创建聚合后的 Pin
            Pin aggregatedPin = Pin.builder()
                    .conversationId(conversationId)
                    .content(aggregatedContent)
                    .confidence(1.0)
                    .build();

            // 保留所有原 Pin 的 sourceMessageIds
            for (Pin pin : activePins) {
                if (pin.getSourceMessageIds() != null) {
                    pin.getSourceMessageIds().forEach(aggregatedPin::addSourceMessageId);
                }
            }

            int compressedTokens = TokenCounterUtil.calculateToken(List.of(Msg.builder()
                    .content(TextBlock.of(aggregatedContent))
                    .build()));

            log.info("[MEMORY]-[{}] Pin 聚合完成: 原 Pin 数量={}, 原 Token={}, 聚合后 Token={}", conversationId, pinCount, totalTokens, compressedTokens);

            // 返回聚合结果，由 Handler 处理清空和保存
            return CompressionResult.aggregated(aggregatedPin, pinCount);
        } catch (Exception e) {
            log.error("[MEMORY]-[{}] Pin 聚合失败", conversationId, e);
            return CompressionResult.notCompressed();
        }
    }

    /**
     * 计算所有 Pin 的总 token 数。
     */
    private int calculatePinTokens(List<Pin> pins) {
        List<Msg> pinMsgs = new ArrayList<>();
        for (Pin pin : pins) {
            pinMsgs.add(Msg.builder().content(TextBlock.of(pin.getContent())).build());
        }
        return TokenCounterUtil.calculateToken(pinMsgs);
    }

    /**
     * 调用 LLM 聚合多个 Pins。
     */
    private String aggregatePins(List<Pin> pins) {
        try {
            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(getPromptOrDefault(promptConfig != null ? promptConfig.getPinAggregationPrompt() : null, Prompts.PIN_AGGREGATION_PROMPT)));

            StringBuilder pinsText = new StringBuilder();
            pinsText.append("现有的 Pins:\n\n");
            for (int i = 0; i < pins.size(); i++) {
                Pin pin = pins.get(i);
                pinsText.append("Pin #").append(i + 1).append(":\n");
                pinsText.append(pin.getContent()).append("\n\n");
            }

            promptMessages.add(new UserMessage(pinsText.toString()));

            String response = chatClient.prompt().messages(promptMessages).call().content();
            log.debug("[MEMORY] LLM Pin 聚合响应: {}", response);
            return response != null ? response.trim() : null;
        } catch (Exception e) {
            log.error("调用 LLM 聚合 Pins 失败", e);
            return null;
        }
    }

    private String getPromptOrDefault(String configuredPrompt, String defaultPrompt) {
        return (configuredPrompt != null && !configuredPrompt.isEmpty()) ? configuredPrompt : defaultPrompt;
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
