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
package com.iflytek.artisan.memory.compression.events;

import lombok.Getter;

/**
 * Pin 聚合事件。
 *
 * <p>当 WorkingMemory 中的 pinnedFacts 数量或 token 总数超过阈值时触发。
 *
 * @author glsong
 * @since 1.0.0
 */
@Getter
public class PinAggregationEvent extends MemoryEvent {

    public PinAggregationEvent(String sessionId) {
        super(sessionId);
    }
}
