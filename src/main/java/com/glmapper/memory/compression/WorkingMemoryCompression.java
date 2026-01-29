package com.glmapper.memory.compression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.glmapper.memory.compression.events.EventHandler;
import com.glmapper.memory.compression.events.EvictedMessageEvent;
import com.glmapper.memory.compression.events.MemoryEventPublisher;
import com.glmapper.memory.compression.events.PinMessageEvent;
import com.glmapper.memory.model.MessagePair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WorkingMemory 压缩执行器。
 *
 * <p>使用 Redis 队列 + 消费者线程的方式处理压缩任务：
 * <ul>
 *   <li>每个 sessionId 对应一个 Redis 队列</li>
 *   <li>同一 sessionId 的任务按顺序进入队列</li>
 *   <li>每个 sessionId 分配一个消费者线程，顺序处理任务</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
@Component
public class WorkingMemoryCompression {

    private static final String QUEUE_KEY_PREFIX = "artisan:wm:queue:";
    private static final int QUEUE_POLL_TIMEOUT_SECONDS = 10;  // 改为10秒，更合理的轮询间隔
    private static final int MAX_IDLE_LOOPS = 6;  // 最多空闲6次（60秒无任务后退出消费者）

    private final StringRedisTemplate redisTemplate;
    private final MemoryEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ExecutorService consumerExecutor;
    private final ConcurrentHashMap<String, AtomicBoolean> activeConsumers;
    private final AtomicBoolean shutdown;

    public WorkingMemoryCompression(StringRedisTemplate redisTemplate, MemoryEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.consumerExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "artisan-memory-consumer");
            t.setDaemon(true);
            return t;
        });
        this.activeConsumers = new ConcurrentHashMap<>();
        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * 提交压缩任务到 Redis 队列。
     *
     * @param sessionId   会话标识符
     * @param currentPair 当前对话对
     * @param evictedPair 从 Tail 移出的消息对（可能为 null）
     */
    public void submitTask(String sessionId, MessagePair currentPair, MessagePair evictedPair) {
        try {
            // 1、创建压缩任务
            CompressionTask task = new CompressionTask(sessionId, currentPair, evictedPair);
            String taskJson = objectMapper.writeValueAsString(task);

            // 2、推入 Redis 队列（LPUSH）
            String queueKey = QUEUE_KEY_PREFIX + sessionId;
            redisTemplate.opsForList().leftPush(queueKey, taskJson);
            log.info("[MEMORY]-[{}] 压缩任务已提交到队列, queueKey: {}", sessionId, queueKey);

            // 3、确保该 sessionId 有消费者线程在运行
            activeConsumers.computeIfAbsent(sessionId, key -> {
                AtomicBoolean running = new AtomicBoolean(true);
                consumerExecutor.submit(() -> consumeQueue(sessionId, running));
                return running;
            });
        } catch (Exception e) {
            log.error("[MEMORY]-[{}] 提交压缩任务失败", sessionId, e);
        }
    }

    /**
     * 消费 Redis 队列中的压缩任务。
     *
     * @param sessionId
     * @param running
     */
    private void consumeQueue(String sessionId, AtomicBoolean running) {
        // 1、拼接队列键
        String queueKey = QUEUE_KEY_PREFIX + sessionId;
        int idleLoops = 0;  // 空闲循环计数器

        // 2、开始消费循环
        while (running.get() && !shutdown.get()) {
            try {
                // 3、从队列右侧弹出任务（BRPOP，阻塞式，最多等待10秒）
                String taskJson = redisTemplate.opsForList()
                        .rightPop(queueKey, QUEUE_POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // 4、队列为空，等待超时
                if (taskJson == null) {
                    idleLoops++;
                    log.debug("[MEMORY]-[{}] 队列空闲，空闲循环次数: {}/{}", sessionId, idleLoops, MAX_IDLE_LOOPS);

                    // 连续空闲超过阈值，停止消费者以释放资源
                    if (idleLoops >= MAX_IDLE_LOOPS) {
                        log.info("[MEMORY]-[{}] 压缩队列连续空闲 {} 次，停止消费者", sessionId, idleLoops);
                        activeConsumers.remove(sessionId);
                        running.set(false);
                        break;
                    }
                    continue;
                }

                // 5、重置空闲计数器
                idleLoops = 0;

                // 6、解析任务
                CompressionTask task = objectMapper.readValue(taskJson, CompressionTask.class);
                log.info("[MEMORY]-[{}] 开始执行压缩任务", sessionId);

                // 7、执行压缩策略
                executeCompressionStrategies(task);
                log.info("[MEMORY]-[{}] 压缩任务执行完成", sessionId);
            } catch (Exception e) {
                log.error("[MEMORY]-[{}] 消费队列时发生错误", sessionId, e);
                // 发生错误时增加空闲计数，避免无限重试
                idleLoops++;
                if (idleLoops >= MAX_IDLE_LOOPS) {
                    log.error("[MEMORY]-[{}] 连续错误超过阈值，停止消费者", sessionId);
                    activeConsumers.remove(sessionId);
                    running.set(false);
                    break;
                }
                try {
                    // 错误后等待一段时间再重试
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("[MEMORY]-[{}] 消费者线程退出", sessionId);
    }

    /**
     * 执行压缩策略。
     *
     * @param task 压缩任务
     */
    private void executeCompressionStrategies(CompressionTask task) {
        String sessionId = task.getSessionId();
        try {
            // 1、Pin 判定、同步执行
            PinMessageEvent pinEvent = new PinMessageEvent(sessionId, task.getCurrentPair());
            eventPublisher.publishEvent(pinEvent);
            log.info("[MEMORY]-[{}] 已发布 PIN 压缩事件", sessionId);

            // 2、 如果有从 Tail 移出的消息对，执行当前轮次摘要
            if (task.getEvictedPair() != null) {
                EvictedMessageEvent evictedEvent = new EvictedMessageEvent(sessionId, task.getEvictedPair());
                eventPublisher.publishEvent(evictedEvent);
                log.info("[MEMORY]-[{}] 已发布 EVICTED_SUMMARY 压缩事件", sessionId);
            }
        } catch (Exception e) {
            log.error("[MEMORY]-[{}] 压缩策略执行失败", sessionId, e);
        }
    }

    /**
     * 关闭压缩执行器。
     */
    public void shutdown() {
        shutdown.set(true);
        // 停止所有消费者
        activeConsumers.values().forEach(running -> running.set(false));
        Map<String, EventHandler> handlers = eventPublisher.getHandlers();
        // 清空 正在运行的消费者记录
        handlers.entrySet().forEach((entry) -> eventPublisher.unregister(entry.getKey(), entry.getValue()));

        activeConsumers.clear();
        // 关闭线程池
        consumerExecutor.shutdown();
        try {
            if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            consumerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("WorkingMemoryCompression executor shutdown");
    }
}
