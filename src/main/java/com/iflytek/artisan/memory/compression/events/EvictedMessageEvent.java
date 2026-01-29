package com.iflytek.artisan.memory.compression.events;

import com.iflytek.artisan.memory.model.MessagePair;

/**
 * @Classname EvictedMessageEvent
 * @Description TODO
 * @Date 1/27/26 2:05 PM
 * @Created by glmapper
 */
public class EvictedMessageEvent extends MemoryEvent {

    private String conversationId;
    private MessagePair evictedMessagePair;

    public EvictedMessageEvent(String conversationId, MessagePair evictedMessagePair) {
        super(conversationId);
        this.evictedMessagePair = evictedMessagePair;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public MessagePair getEvictedMessagePair() {
        return evictedMessagePair;
    }

    public void setEvictedMessagePair(MessagePair evictedMessagePair) {
        this.evictedMessagePair = evictedMessagePair;
    }
}
