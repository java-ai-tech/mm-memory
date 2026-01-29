package com.iflytek.artisan.memory.compression.events;

import com.iflytek.artisan.memory.compression.PinJudgmentStrategy;
import com.iflytek.artisan.memory.model.Pin;
import com.iflytek.artisan.memory.model.WorkingMemory;
import com.iflytek.artisan.memory.storage.WorkingMemoryStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Pin 压缩处理器。
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
@Component
public class PinMessageEventHandler extends EventHandler<PinMessageEvent> {

    @Autowired
    private WorkingMemoryStorage workingMemoryStorage;

    @Autowired
    private PinJudgmentStrategy pinJudgmentStrategy;

    @Autowired
    private MemoryEventPublisher eventPublisher;

    @Override
    public void onEvent(PinMessageEvent event) {
        String conversationId = event.getSessionId();
        if (pinJudgmentStrategy == null) {
            log.debug("[MEMORY]-[{}] 未配置 PinJudgmentStrategy", conversationId);
            return;
        }

        try {
            WorkingMemory workingMemory = workingMemoryStorage.load(conversationId);
            var historyPins = workingMemory.getActivePins();
            var judgment = pinJudgmentStrategy.judgePin(event.getPinMessagePair(), historyPins);
            if (judgment == null || !judgment.shouldPin()) {
                log.info("[MEMORY]-[{}] 不需要创建 Pin", conversationId);
                return;
            }

            String pinContent = judgment.getPinContent();
            if (pinContent == null || pinContent.isEmpty() || "null".equals(pinContent)) {
                log.warn("[MEMORY]-[{}] Pin 内容为空或 null，跳过", conversationId);
                return;
            }

            if (judgment.getNegatesPinId() != null && !judgment.getNegatesPinId().isEmpty()) {
                workingMemory.invalidatePin(judgment.getNegatesPinId());
                log.info("[MEMORY]-[{}] 已失效旧 Pin, pinId: {}", conversationId, judgment.getNegatesPinId());
            }

            Pin newPin = Pin.builder()
                    .conversationId(conversationId)
                    .content(judgment.getPinContent())
                    .confidence(judgment.getConfidence())
                    .build();

            if (event.getPinMessagePair().getUserMessage() != null) {
                newPin.addSourceMessageId(event.getPinMessagePair().getUserMessage().getId());
            }
            if (event.getPinMessagePair().getAssistantMessage() != null) {
                newPin.addSourceMessageId(event.getPinMessagePair().getAssistantMessage().getId());
            }

            workingMemory.addPin(newPin);
            workingMemoryStorage.save(workingMemory);
            log.info("[MEMORY]-[{}] Pin 压缩完成并已保存: pinId: {}", conversationId, newPin.getPinId());

            // 触发 Pin 聚合事件（由 PinAggregationStrategy 判定是否需要聚合）
            PinAggregationEvent aggregationEvent = new PinAggregationEvent(conversationId);
            eventPublisher.publishEvent(aggregationEvent);
        } catch (Exception e) {
            log.error("[MEMORY]-[{}] Pin 压缩失败", conversationId, e);
        }
    }

    @Override
    public String getEventType() {
        return PinMessageEvent.class.getName();
    }
}
