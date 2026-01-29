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

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publisher for memory-related events.
 */
@Slf4j
public class MemoryEventPublisher {

    private final Map<String, EventHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * Registers a listener for memory events.
     */
    public void register(String eventType, EventHandler eventHandler) {
        handlerMap.put(eventType, eventHandler);
    }

    /**
     * Unregisters a listener.
     */
    public void unregister(String eventType, EventHandler eventHandler) {
        handlerMap.remove(eventType, eventHandler);
    }

    public Map<String, EventHandler> getHandlers() {
        return handlerMap;
    }

    /**
     * Publishes an event to the registered handler.
     */
    public void publishEvent(MemoryEvent event) {
        String eventType = event.getClass().getName();
        EventHandler handler = handlerMap.get(eventType);
        if (handler == null) {
            return;
        }

        try {
            handler.onEvent(event);
        } catch (Exception e) {
            log.error("Error notifying handler of event: {}", eventType, e);
        }
    }
}
