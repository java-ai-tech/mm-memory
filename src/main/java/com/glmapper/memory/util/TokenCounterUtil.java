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
package com.glmapper.memory.util;

import com.glmapper.memory.model.ContentBlock;
import com.glmapper.memory.model.Msg;
import com.glmapper.memory.model.TextBlock;
import com.glmapper.memory.model.ToolResultBlock;
import com.glmapper.memory.model.ToolUseBlock;
import java.util.List;
import java.util.Map;

/** Utility for estimating token count in messages. */
public class TokenCounterUtil {

    // Conservative ratio: ~1 token per 2.5 characters (works for mixed English/Chinese)
    private static final double CHARS_PER_TOKEN = 2.5;
    private static final int MESSAGE_OVERHEAD = 5;
    private static final int TOOL_CALL_OVERHEAD = 10;
    private static final int TOOL_RESULT_OVERHEAD = 8;

    /**
     * Calculates the estimated total input tokens for a list of messages.
     *
     * @param messages the list of messages
     * @return estimated number of input tokens
     */
    public static int calculateToken(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int totalTokens = 0;
        for (Msg msg : messages) {
            totalTokens += estimateMessageTokens(msg);
        }
        return totalTokens;
    }

    private static int estimateMessageTokens(Msg msg) {
        if (msg == null) {
            return 0;
        }
        int tokens = MESSAGE_OVERHEAD;
        if (msg.getRole() != null) {
            tokens += estimateTextTokens(msg.getRole().name());
        }
        if (msg.getName() != null) {
            tokens += estimateTextTokens(msg.getName());
        }
        for (ContentBlock block : msg.getContent()) {
            tokens += estimateContentBlockTokens(block);
        }
        return tokens;
    }

    private static int estimateContentBlockTokens(ContentBlock block) {
        if (block == null) {
            return 0;
        }
        if (block instanceof TextBlock) {
            return estimateTextTokens(((TextBlock) block).getText());
        } else if (block instanceof ToolUseBlock) {
            return estimateToolUseBlockTokens((ToolUseBlock) block);
        } else if (block instanceof ToolResultBlock) {
            return estimateToolResultBlockTokens((ToolResultBlock) block);
        }
        return 5; // Minimal overhead for other block types
    }

    private static int estimateToolUseBlockTokens(ToolUseBlock block) {
        int tokens = TOOL_CALL_OVERHEAD;
        if (block.getName() != null) {
            tokens += estimateTextTokens(block.getName());
        }
        if (block.getId() != null) {
            tokens += estimateTextTokens(block.getId());
        }
        if (block.getInput() != null && !block.getInput().isEmpty()) {
            String inputJson = mapToJson(block.getInput());
            tokens += estimateTextTokens(inputJson);
        }
        if (block.getContent() != null) {
            tokens += estimateTextTokens(block.getContent());
        }
        return tokens;
    }

    private static int estimateToolResultBlockTokens(ToolResultBlock block) {
        int tokens = TOOL_RESULT_OVERHEAD;
        if (block.getName() != null) {
            tokens += estimateTextTokens(block.getName());
        }
        if (block.getId() != null) {
            tokens += estimateTextTokens(block.getId());
        }
        if (block.getOutput() != null) {
            for (ContentBlock outputBlock : block.getOutput()) {
                tokens += estimateContentBlockTokens(outputBlock);
            }
        }
        return tokens;
    }

    private static int estimateTextTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    private static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value != null ? value.toString() : "null");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
