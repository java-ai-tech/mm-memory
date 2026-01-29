<div align="center">

# Artisan Memory

### 智能上下文记忆管理系统

为 RAG 应用设计的混合存储记忆管理解决方案

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

</div>

---

## 1. 项目简介

**mm Memory** 是一个专为 **RAG（检索增强生成）** 应用设计的智能上下文记忆管理系统,通过混合存储架构和渐进式压缩策略,解决了长对话场景下的 Token 管理和上下文窗口限制问题。

### 核心特性

- **智能压缩**: 基于 LLM 的自动摘要生成,有效控制 Token 增长
- **分区存储**: Head/Tail/TCW/Pin 四层结构,保护关键信息不丢失
- **高性能**: Redis + MongoDB 混合存储,平衡速度与完整性
- **异步队列**: Redis 队列 + 消费者模式,不阻塞主对话
- **开箱即用**: Spring Boot 自动配置,快速集成

### 适用场景

- 长对话场景下的上下文管理
- 需要 Token 控制的 RAG 应用
- 需要保留完整历史记录的对话系统
- 多轮对话的智能压缩与摘要

---

## 2. 混合存储记忆设计

### 2.1 存储架构

采用 **Redis + MongoDB** 双层存储架构:

| 存储层 | 介质 | 内容 | 生命周期 | 用途 |
|--------|------|------|----------|------|
| **Working Memory** | Redis | 分区结构(Head/Tail/TCW/Pin) | 7天(可配置) | 提交给 LLM |
| **Original Memory** | MongoDB | 完整原始对话记录 | 永久保存 | 审计和 RAG 检索 |

### 2.2 四层分区结构

```
WorkingMemory (Redis)
├── Head (第 1 轮)              → 永不压缩,保留初始上下文
├── Tail (最新 2 轮)             → 永不压缩,确保连贯响应
├── timingContextWindow (TCW)   → 可压缩区域,最大 5 条消息
└── pinnedFacts (Pin)           → 永不压缩,确认事实列表
```

**设计理念**:
- **Head**: 保留第一轮对话,确保初始上下文不丢失
- **Tail**: 保留最新 2 轮对话,保证回复的连贯性
- **TCW**: 唯一可压缩区域,存储压缩后的历史摘要
- **Pin**: 提取的关键事实(如用户姓名、偏好),永不压缩

### 2.3 异步队列架构

```
用户提交对话 → Pin 判定(同步)
               ↓
         压缩任务 → Redis 队列
               ↓
         消费者线程 → 异步处理
               ↓
         当前轮次摘要 → TCW 更新
               ↓
         历史摘要压缩 → 控制 Token
```

**优势**:
- 不阻塞主对话流程
- 按 sessionId 隔离任务队列
- 顺序处理同一会话的任务
- 空闲 60 秒自动释放消费者

---

## 3. 压缩策略

采用 **四层渐进式压缩机制**,自动控制 Token 增长:

### 3.1 Pin 判定策略

**目标**: 提取确认事实,永不压缩

**执行时机**: 每次提交对话时**同步执行**

**判定标准**:
- 包含明确、稳定、不易变化的事实
- 对后续所有回答产生长期影响
- 对既有结论的确认或否定

**示例**:
```
用户: "我叫张三,在北京工作"
↓ 提取 Pin
Pin: "用户姓名是张三,工作地点是北京"
```

### 3.2 当前轮次摘要策略

**目标**: 对从 Tail 移出的对话生成摘要

**执行时机**: 当 Tail 满时(超过 2 轮),通过队列**异步触发**

**摘要规则**:
- 使用陈述句
- 保留已确认的事实、明确的结论
- 删除推理过程、修饰性语言

**示例**:
```
原始对话(2000 tokens):
用户: 讲讲 Redis 的数据结构
助手: Redis 有 5 种基本数据结构: String、Hash、List、Set、ZSet...
(详细展开,2000 tokens)

↓ 压缩

摘要(200 tokens):
"用户询问了 Redis 的数据结构,助手介绍了 Redis 的 5 种基本数据结构及其特点。"
```

### 3.3 历史摘要压缩策略

**目标**: 对 timingContextWindow 中的摘要再次压缩

**执行时机**: 当 timingContextWindow 超过 5 条消息或 3000 tokens 时

**压缩效果**:

| 轮次 | timingContextWindow | Token 数 |
|------|---------------------|----------|
| 第 10 轮后 | 3 个当前轮次摘要 + 2 个原始对话 | ~3000 |
| 第 20 轮后 | 5 个当前轮次摘要 | ~1000 |
| 第 30 轮后 | 2 个历史摘要 + 3 个当前摘要 | ~800 |

### 3.4 Pin 聚合策略

**目标**: 当 Pin 数量或 token 数过多时,聚合 Pin 摘要

**执行时机**: 当 Pin 数量 > 10 或总 token > 300

**示例**:
```
原始 Pins(10条,450 tokens):
- "用户姓名是张三"
- "用户在北京工作"
- "用户是软件工程师"
- ...
- "用户喜欢打篮球"

↓ 聚合

聚合 Pin(1条,150 tokens):
"用户张三是一名在北京工作的软件工程师,平时喜欢打篮球..."
```

### 3.5 压缩策略对比

| 策略 | 触发条件 | 执行方式 | 处理对象 | 存储位置 |
|------|----------|----------|----------|----------|
| **Pin 判定** | 每次提交对话 | 同步 | 当前对话对 | pinnedFacts |
| **当前轮次摘要** | Tail 满 | 队列异步 | evictedPair | timingContextWindow |
| **历史摘要压缩** | TCW 超阈值 | 队列异步 | timingContextWindow | timingContextWindow |
| **Pin 聚合** | Pin > 10 或 Token > 300 | 队列异步 | pinnedFacts | pinnedFacts |

---

## 4. 快速开始

### 4.1 基于本项目快速启动测试

#### 环境要求

- **Java**: 17+
- **Redis**: 6.0+
- **MongoDB**: 4.4+
- **Maven**: 3.6+

#### 启动步骤

```bash
# 1. 克隆项目
git clone <repository-url>
cd mm-memory

# 2. 启动依赖服务(Redis 和 MongoDB)
docker-compose up -d

# 3. 编译并运行
mvn clean install
mvn spring-boot:run

# 4. 测试接口
curl -X POST http://localhost:8080/api/demo/chat \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user123" \
  -d '{"message": "你好,请介绍一下 Java Stream API"}'
```

#### 运行测试脚本

```bash
cd test

# 10 轮对话测试
./dialog_10.sh

# 50 轮对话测试
./dialog_50.sh

# 100 轮对话测试
./dialog_100.sh

# 语义评估测试
./evaluate_semantic.sh --all
```

### 4.2 在其他项目中使用

#### 添加依赖

```xml
<dependency>
    <groupId>com.glmapper</groupId>
    <artifactId>mm-memory</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 配置文件

**application.yml**:

```yaml
spring:
  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 120000ms  # 支持阻塞命令,至少 60 秒以上
      lettuce:
        pool:
          max-active: 8
          max-idle: 8

  # MongoDB 配置
  mongodb:
    uri: mongodb://localhost:27017
    database: mm_memory

# 记忆系统配置
artisan:
  memory:
    # 存储配置
    storage:
      key-prefix: "session:"

    # 工作记忆配置
    working-memory:
      head-size: 1                           # Head 区域最大轮数
      tail-size: 2                           # Tail 区域最大轮数
      expire-days: 7                         # WorkingMemory 过期时间(天)
      max-pin-count: 10                      # Pin 最大数量
      max-pin-tokens: 300                    # Pin 总 token 最大值
      timing-context-window-max-size: 5      # timingContextWindow 最大消息数
      timing-context-window-token-threshold: 3000  # TCW Token 阈值
      current-round-token-threshold: 1000    # 当前轮次摘要阈值

    # 会话管理配置
    session:
      cleanup-interval-minutes: 10
      max-inactive-minutes: 60

    # 压缩策略配置
    compression:
      auto-compression: true
      strategies:
        - CURRENT_ROUND_SUMMARIZATION   # 当前轮次压缩
        - HISTORY_SUMMARIZATION         # 历史摘要压缩
        - PIN_AGGREGATION               # Pin 聚合压缩
        - PIN_JUDGMENT                  # Pin 判断策略
```

#### 使用示例

```java
@Service
public class ChatService {

    @Autowired
    private SessionMemory sessionMemory;

    @Autowired
    private ChatClient chatClient;

    public String chat(String userId, String message) {
        String sessionId = "user:" + userId;

        // 1. 获取会话上下文
        SessionContext context = sessionMemory.getSessionContext(sessionId);

        // 2. 构建用户消息
        Msg userMsg = Msg.builder()
            .role(MsgRole.USER)
            .content(TextBlock.of(message))
            .build();
        context.appendMessage(userMsg);

        // 3. 获取记忆上下文(已压缩,适合提交给 LLM)
        List<Msg> memoryMessages = sessionMemory.getMemoryMessages(context);

        // 4. 获取确认事实(用于 System Prompt)
        List<Pin> pinnedFacts = sessionMemory.getPinnedFacts(context);

        // 5. 构建完整的 LLM 输入
        List<Msg> fullContext = new ArrayList<>();
        fullContext.addAll(memoryMessages);
        fullContext.add(userMsg);

        // 6. 调用 LLM
        String response = chatClient.prompt()
            .messages(convertToSpringAiMessages(fullContext))
            .call()
            .content();

        // 7. 添加助手响应
        Msg assistantMsg = Msg.builder()
            .role(MsgRole.ASSISTANT)
            .content(TextBlock.of(response))
            .build();
        context.appendMessage(assistantMsg);

        // 8. 提交会话上下文(触发压缩流程)
        sessionMemory.commitSessionContext(context);

        return response;
    }
}
```

---

## 配置参数说明

### Working Memory 配置

| 参数 | 说明 | 默认值 | 推荐值 |
|------|------|--------|--------|
| `head-size` | Head 区域最大轮数 | 1 | 1 |
| `tail-size` | Tail 区域最大轮数 | 2 | 2-3 |
| `expire-days` | WorkingMemory 过期天数 | 7 | 7-30 |
| `max-pin-count` | Pin 最大数量 | 10 | 10-20 |
| `max-pin-tokens` | Pin 总 token 最大值 | 300 | 300-500 |
| `timing-context-window-max-size` | TCW 最大消息数 | 5 | 5-10 |
| `timing-context-window-token-threshold` | TCW Token 阈值 | 3000 | 3000-5000 |
| `current-round-token-threshold` | 当前轮次摘要阈值 | 1000 | 1000-2000 |

### 压缩策略配置

| 策略 | 说明 | 推荐场景 |
|------|------|----------|
| `PIN_JUDGMENT` | Pin 判定策略 | 所有场景 |
| `CURRENT_ROUND_SUMMARIZATION` | 当前轮次摘要 | 所有场景 |
| `HISTORY_SUMMARIZATION` | 历史摘要压缩 | 长对话 |
| `PIN_AGGREGATION` | Pin 聚合压缩 | Pin 数量多时 |

---

## 常见问题

### 1. Redis 超时问题

**错误**: `RedisCommandTimeoutException: Command timed out after 5 second(s)`

**原因**: 使用了阻塞命令 `bRPop`,但 Redis 命令超时设置太短

**解决**:
```yaml
spring:
  data:
    redis:
      timeout: 120000ms  # 至少 60 秒以上,建议 120 秒
```

### 2. 压缩触发太频繁

**原因**: Token 阈值设置过低

**解决**: 提高 `current-round-token-threshold` 和 `timing-context-window-token-threshold`

### 3. Pin 提取不准确

**原因**: Prompt 模板不合适或 LLM 能力限制

**解决**: 调整 `src/main/resources/prompts/PIN_JUDGMENT_PROMPT.md`

---

## 相关文档

- [语义评估指南](test/EVALUATION.md)
- [测试脚本使用指南](test/README.md)
- [压缩策略 Prompt 说明](src/main/resources/prompts/)

---

## 许可证

Apache License 2.0

---

<div align="center">

Made with ❤️ by [glmapper](https://github.com/glmapper)

</div>
