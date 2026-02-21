# AI Agent 优化芯片设计参数 - 集成文档

## 概述

本系统集成了 Spring AI，使用 LLM（如 OpenAI GPT-4、DeepSeek 或本地 BitNet）作为 AI Agent，自动分析芯片设计任务的执行结果，并提供优化建议，实现闭环优化。

## 架构设计

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  用户提交   │─────>│  EdaJobService│─────>│  OpenROAD   │
│  设计参数   │      │              │      │   执行      │
└─────────────┘      └──────────────┘      └─────────────┘
                            │                      │
                            │                      ▼
                            │              ┌─────────────┐
                            │              │  结果指标   │
                            │              │ (面积/功耗) │
                            │              └─────────────┘
                            │                      │
                            ▼                      ▼
                     ┌──────────────────────────────┐
                     │   OptimizationService        │
                     │   (AI Agent)                 │
                     │   - 分析结果                 │
                     │   - 识别瓶颈                 │
                     │   - 生成新参数               │
                     └──────────────────────────────┘
                                  │
                                  ▼
                     ┌──────────────────────────────┐
                     │   自动创建新任务             │
                     │   (如果启用 Auto-Optimize)   │
                     └──────────────────────────────┘
```

## 核心组件

### 1. OptimizationService

AI 优化服务的核心类，负责：
- 调用 LLM 分析任务结果
- 生成优化建议
- 自动创建优化任务

**关键方法**：
- `optimizeJobAsync(Long jobId)`: 异步执行优化流程
- `getOptimizationSuggestion(EdaJob job)`: 调用 LLM 获取建议
- `manualOptimize(Long jobId)`: 手动触发优化

### 2. System Prompt 设计

```
你是一个资深的芯片后端设计专家，精通数字 IC 设计流程和 EDA 工具优化。

分析重点：
1. 面积优化：降低芯片面积 (area_um2)
2. 功耗优化：降低功耗 (power_mw)
3. 时序优化：提高频率 (frequency_mhz)
4. 参数平衡：在面积、功耗、性能之间找到最佳平衡点

优化策略：
- utilization: 利用率 (40-90%)
- aspect_ratio: 长宽比 (0.5-2.0)
- core_margin: 核心边距 (1.0-5.0)
- target_frequency: 目标频率
- power_budget: 功耗预算
```

### 3. 数据模型扩展

`EdaJob` 实体新增字段：
- `autoOptimize`: 是否启用自动优化
- `parentJobId`: 父任务 ID（用于追踪优化链）
- `optimizationIteration`: 优化迭代次数
- `errorMessage`: 错误信息

## 配置说明

### application.yml

```yaml
ai:
  optimization:
    enabled: true              # 启用/禁用 AI 优化
    max-iterations: 10         # 最大优化迭代次数
    model: gpt-4               # 使用的模型
  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: ${OPENAI_BASE_URL}
```

### 环境变量

```bash
# OpenAI 官方 API
export OPENAI_API_KEY=sk-your-api-key
export OPENAI_BASE_URL=https://api.openai.com/v1

# DeepSeek API
export OPENAI_API_KEY=sk-your-deepseek-key
export OPENAI_BASE_URL=https://api.deepseek.com/v1

# 本地 BitNet 或其他兼容 API
export OPENAI_API_KEY=local-key
export OPENAI_BASE_URL=http://localhost:8000/v1
```

## API 使用示例

### 1. 提交任务（启用自动优化）

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_name": "my_chip",
      "technology": "28nm",
      "utilization": 70.0,
      "aspect_ratio": 1.0,
      "core_margin": 2.0,
      "target_frequency": 1200.0,
      "power_budget": 150.0
    },
    "autoOptimize": true
  }'
```

### 2. 手动触发优化

```bash
curl -X POST http://localhost:8080/api/optimization/manual/1
```

## 总结

通过集成 Spring AI 和 LLM，系统实现了：
- ✅ 自动分析芯片设计结果
- ✅ 智能生成优化参数
- ✅ 闭环迭代优化
- ✅ 完整的优化链追踪
- ✅ 灵活的配置和扩展
