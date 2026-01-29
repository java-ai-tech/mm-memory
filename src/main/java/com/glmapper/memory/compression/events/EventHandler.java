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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listener for memory-related events.
 */
@Component
public abstract class EventHandler<T extends MemoryEvent> implements InitializingBean {

    @Autowired
    protected MemoryEventPublisher eventPublisher;

    /**
     * Called when a memory event occurs.
     *
     * @param event the memory event
     */
    public abstract void onEvent(T event);

    @Override
    public void afterPropertiesSet() {
        eventPublisher.register(getEventType(), this);
    }

    public abstract String getEventType();
}
