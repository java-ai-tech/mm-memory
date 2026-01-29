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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Tool result content block. */
public class ToolResultBlock extends ContentBlock {

    private String id;
    private String name;
    private List<ContentBlock> output = new ArrayList<>();

    public ToolResultBlock() {
        // Type is automatically handled by @JsonTypeInfo - no need to set it manually
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ContentBlock> getOutput() {
        return output;
    }

    public void setOutput(List<ContentBlock> output) {
        this.output = output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolResultBlock that = (ToolResultBlock) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "ToolResultBlock{" + "id='" + id + '\'' + ", name='" + name + '\'' + '}';
    }
}
