package com.glmapper.memory;

import com.glmapper.memory.compression.WorkingMemoryCompression;
import com.glmapper.memory.config.ArtisanMemoryProperties;
import com.glmapper.memory.management.StorageClientManager;
import com.glmapper.memory.model.MessagePair;
import com.glmapper.memory.model.Msg;
import com.glmapper.memory.model.MsgRole;
import com.glmapper.memory.model.Pin;
import com.glmapper.memory.model.ToolUseBlock;
import com.glmapper.memory.model.WorkingMemory;
import com.glmapper.memory.storage.OriginalStorage;
import com.glmapper.memory.storage.WorkingMemoryStorage;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ArtisanMemory - 混合式自动上下文记忆服务（基于 Redis 和 MongoDB）。
 *
 * <p>本类为 RAG（检索增强生成）应用提供智能上下文记忆管理，主要特性包括：
 * <ul>
 *   <li>分区存储架构：WorkingMemory 包含 Head/Tail/timingContextWindow/pinnedFacts 四个分区</li>
 *   <li>自动压缩机制：当消息数量或 Token 数量超过阈值时触发自动压缩</li>
 *   <li>渐进式压缩策略：Pin判定 -> 当前轮次摘要 -> 历史对话摘要</li>
 *   <li>基于 LLM 的智能摘要：利用大语言模型生成高质量摘要</li>
 *   <li>事件驱动架构：通过事件机制支持监控和扩展</li>
 *   <li>RAG 友好接口：提供便于 RAG 应用的会话检索接口</li>
 * </ul>
 *
 * <p>WorkingMemory 结构：
 * <pre>
 * WorkingMemory (conversationId)
 * ├── Head (最旧 1 轮) - 永不压缩
 * ├── Tail (最新 2 轮) - 永不压缩
 * ├── timingContextWindow (历史摘要 + 当前轮次摘要 + 中间消息，最大 5 条)
 * └── Pinned Facts (确认事实) - 永不压缩
 * </pre>
 *
 * <p>压缩策略执行顺序：
 * <ol>
 *   <li>Pin 判定策略：判断当前对话是否包含确认事实，提取 Pin 存储到 pinnedFacts</li>
 *   <li>当前轮次摘要策略：如果 token 超限则生成摘要，添加到 timingContextWindow</li>
 *   <li>历史对话摘要策略：如果 timingContextWindow 超限则生成历史摘要</li>
 * </ol>
 *
 * @author glsong
 * @since 1.0.0
 */
@Service
@Slf4j
public class SessionMemory {
    private final StorageClientManager clientManager;
    private final ArtisanMemoryProperties.WorkingMemory memoryConfig;
    private final WorkingMemoryCompression workingMemoryCompression;

    private final ConcurrentHashMap<String, SessionContext> contexts;
    private final ScheduledExecutorService sharedScheduler;
    private final String keyPrefix;

    // 配置参数
    private final int headSize;
    private final int tailSize;

    /**
     * 构造一个新的 ArtisanMemory 服务实例。
     *
     * @param properties               配置属性，包含记忆服务的所有配置参数
     * @param clientManager            存储客户端管理器，管理工作存储和原始存储
     * @param workingMemoryCompression 工作记忆压缩执行器
     */
    public SessionMemory(ArtisanMemoryProperties properties, StorageClientManager clientManager, WorkingMemoryCompression workingMemoryCompression) {
        this.clientManager = clientManager;
        this.memoryConfig = properties.getWorkingMemory();
        this.keyPrefix = "session::";
        this.workingMemoryCompression = workingMemoryCompression;
        this.contexts = new ConcurrentHashMap<>();
        this.sharedScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "artisan-memory-shared");
            t.setDaemon(true);
            return t;
        });

        // 初始化配置参数
        this.headSize = memoryConfig.getHeadSize();
        this.tailSize = memoryConfig.getTailSize();
    }

    @PreDestroy
    public void destroy() {
        // Shutdown scheduler
        sharedScheduler.shutdown();
        try {
            if (!sharedScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                sharedScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            sharedScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // Shutdown compression executor
        workingMemoryCompression.shutdown();
        // Clear all contexts
        contexts.clear();
        log.info("ArtisanMemory service destroyed");
    }

    /**
     * 会话开始时创建 session context。
     *
     * @param sessionId 会话标识符
     * @return SessionContext
     */
    public SessionContext getSessionContext(String sessionId) {
        return contexts.computeIfAbsent(sessionId, id -> {
            String storageKey = keyPrefix + id;
            SessionContext context = new SessionContext(id, storageKey);
            log.debug("Session context created: sessionId={}", id);
            return context;
        });
    }

    /**
     * 提交会话上下文，将当前对话对存储并触发压缩策略。
     *
     * <p>处理流程：
     * <ol>
     *   <li>存储原始消息到 MongoDB（永久保留）</li>
     *   <li>更新 Head/Tail：
     *     <ul>
     *       <li>第 1 轮对话添加到 Head</li>
     *       <li>后续对话添加到 Tail</li>
     *       <li>如果 Tail 满了，将最旧的移出</li>
     *     </ul>
     *   </li>
     *   <li>执行压缩策略（事件驱动）：
     *     <ul>
     *       <li>Pin 判定：对当前对话提取 Pin</li>
     *       <li>PairEvictedFromTailEvent → CurrentRoundCompressionStrategy → 添加到 timingContextWindow</li>
     *       <li>TimingContextWindowUpdatedEvent → HistorySummarizationStrategy</li>
     *     </ul>
     *   </li>
     *   <li>保存 WorkingMemory 到 Redis</li>
     * </ol>
     *
     * @param context 会话上下文
     */
    public void commitSessionContext(SessionContext context) {
        if (context == null) {
            return;
        }
        context.updateLastAccessTime();
        String sessionId = context.getSessionId();
        OriginalStorage originalStorage = clientManager.getOriginalStorage();

        // 检查是否有完整的对话对
        MessagePair currentPair = context.getCurrentPair();
        if (currentPair == null || !currentPair.isComplete()) {
            log.warn("[MEMORY]-[{}] 当前对话对不完整，跳过提交", sessionId);
            context.clearCurrentPair();
            return;
        }

        // 1. 存储原始消息到 MongoDB
        originalStorage.append(context.getStorageKey(), currentPair);
        log.info("[MEMORY]-[{}] 本轮对话消息已存储", sessionId);

        // 2. 获取或创建 WorkingMemory
        WorkingMemoryStorage workingMemoryStorage = clientManager.getWorkingMemoryStorage();
        if (workingMemoryStorage == null) {
            log.error("[MEMORY]-[{}] WorkingMemoryStorage 为空，无法提交会话上下文", sessionId);
            context.clearCurrentPair();
            return;
        }

        WorkingMemory workingMemory = workingMemoryStorage.load(sessionId);
        if (workingMemory.getConversationId() == null) {
            workingMemory.setConversationId(sessionId);
        }

        // 3. 更新 Head/Tail，获取从 Tail 移出的消息对（如果有）
        MessagePair evictedPair = updateHeadAndTail(workingMemory, currentPair, sessionId);

        // 4. 保存初始 WorkingMemory（包含 Head/Tail 更新）
        workingMemoryStorage.save(workingMemory);

        // 5. 提交压缩任务到队列（异步执行）
        workingMemoryCompression.submitTask(sessionId, currentPair, evictedPair);

        context.clearCurrentPair();
        log.debug("[MEMORY]-[{}] 会话上下文已提交", sessionId);
    }

    /**
     * 更新 Head 和 Tail 区域。
     *
     * <p>处理逻辑：
     * <ul>
     *   <li>第 1 轮对话：添加到 Head</li>
     *   <li>第 2+ 轮对话：添加到 Tail</li>
     *   <li>如果 Tail 已满：返回被移出的最旧消息对，由调用方处理</li>
     * </ul>
     *
     * <p>示例流程（headSize=1, tailSize=2）：
     * <pre>
     * 第1轮: Head=[1], Tail=[], 返回 null
     * 第2轮: Head=[1], Tail=[2], 返回 null
     * 第3轮: Head=[1], Tail=[2,3], 返回 null
     * 第4轮: Head=[1], Tail=[3,4], 返回 第2轮（移出）
     * 第5轮: Head=[1], Tail=[4,5], 返回 第3轮（移出）
     * </pre>
     *
     * @return 从 Tail 移出的消息对，如果没有移出则返回 null
     */
    private MessagePair updateHeadAndTail(WorkingMemory workingMemory, MessagePair currentPair, String sessionId) {
        int totalRounds = workingMemory.getTotalRounds();
        if (totalRounds == 0) {
            // 第一轮对话，添加到 Head
            workingMemory.setHead(currentPair, headSize);
            log.info("[MEMORY]-[{}] 第一轮对话已添加到 Head", sessionId);
            return null;
        } else {
            // 后续对话，添加到 Tail
            // 如果 Tail 已满，addToTail 会返回被移出的最旧消息对
            MessagePair evictedPair = workingMemory.addToTail(currentPair, tailSize);
            if (evictedPair != null) {
                log.info("[MEMORY]-[{}] 消息对已从 Tail 移出, evictedUserMsgId: {}, tailSize: {}", sessionId, evictedPair.getUserMessage() != null ? evictedPair.getUserMessage()
                        .getId() : "null", workingMemory.getTail().size());
            }
            return evictedPair;
        }
    }

    /**
     * 获取 Working Memory 中的消息（按 Head + timingContextWindow + Tail 顺序组装）。
     *
     * @param context 会话上下文
     * @return 消息列表
     */
    public List<Msg> getMemoryMessages(SessionContext context) {
        if (context == null) {
            return new ArrayList<>();
        }
        context.updateLastAccessTime();
        String sessionId = context.getSessionId();
        WorkingMemoryStorage workingMemoryStorage = clientManager.getWorkingMemoryStorage();
        WorkingMemory workingMemory = workingMemoryStorage.load(sessionId);
        return workingMemory.assembleMessages();
    }

    /**
     * 获取确认事实（Pinned Facts）列表。
     *
     * @param context 会话上下文
     * @return 有效的 Pin 列表
     */
    public List<Pin> getPinnedFacts(SessionContext context) {
        if (context == null) {
            return new ArrayList<>();
        }
        context.updateLastAccessTime();
        String sessionId = context.getSessionId();

        WorkingMemoryStorage workingMemoryStorage = clientManager.getWorkingMemoryStorage();
        if (workingMemoryStorage != null) {
            return workingMemoryStorage.getActivePins(sessionId);
        }
        return new ArrayList<>();
    }

    /**
     * 获取完整的 WorkingMemory。
     *
     * @param context 会话上下文
     * @return WorkingMemory 对象
     */
    public WorkingMemory getWorkingMemory(SessionContext context) {
        if (context == null) {
            return new WorkingMemory();
        }
        context.updateLastAccessTime();
        String sessionId = context.getSessionId();

        WorkingMemoryStorage workingMemoryStorage = clientManager.getWorkingMemoryStorage();
        if (workingMemoryStorage != null) {
            return workingMemoryStorage.load(sessionId);
        }
        return new WorkingMemory();
    }

    /**
     * 获取原始记忆中的所有消息（未压缩的完整历史）。
     *
     * @param sessionId 会话标识符
     * @return 所有原始消息列表（从消息对中提取）
     */
    public List<Msg> getOriginalMessages(String sessionId) {
        SessionContext context = getSessionContext(sessionId);
        context.updateLastAccessTime();

        OriginalStorage originalStorage = clientManager.getOriginalStorage();
        List<MessagePair> pairs = originalStorage.getAll(context.getStorageKey());

        // Extract all messages from pairs
        List<Msg> messages = new ArrayList<>();
        for (MessagePair pair : pairs) {
            messages.addAll(pair.getAllMessages());
        }

        return messages;
    }

    /**
     * 获取原始记忆中的交互消息（用户和最终助手响应）。
     *
     * @param sessionId 会话标识符
     * @return 交互消息列表
     */
    public List<Msg> getInteractionMessages(String sessionId) {
        SessionContext context = getSessionContext(sessionId);
        context.updateLastAccessTime();

        OriginalStorage originalStorage = clientManager.getOriginalStorage();
        List<MessagePair> pairs = originalStorage.getAll(context.getStorageKey());

        List<Msg> interactions = new ArrayList<>();
        for (MessagePair pair : pairs) {
            if (pair.getUserMessage() != null) {
                interactions.add(pair.getUserMessage());
            }
            if (pair.getAssistantMessage() != null && isFinalAssistantResponse(pair.getAssistantMessage())) {
                interactions.add(pair.getAssistantMessage());
            }
        }
        return interactions;
    }

    private boolean isFinalAssistantResponse(Msg msg) {
        if (msg.getRole() != MsgRole.ASSISTANT) {
            return false;
        }
        // Check if message contains tool calls (not a final response)
        return !msg.hasContentBlocks(ToolUseBlock.class);
    }

    /**
     * 构建 System Prompt（包含确认事实）。
     *
     * @param context 会话上下文
     * @return System Prompt 字符串
     */
    public String buildSystemPrompt(SessionContext context) {
        List<Pin> pins = getPinnedFacts(context);
        if (pins.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个严谨助手，必须遵守以下已确认事实：\n");
        for (int i = 0; i < pins.size(); i++) {
            Pin pin = pins.get(i);
            sb.append("- ").append(pin.getContent()).append("\n");
        }
        sb.append("请不要生成未确认的内容。\n");
        return sb.toString();
    }

    /**
     * 构建 User Prompt（按 Head + timingContextWindow + Tail + userInput 组装）。
     *
     * @param context   会话上下文
     * @param userInput 当前用户输入
     * @return User Prompt 字符串
     */
    public String buildUserPrompt(SessionContext context, String userInput) {
        WorkingMemory workingMemory = getWorkingMemory(context);
        List<Msg> messages = workingMemory.assembleMessages();

        StringBuilder sb = new StringBuilder();
        sb.append("历史对话:\n");
        for (Msg msg : messages) {
            String roleStr = msg.getRole() != null ? msg.getRole().name() : "UNKNOWN";
            sb.append("[").append(roleStr).append("] ").append(msg.getTextContent()).append("\n");
        }
        sb.append("\n当前用户输入:\n").append(userInput);
        return sb.toString();
    }

    // ==================== 事件监听器 ====================


    /**
     * 从历史对话恢复 WorkingMemory。
     *
     * @param sessionId 会话标识符
     * @return 恢复后的 WorkingMemory
     */
    public WorkingMemory recoverWorkingMemory(String sessionId) {
        WorkingMemoryStorage workingMemoryStorage = clientManager.getWorkingMemoryStorage();
        OriginalStorage originalStorage = clientManager.getOriginalStorage();
        SessionContext context = getSessionContext(sessionId);

        List<MessagePair> originalPairs = originalStorage.getAll(context.getStorageKey());
        if (originalPairs == null || originalPairs.isEmpty()) {
            log.warn("No original pairs found for recovery: sessionId={}", sessionId);
            return new WorkingMemory();
        }

        WorkingMemory recovered = workingMemoryStorage.recover(sessionId, originalPairs);
        log.info("Recovered working memory from history: sessionId={}, pairsCount={}", sessionId, originalPairs.size());
        return recovered;
    }
}
