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
package com.glmapper.memory.compression.events;

import com.glmapper.memory.compression.CompressionResult;
import com.glmapper.memory.compression.PinAggregationStrategy;
import com.glmapper.memory.model.Pin;
import com.glmapper.memory.model.WorkingMemory;
import com.glmapper.memory.storage.WorkingMemoryStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Pin 聚合事件处理器。
 *
 * <p>负责处理 Pin 聚合事件：
 * <ul>
 *   <li>调用 PinAggregationStrategy 生成聚合后的 Pin</li>
 *   <li>清空现有的 Pins</li>
 *   <li>添加新的聚合 Pin</li>
 *   <li>保存 WorkingMemory 到 Redis</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
@Component
public class PinAggregationEventHandler extends EventHandler<PinAggregationEvent> {

    @Autowired
    private WorkingMemoryStorage workingMemoryStorage;

    @Autowired(required = false)
    private PinAggregationStrategy pinAggregationStrategy;

    @Override
    public void onEvent(PinAggregationEvent event) {
        String conversationId = event.getSessionId();
        if (pinAggregationStrategy == null) {
            log.debug("[MEMORY]-[{}] 未配置 PinAggregationStrategy", conversationId);
            return;
        }

        try {
            // 加载 WorkingMemory
            WorkingMemory workingMemory = workingMemoryStorage.load(conversationId);

            // 执行聚合策略
            CompressionResult result = pinAggregationStrategy.compress(conversationId, workingMemory, null);

            if (!result.isCompressed() || result.getAggregatedPin() == null) {
                log.debug("[MEMORY]-[{}] Pin 聚合未执行或失败", conversationId);
                return;
            }

            // 清空现有 Pins
            workingMemory.getPinnedFacts().clear();
            log.info("[MEMORY]-[{}] 已清空 {} 个原 Pin", conversationId, result.getCompressedCount());

            // 添加聚合后的 Pin
            Pin aggregatedPin = result.getAggregatedPin();
            workingMemory.addPin(aggregatedPin);
            log.info("[MEMORY]-[{}] 已添加聚合 Pin: pinId={}", conversationId, aggregatedPin.getPinId());

            // 保存到 Redis
            workingMemoryStorage.save(workingMemory);
            log.info("[MEMORY]-[{}] Pin 聚合完成并已保存", conversationId);
        } catch (Exception e) {
            log.error("[MEMORY]-[{}] Pin 聚合事件处理失败", conversationId, e);
        }
    }

    @Override
    public String getEventType() {
        return PinAggregationEvent.class.getName();
    }
}
