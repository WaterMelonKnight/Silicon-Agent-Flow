# AI 优化结果展示说明

## 📊 前端展示功能

### 1. 任务列表增强显示

每个任务卡片现在显示：

#### 基础信息
- **任务 ID**：#1, #2, #3...
- **状态**：PENDING, RUNNING, COMPLETED, FAILED
- **创建时间**：格式化的时间戳

#### AI 优化信息（当 autoOptimize=true 时）
```
🤖 AI Opt: Iter 2 ← #1
```
- **Iter X**：当前优化迭代次数（0-10）
- **← #Y**：父任务 ID（表示这是从哪个任务优化而来）

#### 执行结果指标（当任务完成时）
```
Area: 2927.8 μm²  Slack: -2.07 ns
```
- **Area**：芯片面积（绿色显示）
- **Slack**：时序裕量
  - 绿色：时序满足（slack ≥ 0）
  - 红色：时序违例（slack < 0）

### 2. 统计卡片

新增 **AI Optimized** 卡片：
- 显示启用了 AI 优化的任务总数
- 包括所有 `autoOptimize: true` 的任务

统计卡片布局：
```
┌─────────────┬─────────┬───────────┬──────────────┬────────┐
│ Total       │ Running │ Completed │ AI Optimized │ Failed │
│ Missions    │         │           │              │        │
└─────────────┴─────────┴───────────┴──────────────┴────────┘
```

### 3. 优化趋势图表

**📊 OPTIMIZATION TREND** 图表显示：
- X 轴：任务 ID
- Y 轴：芯片面积（μm²）
- 自动绘制所有已完成任务的面积变化趋势
- 可视化展示优化效果

## 🔄 AI 优化工作流程

### 完整优化链示例

```
任务 #1 (基准)
├─ autoOptimize: false
├─ optimizationIteration: 0
├─ parentJobId: null
└─ 结果: Area=5641.66 μm², Slack=-2.07 ns

任务 #2 (AI 优化 - 迭代 1)
├─ autoOptimize: true
├─ optimizationIteration: 1
├─ parentJobId: 1
└─ AI 分析任务 #1，调整参数后重新执行

任务 #3 (AI 优化 - 迭代 2)
├─ autoOptimize: true
├─ optimizationIteration: 2
├─ parentJobId: 2
└─ AI 继续优化，直到满足时序或达到最大迭代次数
```

## 🎯 如何查看 AI 优化效果

### 方法 1：任务列表
1. 打开前端页面 http://localhost:8080
2. 查看左侧任务列表
3. 找到带有 🤖 标记的任务
4. 查看优化迭代次数和父任务关系

### 方法 2：优化趋势图
1. 查看右侧的 **OPTIMIZATION TREND** 图表
2. 观察面积随任务 ID 的变化
3. 下降趋势表示优化有效

### 方法 3：API 查询
```bash
# 查看所有任务
curl http://localhost:8080/api/jobs | python3 -m json.tool

# 查看特定任务
curl http://localhost:8080/api/jobs/1 | python3 -m json.tool
```

## 📈 优化指标说明

### 关键指标

| 指标 | 说明 | 目标 |
|------|------|------|
| **area_um2** | 芯片面积 | 越小越好 |
| **slack_ns** | 时序裕量 | ≥ 0（满足时序）|
| **timing_met** | 时序是否满足 | true |
| **power_mw** | 功耗 | 越小越好 |
| **utilization_percent** | 核心利用率 | 接近目标值 |

### 优化成功标准

AI 优化在以下情况停止：
1. ✅ **时序满足**：slack_ns ≥ 0
2. ⏱️ **达到最大迭代**：optimizationIteration = 10
3. 📉 **无改进空间**：连续迭代无明显提升

## 🤖 启用 AI 优化

### 前端页面
1. 勾选 "Enable AI Auto-Optimization" 复选框
2. 点击 "LAUNCH MISSION" 按钮
3. 系统自动进行多轮优化

### API 调用
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_config": "designs/sky130hd/gcd/config.mk",
      "CORE_UTILIZATION": 65,
      "PLACE_DENSITY": 0.70,
      "CORE_ASPECT_RATIO": 1.0
    },
    "autoOptimize": true
  }'
```

## ⚠️ 当前限制

### AI 优化需要 OpenAI API Key

由于之前为了解决启动问题禁用了 OpenAI 配置，AI 优化功能目前无法工作。

**错误日志示例**：
```
Starting AI optimization for job 3, iteration 1/10
Failed to get optimization suggestion
```

### 启用 AI 优化的步骤

1. **配置 API Key**（在 `application.yml`）：
```yaml
ai:
  openai:
    api-key: your-api-key-here
    base-url: https://api.openai.com
  optimization:
    enabled: true
    model: gpt-4
    max-iterations: 10
```

2. **移除 OpenAI 排除**（在 `SiliconAgentFlowApplication.java`）：
```java
@SpringBootApplication  // 移除 exclude 配置
public class SiliconAgentFlowApplication {
    // ...
}
```

3. **重新编译和启动**：
```bash
mvn clean package -DskipTests
java -jar target/silicon-agent-flow-1.0.0.jar
```

## 🎉 总结

即使没有 AI 优化，前端页面现在也能：
- ✅ 显示任务的优化意图（autoOptimize 标记）
- ✅ 显示优化迭代次数
- ✅ 显示父子任务关系
- ✅ 显示执行结果指标
- ✅ 绘制优化趋势图表

配置 OpenAI API Key 后，AI 将自动：
- 🤖 分析执行结果
- 🔧 调整设计参数
- 🔄 提交优化任务
- 📊 持续改进直到满足目标

---

**更新时间**: 2026-02-23
**版本**: v1.0
