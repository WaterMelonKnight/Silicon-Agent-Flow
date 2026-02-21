# AI Agent 优化芯片设计参数 - 实现总结

## 🎯 实现概述

已成功集成 Spring AI，实现了使用 LLM 自动优化芯片设计参数的完整闭环系统。

## 📦 核心组件

### 1. 依赖配置 (pom.xml)

```xml
<!-- Spring AI OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M4</version>
</dependency>
```

### 2. AI 配置 (AiConfig.java)

```java
@Configuration
public class AiConfig {
    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
```

### 3. 数据模型扩展 (EdaJob.java)

新增字段：
- `autoOptimize`: 是否启用自动优化
- `parentJobId`: 父任务 ID（追踪优化链）
- `optimizationIteration`: 优化迭代次数
- `errorMessage`: 错误信息

### 4. 优化服务 (OptimizationService.java)

**核心功能**：
- `optimizeJobAsync()`: 异步执行优化流程
- `getOptimizationSuggestion()`: 调用 LLM 获取优化建议
- `manualOptimize()`: 手动触发优化

**System Prompt 设计**：
```
你是一个资深的芯片后端设计专家，精通数字 IC 设计流程和 EDA 工具优化。

分析重点：
1. 面积优化：降低芯片面积 (area_um2)
2. 功耗优化：降低功耗 (power_mw)
3. 时序优化：提高频率 (frequency_mhz)
4. 参数平衡：在面积、功耗、性能之间找到最佳平衡点
```

### 5. 服务层集成 (EdaJobService.java)

```java
// 任务完成后自动触发优化
if (result.isSuccess()) {
    updateJobResult(jobId, result.getMetrics(), result.getLog());
    optimizationService.optimizeJobAsync(jobId);  // 触发 AI 优化
}
```

### 6. API 接口

#### OptimizationController.java
- `POST /api/optimization/manual/{jobId}`: 手动触发优化

#### EdaJobController.java (更新)
- `POST /api/jobs`: 支持 `autoOptimize` 参数

## 🔄 优化流程

```
1. 用户提交任务 (autoOptimize: true)
   ↓
2. OpenROAD 执行芯片设计
   ↓
3. 任务完成，获取结果指标
   ↓
4. OptimizationService 自动触发
   ↓
5. 调用 LLM 分析结果
   输入: 当前参数 + 结果指标 + 日志 + 错误信息
   输出: 优化后的参数 JSON
   ↓
6. 自动创建新的优化任务
   ↓
7. 重复步骤 2-6，直到达到最大迭代次数
```

## 📝 配置文件 (application.yml)

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

## 🚀 使用示例

### 提交任务（启用自动优化）

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

### 手动触发优化

```bash
curl -X POST http://localhost:8080/api/optimization/manual/1
```

## 📊 LLM 交互格式

### 输入 Prompt

```
## 当前设计参数
{
  "utilization": 80.0,
  "aspect_ratio": 1.0,
  ...
}

## 执行结果
- area_um2: 2500.0
- power_mw: 160.0
- frequency_mhz: 1100.0
- timing_met: true

## 优化目标
当前迭代次数：1/10
主要目标：减小芯片面积、降低功耗
```

### 输出格式

```json
{
  "design_name": "my_chip_opt_1",
  "technology": "28nm",
  "utilization": 70.0,
  "aspect_ratio": 1.2,
  "core_margin": 2.5,
  "target_frequency": 1150.0,
  "power_budget": 160.0,
  "optimization_reason": "降低利用率以减小面积，调整长宽比改善时序"
}
```

## 🎨 优化策略

### 面积优化
- 降低 `utilization`（40-90%）
- 调整 `aspect_ratio` 改善布局
- 减小 `core_margin`

### 功耗优化
- 降低 `target_frequency`
- 减小 `power_budget`
- 优化 `utilization` 平衡

### 时序优化
- 调整 `aspect_ratio` 减少关键路径
- 增加 `core_margin` 改善布线
- 适当降低 `utilization`

## 📁 项目结构

```
Silicon-Agent-Flow/
├── src/main/java/com/silicon/agentflow/
│   ├── config/
│   │   ├── AiConfig.java              # AI 配置
│   │   └── AsyncConfig.java           # 异步配置
│   ├── controller/
│   │   ├── EdaJobController.java      # 任务 API
│   │   ├── OptimizationController.java # 优化 API
│   │   └── OpenRoadController.java    # OpenROAD API
│   ├── service/
│   │   ├── EdaJobService.java         # 任务服务
│   │   ├── OptimizationService.java   # 优化服务 ⭐
│   │   └── OpenRoadService.java       # OpenROAD 服务
│   ├── entity/
│   │   └── EdaJob.java                # 实体类（已扩展）
│   ├── dto/
│   │   ├── JobSubmitRequest.java      # 请求 DTO（已扩展）
│   │   └── JobResponse.java           # 响应 DTO（已扩展）
│   └── repository/
│       └── EdaJobRepository.java      # 数据访问层
├── src/main/resources/
│   └── application.yml                # 配置文件（已扩展）
├── pom.xml                            # Maven 配置（已添加 Spring AI）
├── README.md                          # 项目说明（已更新）
├── AI_OPTIMIZATION.md                 # AI 优化详细文档 ⭐
├── QUICKSTART_AI.md                   # 快速开始指南 ⭐
└── test_ai_optimization.sh            # 测试脚本 ⭐
```

## ✅ 实现清单

- [x] 添加 Spring AI 依赖
- [x] 创建 AiConfig 配置类
- [x] 扩展 EdaJob 实体（autoOptimize, parentJobId, optimizationIteration, errorMessage）
- [x] 创建 OptimizationService 服务
- [x] 设计 System Prompt
- [x] 实现 LLM 调用逻辑
- [x] 实现自动优化闭环
- [x] 创建 OptimizationController API
- [x] 更新 EdaJobService 集成优化服务
- [x] 更新 DTO（JobSubmitRequest, JobResponse）
- [x] 更新 EdaJobController 支持 autoOptimize
- [x] 配置 application.yml
- [x] 创建测试脚本
- [x] 编写文档（AI_OPTIMIZATION.md, QUICKSTART_AI.md）
- [x] 更新 README.md

## 🔧 环境配置

### 使用 DeepSeek API（推荐）

```bash
export OPENAI_API_KEY=sk-your-deepseek-api-key
export OPENAI_BASE_URL=https://api.deepseek.com/v1
```

### 使用 OpenAI API

```bash
export OPENAI_API_KEY=sk-your-openai-api-key
export OPENAI_BASE_URL=https://api.openai.com/v1
```

### 使用本地模型

```bash
export OPENAI_API_KEY=local-key
export OPENAI_BASE_URL=http://localhost:8000/v1
```

## 🧪 测试

运行测试脚本：

```bash
./test_ai_optimization.sh
```

## 📚 文档

- **AI_OPTIMIZATION.md**: 完整的 AI 优化文档
- **QUICKSTART_AI.md**: 快速开始指南
- **OPENROAD_INTEGRATION.md**: OpenROAD 集成文档
- **README.md**: 项目总览

## 🎯 核心特性

✅ **自动分析**：LLM 自动分析芯片设计结果
✅ **智能优化**：识别性能瓶颈并生成优化参数
✅ **闭环迭代**：自动创建优化任务，持续改进
✅ **灵活配置**：支持多种 LLM API（OpenAI/DeepSeek/本地）
✅ **完整追踪**：通过 parentJobId 追踪优化链
✅ **手动触发**：支持手动优化模式

## 🚀 下一步扩展

1. **多目标优化**：支持用户自定义优化目标（面积/功耗/性能）
2. **优化历史分析**：记录优化历史，分析优化趋势
3. **A/B 测试**：并行生成多组参数，选择最优结果
4. **强化学习**：基于历史数据训练优化策略
5. **可视化**：优化过程可视化展示

## 💡 总结

通过集成 Spring AI，系统实现了完整的 AI Agent 优化闭环，能够自动分析芯片设计结果并持续优化参数，大幅提升了芯片设计的自动化水平和优化效率。
