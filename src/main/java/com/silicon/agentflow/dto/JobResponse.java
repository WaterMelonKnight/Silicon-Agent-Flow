package com.silicon.agentflow.dto;

import com.silicon.agentflow.entity.EdaJob;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class JobResponse {
    private Long id;
    private String status;
    private String logContent;
    private Map<String, Object> parameters;
    private Map<String, Object> resultMetrics;
    private Boolean autoOptimize;
    private Long parentJobId;
    private Integer optimizationIteration;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobResponse from(EdaJob job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setStatus(job.getStatus().name());
        response.setLogContent(job.getLogContent());
        response.setParameters(job.getParameters());
        response.setResultMetrics(job.getResultMetrics());
        response.setAutoOptimize(job.getAutoOptimize());
        response.setParentJobId(job.getParentJobId());
        response.setOptimizationIteration(job.getOptimizationIteration());
        response.setErrorMessage(job.getErrorMessage());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }
}
