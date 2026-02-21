package com.silicon.agentflow.controller;

import com.silicon.agentflow.service.OptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 优化控制器
 */
@Tag(name = "AI Optimization", description = "AI-powered parameter optimization APIs")
@Slf4j
@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final OptimizationService optimizationService;

    /**
     * 手动触发优化
     *
     * @param jobId 已完成的任务 ID
     * @return 优化建议的参数
     */
    @Operation(summary = "Manually trigger optimization", description = "Analyze a completed job and generate optimization suggestions")
    @PostMapping("/manual/{jobId}")
    public ResponseEntity<Map<String, Object>> manualOptimize(@PathVariable Long jobId) {
        try {
            log.info("Manual optimization requested for job {}", jobId);
            Map<String, Object> suggestion = optimizationService.manualOptimize(jobId);

            if (suggestion == null || suggestion.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to generate optimization suggestion"
                ));
            }

            return ResponseEntity.ok(suggestion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to optimize job {}", jobId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Internal server error: " + e.getMessage()
            ));
        }
    }
}
