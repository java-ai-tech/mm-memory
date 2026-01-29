package com.glmapper.memory.compression.events;

import com.glmapper.memory.compression.CompressionResult;
import com.glmapper.memory.compression.HistorySummarizationStrategy;
import com.glmapper.memory.model.WorkingMemory;
import com.glmapper.memory.storage.WorkingMemoryStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 历史摘要压缩处理器。
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
@Component
public class HistorySummaryEventHandler extends EventHandler<HistorySummaryEvent> {

    @Autowired
    private WorkingMemoryStorage workingMemoryStorage;

    @Autowired(required = false)
    private HistorySummarizationStrategy historySummarizationStrategy;

    @Override
    public void onEvent(HistorySummaryEvent event) {
        String conversationId = event.getSessionId();
        if (historySummarizationStrategy == null) {
            log.info("[MEMORY]-[{}] 未配置历史摘要策略", conversationId);
            return;
        }

        try {
            // 获取最新的工作记忆状态
            WorkingMemory workingMemory = workingMemoryStorage.load(conversationId);
            CompressionResult result = historySummarizationStrategy.compress(conversationId, workingMemory, null);
            if (result.isCompressed() && result.getSummaryMsg() != null) {
                workingMemory.clearTimingContextWindow();
                workingMemory.addToTimingContextWindow(result.getSummaryMsg());
                workingMemoryStorage.save(workingMemory);
                log.info("[MEMORY]-[{}] 历史摘要已替换 TCW", conversationId);
            } else {
                log.info("[MEMORY]-[{}] 历史摘要不需要压缩", conversationId);
            }
        } catch (Exception e) {
            log.error("[MEMORY]-[{}] 历史摘要压缩失败", conversationId, e);
        }
    }

    @Override
    public String getEventType() {
        return HistorySummaryEvent.class.getName();
    }
}
