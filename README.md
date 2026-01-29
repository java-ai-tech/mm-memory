
# Artisan Memory - 智能上下文记忆管理系统

## 📋 目录

- [项目概述](#项目概述)
- [核心架构](#核心架构)
- [记忆管理实现](#记忆管理实现)
- [压缩策略详解](#压缩策略详解)
- [存储架构](#存储架构)
- [已发现的问题和优化建议](#已发现的问题和优化建议)
- [快速开始](#快速开始)

---

## 项目概述

Artisan Memory 是一个为 RAG（Retrieval-Augmented Generation）应用设计的智能上下文记忆管理系统。它通过混合存储架构和渐进式压缩策略，解决了长对话场景下的 Token 管理和上下文窗口限制问题。

### 核心特性

1. **混合存储架构**
   - **工作记忆（Working Memory）**：使用 Redis 存储压缩后的当前对话上下文
   - **原始记忆（Original Memory）**：使用 MongoDB 存储完整的历史对话记录

2. **智能压缩系统（事件驱动）**
   - 3 步渐进式压缩流程：Pin判定 → 当前轮次摘要 → 历史摘要
   - 基于 Message Pair（对话对）的压缩单位
   - Pin 机制：提取确认事实，永不压缩
   - 基于 LLM 的智能摘要生成
   - 事件驱动异步执行，不阻塞主流程

3. **会话管理**
   - 多会话并发支持（ConcurrentHashMap）
   - SessionContext 管理当前对话对
   - 自动过期清理

4. **事件驱动架构**
   - MemoryEventPublisher 同步发布事件
   - PairEvictedEventHandler 处理 Tail 移出事件
   - TimingContextWindowEventHandler 处理历史摘要事件
   - 支持自定义事件监听器扩展

---

## 核心架构

### 系统分层

```
┌─────────────────────────────────────────────────────────┐
│                  API Layer (REST)                       │
│              DemoChatController                         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│              Service Layer                              │
│              ArtisanMemory                              │
│  ┌──────────────────────────────────────────────┐     │
│  │  Session Management  │  Event Publisher      │     │
│  │  Pin Judgment        │  Storage Coordinator  │     │
│  └──────────────────────────────────────────────┘     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│            Event Handlers (事件驱动)                     │
│  PairEvictedEventHandler                                 │
│  TimingContextWindowEventHandler                         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│            Compression Strategies                       │
│  Step 1: PinJudgmentStrategy (Pin判定)                 │
│  Step 2: CurrentRoundCompressionStrategy (当前轮摘要)   │
│  Step 3: HistorySummarizationStrategy (历史摘要)       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│              Storage Layer                              │
│  ┌──────────────────────┐  ┌──────────────┐            │
│  │ WorkingMemoryStorage│  │Original      │            │
│  │    (Redis)          │  │Storage       │            │
│  │  - Head             │  │(MongoDB)     │            │
│  │  - Tail             │  │              │            │
│  │  - timingContextWin │  │              │            │
│  │  - pinnedFacts     │  │              │            │
│  └──────────────────────┘  └──────────────┘            │
└─────────────────────────────────────────────────────────┘
```

### WorkingMemory 分区结构

工作记忆采用**分区存储架构**，将对话上下文分为四个区域：

```
WorkingMemory (conversationId)
├── Head (最旧 1 轮) - 永不压缩
│   └── 保留初始上下文，帮助模型理解对话起点
├── Tail (最新 2 轮) - 永不压缩
│   └── 保留当前上下文，确保模型连贯响应
├── timingContextWindow (历史摘要 + 当前轮次摘要，最大 5 条)
│   └── 唯一可压缩区域，存储摘要消息（Msg）
└── pinnedFacts (确认事实) - 永不压缩
    └── 存储从对话中提取的重要事实信息（Pin）
```

**核心设计原则**：
- **Head/Tail/Pin 永不压缩** - 保留完整上下文
- **timingContextWindow 是唯一可压缩区域** - 通过摘要控制 Token 增长
- **原始消息始终保留在 MongoDB** - 用于审计和 RAG 检索

### 核心组件

#### 1. ArtisanMemory（主服务类）

**职责**：
- 会话生命周期管理（创建、删除、清理）
- 消息对的提交和检索（`commitSessionContext`）
- Pin 判定（同步执行）
- 事件发布（异步压缩流程）

**关键方法**：
- `commitSessionContext(context)`: 提交当前对话对，触发压缩流程
- `getMemoryMessages(context)`: 获取用于 LLM 的消息（按 Head + timingContextWindow + Tail 顺序）
- `getPinnedFacts(context)`: 获取确认事实列表
- `buildSystemPrompt(context)`: 构建包含 Pin 的 System Prompt

**核心流程**：
```java
commitSessionContext(context)
  ├─→ 存储原始消息到 MongoDB
  ├─→ 更新 Head/Tail（如果 Tail 满，返回 evictedPair）
  ├─→ 执行 Pin 判定（同步）
  ├─→ 如果 evictedPair 存在，发布 PairEvictedFromTailEvent
  └─→ 保存 WorkingMemory 到 Redis
```

#### 2. SessionContext（会话上下文）

**职责**：
- 存储单个会话的状态信息
- 管理当前对话对（currentPair）
- 跟踪最后访问时间

**关键字段**：
```java
private final String sessionId;           // 会话ID
private final String storageKey;          // 存储键（带前缀）
private volatile long lastAccessTime;     // 最后访问时间
private MessagePair currentPair;           // 当前对话对
```

#### 3. StorageClientManager（存储管理器）

**职责**：
- 管理存储客户端实例（WorkingMemoryStorage、OriginalStorage）
- 统一存储层的访问接口

---

## 记忆管理实现

### 1. 双层存储架构

#### WorkingMemory (工作记忆) - Redis
- **存储介质**: Redis Hash（JSON 序列化）
- **数据结构**: 分区存储（Head/Tail/timingContextWindow/pinnedFacts）
- **数据特点**: 
  - 快速读写（内存）
  - 频繁更新
  - Token 数量受控
- **生命周期**: 随会话动态变化

**分区说明**：

| 分区 | 内容 | 压缩策略 | 最大容量 |
|-----|------|---------|---------|
| **Head** | 最旧的 1 轮对话 | 永不压缩 | 1 轮 |
| **Tail** | 最新的 2 轮对话 | 永不压缩 | 2 轮 |
| **timingContextWindow** | 历史摘要 + 当前轮次摘要 | 可压缩 | 5 条消息 |
| **pinnedFacts** | 确认事实列表 | 永不压缩 | 无限制 |

**消息组装顺序**（用于构建 LLM Prompt）：
```
Head → timingContextWindow → Tail
```

#### OriginalStorage (原始记忆) - MongoDB
- **存储介质**: MongoDB Collection
- **数据内容**: 完整的历史对话记录（MessagePair）
- **数据特点**:
  - 持久化存储
  - 只增不改
  - 完整性保证
- **生命周期**: 长期保存

**与 WorkingMemory 的对比**：

| 特性 | WorkingMemory (Redis) | OriginalStorage (MongoDB) |
|-----|----------------------|---------------------------|
| **内容类型** | 分区存储：Head/Tail/摘要/Pin | 完整原始消息对 |
| **内容变化** | 动态变化（压缩时生成摘要） | 只增不改 |
| **用途** | 提交给 LLM 的上下文 | 完整历史记录存档 |
| **Token 控制** | 严格控制在阈值内 | 无限制增长 |
| **查询方法** | `getMemoryMessages()` | `getOriginalMessages()` |
| **性能** | 高速读写（内存） | 持久化存储（磁盘） |

### 2. 核心流程：commitSessionContext

**完整流程**：

```
用户提交对话对
    ↓
commitSessionContext(context)
    ├─→ 1. 存储原始消息到 MongoDB（永久保留）
    ├─→ 2. 更新 Head/Tail：
    │      ├─→ 第 1 轮：添加到 Head
    │      └─→ 第 2+ 轮：添加到 Tail
    │          └─→ 如果 Tail 满，返回 evictedPair
    ├─→ 3. 执行 Pin 判定（同步）：
    │      └─→ 使用 LLM 判断是否提取 Pin
    │          ├─→ 如果需要失效旧 Pin：标记为 INVALIDATED
    │          └─→ 如果需要创建新 Pin：添加到 pinnedFacts
    ├─→ 4. 如果 evictedPair 存在，发布事件：
    │      └─→ PairEvictedFromTailEvent
    │          └─→ PairEvictedEventHandler
    │              ├─→ 调用 CurrentRoundCompressionStrategy
    │              ├─→ 如果生成摘要：添加到 timingContextWindow
    │              ├─→ 否则：添加原文到 timingContextWindow
    │              └─→ 发布 TimingContextWindowUpdatedEvent
    │                  └─→ TimingContextWindowEventHandler
    │                      ├─→ 调用 HistorySummarizationStrategy
    │                      └─→ 如果生成摘要：清空并替换 timingContextWindow
    └─→ 5. 保存 WorkingMemory 到 Redis
```

**关键代码**：
```java
public void commitSessionContext(SessionContext context) {
    // 1. 存储原始消息到 MongoDB
    originalStorage.append(context.getStorageKey(), currentPair);
    
    // 2. 更新 Head/Tail，获取从 Tail 移出的消息对
    MessagePair evictedPair = updateHeadAndTail(workingMemory, currentPair, sessionId);
    
    // 3. 执行 Pin 判定（同步）
    performPinJudgment(context, workingMemory, currentPair);
    
    // 4. 如果 evictedPair 存在，触发事件驱动压缩
    if (evictedPair != null) {
        PairEvictedFromTailEvent evictedEvent = 
            new PairEvictedFromTailEvent(context, workingMemory, evictedPair);
        eventPublisher.publishEvent(evictedEvent);
    }
    
    // 5. 保存 WorkingMemory 到 Redis
    workingMemoryStorage.save(workingMemory);
}
```

### 3. 事件驱动压缩机制

**事件流程**：

```
PairEvictedFromTailEvent
    ↓
PairEvictedEventHandler.handlePairEvictedFromTail()
    ├─→ 调用 CurrentRoundCompressionStrategy.compress()
    │   ├─→ 如果 token > threshold：生成摘要
    │   └─→ 否则：返回 notCompressed
    ├─→ 如果生成摘要：workingMemory.addToTimingContextWindow(summaryMsg)
    ├─→ 否则：workingMemory.addPairToTimingContextWindow(evictedPair)
    └─→ 发布 TimingContextWindowUpdatedEvent
        ↓
TimingContextWindowEventHandler.handleTimingContextWindowUpdated()
    ├─→ 调用 HistorySummarizationStrategy.compress()
    │   ├─→ 如果 windowSize > maxSize 或 tokens > threshold：生成摘要
    │   └─→ 否则：返回 notCompressed
    └─→ 如果生成摘要：
        ├─→ workingMemory.clearTimingContextWindow()
        └─→ workingMemory.addToTimingContextWindow(summaryMsg)
```

**设计优势**：
- **职责分离**：策略只负责生成摘要，EventHandler 负责存储
- **异步解耦**：压缩流程通过事件异步执行，不阻塞主流程
- **易于扩展**：可以轻松添加新的事件处理器和策略

---

## 压缩策略详解

### 核心概念：Message Pair（对话对）

一次完整对话轮次，包含：
- User message（用户消息）
- Assistant response（助手响应）
- 中间的工具调用消息（可选）

Pin、压缩、删除均以 **完整对话对** 为最小单位。

### Step 1: PinJudgmentStrategy（Pin 判定策略）

**目标**: 判断当前对话对是否包含确认事实（Pin），或是否否定历史 Pin

**执行时机**: 每次 `commitSessionContext` 时同步执行

**执行逻辑**:
1. 使用 LLM 判断当前对话对是否满足 Pin 条件
2. 如果是对历史 Pin 的更正或否定：
   - 定位被否定的历史 Pin（通过 `negatesPinId`）
   - 将旧 Pin 标记为 `INVALIDATED`
3. 如果值得 Pin：
   - 提取 Pin 内容（陈述句形式）
   - 创建 Pin 实体并添加到 `workingMemory.pinnedFacts`

**Pin 判定标准**（供 LLM 使用）:
- 是否包含明确、稳定、不易变化的事实或约束
- 是否对后续所有回答产生长期影响
- 是否是对既有结论的确认或否定

**Pin 数据结构**：
```java
Pin {
    pinId: String              // Pin 唯一标识
    conversationId: String    // 所属会话
    content: String           // Pin 内容（陈述句）
    confidence: double        // 置信度（0.0-1.0）
    status: PinStatus        // ACTIVE / INVALIDATED
    sourceMessageIds: List    // 来源消息 ID
}
```

**效果**: 重要信息被提取为 Pin，永不压缩，可在 System Prompt 中使用

### Step 2: CurrentRoundCompressionStrategy（当前轮次摘要策略）

**目标**: 对从 Tail 移出的消息对进行摘要（仅生成，不存储）

**执行时机**: 当 Tail 满时，通过 `PairEvictedFromTailEvent` 触发

**执行逻辑**:
1. 计算消息对的 token 数量
2. 如果 token > `currentRoundTokenThreshold`（默认 2000）：
   - 使用 LLM 生成陈述句型摘要
   - 返回包含摘要消息的 `CompressionResult`
3. 如果 token <= threshold：
   - 返回 `notCompressed`
   - 由 EventHandler 直接添加原文到 timingContextWindow

**摘要规则**:
- 使用陈述句
- 只保留：已确认的事实、明确的结论
- 删除：推理过程、修饰性语言、尝试性内容

**注意**: 
- 此策略只负责**生成摘要**，不负责存储
- 存储操作由 `PairEvictedEventHandler` 统一处理

### Step 3: HistorySummarizationStrategy（历史摘要策略）

**目标**: 对 timingContextWindow 中的消息进行摘要（仅生成，不存储）

**执行时机**: 当 timingContextWindow 更新时，通过 `TimingContextWindowUpdatedEvent` 触发

**执行逻辑**:
1. 检查 timingContextWindow 的大小和 token 数
2. 如果 `windowSize > maxSize`（默认 5）或 `tokens > threshold`：
   - 使用 LLM 生成历史摘要
   - 返回包含摘要消息的 `CompressionResult`
3. 如果没有超过限制：
   - 返回 `notCompressed`

**摘要要求**:
- 客观陈述
- 仅保留事实、决策、结论
- 不引入新推断

**注意**: 
- 此策略只负责**生成摘要**，不负责存储
- 清空和添加操作由 `TimingContextWindowEventHandler` 统一处理
- 不影响 Head/Tail/Pin

### 压缩策略执行流程

**事件驱动流程**：

```
commitSessionContext()
    ↓
1. Pin 判定（同步）
    ├─→ PinJudgmentStrategy.judgePin()
    ├─→ 如果需要失效旧 Pin：workingMemory.invalidatePin()
    └─→ 如果需要创建新 Pin：workingMemory.addPin()
    ↓
2. 如果 Tail 满，发布 PairEvictedFromTailEvent
    ↓
3. PairEvictedEventHandler（异步）
    ├─→ CurrentRoundCompressionStrategy.compress()
    │   ├─→ token > threshold：生成摘要
    │   └─→ 否则：返回 notCompressed
    ├─→ 如果生成摘要：addToTimingContextWindow(summaryMsg)
    ├─→ 否则：addPairToTimingContextWindow(evictedPair)
    └─→ 发布 TimingContextWindowUpdatedEvent
        ↓
4. TimingContextWindowEventHandler（异步）
    ├─→ HistorySummarizationStrategy.compress()
    │   ├─→ windowSize > maxSize：生成摘要
    │   └─→ 否则：返回 notCompressed
    └─→ 如果生成摘要：
        ├─→ clearTimingContextWindow()
        └─→ addToTimingContextWindow(summaryMsg)
```

**设计原则**：
- **职责分离**：策略只负责生成摘要，EventHandler 负责存储
- **事件驱动**：压缩流程通过事件异步执行，不阻塞主流程
- **保护机制**：Head/Tail/Pin 永不压缩，确保关键信息不丢失
- **Token 控制**：通过摘要机制控制 timingContextWindow 的 Token 增长

---

## 存储架构

### 1. WorkingMemoryStorage 接口

**实现类**: `RedisWorkingMemoryStorage`

**核心操作**:
```java
// 整体操作
WorkingMemory load(String conversationId);       // 加载完整工作记忆
void save(WorkingMemory workingMemory);         // 保存完整工作记忆
void clear(String conversationId);               // 清空工作记忆

// Head 操作
void setHead(String conversationId, MessagePair pair, int maxSize);
List<MessagePair> getHead(String conversationId);

// Tail 操作
void addToTail(String conversationId, MessagePair pair, int maxSize);
List<MessagePair> getTail(String conversationId);

// timingContextWindow 操作
void addToTimingContextWindow(String conversationId, Msg message);
List<Msg> getTimingContextWindow(String conversationId);
void clearTimingContextWindow(String conversationId);
void setTimingContextWindow(String conversationId, List<Msg> messages);

// Pin 操作
void addPin(String conversationId, Pin pin);
boolean invalidatePin(String conversationId, String pinId);
List<Pin> getActivePins(String conversationId);
List<Pin> getAllPins(String conversationId);
boolean deletePin(String conversationId, String pinId);
```

**Redis 数据结构**:
- **Key**: `artisan:working:{conversationId}`
- **Type**: Hash（存储 JSON 序列化的 WorkingMemory）
- **操作**: HSET, HGET, HDEL

**性能考虑**:
- 使用整体加载/保存模式，减少网络往返
- JSON 序列化/反序列化开销较小
- 支持原子性更新整个 WorkingMemory

### 2. OriginalStorage 接口

**实现类**: `MongoOriginalStorage`

**核心操作**:
```java
void append(String key, MessagePair pair);                    // 追加消息对
List<MessagePair> getAll(String key);                       // 获取所有消息对
void clear(String key);                                      // 清空
long count(String key);                                      // 计数
List<MessagePair> getRange(String key, int offset, int limit);  // 分页查询
```

**MongoDB 数据结构**:
```java
@Document(collection = "original_messages")
class OriginalMessageEntity {
    @Id String id;
    @Indexed String storageKey;       // 会话键
    @Indexed long timestamp;          // 时间戳
    MessagePair pair;                 // 消息对
}
```

**索引设计**:
- `storageKey`: 单字段索引，支持按会话查询
- `timestamp`: 单字段索引，支持时间排序

**设计考虑**:
- 存储完整的 MessagePair，保留所有消息（包括工具调用）
- 只增不改，保证历史完整性
- 支持分页查询，便于 RAG 检索

---

## 已发现的问题和优化建议

### 🔴 严重问题

#### 1. 并发安全问题

**问题位置**: `RedisWorkingStorage.delete()`

```java
public void delete(String key, int index) {
    List<Msg> messages = getAll(key);    // ← 读取
    if (index >= 0 && index < messages.size()) {
        messages.remove(index);
        setAll(key, messages);            // ← 写入
    }
}
```

**风险**:
- 高并发下可能出现 **读-改-写** 竞争条件
- 可能导致数据丢失或索引错误

**建议修复**:
```java
// 方案1: 使用 Redis 事务
public void delete(String key, int index) {
    redisTemplate.execute(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
            operations.multi();
            // ... 执行删除逻辑
            return operations.exec();
        }
    });
}

// 方案2: 使用分布式锁
public void delete(String key, int index) {
    String lockKey = "lock:" + key;
    RLock lock = redissonClient.getLock(lockKey);
    try {
        lock.lock();
        // ... 执行删除逻辑
    } finally {
        lock.unlock();
    }
}

// 方案3: 避免使用 delete 操作，采用标记删除
```

#### 2. MongoDB 分页查询逻辑错误 ✅ **已修复**

**问题位置**: `MongoOriginalStorage.getRange()`

**原始代码问题**:
```java
public List<Msg> getRange(String key, int offset, int limit) {
    PageRequest pageRequest = 
        PageRequest.of(offset / limit, limit, Sort.by(...));  // ← 错误
    // ...
}
```

**问题分析**:
- `PageRequest.of(page, size)` 中，page 从 0 开始
- 当 `offset=10, limit=5` 时：
  - 预期跳过前 10 条，返回 5 条
  - 实际: `page = 10/5 = 2`，跳过 10 条 ✓（凑巧正确）
- 当 `offset=15, limit=5` 时：
  - 预期跳过前 15 条，返回 5 条
  - 实际: `page = 15/5 = 3`，跳过 15 条 ✓（凑巧正确）
- 当 `offset=7, limit=5` 时：
  - 预期跳过前 7 条，返回 5 条
  - 实际: `page = 7/5 = 1`，跳过 5 条 ✗（**错误**！）

**✅ 已实现的修复方案**:
```java
@Override
public List<Msg> getRange(String key, int offset, int limit) {
    try {
        // 计算页号并处理 offset 不是 limit 整数倍的情况
        int pageNumber = offset / limit;
        int skipInPage = offset % limit;
        
        PageRequest pageRequest =
                PageRequest.of(pageNumber, limit, Sort.by(Sort.Direction.ASC, "timestamp"));
        List<OriginalMessageEntity> entities = repository.findByStorageKey(key, pageRequest);
        
        // 如果 offset 不是 limit 的整数倍，跳过页内的额外项
        // 并在必要时从下一页获取更多数据以确保返回恰好 limit 条记录
        if (skipInPage > 0 && !entities.isEmpty()) {
            List<Msg> allMessages = new ArrayList<>();
            allMessages.addAll(entities.stream()
                    .skip(skipInPage)
                    .map(OriginalMessageEntity::getMessage)
                    .collect(Collectors.toList()));
            
            // 如果没有足够的消息，从下一页获取
            if (allMessages.size() < limit) {
                int remainingNeeded = limit - allMessages.size();
                PageRequest nextPageRequest =
                        PageRequest.of(pageNumber + 1, limit, 
                                      Sort.by(Sort.Direction.ASC, "timestamp"));
                List<OriginalMessageEntity> nextPageEntities = 
                        repository.findByStorageKey(key, nextPageRequest);
                
                allMessages.addAll(nextPageEntities.stream()
                        .limit(remainingNeeded)
                        .map(OriginalMessageEntity::getMessage)
                        .collect(Collectors.toList()));
            }
            
            return allMessages;
        }
        
        return entities.stream()
                .map(OriginalMessageEntity::getMessage)
                .collect(Collectors.toList());
    } catch (Exception e) {
        log.error("Failed to get range from original storage: key={}, offset={}, limit={}", 
                key, offset, limit, e);
        return new ArrayList<>();
    }
}
```

**修复效果验证**:
| 场景 | offset | limit | 旧实现 | 新实现 ✅ |
|-----|--------|-------|--------|----------|
| 整数倍 | 10 | 5 | 跳过 10 条 ✓ | 跳过 10 条 ✓ |
| 非整数倍 | 7 | 5 | 跳过 5 条 ✗ | 跳过 7 条 ✓ |
| 非整数倍 | 13 | 5 | 跳过 10 条 ✗ | 跳过 13 条 ✓ |
| 跨页 | 8 | 5 | 跳过 5 条 ✗ | 正确跨页获取 ✓ |

#### 3. cleanupInactiveSessions() 的线程安全问题

**问题位置**: `ArtisanMemory.cleanupInactiveSessions()`

```java
private void cleanupInactiveSessions() {
    contexts.entrySet().removeIf(entry -> {
        MemoryContext ctx = entry.getValue();
        if (ctx.isInactive(maxInactiveMillis)) {
            removeSession(ctx.getSessionId());  // ← 可能导致并发问题
            return true;
        }
        return false;
    });
}
```

**问题分析**:
- `removeSession()` 会调用 `contexts.remove(sessionId)`
- 在 `removeIf` 迭代过程中修改 Map，虽然 `ConcurrentHashMap` 支持并发修改，但 `removeSession()` 还会清理存储
- 如果同时有其他线程访问该 session，可能导致数据不一致

**建议修复**:
```java
private void cleanupInactiveSessions() {
    List<String> inactiveSessions = new ArrayList<>();
    
    // 第一步：收集不活跃的会话
    contexts.forEach((sessionId, ctx) -> {
        if (ctx.isInactive(maxInactiveMillis)) {
            inactiveSessions.add(sessionId);
        }
    });
    
    // 第二步：逐个清理
    for (String sessionId : inactiveSessions) {
        try {
            removeSession(sessionId);
        } catch (Exception e) {
            log.error("Failed to cleanup session: {}", sessionId, e);
        }
    }
}
```

### 🟡 中等问题

#### 4. Token 计算不准确

**问题位置**: `TokenCounterUtil.calculateToken()`

```java
private static final double CHARS_PER_TOKEN = 2.5;  // 简化估算
```

**问题**:
- 使用固定的字符/Token 比率
- 对于不同语言（中文、英文、代码）差异较大
- 可能导致压缩触发时机不准确

**建议优化**:
```java
// 方案1: 使用更精确的 Token 计数库
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

public class TokenCounterUtil {
    private static final Encoding encoding = 
        Encodings.newDefaultEncodingRegistry()
            .getEncoding(EncodingType.CL100K_BASE);
    
    public static int calculateToken(String text) {
        return encoding.encode(text).size();
    }
}

// 方案2: 区分语言类型
private static int estimateTextTokens(String text) {
    if (text == null || text.isEmpty()) return 0;
    
    // 统计中文字符
    long chineseChars = text.chars()
        .filter(c -> Character.UnicodeScript.of(c) == 
                     Character.UnicodeScript.HAN)
        .count();
    
    // 中文: 1 字符 ≈ 1.5 token
    // 英文: 1 字符 ≈ 0.25 token
    int chineseTokens = (int)(chineseChars * 1.5);
    int englishTokens = (int)((text.length() - chineseChars) * 0.25);
    
    return chineseTokens + englishTokens;
}
```

#### 5. LLM 调用缺少错误处理和重试机制

**问题位置**: `LLMSummarizationStrategy.generateSummary()`

```java
String response = chatClient.prompt()
    .messages(springAiMessages)
    .call()
    .content();  // ← 没有异常处理和重试
```

**风险**:
- LLM API 调用失败会导致压缩失败
- 网络波动、限流等问题未处理

**建议优化**:
```java
@Retryable(
    value = {RestClientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
private String callLLMWithRetry(List<Message> messages) {
    try {
        return chatClient.prompt()
            .messages(messages)
            .call()
            .content();
    } catch (Exception e) {
        log.error("LLM call failed", e);
        throw e;
    }
}

// 或使用 Resilience4j
@CircuitBreaker(name = "llm", fallbackMethod = "llmFallback")
@RateLimiter(name = "llm")
@Retry(name = "llm")
private String callLLM(List<Message> messages) {
    return chatClient.prompt()
        .messages(messages)
        .call()
        .content();
}

private String llmFallback(List<Message> messages, Exception e) {
    log.warn("LLM call failed, using fallback", e);
    // 返回简单的文本拼接
    return messages.stream()
        .map(m -> m.getContent())
        .collect(Collectors.joining("\n"));
}
```

#### 6. 存储客户端重复创建

**问题位置**: `ArtisanMemory` 中多处

```java
WorkingStorage workingStorage = clientManager.createWorkingStorage();
OriginalStorage originalStorage = clientManager.createOriginalStorage();
```

**问题**:
- 每次调用都创建新实例
- 虽然底层可能复用连接，但对象创建开销仍存在

**建议优化**:
```java
@Service
public class ArtisanMemory {
    private final WorkingStorage workingStorage;    // ← 注入单例
    private final OriginalStorage originalStorage;
    private final OffloadStorage offloadStorage;
    
    public ArtisanMemory(
            ArtisanMemoryProperties properties,
            WorkingStorage workingStorage,
            OriginalStorage originalStorage,
            OffloadStorage offloadStorage,
            Optional<ChatClient> chatClient,
            PromptConfig promptConfig) {
        // ...
        this.workingStorage = workingStorage;
        this.originalStorage = originalStorage;
        this.offloadStorage = offloadStorage;
    }
}
```

### 🟢 优化建议

#### 7. 添加监控和指标

**建议**:
```java
@Service
public class ArtisanMemory {
    private final MeterRegistry meterRegistry;
    
    // 添加监控指标
    private void recordMetrics(String sessionId, CompressionResult result) {
        meterRegistry.counter("memory.compression.count",
            "strategy", result.getStrategyName(),
            "success", String.valueOf(result.isCompressed())
        ).increment();
        
        meterRegistry.gauge("memory.working.size", 
            Tags.of("session", sessionId),
            getWorkingMemorySize(sessionId));
        
        meterRegistry.timer("memory.compression.duration")
            .record(() -> performCompression(...));
    }
}
```

#### 8. 配置参数优化

**当前问题**:
- 很多关键参数硬编码或使用默认值
- 缺少针对不同场景的预设配置

**建议**:
```yaml
artisan:
  memory:
    profiles:
      # 短对话场景
      short-conversation:
        max-token: 16000
        msg-threshold: 50
        last-keep: 20
        enable-llm-compression: false
        
      # 长对话场景
      long-conversation:
        max-token: 131072
        msg-threshold: 100
        last-keep: 50
        enable-llm-compression: true
        
      # 工具密集型场景
      tool-intensive:
        max-token: 65536
        msg-threshold: 80
        min-consecutive-tool-messages: 3  # 更激进地压缩工具调用
        enable-llm-compression: true
```

#### 9. 增加批量操作支持

**建议**:
```java
public interface WorkingStorage {
    // 批量添加（减少网络往返）
    void addBatch(String key, List<Msg> messages);
    
    // 批量会话操作
    Map<String, List<Msg>> getAllBatch(List<String> keys);
}
```

#### 10. 添加压缩预览功能

**建议**:
```java
public class ArtisanMemory {
    /**
     * 预览压缩效果（不实际执行）
     */
    public CompressionPreview previewCompression(String sessionId) {
        List<Msg> messages = getMessages(sessionId);
        
        CompressionPreview preview = new CompressionPreview();
        preview.setOriginalCount(messages.size());
        preview.setOriginalTokens(TokenCounterUtil.calculateToken(messages));
        
        for (CompressionStrategy strategy : compressionStrategies) {
            CompressionResult result = strategy.compress(storageKey, messages);
            if (result.isCompressed()) {
                preview.addStrategyResult(strategy.getName(), result);
                messages = result.getCompressedMessages();
            }
        }
        
        return preview;
    }
}
```

---

## 快速开始

### 1. 环境要求

- Java 17+
- Redis 6.0+
- MongoDB 4.4+
- Maven 3.6+

### 2. 依赖配置

```xml
<dependency>
    <groupId>com.iflytek</groupId>
    <artifactId>artisan-memory</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3. 配置文件

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
    mongodb:
      uri: mongodb://localhost:27017
      database: artisan_memory

artisan:
  memory:
    storage:
      key-prefix: "session:"
    working-memory:
      head-size: 1                                    # Head 区域最大轮数
      tail-size: 2                                    # Tail 区域最大轮数
      timing-context-window-max-size: 5              # timingContextWindow 最大消息数
      timing-context-window-token-threshold: 5000    # timingContextWindow Token 阈值
      current-round-token-threshold: 2000            # 当前轮次摘要触发阈值（token数）
    session:
      cleanup-interval-minutes: 10
      max-inactive-minutes: 60
```

### 4. 使用示例

```java
@Service
public class ChatService {
    @Autowired
    private ArtisanMemory memoryService;
    
    @Autowired
    private ChatClient chatClient;
    
    public String chat(String userId, String message) {
        String sessionId = "user:" + userId;
        
        // 1. 创建用户消息
        Msg userMsg = Msg.builder()
            .role(MsgRole.USER)
            .content(TextBlock.of(message))
            .build();
        
        // 2. 获取会话上下文
        SessionContext context = memoryService.getSessionContext(sessionId);
        
        // 3. 添加用户消息到当前对话对
        context.setUserMessage(userMsg);
        
        // 4. 获取历史上下文（用于 LLM）
        List<Msg> historyMessages = memoryService.getMemoryMessages(context);
        historyMessages.add(userMsg);
        
        // 5. 调用 LLM
        String response = chatClient.prompt()
            .messages(convertMessages(historyMessages))
            .call()
            .content();
        
        // 6. 添加助手响应到当前对话对
        Msg assistantMsg = Msg.builder()
            .role(MsgRole.ASSISTANT)
            .content(TextBlock.of(response))
            .build();
        context.setAssistantMessage(assistantMsg);
        
        // 7. 提交会话上下文（触发压缩流程）
        memoryService.commitSessionContext(context);
        
        return response;
    }
}
```

### 5. REST API 示例

```bash
# 发送消息
curl -X POST http://localhost:8080/api/demo/chat \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user123" \
  -d '{"message": "你好，请介绍一下 Java Stream API"}'

# 查看会话统计
curl http://localhost:8080/api/demo/chat/stats?userId=user123

# 查看交互历史
curl http://localhost:8080/api/demo/chat/interactions?userId=user123

# 清空会话
curl -X POST http://localhost:8080/api/demo/chat/clear?userId=user123
```

---

## 总结

Artisan Memory 是一个设计精良的上下文记忆管理系统，在架构设计和功能实现上都体现了很高的专业水准。主要优势包括：

### ✅ 优势

1. **清晰的分层架构**: 存储层、策略层、服务层、事件层职责明确
2. **分区存储设计**: Head/Tail/timingContextWindow/pinnedFacts 四分区结构，保护关键信息
3. **事件驱动压缩**: 异步执行，不阻塞主流程，易于扩展
4. **Pin 机制**: 提取确认事实，永不压缩，可在 System Prompt 中使用
5. **双层存储设计**: WorkingMemory（Redis）和 OriginalStorage（MongoDB）分离，平衡性能和完整性
6. **职责分离**: 策略只负责生成摘要，EventHandler 负责存储，职责清晰
7. **Spring Boot 集成**: 自动配置，开箱即用

### ⚠️ 需要改进

1. **错误处理**: LLM 调用等关键路径缺少重试和降级机制
2. **监控指标**: 缺少运行时可观测性（压缩次数、Token 节省等）
3. **Token 计算**: 精度不足，影响压缩触发时机
4. **并发安全**: 事件处理器的并发安全性需要进一步验证

通过解决上述问题，该系统可以更加稳定和高效地应用于生产环境。

## License

Apache License 2.0
