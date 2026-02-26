package com.silicon.agentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenROAD Flow Scripts 真实执行服务
 * 负责调用真实的 ORFS Docker 容器执行 EDA 任务
 */
@Slf4j
@Service
public class OrfsExecutorService {

    @Value("${eda.orfs.workspace}")
    private String orfsWorkspace;

    @Value("${eda.orfs.docker.image:openroad/orfs:latest}")
    private String dockerImage;

    @Value("${eda.orfs.docker.enabled:true}")
    private boolean dockerEnabled;

    @Value("${eda.orfs.timeout.minutes:60}")
    private long timeoutMinutes;

    @Value("${eda.orfs.default-design:designs/sky130hd/gcd/config.mk}")
    private String defaultDesign;

    private static final String WORKSPACE_BASE = "workspaces";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行结果类
     */
    public static class OrfsExecutionResult {
        private final boolean success;
        private final int exitCode;
        private final String logFilePath;
        private final Map<String, Object> metrics;
        private final String errorMessage;

        public OrfsExecutionResult(boolean success, int exitCode, String logFilePath,
                                   Map<String, Object> metrics, String errorMessage) {
            this.success = success;
            this.exitCode = exitCode;
            this.logFilePath = logFilePath;
            this.metrics = metrics;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public int getExitCode() { return exitCode; }
        public String getLogFilePath() { return logFilePath; }
        public Map<String, Object> getMetrics() { return metrics; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 执行 OpenROAD Flow Scripts 任务
     */
    public OrfsExecutionResult executeJob(Long jobId, Map<String, Object> parameters) {
        log.info("Starting ORFS execution for job {}", jobId);

        if (!dockerEnabled) {
            log.warn("Docker execution is disabled in configuration");
            return new OrfsExecutionResult(false, -1, null, Map.of(),
                "Docker execution is disabled");
        }

        Path workDir = null;
        Path logFile = null;

        try {
            // 1. 创建工作目录
            workDir = createWorkDirectory(jobId);
            logFile = workDir.resolve("run.log");

            log.info("Created work directory: {}", workDir.toAbsolutePath());
            log.info("Log file: {}", logFile.toAbsolutePath());

            // 2. 构建 Docker 命令
            List<String> command = buildDockerCommand(parameters);
            log.info("Executing command: {}", String.join(" ", command));

            // 3. 创建进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // 4. 注入环境变量
            injectEnvironmentVariables(processBuilder, parameters);

            // 5. 启动进程
            Process process = processBuilder.start();

            // 6. 异步读取日志流（关键：避免阻塞）
            CompletableFuture<Void> stdoutFuture = readStreamAsync(
                process.getInputStream(), logFile, "STDOUT");
            CompletableFuture<Void> stderrFuture = readStreamAsync(
                process.getErrorStream(), logFile, "STDERR");

            // 7. 等待进程完成
            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                String errorMsg = "Process timed out after " + timeoutMinutes + " minutes";
                log.error(errorMsg);
                appendToLog(logFile, "\n[ERROR] " + errorMsg);
                return new OrfsExecutionResult(false, -1,
                    logFile.toAbsolutePath().toString(), Map.of(), errorMsg);
            }

            // 8. 等待日志流读取完成
            stdoutFuture.get(30, TimeUnit.SECONDS);
            stderrFuture.get(30, TimeUnit.SECONDS);

            int exitCode = process.exitValue();
            log.info("Process completed with exit code: {}", exitCode);

            // 9. 读取日志并解析指标
            String logContent = readLogFile(logFile);
            Map<String, Object> metrics = parseOrfsMetrics(logContent);

            // 10. 判断执行结果
            boolean success = (exitCode == 0);
            String errorMessage = success ? null : "Execution failed with exit code: " + exitCode;

            log.info("ORFS execution completed for job {}. Success: {}, Metrics: {}",
                jobId, success, metrics);

            return new OrfsExecutionResult(success, exitCode,
                logFile.toAbsolutePath().toString(), metrics, errorMessage);

        } catch (IOException e) {
            log.error("IOException during ORFS execution for job {}", jobId, e);
            String errorMsg = "IOException: " + e.getMessage();
            if (logFile != null) {
                appendToLog(logFile, "\n[ERROR] " + errorMsg);
            }
            return new OrfsExecutionResult(false, -1,
                logFile != null ? logFile.toAbsolutePath().toString() : null,
                Map.of(), errorMsg);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Execution interrupted for job {}", jobId, e);
            String errorMsg = "Execution interrupted: " + e.getMessage();
            if (logFile != null) {
                appendToLog(logFile, "\n[ERROR] " + errorMsg);
            }
            return new OrfsExecutionResult(false, -1,
                logFile != null ? logFile.toAbsolutePath().toString() : null,
                Map.of(), errorMsg);

        } catch (Exception e) {
            log.error("Unexpected error during ORFS execution for job {}", jobId, e);
            String errorMsg = "Unexpected error: " + e.getMessage();
            if (logFile != null) {
                appendToLog(logFile, "\n[ERROR] " + errorMsg);
            }
            return new OrfsExecutionResult(false, -1,
                logFile != null ? logFile.toAbsolutePath().toString() : null,
                Map.of(), errorMsg);
        }
    }

    /**
     * 创建工作目录
     */
    private Path createWorkDirectory(Long jobId) throws IOException {
        Path workDir = Paths.get(WORKSPACE_BASE, "job-" + jobId);
        Files.createDirectories(workDir);
        return workDir;
    }

    /**
     * 构建 Docker 命令
     */
    private List<String> buildDockerCommand(Map<String, Object> parameters) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");

        // 检查是否为自定义设计（用户上传的文件）
        boolean isCustomDesign = Boolean.TRUE.equals(parameters.get("custom_design"));
        String workspacePath = (String) parameters.get("workspace_path");

        if (isCustomDesign && workspacePath != null) {
            // 自定义设计模式：挂载 ORFS 和自定义工作区
            log.info("Using custom design mode with workspace: {}", workspacePath);

            // 挂载 OpenROAD-flow-scripts 根目录（不是 flow 子目录）
            command.add("-v");
            command.add(orfsWorkspace + ":/OpenROAD-flow-scripts/flow");

            // 挂载自定义工作区到容器内的 /workspace
            command.add("-v");
            Path absoluteWorkspace = Paths.get(workspacePath).toAbsolutePath();
            command.add(absoluteWorkspace.toString() + ":/workspace");

            // 设置工作目录为 flow
            command.add("-w");
            command.add("/OpenROAD-flow-scripts/flow");

            // 镜像名称
            command.add(dockerImage);

            // 执行 make 命令，指向容器内的自定义配置
            command.add("make");
            command.add("DESIGN_CONFIG=/workspace/config.mk");

        } else {
            // 内置示例模式：使用 ORFS 内置的设计
            log.info("Using built-in design mode");

            // 挂载 OpenROAD-flow-scripts/flow 目录
            command.add("-v");
            command.add(orfsWorkspace + ":/OpenROAD-flow-scripts/flow");

            // 设置工作目录为 flow
            command.add("-w");
            command.add("/OpenROAD-flow-scripts/flow");

            // 镜像名称
            command.add(dockerImage);

            // 执行 make 命令
            command.add("make");

            // 获取设计配置
            String designConfig = (String) parameters.getOrDefault("design_config", defaultDesign);
            command.add("DESIGN_CONFIG=" + designConfig);
        }

        return command;
    }

    /**
     * 注入环境变量到进程
     */
    private void injectEnvironmentVariables(ProcessBuilder processBuilder,
                                           Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            log.info("No parameters to inject as environment variables");
            return;
        }

        Map<String, String> env = processBuilder.environment();
        int injectedCount = 0;

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 跳过特殊参数
            if ("design_config".equals(key)) {
                continue;
            }

            // 转换为环境变量格式（大写，下划线分隔）
            String envKey = key.toUpperCase().replace("-", "_");
            String envValue = String.valueOf(value);

            env.put(envKey, envValue);
            injectedCount++;
            log.info("Injected env var: {}={}", envKey, envValue);
        }

        log.info("Total environment variables injected: {}", injectedCount);
    }

    /**
     * 异步读取流并写入日志文件（关键：避免阻塞）
     */
    private CompletableFuture<Void> readStreamAsync(InputStream inputStream,
                                                     Path logFile, String streamType) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 实时写入日志文件
                    appendToLog(logFile, "[" + streamType + "] " + line + "\n");
                    // 同时输出到控制台
                    log.debug("[{}] {}", streamType, line);
                }
            } catch (IOException e) {
                log.error("Error reading {} stream", streamType, e);
                appendToLog(logFile, "[ERROR] Failed to read " + streamType + ": " + e.getMessage() + "\n");
            }
        });
    }

    /**
     * 追加内容到日志文件
     */
    private void appendToLog(Path logFile, String content) {
        try {
            Files.writeString(logFile, content,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write to log file: {}", logFile, e);
        }
    }

    /**
     * 读取日志文件内容
     */
    private String readLogFile(Path logFile) {
        try {
            return Files.readString(logFile);
        } catch (IOException e) {
            log.error("Failed to read log file: {}", logFile, e);
            return "";
        }
    }

    /**
     * 解析 OpenROAD Flow Scripts 日志中的指标
     * 提取面积、功耗等关键指标
     */
    private Map<String, Object> parseOrfsMetrics(String logContent) {
        Map<String, Object> metrics = new HashMap<>();

        if (logContent == null || logContent.isEmpty()) {
            log.warn("Log content is empty, cannot parse metrics");
            return metrics;
        }

        try {
            // 正则表达式：匹配面积指标
            // 示例: "Design area 123.45 um^2" 或 "Total area: 123.45"
            Pattern areaPattern = Pattern.compile(
                "(?:Design|Total|Core)?\\s*[Aa]rea\\s*[:=]?\\s*([0-9]+\\.?[0-9]*)\\s*(?:um\\^?2|µm²)?",
                Pattern.CASE_INSENSITIVE
            );

            // 正则表达式：匹配功耗指标
            // 示例: "Total power: 1.234 mW" 或 "Power = 0.056 W"
            Pattern powerPattern = Pattern.compile(
                "(?:Total|Internal|Switching|Leakage)?\\s*[Pp]ower\\s*[:=]?\\s*([0-9]+\\.?[0-9]*)\\s*([mµ]?W)?",
                Pattern.CASE_INSENSITIVE
            );

            // 正则表达式：匹配时序指标
            // 示例: "slack (MET)" 或 "WNS: 0.123 ns"
            Pattern slackPattern = Pattern.compile(
                "(?:WNS|slack)\\s*[:=]?\\s*([\\-0-9]+\\.?[0-9]*)\\s*(?:ns)?",
                Pattern.CASE_INSENSITIVE
            );

            // 正则表达式：匹配利用率
            // 示例: "Core utilization: 65.5%"
            Pattern utilizationPattern = Pattern.compile(
                "(?:Core)?\\s*[Uu]tilization\\s*[:=]?\\s*([0-9]+\\.?[0-9]*)\\s*%?",
                Pattern.CASE_INSENSITIVE
            );

            // 提取面积
            Matcher areaMatcher = areaPattern.matcher(logContent);
            if (areaMatcher.find()) {
                try {
                    double area = Double.parseDouble(areaMatcher.group(1));
                    metrics.put("area_um2", area);
                    log.info("Extracted area: {} um²", area);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse area value: {}", areaMatcher.group(1));
                }
            }

            // 提取功耗
            Matcher powerMatcher = powerPattern.matcher(logContent);
            if (powerMatcher.find()) {
                try {
                    double power = Double.parseDouble(powerMatcher.group(1));
                    String unit = powerMatcher.group(2);
                    
                    // 统一转换为 mW
                    if (unit != null && unit.toLowerCase().contains("w") && !unit.toLowerCase().contains("m")) {
                        power = power * 1000; // W to mW
                    }
                    
                    metrics.put("power_mw", power);
                    log.info("Extracted power: {} mW", power);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse power value: {}", powerMatcher.group(1));
                }
            }

            // 提取时序裕量 (slack)
            Matcher slackMatcher = slackPattern.matcher(logContent);
            if (slackMatcher.find()) {
                try {
                    double slack = Double.parseDouble(slackMatcher.group(1));
                    metrics.put("slack_ns", slack);
                    metrics.put("timing_met", slack >= 0);
                    log.info("Extracted slack: {} ns (timing {})", slack, slack >= 0 ? "MET" : "VIOLATED");
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse slack value: {}", slackMatcher.group(1));
                }
            }

            // 提取利用率
            Matcher utilizationMatcher = utilizationPattern.matcher(logContent);
            if (utilizationMatcher.find()) {
                try {
                    double utilization = Double.parseDouble(utilizationMatcher.group(1));
                    metrics.put("utilization_percent", utilization);
                    log.info("Extracted utilization: {}%", utilization);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse utilization value: {}", utilizationMatcher.group(1));
                }
            }

            // 添加解析时间戳
            metrics.put("parsed_at", java.time.LocalDateTime.now().toString());

            // 检查是否成功提取到任何指标
            if (metrics.size() <= 1) { // 只有 parsed_at
                log.warn("No metrics extracted from log. Log might not contain expected patterns.");
            } else {
                log.info("Successfully parsed {} metrics from ORFS log", metrics.size() - 1);
            }

        } catch (Exception e) {
            log.error("Error parsing ORFS metrics", e);
            metrics.put("parse_error", e.getMessage());
        }

        return metrics;
    }

    /**
     * 检查 Docker 是否可用
     */
    public boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Docker is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 ORFS 工作空间是否存在
     */
    public boolean isOrfsWorkspaceValid() {
        try {
            Path workspace = Paths.get(orfsWorkspace);
            if (!Files.exists(workspace)) {
                log.error("ORFS workspace does not exist: {}", orfsWorkspace);
                return false;
            }
            
            if (!Files.isDirectory(workspace)) {
                log.error("ORFS workspace is not a directory: {}", orfsWorkspace);
                return false;
            }
            
            // 检查是否包含 Makefile
            Path makefile = workspace.resolve("Makefile");
            if (!Files.exists(makefile)) {
                log.warn("Makefile not found in ORFS workspace: {}", orfsWorkspace);
                return false;
            }
            
            log.info("ORFS workspace is valid: {}", orfsWorkspace);
            return true;
            
        } catch (Exception e) {
            log.error("Error validating ORFS workspace", e);
            return false;
        }
    }

    /**
     * 获取配置信息
     */
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("workspace", orfsWorkspace);
        config.put("docker_image", dockerImage);
        config.put("docker_enabled", dockerEnabled);
        config.put("timeout_minutes", timeoutMinutes);
        config.put("default_design", defaultDesign);
        config.put("docker_available", isDockerAvailable());
        config.put("workspace_valid", isOrfsWorkspaceValid());
        return config;
    }
}
