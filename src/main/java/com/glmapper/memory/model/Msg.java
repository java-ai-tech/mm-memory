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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Message class representing a conversation message.
 *
 * <p>Ignores unknown properties during JSON deserialization for backward compatibility
 * with previously stored data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Msg {

    private String id;
    private MsgRole role;
    private String name;
    private List<ContentBlock> content = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private MessageStatus status = MessageStatus.NORMAL;

    public Msg() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MsgRole getRole() {
        return role;
    }

    public void setRole(MsgRole role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    public void setContent(List<ContentBlock> content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /**
     * Checks if this message is valid (not invalidated).
     *
     * @return true if status is NORMAL, false otherwise
     */
    public boolean isValid() {
        return status == MessageStatus.NORMAL;
    }

    /**
     * Marks this message as invalidated.
     */
    public void invalidate() {
        this.status = MessageStatus.INVALIDATED;
    }

    /**
     * Gets the text content from all TextBlock content blocks.
     *
     * <p>Marked with @JsonIgnore to prevent serialization as a JSON property.
     */
    @JsonIgnore
    public String getTextContent() {
        return content.stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .collect(Collectors.joining("\n"));
    }

    /** Checks if this message contains content blocks of the specified type. */
    public <T extends ContentBlock> boolean hasContentBlocks(Class<T> type) {
        return content.stream().anyMatch(type::isInstance);
    }

    /** Gets all content blocks of the specified type. */
    public <T extends ContentBlock> List<T> getContentBlocks(Class<T> type) {
        return content.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    /** Builder pattern for creating messages. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder class for Msg. */
    public static class Builder {
        private final Msg msg = new Msg();

        public Builder role(MsgRole role) {
            msg.setRole(role);
            return this;
        }

        public Builder name(String name) {
            msg.setName(name);
            return this;
        }

        public Builder content(ContentBlock content) {
            msg.content.add(content);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            msg.setMetadata(metadata);
            return this;
        }

        public Builder id(String id) {
            msg.setId(id);
            return this;
        }

        public Builder status(MessageStatus status) {
            msg.setStatus(status);
            return this;
        }

        public Msg build() {
            if (msg.content.isEmpty()) {
                msg.content.add(TextBlock.of(""));
            }
            return msg;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Msg msg = (Msg) o;
        return Objects.equals(id, msg.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Msg{" + "id='" + id + '\'' + ", role=" + role + ", name='" + name + '\'' + ", status=" + status + '}';
    }
}
