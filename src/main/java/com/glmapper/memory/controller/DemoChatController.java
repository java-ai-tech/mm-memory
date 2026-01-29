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
package com.glmapper.memory.controller;

import com.glmapper.memory.SessionMemory;
import com.glmapper.memory.SessionContext;
import com.glmapper.memory.model.Msg;
import com.glmapper.memory.model.MsgRole;
import com.glmapper.memory.model.TextBlock;
import com.glmapper.memory.service.MockVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo controller for ArtisanMemory with simulated RAG workflow.
 *
 * <p>This controller demonstrates:
 * <ul>
 *   <li>Chat endpoint with automatic memory compression</li>
 *   <li>Mock vector store for RAG retrieval</li>
 *   *Mock LLM model for responses</li>
 *   *Complete logging for analysis</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/demo/chat")
public class DemoChatController {

    private static final Logger log = LoggerFactory.getLogger(DemoChatController.class);

    @Autowired
    private SessionMemory memoryService;
    @Autowired
    private ChatClient chatClient;
    private MockVectorStore vectorStore = new MockVectorStore();

    /**
     * Main chat endpoint with memory management.
     *
     * @param request chat request with message
     * @param userId  user identifier
     * @return chat response
     */
    @PostMapping
    public Map<String, Object> chat(@RequestBody ChatRequest request, @RequestHeader(value = "X-User-Id", defaultValue = "default-user") String userId) {
        long startTime = System.currentTimeMillis();
        String sessionId = "user:" + userId;
        log.info("----------------- Chat Request Started -----------------");
        log.info("Conversation info, Session ID: {}, User ID: {}, Message: {}", sessionId, userId, request.getMessage());
        try {
            // 开始会话
            SessionContext sessionContext = memoryService.getSessionContext(sessionId);
            // mock RAG + LLM workflow
            log.info("[Step 1] Retrieving RAG context...");
            List<Msg> ragContext = retrieveRAGContext(request.getMessage(), sessionId);
            // 2. 历史对话
            log.info("[Step 2] Getting conversation history...");
            List<Msg> conversation = memoryService.getMemoryMessages(sessionContext);
            log.info("Conversation history size: {} messages", conversation.size());
            // 3. 组合上下文提交给 LLM
            List<Msg> fullContext = new ArrayList<>();
            fullContext.addAll(ragContext);
            fullContext.addAll(conversation);

            // 4. Add current user message
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .name("user")
                    .content(TextBlock.of(request.getMessage()))
                    .build();
            fullContext.add(userMsg);

            log.info("[Step 3] Sending to LLM (total messages: {})...", fullContext.size());

            // 5. 调用 LLM
            String llmResponse = chatClient.prompt().messages(convertToSpringAiMessages(fullContext)).call().content();
            log.info("LLM response received.");

            // 6. 存储当前对话轮次
            log.info("[Step 4] Storing conversation to memory...");
            Msg assistantMsg = Msg.builder()
                    .role(MsgRole.ASSISTANT)
                    .name("assistant")
                    .content(TextBlock.of(llmResponse))
                    .build();

            sessionContext.appendMessage(userMsg);
            sessionContext.appendMessage(assistantMsg);
            memoryService.commitSessionContext(sessionContext);

            // 8. Build response
            Map<String, Object> response = new HashMap<>();
            response.put("response", llmResponse);
            response.put("sessionId", sessionId);
            response.put("conversationSize", conversation.size());

            log.info("----------------- Chat Request Completed ({} ms)  -----------------", System.currentTimeMillis() - startTime);

            return response;

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("sessionId", sessionId);
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("processingTimeMs", System.currentTimeMillis() - startTime);
            return errorResponse;
        }
    }

    /**
     * Get original messages (complete history).
     */
    @GetMapping("/original")
    public Map<String, Object> getOriginalMessages(@RequestParam(value = "userId", defaultValue = "default-user") String userId) {
        String sessionId = "user:" + userId;
        log.info("Getting original messages for session: {}", sessionId);

        List<Msg> original = memoryService.getOriginalMessages(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("messages", original);
        result.put("count", original.size());

        return result;
    }


    /**
     * Simulate RAG context retrieval with vector search.
     */
    private List<Msg> retrieveRAGContext(String query, String sessionId) {
        // Mock vector search
        List<Msg> vectorResults = vectorStore.search(query, 3);
        if (!vectorResults.isEmpty()) {
            // Add a system message to provide RAG context
            StringBuilder ragBuilder = new StringBuilder();
            ragBuilder.append("[RAG Context - Retrieved from Vector Store]\n");
            for (int i = 0; i < vectorResults.size(); i++) {
                Msg msg = vectorResults.get(i);
                ragBuilder.append(String.format("\n[Document %d]\n%s\n", i + 1, msg.getTextContent()));
            }
            Msg ragMsg = Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .name("system")
                    .content(TextBlock.of(ragBuilder.toString()))
                    .build();
            List<Msg> ragContext = new ArrayList<>();
            ragContext.add(ragMsg);
            return ragContext;
        }
        return List.of();
    }

    /**
     * Converts ArtisanMemory Msg list to Spring AI Message format.
     */
    private List<Message> convertToSpringAiMessages(List<Msg> messages) {
        List<Message> result = new ArrayList<>();
        for (Msg msg : messages) {
            String content = msg.getTextContent();
            Message springMsg = switch (msg.getRole()) {
                case USER -> new UserMessage(content);
                case ASSISTANT -> new AssistantMessage(content);
                case SYSTEM -> new SystemMessage(content);
                default -> new UserMessage(content);
            };
            result.add(springMsg);
        }
        return result;
    }

    /**
     * Chat request body.
     */
    public static class ChatRequest {

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
