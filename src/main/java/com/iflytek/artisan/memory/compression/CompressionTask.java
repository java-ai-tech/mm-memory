package com.iflytek.artisan.memory.compression;

import com.iflytek.artisan.memory.model.MessagePair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 压缩任务实体。
 *
 * <p>用于在 Redis 队列中传递压缩任务信息。
 *
 * @author glsong
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompressionTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 会话标识符
     */
    private String sessionId;

    /**
     * 当前对话对
     */
    private MessagePair currentPair;

    /**
     * 从 Tail 移出的消息对（可能为 null）
     */
    private MessagePair evictedPair;
}
