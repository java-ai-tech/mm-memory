#!/bin/bash

# Test script for ArtisanMemory chat API
# Each request is followed by a 3-second sleep

BASE_URL="http://localhost:8080/api/demo/chat"

# Test 9
echo "[Test 1] User: test_20260123_glmapper - Hello, 你好，你还记得我吗？"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"你好，我是glmapper"}' | jq .
sleep 5
echo ""

# Test 10
echo "[Test 10] User: test_20260123_glmapper - 那我和我解释下吧，glmapper 包括 gl + mapper, gl 是名字缩写，mapper 是连接的意思，那综合就是我来连接世界"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"那我和我解释下吧，glmapper 包括 gl + mapper, gl 是名字缩写，mapper 是连接的意思，那综合就是我来连接世界"}' | jq .
sleep 5
echo ""

# Test 11
echo "[Test 11] User: test_20260123_glmapper -是不是特别有意思？"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"是不是特别有意思？"}' | jq .
sleep 5
echo ""

# Test 12
echo "[Test 12] User: test_20260123_glmapper - 不过 glmapper 这个名字很少有人知道的，我在社会上的名字叫卫恒，我的朋友、家人、同事他们一般都喊我卫恒，glmapper 很少有人知道，我也希望你能够像 其他人一样"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"不过 glmapper 这个名字很少有人知道的，我在社会上的名字叫卫恒，我的朋友、家人、同事他们一般都喊我卫恒，glmapper 很少有人知道，我也希望你能够像 其他人一样"}' | jq .
sleep 5
echo ""

# Test 13
echo "[Test 13] User: test_20260123_glmapper - 再告诉你，我是一个高级系统架构师，主要是从事大模型应用架构及开发相关工作的，我想了解一下当前主流的大模型应用框架有哪些？都有什么特性？适合在什么场景下使用？比如我最近在研究 rag 的记忆模块，目前主流的开源框架中有哪些好的思路可以参考呢"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"我是一个高级系统架构师，主要是从事大模型应用架构及开发相关工作的，我想了解一下当前主流的大模型应用框架有哪些？都有什么特性？适合在什么场景下使用？比如我最近在研究 rag 的记忆模块，目前主流的开源框架中有哪些好的思路可以参考呢"}' | jq .
sleep 5
echo ""

# Test 14
echo "[Test 14] User: test_20260123_glmapper - 嗯，那下次吧；对了，你还记得我的名字？"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"嗯，那下次吧；对了，你还记得我的名字？"}' | jq .
sleep 5
echo ""

echo "=========================================="
echo "All tests completed!"
echo "=========================================="
