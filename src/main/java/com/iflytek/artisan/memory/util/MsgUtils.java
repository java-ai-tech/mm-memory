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
package com.iflytek.artisan.memory.util;

import com.iflytek.artisan.memory.model.ContentBlock;
import com.iflytek.artisan.memory.model.Msg;
import com.iflytek.artisan.memory.model.MsgRole;
import com.iflytek.artisan.memory.model.TextBlock;
import com.iflytek.artisan.memory.model.ToolResultBlock;
import com.iflytek.artisan.memory.model.ToolUseBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for message operations.
 *
 * <p>This class provides methods for message manipulation, filtering, and analysis
 * used by compression strategies.
 */
public class MsgUtils {

    private static final Logger log = LoggerFactory.getLogger(MsgUtils.class);

    /**
     * Set of plan-related tool names that should be filtered out during compression.
     *
     * <p>This set includes all tools provided by PlanNotebook.
     */
    private static final Set<String> PLAN_RELATED_TOOLS =
            Set.of(
                    "create_plan",
                    "update_plan_info",
                    "revise_current_plan",
                    "update_subtask_state",
                    "finish_subtask",
                    "view_subtasks",
                    "get_subtask_count",
                    "finish_plan",
                    "view_historical_plans",
                    "recover_historical_plan");

    /**
     * Replaces a range of messages in a list with a single new message.
     *
     * @param rawMessages the list of messages to modify
     * @param startIndex the start index of the range to replace (inclusive)
     * @param endIndex the end index of the range to replace (inclusive)
     * @param newMsg the new message to insert at startIndex
     */
    public static void replaceMsg(List<Msg> rawMessages, int startIndex, int endIndex, Msg newMsg) {
        if (rawMessages == null || newMsg == null) {
            return;
        }

        int size = rawMessages.size();

        // Validate indices
        if (startIndex < 0 || endIndex < startIndex || startIndex >= size) {
            return;
        }

        // Ensure endIndex doesn't exceed list size
        int actualEndIndex = Math.min(endIndex, size - 1);

        // Remove messages from startIndex to endIndex (inclusive)
        rawMessages.subList(startIndex, actualEndIndex + 1).clear();

        // Insert newMsg at startIndex position
        rawMessages.add(startIndex, newMsg);
    }

    /**
     * Check if a message is a tool-related message (tool use or tool result).
     *
     * @param msg the message to check
     * @return true if the message contains tool use or tool result blocks
     */
    public static boolean isToolMessage(Msg msg) {
        if (msg == null) {
            return false;
        }
        return msg.hasContentBlocks(ToolUseBlock.class)
                || msg.hasContentBlocks(ToolResultBlock.class);
    }

    /**
     * Check if a message is a tool use message (ASSISTANT with ToolUseBlock).
     *
     * @param msg the message to check
     * @return true if the message is an ASSISTANT message containing ToolUseBlock
     */
    public static boolean isToolUseMessage(Msg msg) {
        if (msg == null) {
            return false;
        }
        return msg.getRole() == MsgRole.ASSISTANT && msg.hasContentBlocks(ToolUseBlock.class);
    }

    /**
     * Check if a message is a tool result message (contains ToolResultBlock).
     *
     * @param msg the message to check
     * @return true if the message contains ToolResultBlock
     */
    public static boolean isToolResultMessage(Msg msg) {
        if (msg == null) {
            return false;
        }
        return msg.hasContentBlocks(ToolResultBlock.class);
    }

    /**
     * Check if a message is a compressed message.
     *
     * <p>A compressed message has metadata with the {@code _compress_meta} key.
     *
     * @param msg the message to check
     * @return true if the message is a compressed message, false otherwise
     */
    public static boolean isCompressedMessage(Msg msg) {
        if (msg == null) {
            return false;
        }

        var metadata = msg.getMetadata();
        if (metadata == null) {
            return false;
        }

        Object compressMeta = metadata.get("_compress_meta");
        return compressMeta != null && compressMeta instanceof Map;
    }

    /**
     * Check if an ASSISTANT message is a final response to the user (not a tool call).
     *
     * @param msg the message to check
     * @return true if the message is an ASSISTANT role message that does not contain tool calls
     */
    public static boolean isFinalAssistantResponse(Msg msg) {
        if (msg == null || msg.getRole() != MsgRole.ASSISTANT) {
            return false;
        }

        // Skip compressed current round messages
        var metadata = msg.getMetadata();
        if (metadata != null) {
            Object compressMeta = metadata.get("_compress_meta");
            if (compressMeta != null && compressMeta instanceof Map) {
                @SuppressWarnings("unchecked")
                var compressMetaMap = (Map<String, Object>) compressMeta;
                if (Boolean.TRUE.equals(compressMetaMap.get("compressed_current_round"))) {
                    return false;
                }
            }
        }

        // A final response should not contain ToolUseBlock
        return !msg.hasContentBlocks(ToolUseBlock.class);
    }

    /**
     * Check if a tool name is plan-related.
     *
     * @param toolName the tool name to check
     * @return true if the tool name is plan-related
     */
    public static boolean isPlanRelatedTool(String toolName) {
        return toolName != null && PLAN_RELATED_TOOLS.contains(toolName);
    }

    /**
     * Filter out messages containing plan-related tool calls.
     *
     * @param messages the messages to filter
     * @return filtered messages without plan-related tool calls
     */
    public static List<Msg> filterPlanRelatedToolCalls(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }

        List<Msg> filtered = new ArrayList<>();
        Set<String> planRelatedToolCallIds = new HashSet<>();

        // First pass: identify plan-related tool call IDs
        for (Msg msg : messages) {
            if (msg.getRole() == MsgRole.ASSISTANT) {
                List<ToolUseBlock> toolUseBlocks = msg.getContentBlocks(ToolUseBlock.class);
                if (toolUseBlocks != null) {
                    for (ToolUseBlock toolUse : toolUseBlocks) {
                        if (toolUse != null && PLAN_RELATED_TOOLS.contains(toolUse.getName())) {
                            planRelatedToolCallIds.add(toolUse.getId());
                        }
                    }
                }
            }
        }

        // Second pass: filter out messages with plan-related tool calls
        for (Msg msg : messages) {
            boolean shouldInclude = true;

            // Check if this is a tool use message with plan-related tools
            if (msg.getRole() == MsgRole.ASSISTANT) {
                List<ToolUseBlock> toolUseBlocks = msg.getContentBlocks(ToolUseBlock.class);
                if (toolUseBlocks != null && !toolUseBlocks.isEmpty()) {
                    // If all tool calls in this message are plan-related, exclude it
                    boolean allPlanRelated = true;
                    for (ToolUseBlock toolUse : toolUseBlocks) {
                        if (toolUse != null && !PLAN_RELATED_TOOLS.contains(toolUse.getName())) {
                            allPlanRelated = false;
                            break;
                        }
                    }
                    if (allPlanRelated && toolUseBlocks.size() > 0) {
                        shouldInclude = false;
                    }
                }
            }

            // Check if this is a tool result message for plan-related tool calls
            if (msg.getRole() == MsgRole.TOOL) {
                List<ToolResultBlock> toolResultBlocks = msg.getContentBlocks(ToolResultBlock.class);
                if (toolResultBlocks != null) {
                    for (ToolResultBlock toolResult : toolResultBlocks) {
                        if (toolResult != null && planRelatedToolCallIds.contains(toolResult.getId())) {
                            shouldInclude = false;
                            break;
                        }
                    }
                }
            }

            if (shouldInclude) {
                filtered.add(msg);
            }
        }

        return filtered;
    }

    /**
     * Calculates the total character count of a message, including all content blocks.
     *
     * @param msg the message to calculate character count for
     * @return the total character count
     */
    public static int calculateMessageCharCount(Msg msg) {
        if (msg == null || msg.getContent() == null) {
            return 0;
        }

        int charCount = 0;
        for (ContentBlock block : msg.getContent()) {
            if (block instanceof TextBlock) {
                String text = ((TextBlock) block).getText();
                if (text != null) {
                    charCount += text.length();
                }
            } else if (block instanceof ToolUseBlock) {
                ToolUseBlock toolUse = (ToolUseBlock) block;
                if (toolUse.getName() != null) {
                    charCount += toolUse.getName().length();
                }
                if (toolUse.getId() != null) {
                    charCount += toolUse.getId().length();
                }
                if (toolUse.getInput() != null) {
                    charCount += toolUse.getInput().toString().length();
                }
            } else if (block instanceof ToolResultBlock) {
                ToolResultBlock toolResult = (ToolResultBlock) block;
                if (toolResult.getName() != null) {
                    charCount += toolResult.getName().length();
                }
                if (toolResult.getId() != null) {
                    charCount += toolResult.getId().length();
                }
                if (toolResult.getOutput() != null) {
                    for (ContentBlock outputBlock : toolResult.getOutput()) {
                        if (outputBlock instanceof TextBlock) {
                            String text = ((TextBlock) outputBlock).getText();
                            if (text != null) {
                                charCount += text.length();
                            }
                        }
                    }
                }
            }
        }
        return charCount;
    }

    /**
     * Calculates the total character count of a list of messages.
     *
     * @param messages the list of messages to calculate character count for
     * @return the total character count across all messages
     */
    public static int calculateMessagesCharCount(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int totalCharCount = 0;
        for (Msg msg : messages) {
            totalCharCount += calculateMessageCharCount(msg);
        }
        return totalCharCount;
    }

    private MsgUtils() {
        // Utility class
    }
}
