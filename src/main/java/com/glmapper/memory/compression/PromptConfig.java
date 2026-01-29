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

import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.glmapper.memory.compression.Prompts.CURRENT_ROUND_COMPRESSION_PROMPT;
import static com.glmapper.memory.compression.Prompts.HISTORY_SUMMARIZATION_PROMPT;
import static com.glmapper.memory.compression.Prompts.PIN_JUDGMENT_PROMPT;

/**
 * Configuration class for compression prompts used by ArtisanMemory.
 *
 * <p>This class allows customization of the prompts used in different compression strategies.
 * All prompts are optional - if not specified, default prompts will be used.
 *
 * <p><b>Configurable Prompts:</b>
 * <ul>
 *   <li><b>Step 1:</b> Pin judgment prompt</li>
 *   <li><b>Step 2:</b> Current round compression prompt</li>
 *   <li><b>Step 3:</b> History summarization prompt</li>
 *   <li><b>Step 4:</b> Pin aggregation prompt</li>
 *   <li><b>Step 5:</b> Tool call compression prompt</li>
 * </ul>
 */
@Data
@Builder
public class PromptConfig {

    /**
     * Step 1: Prompt for pin judgment.
     */
    private String pinJudgmentPrompt;

    /**
     * Step 2: Prompt for current round compression.
     */
    private String currentRoundCompressionPrompt;

    /**
     * Step 3: Prompt for history summarization.
     */
    private String historySummarizationPrompt;

    /**
     * Step 4: Prompt for pin aggregation.
     */
    private String pinAggregationPrompt;

    /**
     * Step 5: Prompt for tool call compression.
     */
    private String toolCallCompressionPrompt;

    @PostConstruct
    public void init() {
        if (this.pinJudgmentPrompt == null || this.pinJudgmentPrompt.isBlank()) {
            // 读取PIN_JUDGMENT_PROMPT.md
            try {
                URI uri = PromptConfig.class.getClassLoader().getResource("prompts/PIN_JUDGMENT_PROMPT.md").toURI();
                PIN_JUDGMENT_PROMPT = Files.readString(Path.of(uri));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.pinJudgmentPrompt = PIN_JUDGMENT_PROMPT;
        }
        if (this.currentRoundCompressionPrompt == null || this.currentRoundCompressionPrompt.isBlank()) {
            // 读取CURRENT_ROUND_COMPRESSION_PROMPT.md
            try {
                URI uri = PromptConfig.class.getClassLoader()
                        .getResource("prompts/CURRENT_ROUND_COMPRESSION_PROMPT.md")
                        .toURI();
                CURRENT_ROUND_COMPRESSION_PROMPT = Files.readString(Path.of(uri));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.currentRoundCompressionPrompt = CURRENT_ROUND_COMPRESSION_PROMPT;
        }
        if (this.historySummarizationPrompt == null || this.historySummarizationPrompt.isBlank()) {
            // 读取HISTORY_SUMMARIZATION_PROMPT.md
            try {
                URI uri = PromptConfig.class.getClassLoader()
                        .getResource("prompts/HISTORY_SUMMARIZATION_PROMPT.md")
                        .toURI();
                HISTORY_SUMMARIZATION_PROMPT = Files.readString(Path.of(uri));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.currentRoundCompressionPrompt = HISTORY_SUMMARIZATION_PROMPT;

        }
        if (this.pinAggregationPrompt == null || this.pinAggregationPrompt.isBlank()) {
            this.pinAggregationPrompt = Prompts.PIN_AGGREGATION_PROMPT;
        }
        if (this.toolCallCompressionPrompt == null || this.toolCallCompressionPrompt.isBlank()) {
            this.toolCallCompressionPrompt = Prompts.TOOL_CALL_COMPRESSION_PROMPT;
        }
    }
}
