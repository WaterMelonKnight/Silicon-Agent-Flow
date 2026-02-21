# Silicon-Agent-Flow

EDA 任务调度系统 - 用于调度芯片设计任务的开源项目

## 技术栈

- **后端**: Spring Boot 3.2.2 + JDK 21
- **数据库**: H2 (开发) / MySQL 8.0 (生产)
- **ORM**: Spring Data JPA
- **EDA 工具**: OpenROAD (集成真实芯片设计工具)
- **Worker**: Python 3.11 + Flask (可选)
- **容器化**: Docker + Docker Compose

## 🎉 新特性：OpenROAD 集成

系统现已集成真实的 EDA 工具 **OpenROAD**！

- ✅ 动态生成 TCL 配置文件
- ✅ Docker 容器化执行
- ✅ 自动解析面积、功耗等指标
- ✅ 异步任务执行
- ✅ 完整的日志捕获

详细文档请查看: [OPENROAD_INTEGRATION.md](OPENROAD_INTEGRATION.md)

## 🤖 新特性：AI Agent 优化

系统现已集成 **Spring AI**，使用 LLM 自动优化芯片设计参数！

- ✅ 自动分析设计结果
- ✅ 智能识别性能瓶颈
- ✅ 生成优化参数建议
- ✅ 闭环迭代优化
- ✅ 支持 OpenAI / DeepSeek / 本地模型

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
