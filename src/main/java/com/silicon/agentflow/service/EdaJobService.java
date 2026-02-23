package com.silicon.agentflow.service;

import com.silicon.agentflow.entity.EdaJob;
import com.silicon.agentflow.repository.EdaJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
