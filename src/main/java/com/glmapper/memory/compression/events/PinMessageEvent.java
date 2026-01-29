package com.glmapper.memory.compression.events;

import com.glmapper.memory.model.MessagePair;

/**
 * @Classname PinMessageEvent
 * @Description PIN 判定事件
 * @Date 1/27/26 1:56 PM
 * @Created by glmapper
 */
public class PinMessageEvent extends MemoryEvent {

    private MessagePair pinMessagePair;
    private String conversationId;

    public PinMessageEvent(String conversationId, MessagePair pinMessagePair) {
        super(conversationId);
        this.pinMessagePair = pinMessagePair;
    }

    public MessagePair getPinMessagePair() {
        return pinMessagePair;
    }

    public void setPinMessagePair(MessagePair pinMessagePair) {
        this.pinMessagePair = pinMessagePair;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
