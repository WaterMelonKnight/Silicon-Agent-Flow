# 更新日志

## [1.1.0] - 2026-02-17

### 🤖 新增 - AI Agent 优化功能

#### 核心功能
- ✅ 集成 Spring AI 框架
- ✅ 支持 OpenAI / DeepSeek / 本地模型
- ✅ 自动分析芯片设计结果
- ✅ 智能生成优化参数建议
- ✅ 闭环迭代优化
- ✅ 完整的优化链追踪

#### 新增文件
- `OptimizationService.java` - AI 优化服务核心类
- `AiConfig.java` - Spring AI 配置
- `OptimizationController.java` - 优化 API 接口
- `AI_OPTIMIZATION.md` - 完整的 AI 优化文档
- `QUICKSTART_AI.md` - 快速开始指南
- `IMPLEMENTATION_SUMMARY.md` - 实现总结
- `test_ai_optimization.sh` - 自动化测试脚本
- `.env.example` - 环境变量配置示例

#### 修改文件
- `EdaJob.java` - 新增字段：autoOptimize, parentJobId, optimizationIteration, errorMessage
- `EdaJobService.java` - 集成 OptimizationService，任务完成后自动触发优化
- `JobSubmitRequest.java` - 新增 autoOptimize 字段
- `JobResponse.java` - 新增优化相关字段
- `EdaJobController.java` - 支持 autoOptimize 参数
- `application.yml` - 添加 AI 优化配置
- `pom.xml` - 添加 Spring AI 依赖
- `README.md` - 更新项目说明

#### API 变更
- `POST /api/jobs` - 支持 `autoOptimize` 参数
- `POST /api/optimization/manual/{jobId}` - 新增手动优化接口

#### 配置项
```yaml
ai:
  optimization:
    enabled: true
    max-iterations: 10
    model: gpt-4
  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: ${OPENAI_BASE_URL}
```

#### 使用示例
```bash
# 提交任务并启用自动优化
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"parameters": {...}, "autoOptimize": true}'

# 手动触发优化
curl -X POST http://localhost:8080/api/optimization/manual/1
```

---

## [1.0.0] - 2026-02-17

### 初始版本
- ✅ Spring Boot 3.2.2 + JDK 21
- ✅ H2 / MySQL 数据库支持
- ✅ OpenROAD 集成
- ✅ 异步任务执行
- ✅ Docker 容器化
- ✅ REST API 接口
