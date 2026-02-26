package com.silicon.agentflow.service;

import com.silicon.agentflow.entity.EdaJob;
import com.silicon.agentflow.repository.EdaJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class EdaJobService {

    private final EdaJobRepository edaJobRepository;
    private final OpenRoadService openRoadService;
    private final OptimizationService optimizationService;
    private EdaJobService self; // 自注入，用于调用 @Async 方法

    // 工作区根目录
    private static final String WORKSPACE_ROOT = "workspaces";

    public EdaJobService(EdaJobRepository edaJobRepository,
                         OpenRoadService openRoadService,
                         @Lazy OptimizationService optimizationService,
                         @Lazy EdaJobService self) {
        this.edaJobRepository = edaJobRepository;
        this.openRoadService = openRoadService;
        this.optimizationService = optimizationService;
        this.self = self;
    }

    @Transactional
    public EdaJob submitJob(Map<String, Object> parameters) {
        EdaJob job = new EdaJob();
        job.setStatus(EdaJob.JobStatus.PENDING);
        job.setParameters(parameters);
        job.setLogContent("Job submitted");

        EdaJob savedJob = edaJobRepository.save(job);
        log.info("Job submitted with ID: {}", savedJob.getId());

        // 通过代理调用异步方法
        self.executeJobAsync(savedJob.getId());

        return savedJob;
    }

    @Transactional
    public EdaJob submitJobWithAutoOptimize(Map<String, Object> parameters, boolean autoOptimize) {
        EdaJob job = new EdaJob();
        job.setStatus(EdaJob.JobStatus.PENDING);
        job.setParameters(parameters);
        job.setAutoOptimize(autoOptimize);
        job.setOptimizationIteration(0);
        job.setLogContent(autoOptimize ?
            "Job submitted with auto-optimization enabled" :
            "Job submitted");

        EdaJob savedJob = edaJobRepository.save(job);
        log.info("Job submitted with ID: {}, auto-optimize: {}", savedJob.getId(), autoOptimize);

        // 通过代理调用异步方法
        self.executeJobAsync(savedJob.getId());

        return savedJob;
    }

    /**
     * 提交带文件上传的任务（支持自定义 Verilog 设计）
     */
    @Transactional
    public EdaJob submitJobWithFiles(
            MultipartFile verilogFile,
            MultipartFile sdcFile,
            Map<String, Object> parameters,
            boolean autoOptimize) throws IOException {

        // 创建任务记录
        EdaJob job = new EdaJob();
        job.setStatus(EdaJob.JobStatus.PENDING);
        job.setParameters(parameters);
        job.setAutoOptimize(autoOptimize);
        job.setOptimizationIteration(0);
        job.setLogContent("Job submitted with custom design files");

        // 保存任务，获取 ID
        EdaJob savedJob = edaJobRepository.save(job);
        Long jobId = savedJob.getId();

        log.info("Job {} submitted with files: verilog={}, sdc={}",
                jobId,
                verilogFile.getOriginalFilename(),
                sdcFile != null ? sdcFile.getOriginalFilename() : "none");

        try {
            // 创建沙箱工作区
            Path jobWorkspace = createJobWorkspace(jobId);
            Path srcDir = jobWorkspace.resolve("src");
            Files.createDirectories(srcDir);

            // 保存 Verilog 文件
            Path verilogPath = srcDir.resolve("design.v");
            Files.copy(verilogFile.getInputStream(), verilogPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved Verilog file to: {}", verilogPath);

            // 保存 SDC 文件（如果提供）
            if (sdcFile != null && !sdcFile.isEmpty()) {
                Path sdcPath = srcDir.resolve("constraint.sdc");
                Files.copy(sdcFile.getInputStream(), sdcPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Saved SDC file to: {}", sdcPath);
            }

            // 生成 config.mk 文件
            generateConfigMk(jobWorkspace, parameters, sdcFile != null);

            // 更新任务参数，添加工作区路径
            parameters.put("workspace_path", jobWorkspace.toAbsolutePath().toString());
            parameters.put("custom_design", true);
            savedJob.setParameters(parameters);
            edaJobRepository.save(savedJob);

            // 通过代理调用异步方法
            self.executeJobAsync(jobId);

            return savedJob;

        } catch (IOException e) {
            log.error("Failed to save files for job {}", jobId, e);
            // 更新任务状态为失败
            savedJob.setStatus(EdaJob.JobStatus.FAILED);
            savedJob.setErrorMessage("Failed to save uploaded files: " + e.getMessage());
            edaJobRepository.save(savedJob);
            throw e;
        }
    }

    /**
     * 创建任务工作区
     */
    private Path createJobWorkspace(Long jobId) throws IOException {
        Path workspacePath = Paths.get(WORKSPACE_ROOT, "job-" + jobId);
        Files.createDirectories(workspacePath);
        log.info("Created workspace for job {}: {}", jobId, workspacePath.toAbsolutePath());
        return workspacePath;
    }

    /**
     * 生成 config.mk 配置文件
     */
    private void generateConfigMk(Path jobWorkspace, Map<String, Object> parameters, boolean hasSdc) throws IOException {
        // 提取参数
        String designName = (String) parameters.getOrDefault("design_name", "custom_design");
        String platform = (String) parameters.getOrDefault("platform", "sky130hd");
        Object utilizationObj = parameters.get("CORE_UTILIZATION");
        double utilization = utilizationObj != null ?
                (utilizationObj instanceof Integer ? ((Integer) utilizationObj).doubleValue() : (Double) utilizationObj) / 100.0 :
                0.6;

        // 构建 config.mk 内容
        StringBuilder configContent = new StringBuilder();
        configContent.append("# Auto-generated config.mk for custom design\n");
        configContent.append("export DESIGN_NAME = ").append(designName).append("\n");
        configContent.append("export PLATFORM = ").append(platform).append("\n");
        configContent.append("export VERILOG_FILES = $(DESIGN_CONFIG)/src/design.v\n");

        if (hasSdc) {
            configContent.append("export SDC_FILE = $(DESIGN_CONFIG)/src/constraint.sdc\n");
        }

        configContent.append("export CORE_UTILIZATION = ").append(String.format("%.2f", utilization)).append("\n");

        // 添加其他可选参数
        if (parameters.containsKey("PLACE_DENSITY")) {
            configContent.append("export PLACE_DENSITY = ").append(parameters.get("PLACE_DENSITY")).append("\n");
        }
        if (parameters.containsKey("CORE_ASPECT_RATIO")) {
            configContent.append("export CORE_ASPECT_RATIO = ").append(parameters.get("CORE_ASPECT_RATIO")).append("\n");
        }

        // 写入文件
        Path configPath = jobWorkspace.resolve("config.mk");
        Files.writeString(configPath, configContent.toString());
        log.info("Generated config.mk for job at: {}", configPath);
    }

    /**
     * 异步执行 EDA 任务
     */
    @Async
    public void executeJobAsync(Long jobId) {
        log.info("Starting async execution for job {}", jobId);

        try {
            // 更新状态为 RUNNING
            updateJobStatus(jobId, EdaJob.JobStatus.RUNNING, "Starting OpenROAD execution...");

            // 获取任务参数
            Optional<EdaJob> jobOpt = edaJobRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                log.error("Job {} not found", jobId);
                return;
            }

            EdaJob job = jobOpt.get();
            Map<String, Object> parameters = job.getParameters();

            // 执行 OpenROAD
            OpenRoadService.ExecutionResult result = openRoadService.executeJob(jobId, parameters);

            // 更新任务结果
            if (result.isSuccess()) {
                updateJobResult(jobId, result.getMetrics(), result.getLog());

                // 触发 AI 优化（如果启用）
                optimizationService.optimizeJobAsync(jobId);
            } else {
                updateJobFailure(jobId, result.getLog(), result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Failed to execute job {}", jobId, e);
            updateJobFailure(jobId, "", "Exception: " + e.getMessage());
        }
    }

    public Optional<EdaJob> getJobById(Long id) {
        return edaJobRepository.findById(id);
    }

    public List<EdaJob> getAllJobs() {
        return edaJobRepository.findAll();
    }

    @Transactional
    public void updateJobStatus(Long id, EdaJob.JobStatus status, String logContent) {
        edaJobRepository.findById(id).ifPresent(job -> {
            job.setStatus(status);
            if (logContent != null) {
                job.setLogContent(job.getLogContent() + "\n" + logContent);
            }
            edaJobRepository.save(job);
            log.info("Job  status updated to {}", id, status);
        });
    }

    @Transactional
    public void updateJobResult(Long id, Map<String, Object> resultMetrics, String executionLog) {
        edaJobRepository.findById(id).ifPresent(job -> {
            job.setResultMetrics(resultMetrics);
            job.setStatus(EdaJob.JobStatus.COMPLETED);
            job.setLogContent(job.getLogContent() + "\n\n" + executionLog + "\n\nJob completed successfully");
            edaJobRepository.save(job);
            log.info("Job {} completed with results: {}", id, resultMetrics);
        });
    }

    @Transactional
    public void updateJobFailure(Long id, String executionLog, String errorMessage) {
        edaJobRepository.findById(id).ifPresent(job -> {
            job.setStatus(EdaJob.JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            String failureLog = job.getLogContent() + "\n\n" + executionLog;
            if (errorMessage != null && !errorMessage.isEmpty()) {
                failureLog += "\n\nError: " + errorMessage;
            }
            job.setLogContent(failureLog);
            edaJobRepository.save(job);
            log.error("Job {} failed: {}", id, errorMessage);
        });
    }
}
