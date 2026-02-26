package com.silicon.agentflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silicon.agentflow.dto.JobResponse;
import com.silicon.agentflow.dto.JobSubmitRequest;
import com.silicon.agentflow.entity.EdaJob;
import com.silicon.agentflow.service.EdaJobService;
import com.silicon.agentflow.service.OrfsExecutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "EDA Jobs", description = "EDA job management APIs")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class EdaJobController {

    private final EdaJobService edaJobService;
    private final OrfsExecutorService orfsExecutorService;
    private final ObjectMapper objectMapper;

    /**
     * 提交任务 - JSON 格式（兼容旧接口，使用内置示例）
     */
    @Operation(summary = "Submit a new EDA job", description = "Submit a new chip design job with optional AI auto-optimization")
    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@RequestBody JobSubmitRequest request) {
        EdaJob job = edaJobService.submitJobWithAutoOptimize(
            request.getParameters(),
            Boolean.TRUE.equals(request.getAutoOptimize())
        );
        return ResponseEntity.ok(JobResponse.from(job));
    }

    /**
     * 提交任务 - 文件上传格式（支持自定义 Verilog 设计）
     */
    @Operation(summary = "Submit a custom design job with file upload",
               description = "Upload custom Verilog and SDC files for chip design")
    @PostMapping("/upload")
    public ResponseEntity<JobResponse> submitJobWithFiles(
            @RequestPart("verilogFile") MultipartFile verilogFile,
            @RequestPart(value = "sdcFile", required = false) MultipartFile sdcFile,
            @RequestPart("parameters") String parametersJson) {

        try {
            log.info("Received file upload request: verilog={}, sdc={}, params={}",
                    verilogFile.getOriginalFilename(),
                    sdcFile != null ? sdcFile.getOriginalFilename() : "none",
                    parametersJson);

            // 解析参数 JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = objectMapper.readValue(parametersJson, Map.class);

            // 提取 autoOptimize 参数
            boolean autoOptimize = Boolean.TRUE.equals(parameters.get("autoOptimize"));
            parameters.remove("autoOptimize");

            // 调用 Service 处理文件上传任务
            EdaJob job = edaJobService.submitJobWithFiles(
                    verilogFile,
                    sdcFile,
                    parameters,
                    autoOptimize
            );

            return ResponseEntity.ok(JobResponse.from(job));

        } catch (Exception e) {
            log.error("Failed to submit job with files", e);
            return ResponseEntity.badRequest().build();
        }
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
