#!/bin/bash

# AI 优化功能测试脚本

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "AI 优化功能测试"
echo "=========================================="

# 1. 提交任务（启用自动优化）
echo -e "\n[1] 提交任务（启用自动优化）..."
JOB_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_name": "test_chip_ai",
      "technology": "28nm",
      "utilization": 80.0,
      "aspect_ratio": 1.0,
      "core_margin": 2.0,
      "target_frequency": 1200.0,
      "power_budget": 180.0
    },
    "autoOptimize": true
  }')

echo "响应: $JOB_RESPONSE"

# 提取 Job ID
JOB_ID=$(echo $JOB_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
echo "任务 ID: $JOB_ID"

# 2. 等待任务完成
echo -e "\n[2] 等待任务完成..."
for i in {1..30}; do
  sleep 2
  STATUS=$(curl -s ${BASE_URL}/api/jobs/${JOB_ID} | grep -o '"status":"[A-Z]*"' | cut -d'"' -f4)
  echo "  第 $i 次检查，状态: $STATUS"

  if [ "$STATUS" == "COMPLETED" ] || [ "$STATUS" == "FAILED" ]; then
    break
  fi
done

# 3. 查看任务结果
echo -e "\n[3] 查看任务结果..."
curl -s ${BASE_URL}/api/jobs/${JOB_ID} | jq '.'

# 4. 手动触发优化
echo -e "\n[4] 手动触发优化..."
curl -s -X POST ${BASE_URL}/api/optimization/manual/${JOB_ID} | jq '.'

echo -e "\n=========================================="
echo "测试完成！"
echo "=========================================="
