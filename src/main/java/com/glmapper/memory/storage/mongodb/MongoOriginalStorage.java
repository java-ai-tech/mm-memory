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
package com.glmapper.memory.storage.mongodb;

import com.glmapper.memory.model.MessagePair;
import com.glmapper.memory.storage.OriginalStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * MongoDB-based implementation of OriginalStorage using Spring Data MongoDB.
 *
 * <p>Uses Spring Data MongoDB Repository for storing original message pairs,
 * indexed by timestamp for efficient chronological queries.
 */
@Slf4j
public class MongoOriginalStorage implements OriginalStorage {

    private final OriginalMessageRepository repository;

    public MongoOriginalStorage(OriginalMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(String key, MessagePair pair) {
        try {
            OriginalMessageEntity entity =
                    new OriginalMessageEntity(key, System.currentTimeMillis(), pair);
            repository.save(entity);
            log.debug(
                    "Appended message pair to original storage: key={}, userMsgId={}",
                    key,
                    pair.getUserMessage() != null ? pair.getUserMessage().getId() : "null");
        } catch (Exception e) {
            log.error("Failed to append message pair to original storage: key={}", key, e);
            throw new RuntimeException("Failed to append message pair to MongoDB", e);
        }
    }

    @Override
    public List<MessagePair> getAll(String key) {
        try {
            List<OriginalMessageEntity> entities =
                    repository.findByStorageKeyOrderByTimestampAsc(key);
            List<MessagePair> pairs =
                    entities.stream()
                            .map(OriginalMessageEntity::getMessagePair)
                            .filter(pair -> pair != null)
                            .collect(Collectors.toList());
            log.debug("Retrieved {} message pairs from original storage: key={}", pairs.size(), key);
            return pairs;
        } catch (Exception e) {
            log.error("Failed to get message pairs from original storage: key={}", key, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void clear(String key) {
        try {
            repository.deleteByStorageKey(key);
            log.debug("Cleared original storage: key={}", key);
        } catch (Exception e) {
            log.error("Failed to clear original storage: key={}", key, e);
        }
    }

    @Override
    public long count(String key) {
        try {
            return repository.countByStorageKey(key);
        } catch (Exception e) {
            log.error("Failed to count message pairs in original storage: key={}", key, e);
            return 0;
        }
    }

    @Override
    public List<MessagePair> getRange(String key, int offset, int limit) {
        try {
            // Calculate page number and handle offset that's not a multiple of limit
            int pageNumber = offset / limit;
            int skipInPage = offset % limit;
            
            PageRequest pageRequest =
                    PageRequest.of(pageNumber, limit, Sort.by(Sort.Direction.ASC, "timestamp"));
            List<OriginalMessageEntity> entities = repository.findByStorageKey(key, pageRequest);
            
            // If offset is not a multiple of limit, skip additional items within the page
            // and potentially fetch more pages to ensure we return exactly 'limit' items
            if (skipInPage > 0 && !entities.isEmpty()) {
                List<MessagePair> allPairs = new ArrayList<>();
                allPairs.addAll(entities.stream()
                        .skip(skipInPage)
                        .map(OriginalMessageEntity::getMessagePair)
                        .filter(pair -> pair != null)
                        .collect(Collectors.toList()));
                
                // If we don't have enough message pairs, fetch from next page
                if (allPairs.size() < limit) {
                    int remainingNeeded = limit - allPairs.size();
                    PageRequest nextPageRequest =
                            PageRequest.of(pageNumber + 1, limit, Sort.by(Sort.Direction.ASC, "timestamp"));
                    List<OriginalMessageEntity> nextPageEntities = 
                            repository.findByStorageKey(key, nextPageRequest);
                    
                    allPairs.addAll(nextPageEntities.stream()
                            .limit(remainingNeeded)
                            .map(OriginalMessageEntity::getMessagePair)
                            .filter(pair -> pair != null)
                            .collect(Collectors.toList()));
                }
                
                log.debug("Retrieved {} message pairs from original storage: key={}, offset={}, limit={}", 
                        allPairs.size(), key, offset, limit);
                return allPairs;
            }
            
            List<MessagePair> pairs = entities.stream()
                    .map(OriginalMessageEntity::getMessagePair)
                    .filter(pair -> pair != null)
                    .collect(Collectors.toList());
            
            log.debug("Retrieved {} message pairs from original storage: key={}, offset={}, limit={}", 
                    pairs.size(), key, offset, limit);
            return pairs;
        } catch (Exception e) {
            log.error("Failed to get range from original storage: key={}, offset={}, limit={}", 
                    key, offset, limit, e);
            return new ArrayList<>();
        }
    }
}
