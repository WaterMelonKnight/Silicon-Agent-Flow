package com.silicon.agentflow.service;

import com.silicon.agentflow.util.LogParser;
import com.silicon.agentflow.util.ProcessExecutor;
import com.silicon.agentflow.util.TclTemplateGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenROAD 集成服务
 * 负责调用 Docker 运行 OpenROAD 工具
 */
@Slf4j
@Service
public class OpenRoadService {

    private final OrfsExecutorService orfsExecutorService;

    @Value("${openroad.docker.image:openroad/flow-ubuntu22.04}")
    private String dockerImage;

    @Value("${openroad.docker.enabled:false}")
    private boolean dockerEnabled;

    @Value("${openroad.timeout.minutes:30}")
    private long timeoutMinutes;

    @Value("${eda.orfs.docker.enabled:true}")
    private boolean orfsEnabled;

    public OpenRoadService(OrfsExecutorService orfsExecutorService) {
        this.orfsExecutorService = orfsExecutorService;
    }

    /**
     * 执行结果
     */
    public static class ExecutionResult {
        private final boolean success;
        private final String log;
        private final Map<String, Object> metrics;
        private final String errorMessage;

        public ExecutionResult(boolean success, String log, Map<String, Object> metrics, String errorMessage) {
            this.success = success;
            this.log = log;
            this.metrics = metrics;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getLog() {
            return log;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 执行 OpenROAD 任务
     *
     * @param jobId 任务 ID
     * @param parameters 任务参数
     * @return 执行结果
     */
    public ExecutionResult executeJob(Long jobId, Map<String, Object> parameters) {
        log.info("Starting OpenROAD execution for job {}", jobId);

        try {
            // 优先使用 ORFS 执行器（真实的 OpenROAD Flow Scripts）
            if (orfsEnabled) {
                log.info("Using ORFS executor for job {}", jobId);
                OrfsExecutorService.OrfsExecutionResult orfsResult =
                    orfsExecutorService.executeJob(jobId, parameters);

                // 读取日志文件内容
                String logContent = "";
                if (orfsResult.getLogFilePath() != null) {
                    try {
                        logContent = java.nio.file.Files.readString(
                            java.nio.file.Paths.get(orfsResult.getLogFilePath()));
                    } catch (Exception e) {
                        log.warn("Failed to read log file: {}", orfsResult.getLogFilePath(), e);
                        logContent = "Log file: " + orfsResult.getLogFilePath();
                    }
                }

                return new ExecutionResult(
                    orfsResult.isSuccess(),
                    logContent,
                    orfsResult.getMetrics(),
                    orfsResult.getErrorMessage()
                );
            }

            // 回退到原有的 TCL 执行方式
            log.info("Using legacy TCL executor for job {}", jobId);

            // 1. 生成 TCL 配置文件
            Path tclFile = TclTemplateGenerator.generateTclConfig(jobId, parameters);
            log.info("Generated TCL config: {}", tclFile.toAbsolutePath());

            // 2. 执行 OpenROAD
            ProcessExecutor.ExecutionResult result;
            if (dockerEnabled) {
                result = executeWithDocker(jobId, tclFile);
            } else {
                result = executeWithTclsh(tclFile);
            }

            // 3. 解析日志
            String combinedLog = result.getCombinedOutput();
            Map<String, Object> metrics = LogParser.parseMetrics(result.getStdout());

            // 4. 检查执行状态
            if (!result.isSuccess()) {
                String errorMsg = "OpenROAD execution failed with exit code: " + result.getExitCode();
                log.error(errorMsg);
                return new ExecutionResult(false, combinedLog, metrics, errorMsg);
            }

            if (LogParser.hasErrors(result.getStdout())) {
                String errorMsg = "OpenROAD execution completed but log contains errors";
                log.warn(errorMsg);
                return new ExecutionResult(false, combinedLog, metrics, errorMsg);
            }

            log.info("OpenROAD execution completed successfully for job {}", jobId);
            return new ExecutionResult(true, combinedLog, metrics, null);

        } catch (Exception e) {
            log.error("Failed to execute OpenROAD for job {}", jobId, e);
            return new ExecutionResult(false, "", Map.of(), "Exception: " + e.getMessage());
        }
    }

    /**
     * 使用 Docker 执行 OpenROAD
     */
    private ProcessExecutor.ExecutionResult executeWithDocker(Long jobId, Path tclFile) {
        log.info("Executing OpenROAD with Docker");

        Path workDir = tclFile.getParent();
        String absoluteWorkDir = workDir.toAbsolutePath().toString();

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("-v");
        command.add(absoluteWorkDir + ":/work");
        command.add("-w");
        command.add("/work");
        command.add(dockerImage);
        command.add("tclsh");
        command.add("config.tcl");

        return ProcessExecutor.execute(command, timeoutMinutes);
    }

    /**
     * 使用本地 tclsh 执行（用于测试）
     */
    private ProcessExecutor.ExecutionResult executeWithTclsh(Path tclFile) {
        log.info("Executing TCL script with local tclsh");

        List<String> command = new ArrayList<>();
        command.add("tclsh");
        command.add(tclFile.toAbsolutePath().toString());

        return ProcessExecutor.execute(command, timeoutMinutes);
    }

    /**
     * 检查 Docker 是否可用
     */
    public boolean isDockerAvailable() {
        try {
            ProcessExecutor.ExecutionResult result = ProcessExecutor.execute(
                    List.of("docker", "--version"), 1);
            return result.isSuccess();
        } catch (Exception e) {
            log.warn("Docker is not available", e);
            return false;
        }
    }

    /**
     * 拉取 OpenROAD Docker 镜像
     */
    public boolean pullDockerImage() {
        log.info("Pulling OpenROAD Docker image: {}", dockerImage);
        try {
            ProcessExecutor.ExecutionResult result = ProcessExecutor.execute(
                    List.of("docker", "pull", dockerImage), 10);
            return result.isSuccess();
        } catch (Exception e) {
            log.error("Failed to pull Docker image", e);
            return false;
        }
    }
}
