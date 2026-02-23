package com.silicon.agentflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silicon.agentflow.entity.EdaJob;
import com.silicon.agentflow.repository.EdaJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AI 优化服务 - 使用 LLM 分析 EDA 任务结果并提供优化建议
 */
@Slf4j
@Service
public class OptimizationService {

    private final DeepSeekClient deepSeekClient;
    private final EdaJobRepository edaJobRepository;
    private final EdaJobService edaJobService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.optimization.enabled:true}")
    private boolean optimizationEnabled;

    @Value("${ai.optimization.max-iterations:10}")
    private int maxIterations;

    public OptimizationService(DeepSeekClient deepSeekClient,
                               EdaJobRepository edaJobRepository,
                               @Lazy EdaJobService edaJobService) {
        this.deepSeekClient = deepSeekClient;
        this.edaJobRepository = edaJobRepository;
        this.edaJobService = edaJobService;
    }

    /**
     * System Prompt - 定义 AI Agent 的角色和任务
     */
    private static final String SYSTEM_PROMPT = """
            你是一个资深的芯片后端设计专家，精通数字 IC 设计流程和 EDA 工具优化。

            你的任务是分析芯片设计任务的执行结果，识别性能瓶颈，并提供优化建议。

            **分析重点**：
            1. 面积优化：降低芯片面积 (area_um2)
            2. 功耗优化：降低功耗 (power_mw)
            3. 时序优化：提高频率 (frequency_mhz)，确保时序收敛 (timing_met)
            4. 参数平衡：在面积、功耗、性能之间找到最佳平衡点

            **优化策略**：
            - utilization: 利用率 (40-90%)，过高会导致布线困难，过低浪费面积
            - aspect_ratio: 长宽比 (0.5-2.0)，影响布线和时序
            - core_margin: 核心边距 (1.0-5.0)，影响 IO 和电源网络
            - target_frequency: 目标频率，影响时序约束
            - power_budget: 功耗预算，影响优化目标

            **输出要求**：
            - 必须返回有效的 JSON 格式
            - 参数值必须在合理范围内
            - 提供简短的优化理由（optimization_reason 字段）
            """;

    /**
     * User Prompt 模板 - 使用 $variable$ 语法避免与 JSON 花括号冲突
     */
    private static final String USER_PROMPT_TEMPLATE = """
            ## 当前设计参数
            $parameters$

            ## 执行结果
            $results$

            ## 日志摘要
            $logSummary$

            ## 错误信息
            $errorInfo$

            ## 优化目标
            当前迭代次数：$iteration$/$maxIterations$
            主要目标：$optimizationGoal$

            请分析上述结果，找出性能瓶颈，并给出一组新的、更优的参数建议。

            返回格式要求：必须是有效的 JSON，包含以下字段：
            - CORE_UTILIZATION: 新的核心利用率 (40-90)
            - PLACE_DENSITY: 新的布局密度 (0.4-0.9)
            - CORE_ASPECT_RATIO: 新的长宽比 (0.5-2.0)
            - optimization_reason: 简短说明调整理由（中文，50字以内）
            """;

    /**
     * 异步执行优化流程
     */
    @Async
    @Transactional
    public void optimizeJobAsync(Long completedJobId) {
        if (!optimizationEnabled) {
            log.info("AI optimization is disabled");
            return;
        }

        try {
            Optional<EdaJob> jobOpt = edaJobRepository.findById(completedJobId);
            if (jobOpt.isEmpty()) {
                log.error("Job {} not found for optimization", completedJobId);
                return;
            }

            EdaJob completedJob = jobOpt.get();

            // 检查是否启用自动优化
            if (!Boolean.TRUE.equals(completedJob.getAutoOptimize())) {
                log.info("Auto-optimize is disabled for job {}", completedJobId);
                return;
            }

            // 检查迭代次数
            int currentIteration = completedJob.getOptimizationIteration();
            if (currentIteration >= maxIterations) {
                log.info("Max iterations ({}) reached for job {}", maxIterations, completedJobId);
                return;
            }

            // 只对成功的任务进行优化
            if (completedJob.getStatus() != EdaJob.JobStatus.COMPLETED) {
                log.warn("Job {} is not completed, skipping optimization", completedJobId);
                return;
            }

            log.info("Starting AI optimization for job {}, iteration {}/{}",
                    completedJobId, currentIteration + 1, maxIterations);

            // 调用 LLM 获取优化建议
            Map<String, Object> optimizedParams = getOptimizationSuggestion(completedJob);

            if (optimizedParams == null || optimizedParams.isEmpty()) {
                log.error("Failed to get optimization suggestion for job {}", completedJobId);
                return;
            }

            // 创建新的优化任务
            EdaJob newJob = new EdaJob();
            newJob.setParameters(optimizedParams);
            newJob.setStatus(EdaJob.JobStatus.PENDING);
            newJob.setAutoOptimize(true);
            newJob.setParentJobId(completedJobId);
            newJob.setOptimizationIteration(currentIteration + 1);
            newJob.setLogContent(String.format(
                    "Auto-optimization job (iteration %d/%d)\nParent job: %d\nOptimization reason: %s",
                    currentIteration + 1, maxIterations, completedJobId,
                    optimizedParams.getOrDefault("optimization_reason", "N/A")
            ));

            EdaJob savedJob = edaJobRepository.save(newJob);
            log.info("Created optimization job {} from parent job {}", savedJob.getId(), completedJobId);

            // 异步执行新任务
            edaJobService.executeJobAsync(savedJob.getId());

        } catch (Exception e) {
            log.error("Failed to optimize job {}", completedJobId, e);
        }
    }

    /**
     * 调用 LLM 获取优化建议
     */
    private Map<String, Object> getOptimizationSuggestion(EdaJob job) {
        try {
            // 准备输入数据
            String parametersJson = objectMapper.writeValueAsString(job.getParameters());
            String resultsJson = formatResults(job.getResultMetrics());
            String logSummary = extractLogSummary(job.getLogContent());
            String errorInfo = job.getErrorMessage() != null ? job.getErrorMessage() : "无错误";
            String optimizationGoal = determineOptimizationGoal(job.getResultMetrics());

            // 构建用户提示词（替换模板变量）
            String userPrompt = USER_PROMPT_TEMPLATE
                    .replace("$parameters$", parametersJson)
                    .replace("$results$", resultsJson)
                    .replace("$logSummary$", logSummary)
                    .replace("$errorInfo$", errorInfo)
                    .replace("$iteration$", String.valueOf(job.getOptimizationIteration() + 1))
                    .replace("$maxIterations$", String.valueOf(maxIterations))
                    .replace("$optimizationGoal$", optimizationGoal);

            // 调用 DeepSeek API
            log.debug("Calling DeepSeek API for optimization suggestion...");
            String response = deepSeekClient.chatCompletion(SYSTEM_PROMPT, userPrompt);

            log.debug("DeepSeek API response: {}", response);

            // 解析 JSON 响应
            return parseJsonResponse(response);

        } catch (Exception e) {
            log.error("Failed to get optimization suggestion", e);
            return null;
        }
    }

    /**
     * 格式化结果指标
     */
    private String formatResults(Map<String, Object> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "无结果数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**性能指标**：\n");
        metrics.forEach((key, value) ->
            sb.append(String.format("- %s: %s\n", key, value))
        );
        return sb.toString();
    }

    /**
     * 提取日志摘要（最后 500 字符）
     */
    private String extractLogSummary(String logContent) {
        if (logContent == null || logContent.isEmpty()) {
            return "无日志";
        }
        int length = logContent.length();
        int start = Math.max(0, length - 500);
        return logContent.substring(start);
    }

    /**
     * 根据当前结果确定优化目标
     */
    private String determineOptimizationGoal(Map<String, Object> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "减小芯片面积，降低功耗";
        }

        StringBuilder goal = new StringBuilder();

        // 检查面积
        Object area = metrics.get("area_um2");
        if (area != null) {
            double areaValue = ((Number) area).doubleValue();
            if (areaValue > 3000) {
                goal.append("减小芯片面积、");
            }
        }

        // 检查功耗
        Object power = metrics.get("power_mw");
        if (power != null) {
            double powerValue = ((Number) power).doubleValue();
            if (powerValue > 150) {
                goal.append("降低功耗、");
            }
        }

        // 检查时序
        Object timingMet = metrics.get("timing_met");
        if (timingMet != null && !((Boolean) timingMet)) {
            goal.append("改善时序收敛、");
        }

        if (goal.length() == 0) {
            return "进一步优化面积和功耗";
        }

        // 移除最后的顿号
        return goal.substring(0, goal.length() - 1);
    }

    /**
     * 解析 LLM 返回的 JSON 响应
     */
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            // 尝试提取 JSON 代码块
            String jsonContent = extractJsonFromMarkdown(response);

            // 解析 JSON
            return objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON response: {}", response, e);
            return null;
        }
    }

    /**
     * 从 Markdown 代码块中提取 JSON
     */
    private String extractJsonFromMarkdown(String text) {
        // 移除 markdown 代码块标记
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * 手动触发优化（用于 API 调用）
     */
    public Map<String, Object> manualOptimize(Long jobId) {
        Optional<EdaJob> jobOpt = edaJobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        EdaJob job = jobOpt.get();
        if (job.getStatus() != EdaJob.JobStatus.COMPLETED) {
            throw new IllegalStateException("Job is not completed: " + jobId);
        }

        return getOptimizationSuggestion(job);
    }
}
