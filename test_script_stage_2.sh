#!/bin/bash

# Test script for ArtisanMemory chat API
# Each request is followed by a 3-second sleep

BASE_URL="http://localhost:8080/api/demo/chat"

# Test 9
echo "[Test 9] User: test_20260123_glmapper - Hello, 你好，你还记得我吗？"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"Hello, 你好，你还记得我吗？"}' | jq .
sleep 5
echo ""

# Test 10
echo "[Test 10] User: test_20260123_glmapper - 上次我们聊到了 llm rag 的记忆模块，我想和你继续聊聊这个话题"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"上次我们聊到了 llm rag 的记忆模块，我想和你继续聊聊这个话题"}' | jq .
sleep 5
echo ""

# Test 14
echo "[Test 14] User: test_20260123_glmapper - 嗯，能帮我介绍下 langchain 的记忆模块是怎么设计和实现的吗？"
curl -s -X POST "${BASE_URL}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test_20260123_glmapper" \
  -d '{"message":"嗯，能帮我介绍下 langchain 的记忆模块是怎么设计和实现的吗？"}' | jq .
sleep 5
echo ""

echo "=========================================="
echo "All tests completed!"
echo "=========================================="
