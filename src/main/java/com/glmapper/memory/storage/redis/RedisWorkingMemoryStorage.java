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
package com.glmapper.memory.storage.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.glmapper.memory.model.MessagePair;
import com.glmapper.memory.model.Msg;
import com.glmapper.memory.model.Pin;
import com.glmapper.memory.model.PinStatus;
import com.glmapper.memory.model.WorkingMemory;
import com.glmapper.memory.storage.WorkingMemoryStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redis 实现的 WorkingMemoryStorage。
 *
 * <p>使用 Redis 的不同数据结构存储 WorkingMemory 的各个分区：
 * <ul>
 *   <li>artisan:wm:{conversationId}:head - List，存储 Head 区域</li>
 *   <li>artisan:wm:{conversationId}:tail - List，存储 Tail 区域</li>
 *   <li>artisan:wm:{conversationId}:tcw - List，存储 timingContextWindow</li>
 *   <li>artisan:wm:{conversationId}:pins - Hash，存储 pinnedFacts</li>
 * </ul>
 *
 * @author glsong
 * @since 1.0.0
 */
@Slf4j
public class RedisWorkingMemoryStorage implements WorkingMemoryStorage {

    private static final String KEY_PREFIX = "artisan:wm:";
    private static final String HEAD_SUFFIX = ":head";
    private static final String TAIL_SUFFIX = ":tail";
    private static final String TCW_SUFFIX = ":tcw";
    private static final String PINS_SUFFIX = ":pins";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int expireDays;

    public RedisWorkingMemoryStorage(StringRedisTemplate redisTemplate, int expireDays) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.expireDays = expireDays;
    }

    // ==================== 完整 WorkingMemory 操作 ====================
    
    @Override
    public WorkingMemory load(String conversationId) {
        try {
            WorkingMemory wm = new WorkingMemory();
            wm.setConversationId(conversationId);
            wm.setHead(new ArrayDeque<>(getHead(conversationId)));
            wm.setTail(new ArrayDeque<>(getTail(conversationId)));
            wm.setTimingContextWindow(getTimingContextWindow(conversationId));
            wm.setPinnedFacts(getAllPins(conversationId));
            return wm;
        } catch (Exception e) {
            log.error("Failed to load working memory: conversationId={}", conversationId, e);
            return new WorkingMemory();
        }
    }

    @Override
    public void save(WorkingMemory workingMemory) {
        if (workingMemory == null || workingMemory.getConversationId() == null) {
            return;
        }
        String conversationId = workingMemory.getConversationId();

        try {
            // 清空并重新保存 Head
            String headKey = getHeadKey(conversationId);
            redisTemplate.delete(headKey);
            if (workingMemory.getHead() != null && !workingMemory.getHead().isEmpty()) {
                for (MessagePair pair : workingMemory.getHead()) {
                    redisTemplate.opsForList().rightPush(headKey, serialize(pair));
                }
            }

            // 清空并重新保存 Tail
            String tailKey = getTailKey(conversationId);
            redisTemplate.delete(tailKey);
            if (workingMemory.getTail() != null && !workingMemory.getTail().isEmpty()) {
                for (MessagePair pair : workingMemory.getTail()) {
                    redisTemplate.opsForList().rightPush(tailKey, serialize(pair));
                }
            }

            // 清空并重新保存 timingContextWindow
            String tcwKey = getTcwKey(conversationId);
            redisTemplate.delete(tcwKey);
            if (workingMemory.getTimingContextWindow() != null && !workingMemory.getTimingContextWindow().isEmpty()) {
                for (Msg msg : workingMemory.getTimingContextWindow()) {
                    redisTemplate.opsForList().rightPush(tcwKey, serialize(msg));
                }
            }

            // 清空并重新保存 Pins
            String pinsKey = getPinsKey(conversationId);
            redisTemplate.delete(pinsKey);
            if (workingMemory.getPinnedFacts() != null && !workingMemory.getPinnedFacts().isEmpty()) {
                for (Pin pin : workingMemory.getPinnedFacts()) {
                    redisTemplate.opsForHash().put(pinsKey, pin.getPinId(), serialize(pin));
                }
            }

            // 设置过期时间
            Duration expireDuration = Duration.ofDays(expireDays);
            redisTemplate.expire(headKey, expireDuration);
            redisTemplate.expire(tailKey, expireDuration);
            redisTemplate.expire(tcwKey, expireDuration);
            redisTemplate.expire(pinsKey, expireDuration);

            log.debug("Saved working memory: conversationId={}, expireDays={}", conversationId, expireDays);
        } catch (Exception e) {
            log.error("Failed to save working memory: conversationId={}", conversationId, e);
            throw new RuntimeException("Failed to save working memory to Redis", e);
        }
    }

    @Override
    public void clear(String conversationId) {
        try {
            redisTemplate.delete(getHeadKey(conversationId));
            redisTemplate.delete(getTailKey(conversationId));
            redisTemplate.delete(getTcwKey(conversationId));
            redisTemplate.delete(getPinsKey(conversationId));
            log.debug("Cleared working memory: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("Failed to clear working memory: conversationId={}", conversationId, e);
        }
    }

    // ==================== Head 操作 ====================

    @Override
    public void setHead(String conversationId, MessagePair pair, int maxSize) {
        try {
            String key = getHeadKey(conversationId);
            Long currentSize = redisTemplate.opsForList().size(key);
            if (currentSize == null || currentSize < maxSize) {
                redisTemplate.opsForList().rightPush(key, serialize(pair));
                log.debug("Added to head: conversationId={}", conversationId);
            }
        } catch (Exception e) {
            log.error("Failed to set head: conversationId={}", conversationId, e);
            throw new RuntimeException("Failed to set head in Redis", e);
        }
    }

    @Override
    public List<MessagePair> getHead(String conversationId) {
        try {
            String key = getHeadKey(conversationId);
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }
            return jsonList.stream().map(json -> deserialize(json, MessagePair.class)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get head: conversationId={}", conversationId, e);
            return new ArrayList<>();
        }
    }

    // ==================== Tail 操作 ====================

    @Override
    public void addToTail(String conversationId, MessagePair pair, int maxSize) {
        try {
            String key = getTailKey(conversationId);
            redisTemplate.opsForList().rightPush(key, serialize(pair));
            // 如果超过最大数量，移除最旧的
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > maxSize) {
                redisTemplate.opsForList().leftPop(key);
            }
            log.debug("Added to tail: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("Failed to add to tail: conversationId={}", conversationId, e);
            throw new RuntimeException("Failed to add to tail in Redis", e);
        }
    }

    @Override
    public List<MessagePair> getTail(String conversationId) {
        try {
            String key = getTailKey(conversationId);
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }
            return jsonList.stream().map(json -> deserialize(json, MessagePair.class)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get tail: conversationId={}", conversationId, e);
            return new ArrayList<>();
        }
    }

    // ==================== timingContextWindow 操作 ====================

    @Override
    public void addToTimingContextWindow(String conversationId, Msg message) {
        try {
            String key = getTcwKey(conversationId);
            redisTemplate.opsForList().rightPush(key, serialize(message));
            log.debug("Added to timingContextWindow: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("Failed to add to timingContextWindow: conversationId={}", conversationId, e);
            throw new RuntimeException("Failed to add to timingContextWindow in Redis", e);
        }
    }

    @Override
    public List<Msg> getTimingContextWindow(String conversationId) {
        try {
            String key = getTcwKey(conversationId);
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
            if (jsonList == null || jsonList.isEmpty()) {
                return new ArrayList<>();
            }
            return jsonList.stream().map(json -> deserialize(json, Msg.class)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get timingContextWindow: conversationId={}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void clearTimingContextWindow(String conversationId) {
        try {
            String key = getTcwKey(conversationId);
            redisTemplate.delete(key);
            log.debug("Cleared timingContextWindow: conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("Failed to clear timingContextWindow: conversationId={}", conversationId, e);
        }
    }

    @Override
    public void setTimingContextWindow(String conversationId, List<Msg> messages) {
        try {
            String key = getTcwKey(conversationId);
            redisTemplate.delete(key);
            if (messages != null && !messages.isEmpty()) {
                for (Msg msg : messages) {
                    redisTemplate.opsForList().rightPush(key, serialize(msg));
                }
            }
            log.debug("Set timingContextWindow: conversationId={}, size={}", conversationId, messages != null ? messages.size() : 0);
        } catch (Exception e) {
            log.error("Failed to set timingContextWindow: conversationId={}", conversationId, e);
            throw new RuntimeException("Failed to set timingContextWindow in Redis", e);
        }
    }

    // ==================== Pin 操作 ====================

    @Override
    public void addPin(String conversationId, Pin pin) {
        try {
            String key = getPinsKey(conversationId);
            redisTemplate.opsForHash().put(key, pin.getPinId(), serialize(pin));
            log.debug("Added pin: conversationId={}, pinId={}", conversationId, pin.getPinId());
        } catch (Exception e) {
            log.error("Failed to add pin: conversationId={}, pinId={}", conversationId, pin.getPinId(), e);
            throw new RuntimeException("Failed to add pin to Redis", e);
        }
    }

    @Override
    public boolean invalidatePin(String conversationId, String pinId) {
        try {
            String key = getPinsKey(conversationId);
            Object pinJson = redisTemplate.opsForHash().get(key, pinId);
            if (pinJson == null) {
                return false;
            }
            Pin pin = deserialize((String) pinJson, Pin.class);
            pin.setStatus(PinStatus.INVALIDATED);
            redisTemplate.opsForHash().put(key, pinId, serialize(pin));
            log.info("Invalidated pin: conversationId={}, pinId={}", conversationId, pinId);
            return true;
        } catch (Exception e) {
            log.error("Failed to invalidate pin: conversationId={}, pinId={}", conversationId, pinId, e);
            return false;
        }
    }

    @Override
    public List<Pin> getActivePins(String conversationId) {
        return getAllPins(conversationId).stream().filter(Pin::isActive).collect(Collectors.toList());
    }

    @Override
    public List<Pin> getAllPins(String conversationId) {
        try {
            String key = getPinsKey(conversationId);
            List<Object> values = redisTemplate.opsForHash().values(key);
            if (values == null || values.isEmpty()) {
                return new ArrayList<>();
            }
            return values.stream().map(json -> deserialize((String) json, Pin.class)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get pins: conversationId={}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deletePin(String conversationId, String pinId) {
        try {
            String key = getPinsKey(conversationId);
            Long deleted = redisTemplate.opsForHash().delete(key, pinId);
            if (deleted != null && deleted > 0) {
                log.info("Deleted pin: conversationId={}, pinId={}", conversationId, pinId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to delete pin: conversationId={}, pinId={}", conversationId, pinId, e);
            return false;
        }
    }

    // ==================== 辅助方法 ====================

    private String getHeadKey(String conversationId) {
        return KEY_PREFIX + conversationId + HEAD_SUFFIX;
    }

    private String getTailKey(String conversationId) {
        return KEY_PREFIX + conversationId + TAIL_SUFFIX;
    }

    private String getTcwKey(String conversationId) {
        return KEY_PREFIX + conversationId + TCW_SUFFIX;
    }

    private String getPinsKey(String conversationId) {
        return KEY_PREFIX + conversationId + PINS_SUFFIX;
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public WorkingMemory recover(String conversationId, List<MessagePair> originalPairs) {
        if (originalPairs == null || originalPairs.isEmpty()) {
            return new WorkingMemory();
        }

        WorkingMemory wm = new WorkingMemory();
        wm.setConversationId(conversationId);

        int totalRounds = originalPairs.size();
        if (totalRounds == 0) {
            return wm;
        }

        int headSize = 1;
        int tailSize = 2;

        if (totalRounds == 1) {
            wm.setHead(new ArrayDeque<>(List.of(originalPairs.get(0))));
        } else {
            wm.setHead(new ArrayDeque<>(originalPairs.subList(0, Math.min(headSize, totalRounds))));
            int tailStart = Math.max(headSize, totalRounds - tailSize);
            if (tailStart < totalRounds) {
                wm.setTail(new ArrayDeque<>(originalPairs.subList(tailStart, totalRounds)));
            }
        }

        save(wm);
        log.info("Recovered working memory from history: conversationId={}, totalRounds={}", conversationId, totalRounds);
        return wm;
    }
}
