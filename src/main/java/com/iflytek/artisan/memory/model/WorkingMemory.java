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
package com.iflytek.artisan.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WorkingMemory - 工作记忆实体类。
 *
 * <p>工作记忆采用分区结构存储对话上下文：
 * <pre>
 * WorkingMemory (conversationId)
 * ├── Head (最旧 1 轮) - 永不压缩
 * ├── Tail (最新 2 轮) - 永不压缩
 * ├── timingContextWindow (历史摘要 + 当前轮次摘要 + 中间消息，最大 5 条)
 * └── Pinned Facts (确认事实) - 永不压缩
 * </pre>
 *
 * <p>核心设计原则：
 * <ul>
 *   <li>Head/Tail/Pin 永不压缩 - 保留完整上下文</li>
 *   <li>timingContextWindow 是唯一可压缩的区域</li>
 *   <li>原始消息始终保留在 MongoDB，用于审计和 RAG 检索</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkingMemory {

    /**
     * 会话标识符
     */
    private String conversationId;

    /**
     * Head 区域 - 最旧的 1 轮对话，永不压缩
     * <p>保留初始上下文，帮助模型理解对话的起点
     */
    @Builder.Default
    private Deque<MessagePair> head = new ArrayDeque<>();

    /**
     * Tail 区域 - 最新的 2 轮对话，永不压缩
     * <p>保留当前上下文，确保模型能够连贯地响应
     */
    @Builder.Default
    private Deque<MessagePair> tail = new ArrayDeque<>();

    /**
     * timingContextWindow 区域 - 历史摘要 + 当前轮次摘要 + 中间消息
     * <p>最大长度为 5 条消息。当超过限制时，触发历史摘要压缩。
     * <p>存储的是消息（Msg），而不是消息对（MessagePair），因为摘要是单条消息形式。
     */
    @Builder.Default
    private List<Msg> timingContextWindow = new ArrayList<>();

    /**
     * Pinned Facts 区域 - 确认事实列表，永不压缩
     * <p>存储从对话中提取的重要事实信息
     */
    @Builder.Default
    private List<Pin> pinnedFacts = new ArrayList<>();

    /**
     * 获取所有有效的 Pin（状态为 ACTIVE）
     *
     * @return 有效的 Pin 列表
     */
    public List<Pin> getActivePins() {
        return pinnedFacts.stream()
                .filter(Pin::isActive)
                .collect(Collectors.toList());
    }

    /**
     * 添加一个新的 Pin
     *
     * @param pin 要添加的 Pin
     */
    public void addPin(Pin pin) {
        if (pinnedFacts == null) {
            pinnedFacts = new ArrayList<>();
        }
        pinnedFacts.add(pin);
    }

    /**
     * 根据 pinId 将 Pin 标记为失效
     *
     * @param pinId 要失效的 Pin ID
     * @return 如果找到并标记成功返回 true，否则返回 false
     */
    public boolean invalidatePin(String pinId) {
        for (Pin pin : pinnedFacts) {
            if (pin.getPinId().equals(pinId)) {
                pin.invalidate();
                return true;
            }
        }
        return false;
    }

    /**
     * 设置 Head 区域（最旧的 N 轮对话）
     *
     * @param pair 消息对
     * @param maxSize 最大保留数量
     */
    public void setHead(MessagePair pair, int maxSize) {
        if (head == null) {
            head = new ArrayDeque<>();
        }
        // 如果 head 已满，不再添加（head 只保留最早的对话）
        if (head.size() < maxSize) {
            head.addLast(pair);
        }
    }

    /**
     * 添加到 Tail 区域（最新的 N 轮对话）
     *
     * <p>如果 Tail 已满，会将最旧的消息对移出并返回，调用方负责将其移动到 timingContextWindow。
     *
     * @param pair 消息对
     * @param maxSize 最大保留数量
     * @return 被移出的消息对，如果 Tail 未满则返回 null
     */
    public MessagePair addToTail(MessagePair pair, int maxSize) {
        if (tail == null) {
            tail = new ArrayDeque<>();
        }
        
        MessagePair evictedPair = null;
        
        // 如果 Tail 已满，先移出最旧的
        if (tail.size() >= maxSize) {
            evictedPair = tail.removeFirst();
        }
        
        // 添加新的消息对到 Tail
        tail.addLast(pair);
        
        return evictedPair;
    }
    
    /**
     * 将消息对的所有消息添加到 timingContextWindow
     *
     * @param pair 消息对
     */
    public void addPairToTimingContextWindow(MessagePair pair) {
        if (pair == null) {
            return;
        }
        if (timingContextWindow == null) {
            timingContextWindow = new ArrayList<>();
        }
        // 添加消息对中的所有消息
        timingContextWindow.addAll(pair.getAllMessages());
    }

    /**
     * 添加消息到 timingContextWindow
     *
     * @param msg 要添加的消息
     */
    public void addToTimingContextWindow(Msg msg) {
        if (timingContextWindow == null) {
            timingContextWindow = new ArrayList<>();
        }
        timingContextWindow.add(msg);
    }

    /**
     * 清空 timingContextWindow
     */
    public void clearTimingContextWindow() {
        if (timingContextWindow != null) {
            timingContextWindow.clear();
        }
    }

    /**
     * 获取 timingContextWindow 的大小
     *
     * @return timingContextWindow 中的消息数量
     */
    public int getTimingContextWindowSize() {
        return timingContextWindow != null ? timingContextWindow.size() : 0;
    }

    /**
     * 将所有记忆消息按顺序组装为列表（用于构建 Prompt）。
     * <p>顺序：Head + timingContextWindow + Tail
     *
     * @return 按顺序组装的消息列表
     */
    public List<Msg> assembleMessages() {
        List<Msg> messages = new ArrayList<>();

        // 1. Head 区域的消息
        if (head != null) {
            for (MessagePair pair : head) {
                messages.addAll(pair.getAllMessages());
            }
        }

        // 2. timingContextWindow 区域的消息
        if (timingContextWindow != null) {
            messages.addAll(timingContextWindow);
        }

        // 3. Tail 区域的消息
        if (tail != null) {
            for (MessagePair pair : tail) {
                messages.addAll(pair.getAllMessages());
            }
        }

        return messages;
    }

    /**
     * 获取总的对话轮次数
     *
     * @return 对话轮次数（Head + Tail）
     */
    public int getTotalRounds() {
        int count = 0;
        if (head != null) {
            count += head.size();
        }
        if (tail != null) {
            count += tail.size();
        }
        return count;
    }

    /**
     * 检查 WorkingMemory 是否为空
     *
     * @return 如果所有区域都为空返回 true
     */
    public boolean isEmpty() {
        return (head == null || head.isEmpty())
                && (tail == null || tail.isEmpty())
                && (timingContextWindow == null || timingContextWindow.isEmpty())
                && (pinnedFacts == null || pinnedFacts.isEmpty());
    }
}
