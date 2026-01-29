package com.glmapper.memory.compression.events;

import com.glmapper.memory.compression.CompressionResult;
import com.glmapper.memory.compression.CurrentRoundCompressionStrategy;
import com.glmapper.memory.model.WorkingMemory;
import com.glmapper.memory.storage.WorkingMemoryStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 当前轮次摘要压缩处理器。
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
@Component
public class EvictedMessageEventHandler extends EventHandler<EvictedMessageEvent> {

    @Autowired
    private WorkingMemoryStorage workingMemoryStorage;

    @Autowired(required = false)
    private CurrentRoundCompressionStrategy currentRoundStrategy;

    @Override
    public void onEvent(EvictedMessageEvent event) {
        String conversationId = event.getSessionId();
        if (event.getEvictedMessagePair() == null) {
            log.info("[MEMORY]-[{}] 没有 evictedPair，跳过", conversationId);
            return;
        }
        WorkingMemory workingMemory = workingMemoryStorage.load(conversationId);
        try {
            if (currentRoundStrategy != null) {
                CompressionResult result = currentRoundStrategy.compress(conversationId, workingMemory, event.getEvictedMessagePair());
                if (result.isCompressed() && result.getSummaryMsg() != null) {
                    workingMemory.addToTimingContextWindow(result.getSummaryMsg());
                    log.info("[MEMORY]-[{}] 当前轮次摘要已添加到 TCW", conversationId);
                } else {
                    workingMemory.addPairToTimingContextWindow(event.getEvictedMessagePair());
                    log.info("[MEMORY]-[{}] 直接添加原文到 TCW", conversationId);
                }
            } else {
                workingMemory.addPairToTimingContextWindow(event.getEvictedMessagePair());
                log.debug("[MEMORY]-[{}] 未配置策略，直接添加原文", conversationId);
            }

            workingMemoryStorage.save(workingMemory);
            log.info("[MEMORY]-[{}] 当前轮次摘要压缩完成并已保存", conversationId);

        } catch (Exception e) {
            log.error("[MEMORY]-[{}] 当前轮次摘要压缩失败", conversationId, e);
        } finally {
            // 这里必须触发去进行历史摘要压缩
            HistorySummaryEvent historyEvent = new HistorySummaryEvent(conversationId);
            eventPublisher.publishEvent(historyEvent);
            log.debug("[MEMORY]-[{}] 已发布 HISTORY_SUMMARY 事件", conversationId);
        }
    }

    @Override
    public String getEventType() {
        return EvictedMessageEvent.class.getName();
    }
}
