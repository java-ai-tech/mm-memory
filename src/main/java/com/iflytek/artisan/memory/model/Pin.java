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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pin 实体类 - 表示一个确认事实（Claim/Pin）。
 *
 * <p>Pin 是从对话中提取的重要事实信息，具有以下特点：
 * <ul>
 *   <li>永不压缩 - Pin 在整个会话生命周期内保持完整</li>
 *   <li>可被更正 - 新的对话可能更正历史 Pin，此时旧 Pin 会被标记为 INVALIDATED</li>
 *   <li>陈述句形式 - Pin 内容以简洁的陈述句形式存储</li>
 * </ul>
 *
 * <p>Pin 用于保留具有长期价值的信息，例如：
 * <ul>
 *   <li>用户的明确偏好设置</li>
 *   <li>重要的约束条件</li>
 *   <li>需要记住的事实信息</li>
 *   <li>决策依据或规则</li>
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
public class Pin {

    /**
     * Pin 的唯一标识符
     */
    @Builder.Default
    private String pinId = UUID.randomUUID().toString();

    /**
     * 所属会话的标识符
     */
    private String conversationId;

    /**
     * Pin 的内容（陈述句形式）
     * <p>例如："用户偏好使用中文交流"、"项目截止日期为2026年3月"
     */
    private String content;

    /**
     * 置信度（0.0 - 1.0）
     * <p>表示该 Pin 的可信程度，由 LLM 判断
     */
    @Builder.Default
    private double confidence = 1.0;

    /**
     * 来源消息的 ID 列表
     * <p>记录该 Pin 是从哪些消息中提取的
     */
    @Builder.Default
    private List<String> sourceMessageIds = new ArrayList<>();

    /**
     * Pin 的当前状态
     */
    @Builder.Default
    private PinStatus status = PinStatus.ACTIVE;

    /**
     * Pin 的创建时间
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Pin 的更新时间
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 检查 Pin 是否有效
     *
     * @return 如果 Pin 状态为 ACTIVE 返回 true，否则返回 false
     */
    public boolean isActive() {
        return status == PinStatus.ACTIVE;
    }

    /**
     * 将 Pin 标记为失效
     */
    public void invalidate() {
        this.status = PinStatus.INVALIDATED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加来源消息 ID
     *
     * @param messageId 消息 ID
     */
    public void addSourceMessageId(String messageId) {
        if (this.sourceMessageIds == null) {
            this.sourceMessageIds = new ArrayList<>();
        }
        this.sourceMessageIds.add(messageId);
    }
}
