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
package com.glmapper.memory.compression;

/**
 * 压缩策略提示词模板
 *
 * <p>按压缩策略顺序组织提示词（从轻量级到重量级）：
 * <ol>
 *   <li>策略 0. 工具调用压缩 - 压缩工具调用和结果</li>
 *   <li>策略 1: Pin 判定 - 判断哪些对话需要长期保留</li>
 *   <li>策略 2: 当前轮次压缩 - 压缩当前对话轮次</li>
 *   <li>策略 3: 历史对话摘要 - 摘要历史对话</li>
 *   <li>策略 4: Pin 聚合 - 聚合多个 Pin 为一个摘要</li>
 * </ol>
 */
public class Prompts {

    // ============================================================================
    // 策略 1: Pin 判定
    // ============================================================================

    /**
     * Pin 判定提示词（旧版本）
     *
     * <p>用于判断一个对话轮次是否应该被 Pin（标记为长期保留）。
     * @deprecated 请使用 {@link #PIN_JUDGMENT_PROMPT_V2} 代替
     */
    @Deprecated
    public static String PIN_JUDGMENT_PROMPT = "你是一位专业的对话分析专家。你的任务是判断一个对话轮次（用户消息 + 助手回复）" + "是否应该被标记为 Pin（长期保留）。\n\n" + "应该 Pin 的条件：\n" + "1. 包含明确、稳定的事实或约束条件（不会改变的）\n" + "2. 对所有未来回复有长期影响\n" + "3. 确认或否定了之前的结论\n" + "4. 包含明确的用户需求、澄清或更正\n\n" + "不应该 Pin 的条件：\n" + "1. 探索性或临时性的对话\n" + "2. 特定上下文的临时信息\n" + "3. 可从其他信息推导出的内容\n" + "4. 纯粹的问候或礼节性对话\n\n" + "另外，检查此对话是否否定或更正了之前的 Pin。如果是，" + "识别应该删除哪个历史 Pin。\n\n" + "请以 JSON 格式回复：\n" + "{\n" + "  \"shouldPin\": true/false,\n" + "  \"reason\": \"简要说明\",\n" + "  \"negatesPinId\": \"需要删除的历史 Pin 的 ID，如果没有则为 null\"\n" + "}";

    /**
     * Pin 判定提示词 V2
     *
     * <p>用于判断一个对话轮次是否包含确认事实（Claim/Pin），并提取 Pin 内容。
     */
    public static final String PIN_JUDGMENT_PROMPT_V2 = """
            你是一位专业的对话分析专家，你的任务是判断一个对话轮次（用户消息 + 助手回复）是否应该被标记为 Pin（长期保留），并且从历史 Pin 中识别是否有需要被否定或更正的 Pin。
            
            我会提供当前对话内容(current_messages)和历史 Pin 消息(historical_pins)，请根据以下原则进行判断。
            
            **基本原则：**
            
            1、应该 Pin 的条件：
            * 包含明确、稳定的事实或约束条件（不会改变的）
            * 对所有未来回复有长期影响
            * 确认或否定了之前的结论
            * 包含明确的用户偏好、需求、澄清或更正
            
            2、不应该 Pin 的条件：
            * 探索性或临时性的对话
            * 特定上下文的临时信息
            * 可从其他信息推导出的内容
            * 纯粹的问候或礼节性对话
            
            **重要说明：**
            
            如果 shouldPin 为 true，你必须提供 pinContent 字段！
            * pinContent 应该是一个简洁的陈述句，提取对话中的核心事实、约束或决策
            * 不要包含推理过程或解释，只保留核心信息
            * 使用第三人称客观陈述（例如："用户偏好使用 PostgreSQL"而不是"我喜欢 PostgreSQL"）
            
            **输出要求**
            
            请严格按照以下 JSON 格式回复，不要添加任何额外的文本或 markdown 标记：
            {
                "shouldPin": true/false,
                "reason": "简要说明判断理由（一句话）",
                "pinContent": "提取的核心内容（陈述句形式），如果 shouldPin 为 false 则为空字符串",
                "confidence": 0.9,
                "negatesPinId": "需要失效的历史 Pin 的 ID，如果没有则为 null"
            }
            
            示例 1 - 应该 Pin：
            {
                "shouldPin": true,
                "reason": "用户明确表达了技术选型偏好",
                "pinContent": "项目必须使用 Spring Boot 3.x 版本",
                "confidence": 0.95,
                "negatesPinId": null
            }
            
            示例 2 - 不应该 Pin：
            {
                "shouldPin": false,
                "reason": "探索性问题，没有明确结论",
                "pinContent": "",
                "confidence": 0.8,
                "negatesPinId": null
            }
            """;

    // ============================================================================
    // 策略 1.5: 工具调用压缩
    // ============================================================================

    /**
     * 工具调用压缩提示词
     *
     * <p>用于压缩包含工具调用（ToolUseBlock 或 ToolResultBlock）的对话轮次。
     */
    public static final String TOOL_CALL_COMPRESSION_PROMPT = "你是一位专业的工具调用摘要专家。你的任务是将工具执行信息压缩为简洁的事实性摘要。\n\n" + "要求：\n" + "- 保留工具名称的原文\n" + "- 摘要输入参数：只提取关键值（不是整个 JSON）\n" + "- 摘要输出结果：关注结果，不是原始数据\n" + "- 使用格式：'使用 [工具名称] 调用参数 [关键参数] -> [结果摘要]'\n" + "- 输出必须是纯文本，不要 markdown 或 JSON\n" + "- 对于失败的调用，简要提及错误\n" + "- 对于搜索/检索工具，提及结果数量和关键发现";

    // ============================================================================
    // 策略 2: 当前轮次压缩
    // ============================================================================

    /**
     * 当前轮次压缩提示词
     *
     * <p>用于压缩当前对话轮次（用户消息 + 助手回复）为简洁的事实性陈述。
     */
    public static String CURRENT_ROUND_COMPRESSION_PROMPT = "你是一位专业的内容压缩专家。你的任务是将一个对话轮次（用户消息 + 助手回复）" + "压缩为简洁的事实性陈述。\n\n" + "要求：\n" + "- 使用陈述性语句\n" + "- 只保留已确认的事实和清晰的结论\n" + "- 删除：推理过程、装饰性语言、试探性或探索性内容\n" + "- 输出必须是纯文本，不要 markdown 或 JSON\n" + "- 保留未来参考所需的所有关键信息";

    // ============================================================================
    // 策略 3: 历史对话摘要
    // ============================================================================

    /**
     * 历史对话摘要提示词
     *
     * <p>用于将多个历史对话轮次摘要为简洁的事实性陈述。
     */
    public static String HISTORY_SUMMARIZATION_PROMPT = "你是一位专业的对话摘要专家。你的任务是将历史对话轮次摘要为简洁的事实性陈述。\n\n" + "要求：\n" + "- 使用客观的陈述性语句\n" + "- 只保留事实、决策和结论\n" + "- 不要引入新的推断\n" + "- 输出必须是纯文本，不要 markdown 或 JSON\n" + "- 保留未来参考所需的所有关键信息";

    // ============================================================================
    // 策略 4: Pin 聚合
    // ============================================================================

    /**
     * Pin 聚合提示词
     *
     * <p>用于将多个 Pin（长期保留的对话）聚合为一个综合摘要。
     */
    public static String PIN_AGGREGATION_PROMPT = "你是一位专业的信息整合专家。你的任务是将多个 Pin（长期保留的对话）" + "聚合为一个综合的 Pin 摘要。\n\n" + "要求：\n" + "- 提取核心约束和已确认的重要结论\n" + "- 识别长期有效的用户偏好和前提条件\n" + "- 整合相关信息\n" + "- 输出必须是纯文本，不要 markdown 或 JSON\n" + "- 保留未来参考所需的所有关键信息";

    private Prompts() {
        // 工具类
    }
}
