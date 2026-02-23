# Silicon Agent Flow - 快速使用指南

## 🚀 快速启动

### 使用启动脚本（推荐）

```bash
# 进入项目目录
cd /workspace/Silicon-Agent-Flow

# 查看帮助
./start.sh help

# 启动应用
./start.sh start

# 查看状态
./start.sh status

# 查看日志
./start.sh logs

# 实时查看日志
./start.sh logs -f

# 停止应用
./start.sh stop

# 重启应用
./start.sh restart

# 编译应用
./start.sh build
```

## 📋 完整功能清单

### ✅ 已实现功能

1. **数据库迁移**
   - ✅ 从 MySQL 迁移到 H2 内存数据库
   - ✅ 自动创建表结构
   - ✅ 支持 H2 控制台访问

2. **ORFS 真实执行**
   - ✅ Docker 容器化执行
   - ✅ 实时日志流式输出
   - ✅ 环境变量动态注入
   - ✅ 指标自动解析（面积、功耗、时序）

3. **异步任务处理**
   - ✅ 通过自注入解决 Spring AOP 限制
   - ✅ API 快速响应（0.065 秒）
   - ✅ 后台异步执行任务

4. **前端页面优化**
   - ✅ Cyberpunk 风格界面
   - ✅ 实时任务状态显示
   - ✅ AI 优化信息展示
   - ✅ 优化趋势图表
   - ✅ 执行结果指标展示

5. **服务管理**
   - ✅ 统一启动脚本
   - ✅ 进程管理
   - ✅ 日志查看
   - ✅ 健康检查

### ⚠️ 待配置功能

**AI 自动优化**（需要 OpenAI API Key）
- 当前状态：已实现但未配置
- 配置方法：见下文"启用 AI 优化"

## 🌐 访问地址

- **前端页面**: http://localhost:8080
- **H2 控制台**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:silicondb`
  - Username: `sa`
  - Password: (留空)

## 📊 API 端点

### 任务管理

```bash
# 提交任务（不启用 AI 优化）
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "parameters": {
      "design_config": "designs/sky130hd/gcd/config.mk",
      "CORE_UTILIZATION": 65,
      "PLACE_DENSITY": 0.70,
      "CORE_ASPECT_RATIO": 1.0
    },
    "autoOptimize": false
  }'

# 提交任务（启用 AI 优化）
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

# 查看所有任务
curl http://localhost:8080/api/jobs

# 查看特定任务
curl http://localhost:8080/api/jobs/1

# 检查 ORFS 配置
curl http://localhost:8080/api/jobs/orfs/config
```

### 可用设计

- `designs/sky130hd/gcd/config.mk` - GCD (Greatest Common Divisor)
- `designs/sky130hd/aes/config.mk` - AES (Advanced Encryption)
- `designs/sky130hd/ibex/config.mk` - IBEX (RISC-V Core)
- `designs/sky130hd/jpeg/config.mk` - JPEG Encoder

## 🤖 启用 AI 优化

### 步骤 1：配置 OpenAI API Key

编辑 `src/main/resources/application.yml`，添加：

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

### 步骤 2：移除 OpenAI 排除

编辑 `src/main/java/com/silicon/agentflow/SiliconAgentFlowApplication.java`：

```java
@SpringBootApplication  // 移除 exclude 配置
public class SiliconAgentFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiliconAgentFlowApplication.class, args);
    }
}
```

### 步骤 3：重新编译和启动

```bash
./start.sh stop
./start.sh build
./start.sh start
```

## 📈 前端功能说明

### 任务列表显示

每个任务卡片显示：
- **任务 ID**: #1, #2, #3...
- **状态**: PENDING, RUNNING, COMPLETED, FAILED
- **AI 优化标记**: 🤖 AI Opt: Iter X ← #Y
- **执行结果**: Area: XXX μm², Slack: XXX ns

### 统计卡片

- **Total Missions**: 总任务数
- **Running**: 运行中的任务
- **Completed**: 已完成的任务
- **AI Optimized**: 启用 AI 优化的任务数
- **Failed**: 失败的任务

### 优化趋势图表

自动绘制所有已完成任务的面积变化趋势，可视化展示优化效果。

## 🔧 故障排查

### 应用无法启动

```bash
# 检查端口占用
./start.sh status

# 如果端口被占用，停止旧进程
./start.sh stop

# 重新启动
./start.sh start
```

### 查看详细日志

```bash
# 查看最近 100 行日志
./start.sh logs 100

# 实时查看日志
./start.sh logs -f
```

### ORFS 执行失败

```bash
# 检查 ORFS 配置
curl http://localhost:8080/api/jobs/orfs/config

# 检查 Docker 是否运行
docker ps

# 检查 ORFS 镜像
docker images | grep openroad

# 检查工作空间
ls -la /workspace/OpenROAD-flow-scripts/flow/
```

## 📚 相关文档

- **ORFS 设置指南**: `ORFS_SETUP.md`
- **前端使用指南**: `FRONTEND_GUIDE.md`
- **AI 优化展示**: `AI_OPTIMIZATION_DISPLAY.md`
- **启动问题修复**: `STARTUP_FIX.md`
- **最终总结**: `FINAL_SUMMARY.md`

## 🎯 典型使用流程

### 1. 启动应用

```bash
./start.sh start
```

### 2. 访问前端页面

打开浏览器访问: http://localhost:8080

### 3. 提交任务

- 选择设计配置（GCD/AES/IBEX/JPEG）
- 调整参数（CORE_UTILIZATION, PLACE_DENSITY 等）
- 可选：勾选"启用 AI 自动优化"
- 点击"LAUNCH MISSION"

### 4. 查看结果

- 任务列表实时更新状态
- 点击任务查看详细日志
- 查看优化趋势图表
- 查看执行结果指标

### 5. 停止应用

```bash
./start.sh stop
```

## 💡 提示

- 每个 ORFS 任务大约需要 10-15 分钟完成
- 启用 AI 优化后，系统会自动进行多轮优化（最多 10 轮）
- 可以同时提交多个任务，它们会并发执行
- 日志文件会持续增长，定期清理 `app.log`

## 🆘 获取帮助

```bash
# 查看启动脚本帮助
./start.sh help

# 查看应用状态
./start.sh status

# 查看实时日志
./start.sh logs -f
```

---

**版本**: v1.0
**更新时间**: 2026-02-23
**项目地址**: /workspace/Silicon-Agent-Flow
