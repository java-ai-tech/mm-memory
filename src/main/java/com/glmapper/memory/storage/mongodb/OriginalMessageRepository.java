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

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for OriginalMessageEntity.
 *
 * <p>Provides standard CRUD operations and custom queries for message storage.
 */
@Repository
public interface OriginalMessageRepository extends MongoRepository<OriginalMessageEntity, String> {

    /**
     * Find all messages by storage key, ordered by timestamp.
     *
     * @param storageKey the storage key
     * @return list of messages
     */
    List<OriginalMessageEntity> findByStorageKeyOrderByTimestampAsc(String storageKey);

    /**
     * Count messages by storage key.
     *
     * @param storageKey the storage key
     * @return count of messages
     */
    long countByStorageKey(String storageKey);

    /**
     * Delete all messages by storage key.
     *
     * @param storageKey the storage key
     */
    void deleteByStorageKey(String storageKey);

    /**
     * Find messages by storage key with pagination.
     *
     * @param storageKey the storage key
     * @param pageable   pagination information
     * @return list of messages
     */
    @Query("{ 'storageKey': ?0 }")
    List<OriginalMessageEntity> findByStorageKey(String storageKey, Pageable pageable);
}
