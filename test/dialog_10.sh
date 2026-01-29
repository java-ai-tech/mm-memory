#!/bin/bash
BASE_URL="http://localhost:8080/api/demo/chat"
USER_ID=test_20260123_glmapper

echo "[Test Start] 10 rounds dialog"

echo "[Round 1]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你好，我是 glmapper"}' | jq .
sleep 1

echo "[Round 2]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我们刚才聊了什么？"}' | jq .
sleep 1

echo "[Round 3]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"请记住：我在测试多轮对话记忆"}' | jq .
sleep 1

echo "[Round 4]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你还记得我刚才说要你记住什么吗？"}' | jq .
sleep 1

echo "[Round 5]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"现在开始随便聊点别的"}' | jq .
sleep 1

echo "[Round 6]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"说一下 RAG 和 memory 的区别"}' | jq .
sleep 1

echo "[Round 7]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"如果窗口变大，会发生什么？"}' | jq .
sleep 1

echo "[Round 8]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"回到最开始，我是谁？"}' | jq .
sleep 1

echo "[Round 9]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我现在在做什么实验？"}' | jq .
sleep 1

echo "[Round 10]"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"总结一下我们这段对话"}' | jq .
