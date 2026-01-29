#!/bin/bash
BASE_URL="http://localhost:8080/api/demo/chat"
USER_ID=test_20260127_glmapper_50

echo "[Test Start] 50 rounds dialog test - Memory Compression & Long-term Retention"

# ==================== Phase 1: Basic Information (Rounds 1-10) ====================
echo ""
echo "=== Phase 1: Basic Information & Short-term Memory ==="

echo "[Round 1] - Self Introduction"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你好，我叫李明，是一名全栈开发工程师，在杭州工作"}' | jq .
sleep 1

echo "[Round 2] - Work Experience"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我有5年的开发经验，之前在阿里和字节跳动工作过"}' | jq .
sleep 1

echo "[Round 3] - Tech Stack"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我精通Java、Go、JavaScript，熟悉Kubernetes和Docker"}' | jq .
sleep 1

echo "[Round 4] - Current Focus"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"现在主要关注云原生和微服务架构设计"}' | jq .
sleep 1

echo "[Round 5] - Personal Info"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我今年30岁，已婚，有一个孩子"}' | jq .
sleep 1

echo "[Round 6] - Short Test 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我叫什么名字？"}' | jq .
sleep 1

echo "[Round 7] - Short Test 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我在哪个城市工作？"}' | jq .
sleep 1

echo "[Round 8] - Tech Question"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我之前在哪些公司工作过？"}' | jq .
sleep 1

echo "[Round 9] - Confirmation Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"请记住：我正在考虑跳槽，目标是去一家AI公司"}' | jq .
sleep 1

echo "[Round 10] - Immediate Recall"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我刚才说我的职业目标是什么？"}' | jq .
sleep 1

# ==================== Phase 2: Technical Discussions (Rounds 11-20) ====================
echo ""
echo "=== Phase 2: Technical Deep Dive ==="

echo "[Round 11] - System Design Topic 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"让我们聊聊分布式系统的CAP理论"}' | jq .
sleep 1

echo "[Round 12] - CAP Follow-up"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"在实际项目中，你如何在一致性和可用性之间做权衡？"}' | jq .
sleep 1

echo "[Round 13] - Database Topic"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"讲讲关系型数据库和NoSQL的区别"}' | jq .
sleep 1

echo "[Round 14] - Database Deep Dive"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"什么时候用MongoDB，什么时候用PostgreSQL？"}' | jq .
sleep 1

echo "[Round 15] - Message Queue"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我在项目中用过Kafka和RabbitMQ"}' | jq .
sleep 1

echo "[Round 16] - MQ Comparison"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Kafka和RabbitMQ各自适合什么场景？"}' | jq .
sleep 1

echo "[Round 17] - Caching Strategy"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Redis的缓存穿透、缓存击穿、缓存雪崩有什么区别？"}' | jq .
sleep 1

echo "[Round 18] - Cache Solution"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如何解决这些问题？"}' | jq .
sleep 1

echo "[Round 19] - Memory Management"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我最近在研究LLM的记忆管理系统"}' | jq .
sleep 1

echo "[Round 20] - Memory System Discussion"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"长对话的记忆压缩策略有哪些？"}' | jq .
sleep 1

# ==================== Phase 3: Mixed Topics & Context Switch (Rounds 21-30) ====================
echo ""
echo "=== Phase 3: Context Switch & Long-distance Recall ==="

echo "[Round 21] - Back to Personal Info"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"回到我们最开始的话题，我叫什么，多大年龄？"}' | jq .
sleep 1

echo "[Round 22] - Work History Check"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我说过我在哪些公司工作过？"}' | jq .
sleep 1

echo "[Round 23] - Tech Stack Recall"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我会哪些编程语言和技术？"}' | jq .
sleep 1

echo "[Round 24] - New Topic: AI"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"现在聊聊AI，Transformer架构的核心思想是什么？"}' | jq .
sleep 1

echo "[Round 25] - Attention Mechanism"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"Self-Attention是如何工作的？"}' | jq .
sleep 1

echo "[Round 26] - LLM Topic"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"GPT和BERT的主要区别是什么？"}' | jq .
sleep 1

echo "[Round 27] - RAG Discussion"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"RAG系统的核心组件有哪些？"}' | jq .
sleep 1

echo "[Round 28] - Vector Database"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"向量数据库和传统数据库有什么区别？"}' | jq .
sleep 1

echo "[Round 29] - Embedding Models"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"常用的embedding模型有哪些？"}' | jq .
sleep 1

echo "[Round 30] - Back to Career Goal"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我之前说我的职业目标是什么？还记得吗？"}' | jq .
sleep 1

# ==================== Phase 4: Project Details & Complex Scenarios (Rounds 31-40) ====================
echo ""
echo "=== Phase 4: Project Details ==="

echo "[Round 31] - Project Introduction"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我正在做一个电商推荐系统项目"}' | jq .
sleep 1

echo "[Round 32] - Project Tech Stack"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"项目使用Spark做数据处理，TensorFlow做模型训练"}' | jq .
sleep 1

echo "[Round 33] - Team Info"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"团队有8个人，包括2名算法工程师"}' | jq .
sleep 1

echo "[Round 34] - Project Challenges"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我们面临的主要挑战是实时性要求高"}' | jq .
sleep 1

echo "[Round 35] - Solution"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"采用Flink做实时计算，Redis做缓存"}' | jq .
sleep 1

echo "[Round 36] - Project Status"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"项目已经上线，日活用户100万"}' | jq .
sleep 1

echo "[Round 37] - Performance Metrics"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"推荐响应时间控制在100ms以内"}' | jq .
sleep 1

echo "[Round 38] - Future Plans"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"计划引入大模型做语义理解和个性化推荐"}' | jq .
sleep 1

echo "[Round 39] - Project Recall Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我正在做的项目是什么？团队有多少人？"}' | jq .
sleep 1

echo "[Round 40] - Tech Stack Recall"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"这个项目使用了哪些技术栈？"}' | jq .
sleep 1

# ==================== Phase 5: Comprehensive Testing (Rounds 41-50) ====================
echo ""
echo "=== Phase 5: Comprehensive Memory Test ==="

echo "[Round 41] - Cross-domain Question 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"请结合我的技术背景，说说我在推荐系统项目中的角色"}' | jq .
sleep 1

echo "[Round 42] - Cross-domain Question 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我之前的工作经验如何帮助当前的项目？"}' | jq .
sleep 1

echo "[Round 43] - Personal Life"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我平时喜欢游泳和看书，这是我的放松方式"}' | jq .
sleep 1

echo "[Round 44] - Long Distance Recall 1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"现在回到最开始，完整介绍我自己"}' | jq .
sleep 1

echo "[Round 45] - Long Distance Recall 2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的工作经历、当前项目、技术栈和职业目标分别是什么？"}' | jq .
sleep 1

echo "[Round 46] - Fact Verification"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"确认一下：我在杭州工作，30岁，已婚有孩子，对吗？"}' | jq .
sleep 1

echo "[Round 47] - Tech Verification"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我熟悉Java、Go、JavaScript，还有Kubernetes和Docker，对吗？"}' | jq .
sleep 1

echo "[Round 48] - Project Verification"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的推荐系统项目日活100万，响应时间100ms，对吗？"}' | jq .
sleep 1

echo "[Round 49] - Comprehensive Summary"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"请全面总结我们讨论过的所有关于我的信息"}' | jq .
sleep 1

echo "[Round 50] - Final Test"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"在这段对话中，哪些是你认为最重要的关键信息？这些信息应该如何被记住和使用？"}' | jq .

echo ""
echo "[Test Complete] 50 rounds dialog test finished"
echo "Expected Results:"
echo "- WorkingMemory should have proper Head/Tail distribution"
echo "- timingContextWindow should contain compressed summaries"
echo "- Pinned Facts should extract key personal information"
echo "- Long-distance recall should work effectively"
