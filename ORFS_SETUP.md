# OpenROAD Flow Scripts 集成说明

## 概述

本项目已成功集成真实的 OpenROAD Flow Scripts (ORFS) Docker 执行引擎，可以运行真实的芯片设计任务。

## 已完成的修改

### 1. 配置文件修改 (application.yml)

添加了 ORFS 配置项：

```yaml
eda:
  orfs:
    workspace: /workspace/OpenROAD-flow-scripts  # ⚠️ 请修改为实际路径
    docker:
      image: openroad/orfs:latest
      enabled: true
    timeout:
      minutes: 60
    default-design: designs/sky130hd/gcd/config.mk
```

### 2. 新增服务类

#### OrfsExecutorService.java
- 真实的 Docker 调度逻辑
- 异步日志流处理（避免阻塞）
- 环境变量注入
- 指标解析（面积、功耗、时序等）
- 健壮的异常处理

#### 核心功能：
- ✅ 构建 Docker 命令挂载 ORFS 工作空间
- ✅ 动态注入环境变量（从 parameters 转换）
- ✅ 异步读取 stdout/stderr 并实时写入日志文件
- ✅ 解析 ORFS 输出提取关键指标
- ✅ 完整的错误处理（IOException, InterruptedException）

### 3. 修改现有服务

#### OpenRoadService.java
- 集成 OrfsExecutorService
- 优先使用 ORFS 执行器
- 保留原有 TCL 执行方式作为回退

#### EdaJobController.java
- 新增 `/api/jobs/orfs/config` 端点
- 用于检查 ORFS 配置状态

## 配置步骤

### 1. 修改工作空间路径

编辑 `src/main/resources/application.yml`：

```yaml
eda:
  orfs:
    workspace: /你的实际路径/OpenROAD-flow-scripts  # 修改这里
```

**重要提示：** 请将路径修改为你宿主机上 OpenROAD-flow-scripts 的实际绝对路径。

### 2. 验证 Docker 镜像

确保已拉取 ORFS 镜像：

```bash
docker images | grep openroad/orfs
```

如果没有，请拉取：

```bash
docker pull openroad/orfs:latest
```

### 3. 验证工作空间

确保工作空间包含 Makefile：

```bash
ls -la /你的路径/OpenROAD-flow-scripts/Makefile
```

## 使用方法

### 1. 启动应用

```bash
cd /workspace/Silicon-Agent-Flow
mvn clean package -DskipTests
java -jar target/silicon-agent-flow-1.0.0.jar
```

或使用 Docker Compose：

```bash
docker-compose up -d
```

### 2. 检查 ORFS 配置

访问健康检查端点：

```bash
curl http://localhost:8080/api/jobs/orfs/config
```

返回示例：

```json
{
  "workspace": "/workspace/OpenROAD-flow-scripts",
  "docker_image": "openroad/orfs:latest",
  "docker_enabled": true,
  "timeout_minutes": 60,
  "default_design": "designs/sky130hd/gcd/config.mk",
  "docker_available": true,
  "workspace_valid": true
}
```

### 3. 提交 EDA 任务

#### 运行 GCD 测试芯片（默认）

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {},
    "autoOptimize": false
  }'
```

#### 运行自定义设计

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_config": "designs/sky130hd/gcd/config.mk"
    },
    "autoOptimize": false
  }'
```

#### 注入环境变量参数

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_config": "designs/sky130hd/gcd/config.mk",
      "CORE_UTILIZATION": "65",
      "CORE_ASPECT_RATIO": "1",
      "PLACE_DENSITY": "0.70"
    },
    "autoOptimize": false
  }'
```

**说明：** parameters 中的参数会自动转换为环境变量注入到 Docker 容器中。
- `CORE_UTILIZATION` → 环境变量 `CORE_UTILIZATION=65`
- `design_config` → 作为 DESIGN_CONFIG 参数传递给 make

### 4. 查询任务状态

```bash
# 获取所有任务
curl http://localhost:8080/api/jobs

# 获取特定任务
curl http://localhost:8080/api/jobs/1
```

### 5. 查看日志文件

任务执行日志保存在：

```
workspaces/job-{id}/run.log
```

例如：

```bash
cat workspaces/job-1/run.log
```

## 执行流程

1. **接收任务** → EdaJobService.submitJob()
2. **异步执行** → EdaJobService.executeJobAsync()
3. **调用 ORFS** → OrfsExecutorService.executeJob()
4. **构建命令** → 生成 Docker 命令
5. **注入环境变量** → 从 parameters 转换
6. **启动容器** → ProcessBuilder.start()
7. **异步读取日志** → CompletableFuture 处理 stdout/stderr
8. **实时写入文件** → workspaces/job-{id}/run.log
9. **等待完成** → process.waitFor()
10. **解析指标** → 提取面积、功耗、时序等
11. **更新数据库** → 保存结果到 EdaJob

## 日志文件格式

```
[STDOUT] make: Entering directory '/OpenROAD-flow-scripts'
[STDOUT] Running synthesis...
[STDOUT] Design area 1234.56 um^2
[STDOUT] Total power: 0.567 mW
[STDERR] Warning: some timing paths failed
[ERROR] Process timed out after 60 minutes
```

## 指标解析

OrfsExecutorService 会自动从日志中提取以下指标：

- **area_um2**: 芯片面积（平方微米）
- **power_mw**: 功耗（毫瓦）
- **slack_ns**: 时序裕量（纳秒）
- **timing_met**: 时序是否满足（布尔值）
- **utilization_percent**: 核心利用率（百分比）

解析使用正则表达式匹配常见的 ORFS 输出格式。

## 故障排查

### 问题 1: Docker 不可用

**症状：** `docker_available: false`

**解决：**
```bash
# 检查 Docker 是否运行
docker ps

# 检查 Docker 权限
sudo usermod -aG docker $USER
```

### 问题 2: 工作空间无效

**症状：** `workspace_valid: false`

**解决：**
1. 检查路径是否正确
2. 确保 Makefile 存在
3. 检查文件权限

```bash
ls -la /你的路径/OpenROAD-flow-scripts/
```

### 问题 3: 任务超时

**症状：** 任务状态为 FAILED，日志显示 "Process timed out"

**解决：**
增加超时时间（application.yml）：

```yaml
eda:
  orfs:
    timeout:
      minutes: 120  # 增加到 2 小时
```

### 问题 4: 找不到设计配置

**症状：** make 报错 "No such file or directory"

**解决：**
检查设计配置路径是否正确：

```bash
ls /你的路径/OpenROAD-flow-scripts/designs/sky130hd/gcd/config.mk
```

## API 文档

启动应用后访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

## 数据库

使用 H2 内存数据库，访问控制台：

```
http://localhost:8080/h2-console
```

连接信息：
- JDBC URL: `jdbc:h2:mem:silicondb`
- Username: `sa`
- Password: (留空)

## 下一步

1. 修改 `application.yml` 中的 `eda.orfs.workspace` 路径
2. 重新编译并启动应用
3. 访问 `/api/jobs/orfs/config` 验证配置
4. 提交测试任务运行 GCD 设计
5. 查看日志文件和解析的指标

## 技术亮点

✅ **异步日志处理** - 使用 CompletableFuture 避免阻塞
✅ **实时日志写入** - 边读取边写入文件
✅ **环境变量注入** - 动态参数传递
✅ **健壮异常处理** - IOException, InterruptedException 完整处理
✅ **指标自动解析** - 正则表达式提取关键数据
✅ **超时保护** - 防止任务无限运行
✅ **工作目录隔离** - 每个任务独立的日志目录

## 联系方式

如有问题，请查看日志文件或联系开发团队。
