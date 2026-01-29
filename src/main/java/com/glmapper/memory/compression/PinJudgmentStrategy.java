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
package com.glmapper.memory.compression;

import com.glmapper.memory.model.MessagePair;
import com.glmapper.memory.model.Pin;
import com.glmapper.memory.model.WorkingMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Pin 判定策略 - 步骤 1：判断当前对话轮次是否包含确认事实（Claim/Pin）。
 *
 * <p>此策略是压缩流程的第一步，使用 LLM 智能判断当前对话轮次是否包含值得长期保留的重要信息：
 * <ul>
 *   <li>判断当前对话轮次是否包含确认事实</li>
 *   <li>如果是对历史 Pin 的更正，将原 Pin 标记为 INVALIDATED</li>
 *   <li>提取 Pin 内容（陈述句形式），存储到 WorkingMemory 的 pinnedFacts</li>
 * </ul>
 *
 * <p>Pin 系统用于保留具有长期价值的信息，例如：
 * <ul>
 *   <li>用户的明确偏好设置</li>
 *   <li>重要的约束条件</li>
 *   <li>需要记住的事实信息</li>
 *   <li>决策依据或规则</li>
 * </ul>
 *
 * <p>注意：Pin 永不压缩，会在整个会话生命周期内保持完整。
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
public class PinJudgmentStrategy implements CompressionStrategy {

    private final ChatClient chatClient;
    private final PromptConfig promptConfig;

    public PinJudgmentStrategy(ChatClient chatClient, PromptConfig promptConfig) {
        this.chatClient = chatClient;
        this.promptConfig = promptConfig;
    }

    /**
     * 执行 Pin 判定策略。
     *
     * @param conversationId 会话上下文
     * @param workingMemory  工作记忆
     * @param currentPair    当前对话对
     * @return 压缩结果，如果创建了新 Pin 或失效了旧 Pin 则返回 compressed
     */
    @Override
    public CompressionResult compress(String conversationId, WorkingMemory workingMemory, MessagePair currentPair) {
        if (chatClient == null) {
            log.warn("ChatClient not available, skipping pin judgment");
            return CompressionResult.notCompressed();
        }

        if (conversationId == null || currentPair == null || !currentPair.isComplete()) {
            log.warn("No complete current pair, skipping pin judgment");
            return CompressionResult.notCompressed();
        }

        // 获取历史 Pin
        List<Pin> historyPins = workingMemory.getActivePins();

        // 调用 LLM 进行判断
        PinJudgmentResult judgment = judgePin(currentPair, historyPins);
        if (judgment == null) {
            return CompressionResult.notCompressed();
        }

        log.info("{} judgment completed: shouldPin={}, reason={}, sessionId={}", this.getName(), judgment.shouldPin(), judgment.getReason(), conversationId);

        boolean modified = false;

        // 如果需要 Pin 当前对话
        if (judgment.shouldPin() && judgment.getPinContent() != null && !judgment.getPinContent().isEmpty()) {
            // 创建新的 Pin 实体
            Pin newPin = Pin.builder()
                    .conversationId(conversationId)
                    .content(judgment.getPinContent())
                    .confidence(judgment.getConfidence())
                    .build();

            // 添加来源消息 ID
            if (currentPair.getUserMessage() != null) {
                newPin.addSourceMessageId(currentPair.getUserMessage().getId());
            }
            if (currentPair.getAssistantMessage() != null) {
                newPin.addSourceMessageId(currentPair.getAssistantMessage().getId());
            }

            // 如果是对历史 Pin 的更正，先失效旧 Pin
            if (judgment.getNegatesPinId() != null && !judgment.getNegatesPinId().isEmpty()) {
                boolean invalidated = workingMemory.invalidatePin(judgment.getNegatesPinId());
                if (invalidated) {
                    log.info("Invalidated historical pin: pinId={}, sessionId={}", judgment.getNegatesPinId(), conversationId);
                }
            }

            // 添加新 Pin 到 WorkingMemory
            workingMemory.addPin(newPin);
            log.info("Created new pin: pinId={}, content={}, sessionId={}", newPin.getPinId(), newPin.getContent(), conversationId);
            modified = true;
        }

        if (modified) {
            return CompressionResult.compressed(List.of(), 0);
        }
        return CompressionResult.notCompressed();
    }

    /**
     * 使用 LLM 判断当前对话轮次是否应该被标记为 Pin（公共方法）。
     *
     * @param currentPair 当前对话轮次
     * @param historyPins 历史已标记为 Pin 的列表
     * @return PinJudgmentResult 包含判断结果和理由，如果判断失败返回 null
     */
    public PinJudgmentResult judgePin(MessagePair currentPair, List<Pin> historyPins) {
        try {
            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(getPromptOrDefault(promptConfig != null ? promptConfig.getPinJudgmentPrompt() : null, Prompts.PIN_JUDGMENT_PROMPT_V2)));

            StringBuilder promptText = new StringBuilder();
            promptText.append("current_messages:\n");
            promptText.append("User: ").append(currentPair.getUserMessage().getTextContent()).append("\n");
            if (currentPair.getAssistantMessage() != null) {
                promptText.append("Assistant: ")
                        .append(currentPair.getAssistantMessage().getTextContent())
                        .append("\n");
            }

            // 添加历史 Pin 信息
            if (!historyPins.isEmpty()) {
                promptText.append("\nhistorical_pins:\n");
                for (int i = 0; i < historyPins.size(); i++) {
                    Pin pin = historyPins.get(i);
                    promptText.append("Pin #").append(i + 1).append(" (ID: ").append(pin.getPinId()).append("):\n");
                    promptText.append("Content: ").append(pin.getContent()).append("\n");
                }
            }

            promptMessages.add(new UserMessage(promptText.toString()));

            // 调用 LLM
            String response = chatClient.prompt().messages(promptMessages).call().content();

            // 记录 LLM 原始响应用于调试
            log.info("LLM Pin judgment response: {}", response);

            // 解析 JSON 响应
            return parseJudgmentResponse(response);
        } catch (Exception e) {
            log.error("Failed to judge pin", e);
            return null;
        }
    }

    private PinJudgmentResult parseJudgmentResponse(String response) {
        try {
            boolean shouldPin = response.contains("\"shouldPin\": true") || response.contains("\"shouldPin\":true");
            String reason = extractJsonField(response, "reason");
            String negatesPinId = extractJsonField(response, "negatesPinId");
            String pinContent = extractJsonField(response, "pinContent");
            double confidence = 1.0;

            // 添加诊断日志
            log.debug("Parsed Pin judgment - shouldPin: {}, reason: {}, pinContent: '{}', negatesPinId: {}", shouldPin, reason, pinContent, negatesPinId);

            String confidenceStr = extractJsonField(response, "confidence");
            if (confidenceStr != null && !confidenceStr.isEmpty()) {
                try {
                    confidence = Double.parseDouble(confidenceStr);
                } catch (NumberFormatException ignored) {
                }
            }

            if ("null".equals(negatesPinId) || negatesPinId == null || negatesPinId.isEmpty()) {
                negatesPinId = null;
            }

            // 验证 pinContent
            if (shouldPin && (pinContent == null || pinContent.isEmpty() || "null".equals(pinContent))) {
                log.warn("LLM indicated shouldPin=true but pinContent is empty or null. Response: {}", response);
            }

            return new PinJudgmentResult(shouldPin, reason, negatesPinId, pinContent, confidence);
        } catch (Exception e) {
            log.error("Failed to parse pin judgment response: {}", response, e);
            return null;
        }
    }

    private String extractJsonField(String json, String fieldName) {
        try {
            // 查找字段名
            int start = json.indexOf("\"" + fieldName + "\"");
            if (start == -1) {
                log.debug("Field '{}' not found in JSON response", fieldName);
                return null;
            }

            // 查找冒号
            start = json.indexOf(":", start);
            if (start == -1) {
                log.debug("Colon not found after field '{}'", fieldName);
                return null;
            }
            start++;

            // 跳过空白字符
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                start++;
            }
            if (start >= json.length()) {
                log.debug("Reached end of JSON after field '{}'", fieldName);
                return null;
            }

            // 判断值的类型并提取
            int end;
            char startChar = json.charAt(start);

            if (startChar == '"') {
                // 字符串值
                start++;
                end = start;
                // 处理转义字符
                while (end < json.length()) {
                    if (json.charAt(end) == '"' && (end == start || json.charAt(end - 1) != '\\')) {
                        break;
                    }
                    end++;
                }
                if (end >= json.length()) {
                    log.warn("Unterminated string for field '{}'", fieldName);
                    return null;
                }
            } else if (startChar == 'n' && json.startsWith("null", start)) {
                // null 值
                return null;
            } else {
                // 数字或布尔值
                end = start;
                while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']' && !Character.isWhitespace(json.charAt(end))) {
                    end++;
                }
            }

            String value = json.substring(start, end).trim();
            log.debug("Extracted field '{}': '{}'", fieldName, value);
            return value;

        } catch (Exception e) {
            log.error("Failed to extract field '{}' from JSON", fieldName, e);
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
        return 1;
    }

    @Override
    public String getName() {
        return "PIN_JUDGMENT";
    }

    /**
     * Pin 判断结果
     */
    public static class PinJudgmentResult {
        private final boolean shouldPin;
        private final String reason;
        private final String negatesPinId;
        private final String pinContent;
        private final double confidence;

        public PinJudgmentResult(boolean shouldPin, String reason, String negatesPinId, String pinContent, double confidence) {
            this.shouldPin = shouldPin;
            this.reason = reason;
            this.negatesPinId = negatesPinId;
            this.pinContent = pinContent;
            this.confidence = confidence;
        }

        public boolean shouldPin() {
            return shouldPin;
        }

        public String getReason() {
            return reason;
        }

        public String getNegatesPinId() {
            return negatesPinId;
        }

        public String getPinContent() {
            return pinContent;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}
