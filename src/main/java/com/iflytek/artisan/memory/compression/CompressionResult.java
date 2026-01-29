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
package com.iflytek.artisan.memory.compression;

import com.iflytek.artisan.memory.model.MessagePair;
import com.iflytek.artisan.memory.model.Msg;
import com.iflytek.artisan.memory.model.Pin;
import java.util.List;

/**
 * 压缩操作的结果。
 *
 * <p>此类封装了压缩策略执行后的结果信息，包括：
 * <ul>
 *   <li>是否成功执行压缩</li>
 *   <li>压缩后的消息对列表</li>
 *   <li>压缩后的摘要消息（可选）</li>
 *   <li>压缩的消息对数量</li>
 *   <li>聚合后的 Pin（仅用于 Pin 聚合策略）</li>
 * </ul>
 *
 * <p>使用工厂方法 {@link #compressed()} 或 {@link #notCompressed()} 创建实例。
 */
public class CompressionResult {

    /** 是否成功执行压缩 */
    private final boolean compressed;

    /** 压缩后的消息对列表 */
    private final List<MessagePair> compressedPairs;

    /** 压缩后的摘要消息（用于当前轮次摘要等场景） */
    private final Msg summaryMsg;

    /** 压缩的消息对数量 */
    private final int pairsCompressed;

    /** 聚合后的 Pin（仅用于 Pin 聚合策略） */
    private final Pin aggregatedPin;


    /**
     * 构造压缩结果。
     *
     * @param compressed       是否成功执行压缩
     * @param compressedPairs  压缩后的消息对列表
     * @param summaryMsg       压缩后的摘要消息（可选）
     * @param pairsCompressed  压缩的消息对数量
     * @param aggregatedPin    聚合后的 Pin（可选）
     */
    public CompressionResult(
            boolean compressed,
            List<MessagePair> compressedPairs,
            Msg summaryMsg,
            int pairsCompressed,
            Pin aggregatedPin) {
        this.compressed = compressed;
        this.compressedPairs = compressedPairs;
        this.summaryMsg = summaryMsg;
        this.pairsCompressed = pairsCompressed;
        this.aggregatedPin = aggregatedPin;
    }

    /**
     * 创建一个表示未压缩的结果。
     *
     * <p>当压缩策略不适用或压缩失败时使用此方法。
     *
     * @return 未压缩的结果对象
     */
    public static CompressionResult notCompressed() {
        return new CompressionResult(false, List.of(), null, 0, null);
    }

    /**
     * 创建一个表示成功压缩的结果（返回消息对列表）。
     *
     * @param compressedPairs  压缩后的消息对列表
     * @param pairsCompressed  压缩的消息对数量
     * @return 压缩成功的结果对象
     */
    public static CompressionResult compressed(
            List<MessagePair> compressedPairs, int pairsCompressed) {
        return new CompressionResult(true, compressedPairs, null, pairsCompressed, null);
    }

    /**
     * 创建一个表示成功压缩的结果（返回摘要消息）。
     *
     * <p>用于当前轮次摘要等场景，策略生成摘要但不负责存储。
     *
     * @param summaryMsg       生成的摘要消息
     * @param pairsCompressed  压缩的消息对数量
     * @return 压缩成功的结果对象
     */
    public static CompressionResult compressedWithSummary(
            Msg summaryMsg, int pairsCompressed) {
        return new CompressionResult(true, List.of(), summaryMsg, pairsCompressed, null);
    }

    /**
     * 创建一个 Pin 聚合的结果。
     *
     * <p>用于 Pin 聚合策略，返回聚合后的 Pin。
     *
     * @param aggregatedPin    聚合后的 Pin
     * @param pairsCompressed  被聚合的 Pin 数量
     * @return 聚合成功的结果对象
     */
    public static CompressionResult aggregated(Pin aggregatedPin, int pairsCompressed) {
        return new CompressionResult(true, List.of(), null, pairsCompressed, aggregatedPin);
    }

    /**
     * 检查是否成功执行了压缩。
     *
     * @return 如果成功压缩返回 true，否则返回 false
     */
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * 获取压缩后的消息对列表。
     *
     * @return 压缩后的消息对列表
     */
    public List<MessagePair> getCompressedPairs() {
        return compressedPairs;
    }

    /**
     * 获取压缩后的摘要消息。
     *
     * @return 摘要消息，如果没有则返回 null
     */
    public Msg getSummaryMsg() {
        return summaryMsg;
    }

    /**
     * 获取压缩后的消息列表（从消息对中提取）。
     *
     * @return 压缩后的消息列表
     * @deprecated 请使用 {@link #getCompressedPairs()} 代替。
     */
    @Deprecated
    public List<com.iflytek.artisan.memory.model.Msg> getCompressedMessages() {
        List<com.iflytek.artisan.memory.model.Msg> messages = new java.util.ArrayList<>();
        for (MessagePair pair : compressedPairs) {
            messages.addAll(pair.getAllMessages());
        }
        return messages;
    }

    /**
     * 获取压缩的消息对数量。
     *
     * @return 压缩的消息对数量
     */
    public int getPairsCompressed() {
        return pairsCompressed;
    }

    /**
     * 获取压缩的消息数量（已废弃）。
     *
     * @return 压缩的消息对数量（与 getPairsCompressed 相同）
     * @deprecated 请使用 {@link #getPairsCompressed()} 代替。
     */
    @Deprecated
    public int getMessagesCompressed() {
        return pairsCompressed;
    }

    /**
     * 获取聚合后的 Pin。
     *
     * @return 聚合后的 Pin，如果不是 Pin 聚合则返回 null
     */
    public Pin getAggregatedPin() {
        return aggregatedPin;
    }

    /**
     * 获取压缩的数量（兼容方法）。
     *
     * @return 压缩的数量
     */
    public int getCompressedCount() {
        return pairsCompressed;
    }

}
