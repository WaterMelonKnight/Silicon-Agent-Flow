# OpenROAD 集成指南

## 概述

Silicon-Agent-Flow 现已集成真实的 EDA 工具 OpenROAD。系统会根据任务参数动态生成 TCL 配置文件，通过 Docker 执行 OpenROAD，并自动解析结果指标。

## 核心组件

### 1. ProcessExecutor（进程执行器）
- 位置: `util/ProcessExecutor.java`
- 功能: 执行外部命令（Docker、tclsh 等）
- 特性:
  - 捕获标准输出和标准错误
  - 支持超时控制（默认 30 分钟）
  - 实时日志输出
  - 线程安全

### 2. TclTemplateGenerator（TCL 模板生成器）
- 位置: `util/TclTemplateGenerator.java`
- 功能: 根据 JSON 参数生成 config.tcl 文件
- 支持参数:
  - `design_name`: 设计名称
  - `technology`: 工艺节点（如 28nm）
  - `utilization`: 核心利用率（%）
  - `aspect_ratio`: 长宽比
  - `core_margin`: 核心边距
  - `target_frequency`: 目标频率（MHz）
  - `power_budget`: 功耗预算（mW）

### 3. LogParser（日志解析器）
- 位置: `util/LogParser.java`
- 功能: 从 OpenROAD 日志中提取关键指标
- 支持的正则表达式:
  ```java
  // 面积: "Total Area: 1234.5" 或 "Area: 1234.5"
  Pattern.compile("(?:Total\\s+)?Area\\s*[:=]\\s*([0-9]+\\.?[0-9]*)")

  // 功耗: "Total Power: 0.56" 或 "Power: 0.56"
  Pattern.compile("(?:Total\\s+)?Power\\s*[:=]\\s*([0-9]+\\.?[0-9]*)")

  // 频率: "Frequency: 1000 MHz"
  Pattern.compile("(?:Frequency|Clock)\\s*[:=]\\s*([0-9]+\\.?[0-9]*)\\s*(?:MHz)?")

  // 利用率: "Core Utilization: 65.5%"
  Pattern.compile("(?:Core\\s+)?Utilization\\s*[:=]\\s*([0-9]+\\.?[0-9]*)\\s*%?")
  ```

### 4. OpenRoadService（OpenROAD 服务）
- 位置: `service/OpenRoadService.java`
- 功能: 集成 OpenROAD 执行流程
- 执行流程:
  1. 生成 TCL 配置文件
  2. 调用 Docker 运行 OpenROAD
  3. 捕获标准输出
  4. 解析结果指标
  5. 返回执行结果

## 配置说明

### application.yml

```yaml
openroad:
  docker:
    enabled: false  # 是否启用 Docker 执行
    image: openroad/flow-ubuntu22.04  # Docker 镜像
  timeout:
    minutes: 30  # 执行超时时间
```

### 执行模式

#### 模式 1: 本地 tclsh（测试模式）
```yaml
openroad:
  docker:
    enabled: false
```
- 使用本地 tclsh 执行 TCL 脚本
- 适合开发和测试
- 需要安装 tclsh: `apt-get install tcl`

#### 模式 2: Docker 模式（生产模式）
```yaml
openroad:
  docker:
    enabled: true
    image: openroad/flow-ubuntu22.04
```
- 使用 Docker 容器执行 OpenROAD
- 适合生产环境
- 需要安装 Docker 并拉取镜像

## API 使用

### 1. 提交 OpenROAD 任务

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
    }
  }'
```

响应示例:
```json
{
  "id": 1,
  "status": "PENDING",
  "logContent": "Job submitted",
  "parameters": {
    "design_name": "my_chip",
    "technology": "28nm",
    "utilization": 70.0,
    "aspect_ratio": 1.0,
    "target_frequency": 1200.0
  },
  "resultMetrics": null,
  "createdAt": "2026-02-17T10:00:00",
  "updatedAt": "2026-02-17T10:00:00"
}
```

### 2. 查询任务状态

```bash
curl http://localhost:8080/api/jobs/1
```

响应示例（执行中）:
```json
{
  "id": 1,
  "status": "RUNNING",
  "logContent": "Job submitted\nStarting OpenROAD execution...",
  "parameters": {...},
  "resultMetrics": null
}
```

响应示例（完成）:
```json
{
  "id": 1,
  "status": "COMPLETED",
  "logContent": "Job submitted\nStarting OpenROAD execution...\n\n=== STDOUT ===\n...\nTotal Area: 2345.67 um2\nTotal Power: 123.45 mW\n...",
  "parameters": {...},
  "resultMetrics": {
    "area_um2": 2345.67,
    "power_mw": 123.45,
    "frequency_mhz": 1150.0,
    "utilization_percent": 70.0,
    "parsed_at": "2026-02-17T10:00:35"
  }
}
```

### 3. 检查 Docker 状态

```bash
# 检查 Docker 是否可用
curl http://localhost:8080/api/openroad/docker/check

# 拉取 OpenROAD 镜像
curl -X POST http://localhost:8080/api/openroad/docker/pull
```

## 工作流程

```
1. 用户提交任务
   ↓
2. 创建 EdaJob（状态: PENDING）
   ↓
3. 异步执行 executeJobAsync()
   ↓
4. 更新状态为 RUNNING
   ↓
5. 生成 TCL 配置文件（work/job_{id}/config.tcl）
   ↓
6. 执行 OpenROAD
   - Docker 模式: docker run -v ... openroad/flow-ubuntu22.04 tclsh config.tcl
   - 本地模式: tclsh config.tcl
   ↓
7. 捕获标准输出和标准错误
   ↓
8. 解析日志提取指标
   - area_um2
   - power_mw
   - frequency_mhz
   - utilization_percent
   ↓
9. 更新任务状态
   - 成功: COMPLETED + resultMetrics
   - 失败: FAILED + errorMessage
```

## 文件结构

```
work/
└── job_{id}/
    └── config.tcl  # 生成的 TCL 配置文件
```

## Docker 命令示例

```bash
# 拉取 OpenROAD 镜像
docker pull openroad/flow-ubuntu22.04

# 手动运行测试
docker run --rm \
  -v $(pwd)/work/job_1:/work \
  -w /work \
  openroad/flow-ubuntu22.04 \
  tclsh config.tcl
```

## 故障排查

### 问题 1: Docker 不可用
```bash
# 检查 Docker 是否安装
docker --version

# 检查 Docker 服务状态
systemctl status docker

# 启动 Docker 服务
systemctl start docker
```

### 问题 2: tclsh 不可用（本地模式）
```bash
# 安装 tcl
apt-get update && apt-get install -y tcl

# 验证安装
tclsh --version
```

### 问题 3: 任务一直处于 RUNNING 状态
- 检查日志: `docker logs silicon-app`
- 检查超时设置: `openroad.timeout.minutes`
- 检查工作目录权限: `work/job_{id}/`

### 问题 4: 无法解析结果指标
- 检查 TCL 脚本输出格式
- 确保日志包含: "Total Area: xxx" 和 "Total Power: xxx"
- 查看 LogParser 正则表达式是否匹配

## 扩展建议

1. **消息队列集成**: 使用 RabbitMQ 或 Kafka 替代 @Async
2. **分布式执行**: 支持多个 Worker 节点
3. **任务优先级**: 添加优先级队列
4. **结果缓存**: 相同参数的任务复用结果
5. **实时进度**: WebSocket 推送执行进度
6. **资源限制**: Docker 容器资源限制（CPU、内存）
7. **任务重试**: 失败任务自动重试机制
8. **监控告警**: Prometheus + Grafana 监控

## 参考资料

- OpenROAD 官网: https://theopenroadproject.org/
- OpenROAD GitHub: https://github.com/The-OpenROAD-Project/OpenROAD
- Docker Hub: https://hub.docker.com/r/openroad/flow-ubuntu22.04
