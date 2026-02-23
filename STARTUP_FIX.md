# 应用启动问题修复记录

## 问题描述

应用启动时遇到 Spring Bean 初始化失败：

```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'openAiEmbeddingModel'
OpenAI API key must be set. Use the connection property: spring.ai.openai.api-key
```

## 根本原因

项目中包含了 Spring AI OpenAI 依赖和配置类 `AiConfig.java`，但没有配置 OpenAI API key。Spring Boot 自动配置尝试初始化 OpenAI 相关的 Bean，导致启动失败。

## 解决方案

在主应用类中排除 OpenAI 自动配置：

**文件**: `src/main/java/com/silicon/agentflow/SiliconAgentFlowApplication.java`

```java
@SpringBootApplication(exclude = {
    org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
})
public class SiliconAgentFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiliconAgentFlowApplication.class, args);
    }
}
```

## 修复步骤

1. 修改 `SiliconAgentFlowApplication.java` 添加 exclude 配置
2. 重新编译：`mvn clean package -DskipTests`
3. 停止旧进程：`pkill -f silicon-agent-flow-1.0.0.jar`
4. 启动新应用：`nohup java -jar target/silicon-agent-flow-1.0.0.jar > app.log 2>&1 &`

## 验证结果

✅ 应用成功启动在端口 8080
✅ H2 数据库初始化成功
✅ ORFS 配置检查通过：
   - Docker 可用
   - 工作空间有效
   - 所有配置正确

## 测试端点

```bash
# 检查 ORFS 配置
curl http://localhost:8080/api/jobs/orfs/config

# 访问前端页面
curl http://localhost:8080/

# 提交测试任务
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"parameters":{},"autoOptimize":false}'
```

## 注意事项

- OpenAI 相关功能（AiConfig.java）目前被禁用
- 如需使用 AI 优化功能，需要配置 OpenAI API key 并移除 exclude
- ORFS 核心功能不依赖 OpenAI，可以正常使用

---

**修复时间**: 2026-02-23 13:40
**状态**: ✅ 已解决
