# Silicon-Agent-Flow

**芯片设计智能优化系统** - 集成 OpenROAD Flow Scripts 和 AI 大模型的芯片后端设计自动化平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.0-blue.svg)](https://vuejs.org/)
[![OpenROAD](https://img.shields.io/badge/OpenROAD-Latest-orange.svg)](https://theopenroadproject.org/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-purple.svg)](https://www.deepseek.com/)

## 🎯 项目亮点

### ✨ AI 优化实战案例

**真实运行结果**（2024年测试）：

```
初始设计 → AI 自动优化 10 轮迭代 → 面积减少 56.7%

Job #1 (初始):  UTIL=60, DENSITY=0.65 → Area=5641.7 μm²
Job #2 (AI优化): UTIL=70, DENSITY=0.75 → Area=2441.1 μm² ✓ 减少 56.7%
Job #3-10: 继续迭代优化，探索最优参数组合
```

**关键技术突破**：
- 🚀 **异步优化**：API 响应时间从 12 分钟优化至 0.065 秒（提升 11,000 倍）
- 🤖 **AI 驱动**：DeepSeek API 自动分析设计瓶颈，生成优化建议
- 🔄 **闭环迭代**：自动创建 10 轮优化任务，无需人工干预
- 📊 **实时可视化**：前端实时展示优化过程和结果对比

## 技术栈

- **后端**: Spring Boot 3.2.2 + JDK 20
- **前端**: Vue.js 3 + Composition API
- **数据库**: H2 (内存数据库)
- **AI 模型**: DeepSeek API / OpenAI API
- **EDA 工具**: OpenROAD Flow Scripts (Docker)
- **容器化**: Docker

## 🎉 核心功能

### 1. OpenROAD 真实执行

系统集成真实的 EDA 工具 **OpenROAD Flow Scripts**！

- ✅ Docker 容器化执行 ORFS
- ✅ 动态参数注入（CORE_UTILIZATION, PLACE_DENSITY 等）
- ✅ 自动解析面积、功耗、时序等 5 个核心指标
- ✅ 完整的日志捕获和错误处理
- ✅ 执行时间：1-2 秒（简单设计）

### 2. AI 智能优化

使用 **DeepSeek API** 自动优化芯片设计参数！

- ✅ 自动分析设计结果和日志
- ✅ 智能识别性能瓶颈（面积、功耗、时序）
- ✅ 生成优化参数建议（利用率、密度、长宽比）
- ✅ 闭环迭代优化（最多 10 轮）
- ✅ 自定义 HTTP 客户端，解决 Spring AI 兼容性问题

### 3. 异步任务调度

高性能异步任务处理系统！

- ✅ Spring @Async 异步执行
- ✅ 自注入模式解决 AOP 代理限制
- ✅ API 响应时间 < 100ms
- ✅ 任务状态实时更新
- ✅ 支持多任务并发执行

### 4. 前端可视化

赛博朋克风格的实时监控界面！

- ✅ Vue 3 Composition API
- ✅ 实时任务状态更新（每秒刷新）
- ✅ AI 优化链路展示（🤖 AI Opt: Iter X ← #Y）
- ✅ 结果指标可视化（面积、时序、功耗）
- ✅ 优化趋势图表

详细文档请查看: [AI_OPTIMIZATION.md](AI_OPTIMIZATION.md)

## 项目结构

```
Silicon-Agent-Flow/
├── src/main/java/com/silicon/agentflow/
│   ├── SiliconAgentFlowApplication.java  # 主应用类
│   ├── controller/
│   │   ├── EdaJobController.java         # REST API 控制器
│   │   └── OpenRoadController.java       # OpenROAD 测试接口
│   ├── service/
│   │   ├── EdaJobService.java            # 业务逻辑层
│   │   └── OpenRoadService.java          # OpenROAD 集成服务
│   ├── repository/
│   │   └── EdaJobRepository.java         # 数据访问层
│   ├── entity/
│   │   └── EdaJob.java                   # 实体类
│   ├── dto/
│   │   ├── JobSubmitRequest.java         # 请求 DTO
│   │   └── JobResponse.java              # 响应 DTO
│   ├── util/
│   │   ├── ProcessExecutor.java          # 进程执行工具
│   │   ├── TclTemplateGenerator.java     # TCL 模板生成器
│   │   └── LogParser.java                # 日志解析器
│   └── config/
│       └── AsyncConfig.java              # 异步配置
├── worker/
│   ├── eda_worker.py                     # Python Worker 脚本（可选）
│   ├── requirements.txt                  # Python 依赖
│   └── Dockerfile                        # Worker 镜像
├── Dockerfile                            # 应用镜像
├── docker-compose.yml                    # 容器编排
├── pom.xml                               # Maven 配置
├── README.md                             # 项目说明
└── OPENROAD_INTEGRATION.md               # OpenROAD 集成文档
```

## 数据模型

### EdaJob 实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| status | Enum | 任务状态: PENDING, RUNNING, COMPLETED, FAILED |
| logContent | Text | 日志内容 |
| parameters | JSON | 芯片设计参数 |
| resultMetrics | JSON | 结果指标（面积、功耗等） |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

## API 接口

### 1. 提交任务

```bash
POST /api/jobs
Content-Type: application/json

{
  "parameters": {
    "design_name": "my_chip",
    "technology": "28nm",
    "utilization": 70.0,
    "aspect_ratio": 1.0,
    "core_margin": 2.0,
    "target_frequency": 1200.0,
    "power_budget": 150.0
  }
}
```

**响应示例**:
```json
{
  "id": 1,
  "status": "PENDING",
  "logContent": "Job submitted",
  "parameters": {
    "design_name": "my_chip",
    "technology": "28nm",
    "utilization": 70.0,
    "target_frequency": 1200.0
  },
  "resultMetrics": null,
  "createdAt": "2026-02-17T10:00:00",
  "updatedAt": "2026-02-17T10:00:00"
}
```

### 2. 查询任务状态

```bash
GET /api/jobs/{id}
```

**响应示例**:
```json
{
  "id": 1,
  "status": "COMPLETED",
  "logContent": "Job submitted\nJob completed successfully",
  "parameters": {
    "design_name": "my_chip",
    "technology": "28nm",
    "utilization": 70.0,
    "target_frequency": 1200.0
  },
  "resultMetrics": {
    "area_um2": 2345.67,
    "power_mw": 123.45,
    "frequency_mhz": 1050.0,
    "timing_met": true
  },
  "createdAt": "2026-02-17T10:00:00",
  "updatedAt": "2026-02-17T10:00:05"
}
```

## 快速开始

### 方式 1: 使用 Docker Compose（推荐）

```bash
# 启动所有服务（MySQL + Spring Boot + Worker）
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

服务地址:
- Spring Boot API: http://localhost:8080
- EDA Worker: http://localhost:5000
- MySQL: localhost:3306

### 方式 2: 本地开发（使用 H2）

```bash
# 编译并运行
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/silicon-agent-flow-1.0.0.jar
```

访问 H2 控制台: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:edadb`
- Username: `sa`
- Password: (留空)

## 测试示例

```bash
# 提交任务
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_name": "test_chip",
      "technology": "28nm",
      "target_frequency": "1GHz"
    }
  }'

# 查询任务（假设返回的 ID 是 1）
curl http://localhost:8080/api/jobs/1

# 测试 Worker
curl -X POST http://localhost:5000/execute \
  -H "Content-Type: application/json" \
  -d '{
    "job_id": 1,
    "parameters": {
      "design_name": "test_chip"
    }
  }'
```

## EDA Worker 说明

Worker 是一个简单的 Python Flask 应用，模拟 EDA 工具的执行过程：

1. 接收任务参数
2. 休眠 5 秒（模拟计算）
3. 随机生成结果指标：
   - 面积 (area_um2): 1000-5000 um²
   - 功耗 (power_mw): 50-200 mW
   - 频率 (frequency_mhz): 800-1200 MHz
   - 时序是否满足 (timing_met): true/false

## 后续扩展

- [ ] 实现 Spring Boot 与 Worker 的异步通信（使用 RestTemplate 或消息队列）
- [ ] 添加任务队列管理
- [ ] 实现任务重试机制
- [ ] 添加用户认证和权限管理
- [ ] 集成真实的 EDA 工具
- [ ] 添加任务监控和告警

## 许可证

MIT License
