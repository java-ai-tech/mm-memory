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
package com.glmapper.memory;

import com.glmapper.memory.model.MessagePair;
import com.glmapper.memory.model.Msg;
import com.glmapper.memory.model.MsgRole;

/**
 * 会话上下文 - ArtisanMemory 内部使用。
 *
 * <p>此轻量级对象持有单个会话的状态，包括：
 * <ul>
 *   <li>会话 ID</li>
 *   <li>存储键</li>
 *   <li>最后访问时间</li>
 *   <li>当前未完成的消息对</li>
 * </ul>
 *
 * <p>注意：这不是 Spring Bean，而是一个简单的状态持有对象。
 * 每个由 ArtisanMemory 管理的会话都有自己的 MemoryContext 实例，
 * 存储在 ConcurrentHashMap 中。
 *
 * @author glsong
 * @since 1.0.0
 */
public class SessionContext {

    private final String sessionId;
    private final String storageKey;
    private volatile long lastAccessTime;
    private volatile MessagePair currentPair = new MessagePair(); // 跟踪当前未完成的消息对

    /**
     * 创建一个新的 MemoryContext。
     *
     * @param sessionId  会话标识符
     * @param storageKey Redis/MongoDB 的存储键
     */
    SessionContext(String sessionId, String storageKey) {
        this.sessionId = sessionId;
        this.storageKey = storageKey;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * Gets the session ID.
     *
     * @return session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the storage key.
     *
     * @return storage key
     */
    public String getStorageKey() {
        return storageKey;
    }

    /**
     * Gets the last access time in milliseconds.
     *
     * @return last access time
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * 更新最后访问时间为当前时间。
     */
    public void updateLastAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 检查此会话是否在指定的时长内未活跃。
     *
     * @param maxInactiveMillis 最大不活跃时间（毫秒）
     * @return 如果会话不活跃返回 true，否则返回 false
     */
    public boolean isInactive(long maxInactiveMillis) {
        return System.currentTimeMillis() - lastAccessTime > maxInactiveMillis;
    }

    /**
     * Gets the current incomplete message pair.
     *
     * @return current pair, or null if none
     */
    public MessagePair getCurrentPair() {
        return currentPair;
    }

    /**
     * Sets the current incomplete message pair.
     *
     * @param pair the current pair
     */
    public void setCurrentPair(MessagePair pair) {
        this.currentPair = pair;
    }

    /**
     * Clears the current incomplete message pair.
     */
    public void clearCurrentPair() {
        this.currentPair = null;
    }

    /**
     * 向当前轮对话追加一条消息。
     *
     * <p>此方法仅操作 currentPair，不触发存储和压缩。
     * 消息会被组织成 MessagePair（消息对）。
     *
     * <p>消息对是用户消息和助手回复的原子单位，包含以下三种类型：
     * <ul>
     *   <li>用户消息（USER）：开启一个新的消息对</li>
     *   <li>助手消息（ASSISTANT）：完成当前的消息对</li>
     *   <li>工具消息（TOOL）：作为中间消息添加到当前消息对中</li>
     * </ul>
     *
     * <p>注意：此方法不进行实际的存储操作，存储操作由 {@link SessionMemory#commitSessionContext(SessionContext)} 完成。
     *
     * @param message 要追加的消息
     * @return this SessionContext for method chaining
     */
    public SessionContext appendMessage(Msg message) {
        if (message == null) {
            return this;
        }
        // 这个会被重置
        if (currentPair == null) {
            currentPair = new MessagePair();
        }

        if (message.getRole() == MsgRole.USER) {
            currentPair.setUserMessage(message);
        } else if (message.getRole() == MsgRole.ASSISTANT) {
            currentPair.setAssistantMessage(message);
        } else {
            currentPair.getIntermediateMessages().add(message);
        }
        return this;
    }

    @Override
    public String toString() {
        return "MemoryContext{" + "sessionId='" + sessionId + '\'' + ", storageKey='" + storageKey + '\'' + ", lastAccessTime=" + lastAccessTime + '}';
    }
}
