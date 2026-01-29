#!/bin/bash
BASE_URL="http://localhost:8080/api/demo/chat"
USER_ID=test_20260127_glmapper_100

echo "[Test Start] 100 rounds dialog test - Extreme Memory Compression & Multi-context Management"

# ==================== Phase 1: Identity Foundation (Rounds 1-15) ====================
echo ""
echo "=== Phase 1: Identity Foundation & Core Information ==="

echo "[Round 1] - Complete Introduction"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你好，让我完整介绍一下自己。我叫王伟，35岁，是一名资深系统架构师，目前在上海工作。我已婚，有两个孩子，分别8岁和5岁。"}' | jq .
sleep 1

echo "[Round 2] - Education Background"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我本科毕业于浙江大学计算机专业，后来在上海交大读了软件工程的硕士"}' | jq .
sleep 1

echo "[Round 3] - Career Timeline"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"毕业后我先后在华为工作了3年，腾讯工作了5年，现在在一家金融科技公司担任首席架构师"}' | jq .
sleep 1

echo "[Round 4] - Core Expertise"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的核心专长包括：分布式系统设计、高并发架构、微服务治理、云原生技术栈"}' | jq .
sleep 1

echo "[Round 5] - Technology Stack Details"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"编程语言精通：Java、Go、Python、C++。熟悉JavaScript和TypeScript"}' | jq .
sleep 1

echo "[Round 6] - Framework & Tools"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"框架方面：Spring全家桶、gRPC、Dubbo、Kafka、Redis集群、Kubernetes、Istio"}' | jq .
sleep 1

echo "[Round 7] - Current Responsibilities"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"目前负责公司的核心交易系统，日均处理交易量500万笔，系统可用性要求99.99%"}' | jq .
sleep 1

echo "[Round 8] - Team Structure"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我带领一个20人的技术团队，包括后端开发、DevOps、SRE三个小组"}' | jq .
sleep 1

echo "[Round 9] - Personal Goals"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的职业目标是在3年内成为公司CTO，5年内创立自己的技术服务公司"}' | jq .
sleep 1

echo "[Round 10] - Learning Focus"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"最近在深入学习AI/ML技术，特别是LLM在金融领域的应用"}' | jq .
sleep 1

echo "[Round 11] - Immediate Test 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我叫什么名字？在哪个城市工作？"}' | jq .
sleep 1

echo "[Round 12] - Immediate Test 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的教育背景是什么？"}' | jq .
sleep 1

echo "[Round 13] - Immediate Test 3"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的职业经历是怎样的？"}' | jq .
sleep 1

echo "[Round 14] - Immediate Test 4"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我现在负责什么系统？有什么技术指标要求？"}' | jq .
sleep 1

echo "[Round 15] - Immediate Test 5"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的职业目标是什么？"}' | jq .
sleep 1

# ==================== Phase 2: Deep Technical Discussions (Rounds 16-35) ====================
echo ""
echo "=== Phase 2: Deep Technical Architecture Discussions ==="

echo "[Round 16] - Distributed Systems Topic"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"让我们深入讨论分布式事务。Saga、TCC、本地消息表，这些方案的优缺点？"}' | jq .
sleep 1

echo "[Round 17] - Distributed Transactions Follow-up"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"在金融场景下，如何保证资金交易的最终一致性？"}' | jq .
sleep 1

echo "[Round 18] - Consensus Algorithms"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Raft和Paxos有什么区别？实际项目中如何选择？"}' | jq .
sleep 1

echo "[Round 19] - High Availability Design"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"多活架构的关键设计要点是什么？如何处理跨数据中心的数据同步？"}' | jq .
sleep 1

echo "[Round 20] - Rate Limiting"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"限流算法：令牌桶、漏桶、固定窗口、滑动窗口，各有什么特点？"}' | jq .
sleep 1

echo "[Round 21] - Circuit Breaker"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"熔断降级策略如何设计？Sentinel和Hystrix的对比？"}' | jq .
sleep 1

echo "[Round 22] - Database Sharding"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"数据库分库分表的策略有哪些？如何处理分片键的选择？"}' | jq .
sleep 1

echo "[Round 23] - Distributed Cache"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Redis Cluster的架构原理是什么？如何解决缓存一致性问题？"}' | jq .
sleep 1

echo "[Round 24] - Message Queue Design"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何保证消息不丢失？如何保证消息不重复消费？"}' | jq .
sleep 1

echo "[Round 25] - Event Sourcing"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"事件溯源架构的优缺点？CQRS模式如何应用？"}' | jq .
sleep 1

echo "[Round 26] - Microservices Patterns"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"微服务拆分的原则是什么？如何避免分布式单体架构？"}' | jq .
sleep 1

echo "[Round 27] - Service Mesh"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Istio的架构设计原理？Sidecar模式的优势和挑战？"}' | jq .
sleep 1

echo "[Round 28] - Observability"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"可观测性三大支柱：Metrics、Logging、Tracing如何落地？"}' | jq .
sleep 1

echo "[Round 29] - Performance Optimization"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"系统性能优化的方法论？从哪些维度入手？"}' | jq .
sleep 1

echo "[Round 30] - Capacity Planning"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何做容量规划？如何预估系统瓶颈？"}' | jq .
sleep 1

echo "[Round 31] - Cost Optimization"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"云原生架构的成本优化策略？如何平衡性能和成本？"}' | jq .
sleep 1

echo "[Round 32] - Security Design"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"金融级安全架构设计要点？如何防范DDoS、SQL注入、XSS？"}' | jq .
sleep 1

echo "[Round 33] - Identity & Authentication"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"OAuth2.0和OpenID Connect的流程？JWT的优缺点？"}' | jq .
sleep 1

echo "[Round 34] - Data Governance"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"数据治理最佳实践？如何保证数据质量？"}' | jq .
sleep 1

echo "[Round 35] - Long Distance Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"回顾一下，我的核心专长是什么？主要负责什么系统？"}' | jq .
sleep 1

# ==================== Phase 3: AI/ML Deep Dive (Rounds 36-55) ====================
echo ""
echo "=== Phase 3: AI/ML & LLM Technology Deep Dive ==="

echo "[Round 36] - LLM Fundamentals"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"讲讲大语言模型的训练流程？预训练、SFT、RLHF的作用？"}' | jq .
sleep 1

echo "[Round 37] - Transformer Architecture"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Transformer的多头注意力机制原理？Positional Encoding的作用？"}' | jq .
sleep 1

echo "[Round 38] - Model Architecture"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Encoder-only、Decoder-only、Encoder-Decoder架构的区别和适用场景？"}' | jq .
sleep 1

echo "[Round 39] - Prompt Engineering"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Prompt工程的最佳实践？Few-shot、Chain-of-Thought的原理？"}' | jq .
sleep 1

echo "[Round 40] - Fine-tuning Strategies"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"LoRA和QLoRA的原理？全量微调和参数高效微调如何选择？"}' | jq .
sleep 1

echo "[Round 41] - RAG Architecture"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"RAG系统的架构设计？向量检索和重排序策略？"}' | jq .
sleep 1

echo "[Round 42] - Vector Database"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"向量索引算法：HNSW、IVF、PQ的区别？如何选择？"}' | jq .
sleep 1

echo "[Round 43] - Context Window Management"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"长对话的上下文管理策略？Sliding Window、摘要压缩、向量检索的优劣？"}' | jq .
sleep 1

echo "[Round 44] - Agent Systems"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"AI Agent的核心组件？Tool Use、Function Calling的设计模式？"}' | jq .
sleep 1

echo "[Round 45] - Multi-Agent Systems"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"多智能体协作模式？AutoGPT、BabyAGI的设计思路？"}' | jq .
sleep 1

echo "[Round 46] - Memory Systems"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"AI记忆系统的设计？短期记忆、长期记忆、向量检索如何结合？"}' | jq .
sleep 1

echo "[Round 47] - Knowledge Graph"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"知识图谱在LLM应用中的作用？GraphRAG的原理？"}' | jq .
sleep 1

echo "[Round 48] - Model Deployment"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"LLM推理优化技术？Quantization、Pruning、Distillation？"}' | jq .
sleep 1

echo "[Round 49] - Serving Architecture"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"vLLM、TGI、TensorRT-LLM的对比？如何选型？"}' | jq .
sleep 1

echo "[Round 50] - Cross-domain Integration"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"结合我的金融背景，谈谈LLM在金融领域的应用场景"}' | jq .
sleep 1

echo "[Round 51] - Risk Management"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"LLM的幻觉问题如何缓解？在金融场景如何保证准确性？"}' | jq .
sleep 1

echo "[Round 52] - Compliance & Governance"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"AI模型的可解释性、公平性、隐私保护如何保证？"}' | jq .
sleep 1

echo "[Round 53] - MLOps"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"LLM Ops的最佳实践？模型版本管理、A/B测试、监控告警？"}' | jq .
sleep 1

echo "[Round 54] - Cost Management"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"LLM应用的成本优化策略？Token计算、缓存策略、模型路由？"}' | jq .
sleep 1

echo "[Round 55] - Future Trends"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你对LLM未来发展的看法？多模态、推理能力、智能体方向？"}' | jq .
sleep 1

# ==================== Phase 4: Context Switch & Integration (Rounds 56-75) ====================
echo ""
echo "=== Phase 4: Context Switch & Domain Integration ==="

echo "[Round 56] - Recall Identity"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"回到最开始，完整介绍我自己（姓名、年龄、教育、工作经历）"}' | jq .
sleep 1

echo "[Round 57] - Recall Current Role"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我现在的工作职责是什么？负责什么系统？团队规模多大？"}' | jq .
sleep 1

echo "[Round 58] - Recall Goals"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的职业目标和学习重点是什么？"}' | jq .
sleep 1

echo "[Round 59] - New Topic: Leadership"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"除了技术，我也很关注技术管理和领导力。聊聊技术团队的成长路径？"}' | jq .
sleep 1

echo "[Round 60] - Team Management"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何培养技术骨干？如何做技术绩效考核？"}' | jq .
sleep 1

echo "[Round 61] - Tech Culture"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何建设优秀的技术文化？代码审查、技术分享、创新机制？"}' | jq .
sleep 1

echo "[Round 62] - Recruitment"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"技术人才的招聘标准和面试方法？如何识别优秀工程师？"}' | jq .
sleep 1

echo "[Round 63] - Career Development"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"IC和管理路线如何选择？技术专家的成长路径？"}' | jq .
sleep 1

echo "[Round 64] - Cross-functional Collaboration"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"技术部门如何与产品、运营、市场部门有效协作？"}' | jq .
sleep 1

echo "[Round 65] - Stakeholder Management"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何向上管理？如何向非技术背景的高管解释技术方案？"}' | jq .
sleep 1

echo "[Round 66] - Product Thinking"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"技术人如何培养产品思维？如何平衡技术完美和业务价值？"}' | jq .
sleep 1

echo "[Round 67] - Personal Branding"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"技术专家如何建立个人品牌？技术博客、开源贡献、演讲分享？"}' | jq .
sleep 1

echo "[Round 68] - Work-Life Balance"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何在高强度工作中保持工作生活平衡？我的兴趣是跑步和阅读"}' | jq .
sleep 1

echo "[Round 69] - Continuous Learning"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何持续学习？我的学习方法和知识管理习惯？"}' | jq .
sleep 1

echo "[Round 70] - Networking"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"技术人脉如何积累？参加技术会议、加入技术社区？"}' | jq .
sleep 1

echo "[Round 71] - Mentorship"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"作为资深架构师，我如何指导新人？Mentorship的心得？"}' | jq .
sleep 1

echo "[Round 72] - Failure Stories"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"分享一个我经历过的重大技术故障和教训？"}' | jq .
sleep 1

echo "[Round 73] - Success Stories"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我最自豪的技术项目是什么？解决了什么挑战？"}' | jq .
sleep 1

echo "[Round 74] - Integration Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"结合我的技术背景和团队管理经验，谈谈我的核心竞争力"}' | jq .
sleep 1

echo "[Round 75] - Comprehensive Recall"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"总结一下：我的身份、职业、技术栈、管理经验、学习目标"}' | jq .
sleep 1

# ==================== Phase 5: Extreme Memory Test (Rounds 76-100) ====================
echo ""
echo "=== Phase 5: Extreme Long-distance Memory Recall ==="

echo "[Round 76] - Ultra-long Test 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"回溯到我们对话的第1轮，我说的第一句话是什么？"}' | jq .
sleep 1

echo "[Round 77] - Ultra-long Test 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我毕业于哪些学校？什么专业？"}' | jq .
sleep 1

echo "[Round 78] - Ultra-long Test 3"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的职业发展路径是什么？在哪些公司工作过？"}' | jq .
sleep 1

echo "[Round 79] - Ultra-long Test 4"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我会哪些编程语言、框架和工具？"}' | jq .
sleep 1

echo "[Round 80] - Ultra-long Test 5"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我现在负责的系统有什么技术要求？团队规模多大？"}' | jq .
sleep 1

echo "[Round 81] - Cross-domain Test 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我们在中间阶段讨论过哪些分布式系统技术？列举5个"}' | jq .
sleep 1

echo "[Round 82] - Cross-domain Test 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我们讨论过哪些AI/ML技术？列举5个核心技术点"}' | jq .
sleep 1

echo "[Round 83] - Cross-domain Test 3"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的学习重点是什么？这如何与我的工作结合？"}' | jq .
sleep 1

echo "[Round 84] - Detail Verification 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"确认：我35岁，在上海，已婚有2个孩子，对吗？"}' | jq .
sleep 1

echo "[Round 85] - Detail Verification 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"确认：我的核心交易系统日处理500万笔，可用性99.99%，对吗？"}' | jq .
sleep 1

echo "[Round 86] - Detail Verification 3"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"确认：我精通Java、Go、Python、C++，熟悉JavaScript和TypeScript，对吗？"}' | jq .
sleep 1

echo "[Round 87] - Synthesis Test 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"基于我所有的背景，分析我适合创业的方向"}' | jq .
sleep 1

echo "[Round 88] - Synthesis Test 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"基于我的技术栈和行业经验，我应该关注什么技术趋势？"}' | jq .
sleep 1

echo "[Round 89] - Contradiction Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如果我告诉你我现在30岁，这与之前说的矛盾吗？"}' | jq .
sleep 1

echo "[Round 90] - Prioritization Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"在我们100轮对话中，哪些信息是最重要的？应该优先记住什么？"}' | jq .
sleep 1

echo "[Round 91] - Memory Structure Analysis"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你认为一个理想的记忆系统应该如何组织这些信息？"}' | jq .
sleep 1

echo "[Round 92] - Compression Quality Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如果让你用10句话总结我们的整个对话，你会怎么说？"}' | jq .
sleep 1

echo "[Round 93] - Key Information Extraction"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"提取对话中关于我的5个最关键的事实"}' | jq .
sleep 1

echo "[Round 94] - Contextual Understanding"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的技术背景、工作经历和学习目标之间有什么逻辑关联？"}' | jq .
sleep 1

echo "[Round 95] - Future Application"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"基于这些信息，你会在未来对话中如何利用这些记忆？"}' | jq .
sleep 1

echo "[Round 96] - Missing Info Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"有没有什么重要信息我们讨论过但你可能记不住的？"}' | jq .
sleep 1

echo "[Round 97] - Self-reflection"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你觉得在这100轮对话中，你的记忆管理表现如何？"}' | jq .
sleep 1

echo "[Round 98] - Comprehensive Profile"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"请生成一份完整的用户画像，包括所有关键信息"}' | jq .
sleep 1

echo "[Round 99] - Meta-cognitive Question"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"在这样的长对话中，Head、Tail、Context Window、Pin Facts应该如何协作？"}' | jq .
sleep 1

echo "[Round 100] - Final Challenge"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"终极挑战：在不回顾历史的情况下，请尽可能详细地回忆我们100轮对话的所有关键信息，包括我的身份、背景、我们讨论的所有技术话题、管理理念，以及这些信息之间的关联。你认为记忆系统的核心价值是什么？"}' | jq .

echo ""
echo "[Test Complete] 100 rounds dialog test finished"
echo ""
echo "Expected Memory System Behavior:"
echo "================================"
echo "1. Head Region (Round 1): Should preserve initial introduction"
echo "2. Tail Region (Rounds 99-100): Should contain recent context"
echo "3. Timing Context Window: Should have compressed summaries from rounds 2-98"
echo "4. Pinned Facts: Should extract:"
echo "   - Name: 王伟"
echo "   - Age: 35岁"
echo "   - Location: 上海"
echo "   - Family: 已婚，两个孩子"
echo "   - Role: 首席架构师，20人团队"
echo "   - System: 核心交易系统，500万笔/天，99.99%可用性"
echo "   - Tech Stack: Java, Go, Python, C++, JS, TS"
echo "   - Career Goal: 3年CTO, 5年创业"
echo "   - Learning Focus: AI/ML, LLM in Finance"
echo ""
echo "5. Compression Checkpoints:"
echo "   - After Round 20: First summary (distributed systems)"
echo "   - After Round 40: Second summary (AI/ML technologies)"
echo "   - After Round 60: Third summary (management topics)"
echo "   - After Round 80: Fourth summary (integration phase)"
echo ""
echo "6. Long-distance Recall Accuracy:"
echo "   - Identity info: >95%"
echo "   - Technical topics: >80%"
echo "   - Management topics: >75%"
echo "   - Context integration: >70%"
echo ""
echo "Please check the system logs and WorkingMemory structure to verify:"
echo "- Proper Head/Tail distribution"
echo "- Effective timingContextWindow compression"
echo "- Accurate Pin extraction and retention"
echo "- Successful long-distance information retrieval"
