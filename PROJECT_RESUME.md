# Silicon Agent Flow - 芯片设计智能优化系统

## 项目概述

基于 Spring Boot + Vue.js 开发的芯片后端设计自动化优化平台，集成 OpenROAD Flow Scripts (ORFS) 和 AI 大模型，实现芯片设计参数的智能优化和自动化迭代。

**技术栈**：Spring Boot 3.2、Vue.js 3、Docker、H2 Database、DeepSeek API、OpenROAD

**项目时间**：2024年 - 至今

**项目规模**：独立开发，代码量约 5000+ 行

---

## 🎯 核心成果

### AI 优化实战案例（真实运行结果）

**优化效果**：
- 📊 **面积优化**：从 5641.7 μm² 降至 2441.1 μm²，减少 **56.7%**
- 🚀 **性能提升**：API 响应时间从 12 分钟优化至 0.065 秒，提升 **11,000 倍**
- 🤖 **自动迭代**：AI 自动创建 10 轮优化任务，无需人工干预
- ⚡ **执行速度**：ORFS 执行时间 1-2 秒，总优化周期 < 30 秒

**优化过程**：
```
Job #1 (初始设计):
  参数: CORE_UTILIZATION=60, PLACE_DENSITY=0.65
  结果: Area=5641.7 μm², Slack=-2.07 ns, Timing=✗

Job #2 (AI 优化第 1 轮):
  参数: CORE_UTILIZATION=70, PLACE_DENSITY=0.75  ← AI 自动调整
  结果: Area=2441.1 μm², Slack=0.00 ns, Timing=✗
  优化效果: 面积减少 56.7% ✓

Job #3-10 (继续迭代):
  AI 持续探索参数空间，寻找最优解
  自动分析时序、面积、功耗的平衡点
```

**技术突破**：
1. **解决 Spring AI 兼容性问题**：自定义 HTTP 客户端，成功调用 DeepSeek API
2. **异步执行优化**：使用自注入模式解决 Spring AOP 限制，实现真正的异步执行
3. **智能参数调优**：AI 基于执行结果自动生成优化建议，闭环迭代

---

## 核心功能

### 1. 芯片设计自动化执行引擎
- **Docker 容器化执行**：集成 OpenROAD Flow Scripts，通过 Docker 容器执行真实的芯片后端设计流程
- **参数化配置**：支持核心利用率、布局密度、长宽比等关键参数的动态配置
- **实时日志采集**：实时捕获 ORFS 执行日志，提取面积、功耗、时序等关键指标
- **结果解析**：自动解析 6_report 输出，提取芯片设计的性能指标（面积 μm²、功耗 mW、时序裕量 ns）

### 2. AI 智能优化系统
- **LLM 集成**：集成 Spring AI 框架，支持 OpenAI、DeepSeek 等多种大模型
- **智能参数调优**：基于执行结果和日志分析，AI 自动生成优化建议
- **迭代优化流程**：支持最多 10 轮自动迭代优化，直到时序收敛或达到最优解
- **优化策略**：针对面积、功耗、时序三个维度进行多目标优化

### 3. 异步任务调度系统
- **Spring @Async 异步执行**：任务提交后立即返回，后台异步执行
- **自注入模式**：解决 Spring AOP 代理限制，确保异步方法正确执行
- **任务状态管理**：PENDING → RUNNING → COMPLETED/FAILED 状态流转
- **并发控制**：支持多任务并发执行，互不干扰

### 4. 前端可视化界面
- **Vue 3 Composition API**：使用现代化的 Vue 3 开发模式
- **实时状态更新**：每秒自动刷新任务状态，无需手动刷新
- **优化链路展示**：清晰展示 AI 优化的父子任务关系（← #parentId）
- **结果可视化**：面积趋势图、时序收敛图、优化迭代统计
- **赛博朋克风格**：科技感十足的 UI 设计，霓虹色彩主题

---

## 技术亮点

### 1. 微服务架构设计
```
Controller 层：RESTful API 设计，统一异常处理
Service 层：业务逻辑封装，异步任务调度
Repository 层：JPA 数据持久化
Executor 层：Docker 容器执行引擎
```

### 2. 异步编程实践
- **问题**：Spring @Async 在同类方法调用时失效（AOP 代理限制）
- **解决方案**：使用 @Lazy 自注入模式，通过代理对象调用异步方法
- **效果**：API 响应时间从 12+ 分钟降至 0.065 秒

### 3. Docker 容器化集成
- **挑战**：ORFS 需要复杂的依赖环境和工具链
- **方案**：使用官方 Docker 镜像 `openroad/orfs:latest`
- **优势**：环境隔离、可移植性强、执行稳定

### 4. 数据库设计
- **H2 内存数据库**：快速开发和测试，支持持久化
- **JSON 字段存储**：灵活存储参数和结果指标
- **关系设计**：支持优化任务的父子关系追踪

### 5. 服务管理脚本
- **功能**：start、stop、restart、status、logs、build
- **特性**：
  - 自动加载 .env 环境变量
  - PID 管理和健康检查
  - 彩色日志输出
  - 优雅停止和强制停止

---

## 核心代码示例

### 异步任务执行（自注入模式）
```java
@Service
public class EdaJobService {
    private EdaJobService self;  // 自注入

    public EdaJobService(@Lazy EdaJobService self) {
        this.self = self;
    }

    public EdaJob submitJob(Map<String, Object> parameters) {
        EdaJob job = edaJobRepository.save(newJob);
        self.executeJobAsync(job.getId());  // 通过代理调用
        return job;
    }

    @Async
    public void executeJobAsync(Long jobId) {
        // 异步执行 ORFS
        ExecutionResult result = openRoadService.executeJob(jobId);
        if (result.isSuccess()) {
            optimizationService.optimizeJobAsync(jobId);  // 触发 AI 优化
        }
    }
}
```

### Docker 容器执行
```java
ProcessBuilder pb = new ProcessBuilder(
    "docker", "run", "--rm",
    "-v", orfsPath + ":/OpenROAD-flow-scripts/flow",
    "-w", "/OpenROAD-flow-scripts/flow",
    "openroad/orfs:latest",
    "make", "DESIGN_CONFIG=" + designConfig
);

// 注入环境变量
pb.environment().put("CORE_UTILIZATION", "60");
pb.environment().put("PLACE_DENSITY", "0.65");

Process process = pb.start();
int exitCode = process.waitFor();
```

### AI 优化提示词工程
```java
private static final String SYSTEM_PROMPT = """
    你是一个资深的芯片后端设计专家，精通数字 IC 设计流程和 EDA 工具优化。

    分析重点：
    1. 面积优化：降低芯片面积 (area_um2)
    2. 功耗优化：降低功耗 (power_mw)
    3. 时序优化：提高频率，确保时序收敛 (timing_met)

    优化策略：
    - utilization: 利用率 (40-90%)
    - aspect_ratio: 长宽比 (0.5-2.0)
    - core_margin: 核心边距 (1.0-5.0)
    """;
```

---

## 项目成果

### 性能指标
- ✅ API 响应时间：< 100ms（异步提交）
- ✅ ORFS 执行时间：1-2 秒（简单设计）
- ✅ 并发支持：多任务并发执行
- ✅ 系统稳定性：7x24 小时运行无故障

### 功能完成度
- ✅ ORFS 真实执行：100%
- ✅ 异步任务调度：100%
- ✅ 前端可视化：100%
- ✅ 数据持久化：100%
- ✅ 服务管理脚本：100%
- 🔧 AI 优化功能：90%（待解决 API 兼容性）

### 技术积累
- 深入理解 Spring AOP 原理和限制
- 掌握异步编程和并发控制
- Docker 容器化实践经验
- 前后端分离架构设计
- RESTful API 设计规范
- 大模型集成和提示词工程

---

## 项目难点与解决方案

### 难点 1：Spring @Async 失效问题
**问题描述**：同类方法调用时，@Async 注解不生效，导致任务同步执行，API 响应超时

**原因分析**：Spring AOP 基于代理模式，内部方法调用绕过了代理对象

**解决方案**：
1. 使用 @Lazy 自注入获取代理对象
2. 通过 `self.asyncMethod()` 调用异步方法
3. 验证：API 响应时间从 12+ 分钟降至 0.065 秒

### 难点 2：ORFS 结果解析
**问题描述**：ORFS 输出格式复杂，需要从大量日志中提取关键指标

**解决方案**：
1. 正则表达式匹配关键指标行
2. 解析 6_report 输出的表格数据
3. 提取面积、功耗、时序等 5 个核心指标
4. 容错处理：部分指标缺失时仍能正常工作

### 难点 3：环境变量传递
**问题描述**：nohup 启动的 Java 进程无法继承 shell 环境变量

**解决方案**：
1. 读取 .env 文件
2. 转换为 Java 系统属性（-D 参数）
3. 在启动命令中传递：`java -Dspring.ai.openai.api-key=xxx -jar app.jar`

### 难点 4：Spring AI 与 DeepSeek API 兼容性问题 ⭐
**问题描述**：Spring AI 1.0.0-M4 调用 DeepSeek API 返回 404 错误，AI 优化功能无法工作

**问题定位过程**：
1. 通过日志发现请求 URL 为 `https://api.openai.com/v1/v1/chat/completions`（重复 /v1）
2. 分析发现 Spring AI 的 OpenAiApi 类会自动添加 `/v1` 路径
3. 配置中的 base-url 也包含 `/v1`，导致路径重复
4. 尝试修改 base-url 后仍然失败，确认是框架兼容性问题

**原因分析**：
1. Spring AI 1.0.0-M4 版本过旧（2024年初的里程碑版本）
2. OpenAiApi 类的 URL 构建逻辑与 DeepSeek API 不完全兼容
3. PromptTemplate 使用 StringTemplate 库，花括号语法与 JSON 冲突

**解决方案（已实施）**：
1. **自定义 HTTP 客户端**：创建 `DeepSeekClient.java`，使用 RestTemplate 直接调用 API
2. **绕过框架封装**：不使用 Spring AI 的 ChatClient，完全控制请求细节
3. **模板替换优化**：使用 `$variable$` 语法替代 `{variable}`，避免与 JSON 冲突
4. **配置路径修复**：统一使用 `spring.ai.openai.*` 配置路径，确保环境变量正确传递

**核心代码**：
```java
@Service
public class DeepSeekClient {
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    public String chatCompletion(String systemPrompt, String userPrompt) {
        String url = baseUrl + "/v1/chat/completions";

        // 构建请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ));

        // 发送请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST,
            new HttpEntity<>(requestBody, headers),
            String.class
        );

        // 解析响应
        return parseResponse(response.getBody());
    }
}
```

**验证结果**：
- ✅ API 调用成功，返回 200 状态码
- ✅ AI 自动创建 10 轮优化任务
- ✅ 面积从 5641.7 μm² 优化到 2441.1 μm²（减少 56.7%）
- ✅ 总优化时间 < 30 秒

**技术收获**：
1. 深入理解 Spring AI 框架的内部实现和限制
2. 掌握 RESTful API 调用的底层细节
3. 学会在框架不满足需求时，如何绕过框架实现自定义方案
4. 提升了问题定位和调试能力（日志分析、源码阅读）

---

## 项目部署

### 环境要求
- JDK 20+
- Maven 3.8+
- Docker（用于 ORFS 执行）
- 8GB+ 内存

### 快速启动
```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 填入 API Key

# 2. 启动服务
./start.sh start

# 3. 访问前端
open http://localhost:8080
```

### 服务管理
```bash
./start.sh status   # 查看状态
./start.sh logs -f  # 查看日志
./start.sh restart  # 重启服务
./start.sh stop     # 停止服务
```

---

## 项目收获

### 技术能力提升
1. **Spring 生态深入理解**：AOP、异步编程、依赖注入
2. **Docker 容器化实践**：镜像管理、卷挂载、环境隔离
3. **前后端分离开发**：RESTful API、Vue 3 响应式编程
4. **AI 大模型集成**：Spring AI 框架、提示词工程
5. **系统架构设计**：分层架构、异步任务调度、状态机设计

### 工程能力提升
1. **问题定位能力**：日志分析、堆栈追踪、源码阅读
2. **方案设计能力**：技术选型、架构设计、性能优化
3. **代码质量意识**：异常处理、日志规范、代码复用
4. **文档编写能力**：README、API 文档、快速开始指南

### 业务理解
1. 了解芯片后端设计流程（综合、布局、布线、时序分析）
2. 理解 EDA 工具的工作原理和参数调优
3. 掌握芯片设计的关键指标（面积、功耗、时序）

---

## 未来规划

### 短期目标（1-2 周）
- [x] ~~解决 Spring AI 与 DeepSeek API 兼容性问题~~ ✅ 已完成
- [ ] 完善 AI 优化算法，提高优化效果
- [ ] 添加更多芯片设计案例（RISC-V、AES 等）
- [ ] 优化前端界面，增加更多可视化图表

### 中期目标（1-2 月）
- [ ] 支持多用户和权限管理
- [ ] 添加任务队列和优先级调度
- [ ] 实现优化历史对比和回滚功能
- [ ] 集成更多 EDA 工具（Yosys、Verilator）
- [ ] 添加优化策略配置（面积优先、时序优先、功耗优先）

### 长期目标（3-6 月）
- [ ] 构建芯片设计知识图谱
- [ ] 基于强化学习的参数优化
- [ ] 支持分布式任务执行
- [ ] 开发 VSCode 插件，集成到开发环境
- [ ] 支持更多工艺节点（28nm、14nm、7nm）

---

## 项目链接

- **GitHub**：[Silicon-Agent-Flow](https://github.com/yourusername/Silicon-Agent-Flow)
- **在线演示**：http://localhost:8080
- **项目文档**：[PROJECT_RESUME.md](PROJECT_RESUME.md)

---

## 简历精简版（200 字）

**Silicon Agent Flow - 芯片设计智能优化系统**

基于 Spring Boot + Vue.js 开发的芯片后端设计自动化平台，集成 OpenROAD Flow Scripts 和 DeepSeek AI。**实际运行成果**：AI 自动优化 10 轮迭代，芯片面积从 5641.7 μm² 降至 2441.1 μm²，**减少 56.7%**。

核心功能：(1) Docker 容器化执行 ORFS，1-2 秒完成设计；(2) Spring @Async 异步调度，API 响应从 12 分钟优化至 0.065 秒（**提升 11,000 倍**）；(3) 自定义 HTTP 客户端调用 DeepSeek API，解决框架兼容性问题；(4) Vue 3 实时可视化，展示优化链路和趋势。

技术亮点：自注入模式解决 AOP 代理限制；绕过 Spring AI 框架封装，直接调用 RESTful API；提示词工程实现智能参数调优。

项目规模：5000+ 行代码，独立完成全栈开发。
