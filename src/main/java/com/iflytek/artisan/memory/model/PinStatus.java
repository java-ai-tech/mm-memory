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
package com.iflytek.artisan.memory.model;

/**
 * Pin 状态枚举。
 *
 * <p>定义 Pin（确认事实）的生命周期状态：
 * <ul>
 *   <li>ACTIVE - 有效状态，该 Pin 仍然有效并应用于对话</li>
 *   <li>INVALIDATED - 已失效状态，该 Pin 已被更正或删除</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
public enum PinStatus {
    /**
     * 有效状态 - Pin 仍然有效
     */
    ACTIVE,

    /**
     * 已失效状态 - Pin 已被更正或删除
     */
    INVALIDATED
}
