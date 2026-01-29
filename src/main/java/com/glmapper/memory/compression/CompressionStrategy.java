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

import com.glmapper.memory.SessionContext;
import com.glmapper.memory.model.MessagePair;
import com.glmapper.memory.model.WorkingMemory;
import org.springframework.core.Ordered;

/**
 * 压缩策略接口。
 *
 * <p>定义了压缩操作的基本方法，所有具体的压缩策略都应实现此接口。
 *
 * <p>策略执行顺序（按修改方案）：
 * <ol>
 *   <li>Pin 判定策略 - 提取 Pin 实体，存储到 pinnedFacts</li>
 *   <li>当前轮次摘要策略 - 将摘要/原文添加到 timingContextWindow</li>
 *   <li>历史对话摘要策略 - 仅对 timingContextWindow 进行摘要</li>
 * </ol>
 *
 * @author glsong
 * @since 1.0.0
 */
public interface CompressionStrategy extends Ordered {

    /**
     * 执行压缩操作（基于 WorkingMemory）。
     *
     * <p>新的压缩策略应该实现此方法，直接操作 WorkingMemory 的各个分区。
     *
     * @param conversationId 会话上下文，包含当前对话状态
     * @param workingMemory  工作记忆，包含 Head/Tail/timingContextWindow/pinnedFacts
     * @param currentPair    当前对话对（刚刚完成的对话）
     * @return 压缩结果，表示是否执行了压缩操作
     */
    CompressionResult compress(String conversationId, WorkingMemory workingMemory, MessagePair currentPair);


    /**
     * 获取此策略的名称。
     *
     * @return 策略名称
     */
    String getName();
}
