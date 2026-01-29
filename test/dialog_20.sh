#!/bin/bash
BASE_URL="http://localhost:8080/api/demo/chat"
USER_ID=test_20260127_glmapper_20

echo "[Test Start] 20 rounds dialog test"

echo "[Round 1] - 基本介绍"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"你好，我是张三，是一名软件工程师"}' | jq .
sleep 1

echo "[Round 2] - 询问职业"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的职业是什么？"}' | jq .
sleep 1

echo "[Round 3] - 引入新信息"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我住在北京市海淀区，今年28岁"}' | jq .
sleep 1

echo "[Round 4] - 测试短时记忆"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我刚才告诉你我住在哪里？"}' | jq .
sleep 1

echo "[Round 5] - 技术话题1"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"请记住：我主要使用Java和Python进行开发"}' | jq .
sleep 1

echo "[Round 6] - 技术话题2"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"熟悉Spring Boot和Django框架"}' | jq .
sleep 1

echo "[Round 7] - 交叉验证"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我刚才说我会哪些编程语言？"}' | jq .
sleep 1

echo "[Round 8] - 切换话题"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"现在聊点别的，讲讲什么是微服务架构"}' | jq .
sleep 1

echo "[Round 9] - 技术讨论"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"微服务的优势和挑战是什么？"}' | jq .
sleep 1

echo "[Round 10] - 长距离记忆测试"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"回到最开始，我是谁，做什么工作的？"}' | jq .
sleep 1

echo "[Round 11] - 添加爱好信息"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我平时喜欢打篮球和阅读技术书籍"}' | jq .
sleep 1

echo "[Round 12] - 询问爱好"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我有什么爱好？"}' | jq .
sleep 1

echo "[Round 13] - 项目信息"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我目前正在做一个记忆管理系统的项目"}' | jq .
sleep 1

echo "[Round 14] - 项目细节"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"这个项目使用了Redis和MongoDB作为存储"}' | jq .
sleep 1

echo "[Round 15] - 交叉验证项目信息"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的项目使用了什么数据库？"}' | jq .
sleep 1

echo "[Round 16] - 增加细节"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我们的团队有5个人，我是技术负责人"}' | jq .
sleep 1

echo "[Round 17] - 团队信息验证"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我们团队有多少人，我在团队中的角色是什么？"}' | jq .
sleep 1

echo "[Round 18] - 职业目标"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"我的职业目标是成为一名架构师"}' | jq .
sleep 1

echo "[Round 19] - 综合回忆"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"请总结一下我的信息：姓名、年龄、职业、所在地、技术栈、爱好、项目情况和职业目标"}' | jq .
sleep 1

echo "[Round 20] - 最终测试"
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"message":"在我们这段对话中，哪些信息是最重要的？"}' | jq .

echo "[Test Complete] 20 rounds dialog test finished"
