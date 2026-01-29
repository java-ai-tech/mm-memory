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
package com.iflytek.artisan.memory.storage;

import com.iflytek.artisan.memory.model.MessagePair;
import com.iflytek.artisan.memory.model.Msg;
import com.iflytek.artisan.memory.model.Pin;
import com.iflytek.artisan.memory.model.WorkingMemory;

import java.util.List;

/**
 * WorkingMemoryStorage - 工作记忆存储接口。
 *
 * <p>定义了工作记忆的分区存储操作，支持以下区域：
 * <ul>
 *   <li>Head - 最旧的对话轮次</li>
 *   <li>Tail - 最新的对话轮次</li>
 *   <li>timingContextWindow - 历史摘要和中间消息</li>
 *   <li>pinnedFacts - 确认事实</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
public interface WorkingMemoryStorage {

    /**
     * 加载完整的工作记忆
     *
     * @param conversationId 会话标识符
     * @return 工作记忆对象，如果不存在则返回空的 WorkingMemory
     */
    WorkingMemory load(String conversationId);

    /**
     * 保存完整的工作记忆
     *
     * @param workingMemory 要保存的工作记忆
     */
    void save(WorkingMemory workingMemory);

    /**
     * 清空指定会话的工作记忆
     *
     * @param conversationId 会话标识符
     */
    void clear(String conversationId);

    /**
     * 从历史对话恢复工作记忆
     *
     * @param conversationId 会话标识符
     * @param originalPairs 原始消息对列表
     * @return 恢复后的工作记忆
     */
    WorkingMemory recover(String conversationId, List<MessagePair> originalPairs);

    // ==================== Head 操作 ====================

    /**
     * 设置 Head 区域（最旧的对话轮次）
     *
     * @param conversationId 会话标识符
     * @param pair 消息对
     * @param maxSize 最大保留数量
     */
    void setHead(String conversationId, MessagePair pair, int maxSize);

    /**
     * 获取 Head 区域的所有消息对
     *
     * @param conversationId 会话标识符
     * @return Head 区域的消息对列表
     */
    List<MessagePair> getHead(String conversationId);

    // ==================== Tail 操作 ====================

    /**
     * 添加消息对到 Tail 区域（最新的对话轮次）
     *
     * @param conversationId 会话标识符
     * @param pair 消息对
     * @param maxSize 最大保留数量
     */
    void addToTail(String conversationId, MessagePair pair, int maxSize);

    /**
     * 获取 Tail 区域的所有消息对
     *
     * @param conversationId 会话标识符
     * @return Tail 区域的消息对列表
     */
    List<MessagePair> getTail(String conversationId);

    // ==================== timingContextWindow 操作 ====================

    /**
     * 添加消息到 timingContextWindow 区域
     *
     * @param conversationId 会话标识符
     * @param message 消息
     */
    void addToTimingContextWindow(String conversationId, Msg message);

    /**
     * 获取 timingContextWindow 区域的所有消息
     *
     * @param conversationId 会话标识符
     * @return timingContextWindow 区域的消息列表
     */
    List<Msg> getTimingContextWindow(String conversationId);

    /**
     * 清空 timingContextWindow 区域
     *
     * @param conversationId 会话标识符
     */
    void clearTimingContextWindow(String conversationId);

    /**
     * 设置 timingContextWindow 区域的所有消息（替换现有内容）
     *
     * @param conversationId 会话标识符
     * @param messages 消息列表
     */
    void setTimingContextWindow(String conversationId, List<Msg> messages);

    // ==================== Pin 操作 ====================

    /**
     * 添加 Pin 到 pinnedFacts 区域
     *
     * @param conversationId 会话标识符
     * @param pin 要添加的 Pin
     */
    void addPin(String conversationId, Pin pin);

    /**
     * 将指定的 Pin 标记为失效
     *
     * @param conversationId 会话标识符
     * @param pinId 要失效的 Pin ID
     * @return 如果找到并标记成功返回 true，否则返回 false
     */
    boolean invalidatePin(String conversationId, String pinId);

    /**
     * 获取所有有效的 Pin（状态为 ACTIVE）
     *
     * @param conversationId 会话标识符
     * @return 有效的 Pin 列表
     */
    List<Pin> getActivePins(String conversationId);

    /**
     * 获取所有 Pin（包括已失效的）
     *
     * @param conversationId 会话标识符
     * @return 所有 Pin 列表
     */
    List<Pin> getAllPins(String conversationId);

    /**
     * 删除指定的 Pin
     *
     * @param conversationId 会话标识符
     * @param pinId 要删除的 Pin ID
     * @return 如果找到并删除成功返回 true，否则返回 false
     */
    boolean deletePin(String conversationId, String pinId);
}
