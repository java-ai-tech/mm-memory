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
package com.iflytek.artisan.memory.storage;

import com.iflytek.artisan.memory.model.MessagePair;
import java.util.List;

/**
 * Storage interface for original memory.
 *
 * <p>Original memory contains complete, uncompressed message pair history.
 * This storage is append-only and needs to support large volumes of data.
 */
public interface OriginalStorage {

    /**
     * Appends a message pair to original storage.
     *
     * @param key the storage key (e.g., session ID)
     * @param pair the message pair to append
     */
    void append(String key, MessagePair pair);

    /**
     * Gets all message pairs from original storage in chronological order.
     *
     * @param key the storage key
     * @return list of all message pairs, or empty list if not found
     */
    List<MessagePair> getAll(String key);

    /**
     * Clears all message pairs from original storage.
     *
     * @param key the storage key
     */
    void clear(String key);

    /**
     * Gets the total count of message pairs in original storage.
     *
     * @param key the storage key
     * @return the message pair count
     */
    long count(String key);

    /**
     * Gets message pairs within a specified range.
     *
     * @param key the storage key
     * @param offset the starting offset (0-based)
     * @param limit the maximum number of message pairs to return
     * @return list of message pairs within the range
     */
    List<MessagePair> getRange(String key, int offset, int limit);
}
