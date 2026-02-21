package com.silicon.agentflow.controller;

import com.silicon.agentflow.entity.EdaJob;
import com.silicon.agentflow.service.EdaJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "Files", description = "File access APIs for job logs and results")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final EdaJobService edaJobService;

    @Operation(summary = "Get job log file", description = "Retrieve the log content for a specific job")
    @GetMapping("/job-{jobId}/run.log")
    public ResponseEntity<String> getJobLog(@PathVariable Long jobId) {
        Optional<EdaJob> jobOpt = edaJobService.getJobById(jobId);

        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EdaJob job = jobOpt.get();
        String logContent = job.getLogContent();

        if (logContent == null || logContent.isEmpty()) {
            logContent = "No log content available for job #" + jobId;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(logContent);
    }

    @Operation(summary = "Get job log as JSON", description = "Retrieve the log content as JSON for a specific job")
    @GetMapping("/api/jobs/{jobId}/log")
    public ResponseEntity<LogResponse> getJobLogJson(@PathVariable Long jobId) {
        Optional<EdaJob> jobOpt = edaJobService.getJobById(jobId);

        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EdaJob job = jobOpt.get();
        String logContent = job.getLogContent();

        if (logContent == null || logContent.isEmpty()) {
            logContent = "No log content available for job #" + jobId;
        }

        return ResponseEntity.ok(new LogResponse(jobId, logContent));
    }

    public record LogResponse(Long jobId, String logContent) {}
}
