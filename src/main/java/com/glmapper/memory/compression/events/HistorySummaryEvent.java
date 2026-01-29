package com.glmapper.memory.compression.events;

/**
 * @Classname HistorySummaryEvent
 * @Description 历史摘要压缩事件
 * @Date 1/27/26 2:52 PM
 * @Created by glmapper
 */
public class HistorySummaryEvent extends MemoryEvent {

    private String conversationId;

    protected HistorySummaryEvent(String sessionId) {
        super(sessionId);
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
