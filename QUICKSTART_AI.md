# AI 优化功能快速开始

## 1. 配置环境变量

```bash
# 使用 DeepSeek API（推荐，性价比高）
export OPENAI_API_KEY=sk-your-deepseek-api-key
export OPENAI_BASE_URL=https://api.deepseek.com/v1

# 或使用 OpenAI API
export OPENAI_API_KEY=sk-your-openai-api-key
export OPENAI_BASE_URL=https://api.openai.com/v1
```

## 2. 启动应用

```bash
# 方式 1: 使用 Maven
mvn spring-boot:run

# 方式 2: 使用 Docker Compose
docker-compose up -d
```

## 3. 提交任务（启用自动优化）

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_name": "my_chip",
      "technology": "28nm",
      "utilization": 80.0,
      "aspect_ratio": 1.0,
      "core_margin": 2.0,
      "target_frequency": 1200.0,
      "power_budget": 180.0
    },
    "autoOptimize": true
  }'
```

**响应示例**：
```json
{
  "id": 1,
  "status": "PENDING",
  "autoOptimize": true,
  "optimizationIteration": 0,
  "parentJobId": null
}
```

## 4. 查询任务状态

```bash
curl http://localhost:8080/api/jobs/1
```

**完成后的响应**：
```json
{
  "id": 1,
  "status": "COMPLETED",
  "resultMetrics": {
    "area_um2": 2500.0,
    "power_mw": 160.0,
    "frequency_mhz": 1100.0,
    "timing_met": true
  },
  "autoOptimize": true,
  "optimizationIteration": 0
}
```

## 5. 查看自动生成的优化任务

系统会自动创建优化任务（Job ID: 2）：

```bash
curl http://localhost:8080/api/jobs/2
```

**响应示例**：
```json
{
  "id": 2,
  "status": "RUNNING",
  "parameters": {
    "design_name": "my_chip_opt_1",
    "utilization": 70.0,
    "aspect_ratio": 1.2,
    "optimization_reason": "降低利用率以减小面积，调整长宽比改善时序"
  },
  "autoOptimize": true,
  "parentJobId": 1,
  "optimizationIteration": 1
}
```

## 6. 手动触发优化（可选）

如果不想启用自动优化，可以手动触发：

```bash
curl -X POST http://localhost:8080/api/optimization/manual/1
```

**响应示例**：
```json
{
  "design_name": "my_chip_optimized",
  "technology": "28nm",
  "utilization": 70.0,
  "aspect_ratio": 1.2,
  "core_margin": 2.5,
  "target_frequency": 1150.0,
  "power_budget": 160.0,
  "optimization_reason": "降低利用率和功耗预算以减小面积和功耗"
}
```

## 7. 运行测试脚本

```bash
./test_ai_optimization.sh
```

## 配置说明

在 `application.yml` 中调整优化参数：

```yaml
ai:
  optimization:
    enabled: true          # 启用/禁用 AI 优化
    max-iterations: 10     # 最大优化迭代次数
    model: gpt-4           # 或 deepseek-chat
```

## 优化流程

1. **提交任务** → 设置 `autoOptimize: true`
2. **任务执行** → OpenROAD 执行芯片设计
3. **AI 分析** → LLM 分析结果并生成优化参数
4. **自动创建** → 系统自动创建优化任务
5. **迭代优化** → 重复步骤 2-4，直到达到最大迭代次数

## 监控日志

```bash
# 查看优化日志
docker-compose logs -f app | grep "optimization"

# 关键日志：
# - "Starting AI optimization for job X"
# - "Created optimization job Y from parent job X"
```

## 故障排查

### 问题：优化任务未自动创建

**检查**：
- 确认 `autoOptimize` 设置为 `true`
- 确认 `ai.optimization.enabled` 为 `true`
- 确认任务状态为 `COMPLETED`
- 检查是否达到最大迭代次数

### 问题：LLM API 调用失败

**检查**：
- 验证 API Key 是否正确
- 验证 Base URL 是否可访问
- 检查网络连接
- 查看应用日志获取详细错误信息

## 成本控制

每次优化会调用 LLM API，建议：
- 使用 DeepSeek API（成本更低）
- 设置合理的 `max-iterations`（建议 5-10 次）
- 监控 API 使用量

## 下一步

查看完整文档：[AI_OPTIMIZATION.md](AI_OPTIMIZATION.md)
