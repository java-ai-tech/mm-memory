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
package com.glmapper.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a complete conversation turn consisting of a user message and assistant response.
 *
 * <p>This is the atomic unit for Pin, compression, and deletion operations according to the
 * lightweight execution plan.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MessagePair {
    private Msg userMessage;
    private Msg assistantMessage;
    // Tool calls/results between user and assistant
    private List<Msg> intermediateMessages;

    public MessagePair() {
        this.intermediateMessages = new ArrayList<>();
    }

    public MessagePair(Msg userMessage, Msg assistantMessage) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.intermediateMessages = new ArrayList<>();
    }

    public MessagePair(Msg userMessage, Msg assistantMessage, List<Msg> intermediateMessages) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.intermediateMessages = intermediateMessages != null ? intermediateMessages : new ArrayList<>();
    }


    /**
     * Gets all messages in this pair as a flat list (user, intermediates, assistant).
     *
     * @return list of all messages
     */
    public List<Msg> getAllMessages() {
        List<Msg> all = new ArrayList<>();
        if (userMessage != null) {
            all.add(userMessage);
        }
        all.addAll(intermediateMessages);
        if (assistantMessage != null) {
            all.add(assistantMessage);
        }
        return all;
    }

    /**
     * Checks if this pair is complete (has both user and assistant messages).
     *
     * @return true if complete, false otherwise
     */
    public boolean isComplete() {
        return userMessage != null && assistantMessage != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessagePair that = (MessagePair) o;
        return Objects.equals(userMessage, that.userMessage) && Objects.equals(assistantMessage, that.assistantMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userMessage, assistantMessage);
    }

    @Override
    public String toString() {
        return "MessagePair{" + "userMessage=" + (userMessage != null ? userMessage.getId() : "null") + ", assistantMessage=" + (assistantMessage != null ? assistantMessage.getId() : "null") + ", intermediateCount=" + intermediateMessages.size() + '}';
    }
}
