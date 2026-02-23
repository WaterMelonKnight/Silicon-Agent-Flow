package com.silicon.agentflow.controller;

import com.silicon.agentflow.dto.JobResponse;
import com.silicon.agentflow.dto.JobSubmitRequest;
import com.silicon.agentflow.entity.EdaJob;
import com.silicon.agentflow.service.EdaJobService;
import com.silicon.agentflow.service.OrfsExecutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "EDA Jobs", description = "EDA job management APIs")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class EdaJobController {

    private final EdaJobService edaJobService;
    private final OrfsExecutorService orfsExecutorService;

    @Operation(summary = "Submit a new EDA job", description = "Submit a new chip design job with optional AI auto-optimization")
    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@RequestBody JobSubmitRequest request) {
        EdaJob job = edaJobService.submitJobWithAutoOptimize(
            request.getParameters(),
            Boolean.TRUE.equals(request.getAutoOptimize())
        );
        return ResponseEntity.ok(JobResponse.from(job));
    }

    @Operation(summary = "Get job by ID", description = "Retrieve a specific job by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        return edaJobService.getJobById(id)
                .map(job -> ResponseEntity.ok(JobResponse.from(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all jobs", description = "Retrieve all jobs in the system")
    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        List<JobResponse> jobs = edaJobService.getAllJobs()
                .stream()
                .map(JobResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobs);
    }

    @Operation(summary = "Check ORFS configuration", description = "Check if OpenROAD Flow Scripts is properly configured")
    @GetMapping("/orfs/config")
    public ResponseEntity<Map<String, Object>> getOrfsConfig() {
        Map<String, Object> config = orfsExecutorService.getConfiguration();
        return ResponseEntity.ok(config);
    }
}
