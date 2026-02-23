# 前端页面使用指南

## 访问地址

启动应用后，访问：
```
http://localhost:8080
```

## 页面功能

### 1. ORFS 状态检查（左上角）
- **Docker 状态**：显示 Docker 是否可用
- **Workspace 状态**：显示 ORFS 工作空间是否有效
- 绿色 ✓ 表示正常，红色 ✗ 表示异常

### 2. 控制面板（左侧）

#### 设计配置
- **DESIGN CONFIG**：选择要运行的芯片设计
  - GCD：最大公约数电路（推荐用于测试）
  - AES：高级加密标准
  - IBEX：RISC-V 处理器核心
  - JPEG：JPEG 编码器

#### 参数配置
- **CORE UTILIZATION (%)**：核心利用率（10-90%）
  - 默认：65%
  - 建议：60-70% 之间

- **PLACE DENSITY**：布局密度（0.1-1.0）
  - 默认：0.70
  - 较高值会使布局更紧凑

- **CORE ASPECT RATIO**：核心长宽比（0.5-2.0）
  - 默认：1.0（正方形）
  - 1.0 表示正方形芯片

#### AI 自动优化
- 勾选后会启用 AI 优化功能
- 默认关闭（推荐先手动运行测试）

#### 启动按钮
- 点击 **🚀 START OPTIMIZATION MISSION** 提交任务
- 提交中会显示 **⏳ LAUNCHING...**

### 3. 任务列表（左下角）
- 显示最近 5 个任务
- 点击任务可在终端查看日志
- 状态标识：
  - 🟡 PENDING：等待执行
  - 🔵 RUNNING：正在运行
  - 🟢 COMPLETED：已完成
  - 🔴 FAILED：失败

### 4. 统计卡片（右上角）
- **Total Missions**：总任务数
- **Running**：运行中的任务
- **Completed**：已完成的任务
- **Failed**：失败的任务

### 5. 优化趋势图（右中）
- 显示芯片面积优化趋势
- X 轴：任务 ID
- Y 轴：面积（um²）

### 6. 实时终端（右下角）
- 显示系统日志和任务状态
- 绿色：成功信息
- 红色：错误信息
- 青色：时间戳
- 点击任务可查看详细日志

## 快速开始

### 1. 运行 GCD 测试任务

1. 确保 ORFS 状态显示为绿色 ✓
2. 保持默认参数：
   - DESIGN CONFIG: GCD
   - CORE UTILIZATION: 65
   - PLACE DENSITY: 0.70
   - CORE ASPECT RATIO: 1.0
3. 不勾选 AI AUTO-OPTIMIZE
4. 点击 **🚀 START OPTIMIZATION MISSION**
5. 在终端查看执行日志
6. 等待任务完成（状态变为 COMPLETED）

### 2. 查看任务结果

1. 在左侧任务列表中点击已完成的任务
2. 终端会显示详细日志
3. 查看统计卡片中的数据更新
4. 查看优化趋势图中的面积数据

### 3. 运行自定义参数

```
设计：IBEX (RISC-V Core)
核心利用率：70%
布局密度：0.75
长宽比：1.2
```

## 故障排查

### 问题 1：ORFS 状态显示红色 ✗

**Docker 不可用**
```bash
# 检查 Docker
docker ps

# 重启 Docker
sudo systemctl restart docker
```

**Workspace 无效**
```bash
# 检查工作空间
ls -la /workspace/OpenROAD-flow-scripts/flow/Makefile

# 修改配置
vim src/main/resources/application.yml
```

### 问题 2：任务提交失败

1. 检查后端服务是否运行
2. 查看浏览器控制台错误
3. 检查网络连接

### 问题 3：任务一直 RUNNING

- GCD 任务通常需要 5-15 分钟
- IBEX 等复杂设计可能需要 30-60 分钟
- 可以查看服务器日志：`docker-compose logs -f app`

### 问题 4：任务状态为 FAILED

1. 点击任务查看错误日志
2. 检查参数是否合理
3. 查看服务器日志文件：`cat workspaces/job-{id}/run.log`

## 技术细节

### API 端点
- `POST /api/jobs` - 提交任务
- `GET /api/jobs` - 获取所有任务
- `GET /api/jobs/{id}` - 获取特定任务
- `GET /api/jobs/orfs/config` - 获取 ORFS 配置

### 数据刷新
- 任务列表：每 3 秒自动刷新
- ORFS 配置：页面加载时检查一次
- 时钟：每秒更新

### 浏览器兼容性
- Chrome/Edge：完全支持
- Firefox：完全支持
- Safari：完全支持

## 高级功能

### 1. AI 自动优化
- 勾选 **🤖 AI AUTO-OPTIMIZE**
- 系统会自动调整参数进行多轮优化
- 查看优化趋势图了解改进效果

### 2. 批量任务
- 可以连续提交多个任务
- 系统会按顺序执行
- 在任务列表中查看所有任务状态

### 3. 日志分析
- 点击任务查看完整日志
- 红色行表示错误
- 绿色行表示成功步骤

## 性能优化建议

1. **首次运行**：使用 GCD 设计测试
2. **参数调整**：小幅度调整参数（±5%）
3. **并发任务**：避免同时运行多个大型设计
4. **日志清理**：定期清理 `workspaces/` 目录

## 示例参数组合

### 高性能设计
```
CORE_UTILIZATION: 60
PLACE_DENSITY: 0.65
CORE_ASPECT_RATIO: 1.0
```

### 高密度设计
```
CORE_UTILIZATION: 75
PLACE_DENSITY: 0.80
CORE_ASPECT_RATIO: 1.0
```

### 长条形芯片
```
CORE_UTILIZATION: 65
PLACE_DENSITY: 0.70
CORE_ASPECT_RATIO: 2.0
```

---

**提示**：首次使用建议先运行 GCD 测试设计，确保系统正常工作后再尝试其他设计。
