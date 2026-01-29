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
package com.iflytek.artisan.memory.storage.mongodb;

import com.iflytek.artisan.memory.model.MessagePair;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB entity for storing original message pairs.
 *
 * <p>Each message pair is stored with its storage key and timestamp for efficient querying.
 */
@Document(collection = "artisan_original_message_pairs")
@CompoundIndex(name = "storageKey_timestamp", def = "{'storageKey': 1, 'timestamp': 1}")
@Data
public class OriginalMessageEntity {

    @Id
    private String id;
    @Indexed
    private String storageKey;
    @Indexed
    private Long timestamp;
    private MessagePair messagePair;

    public OriginalMessageEntity() {
    }

    public OriginalMessageEntity(String storageKey, Long timestamp, MessagePair messagePair) {
        this.storageKey = storageKey;
        this.timestamp = timestamp;
        this.messagePair = messagePair;
    }
}
