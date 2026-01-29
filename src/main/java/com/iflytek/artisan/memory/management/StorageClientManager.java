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
package com.iflytek.artisan.memory.management;

import com.iflytek.artisan.memory.storage.OriginalStorage;
import com.iflytek.artisan.memory.storage.WorkingMemoryStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 存储客户端管理器。
 *
 * <p>管理所有存储实例的生命周期，包括：
 * <ul>
 *   <li>WorkingMemoryStorage - 分区工作记忆存储（Redis）</li>
 *   <li>OriginalStorage - 原始消息存储（MongoDB）</li>
 * </ul>
 */
@Component
public class StorageClientManager {

    private final WorkingMemoryStorage workingMemoryStorage;
    private final OriginalStorage originalStorage;

    @Autowired
    public StorageClientManager(
            WorkingMemoryStorage workingMemoryStorage,
            OriginalStorage originalStorage) {
        this.workingMemoryStorage = workingMemoryStorage;
        this.originalStorage = originalStorage;
    }

    /**
     * 获取 WorkingMemoryStorage 实例。
     *
     * @return WorkingMemoryStorage 实例
     */
    public WorkingMemoryStorage getWorkingMemoryStorage() {
        return workingMemoryStorage;
    }

    /**
     * 获取 OriginalStorage 实例。
     *
     * @return OriginalStorage 实例
     */
    public OriginalStorage getOriginalStorage() {
        return originalStorage;
    }
}

