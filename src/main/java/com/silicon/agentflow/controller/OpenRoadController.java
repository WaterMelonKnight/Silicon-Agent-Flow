package com.silicon.agentflow.controller;

import com.silicon.agentflow.service.OpenRoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenROAD 测试控制器
 */
@RestController
@RequestMapping("/api/openroad")
@RequiredArgsConstructor
public class OpenRoadController {

    private final OpenRoadService openRoadService;

    /**
     * 检查 Docker 是否可用
     */
    @GetMapping("/docker/check")
    public ResponseEntity<Map<String, Object>> checkDocker() {
        boolean available = openRoadService.isDockerAvailable();
        Map<String, Object> response = new HashMap<>();
        response.put("docker_available", available);
        response.put("message", available ? "Docker is available" : "Docker is not available");
        return ResponseEntity.ok(response);
    }

    /**
     * 拉取 OpenROAD Docker 镜像
     */
    @PostMapping("/docker/pull")
    public ResponseEntity<Map<String, Object>> pullDockerImage() {
        boolean success = openRoadService.pullDockerImage();
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Image pulled successfully" : "Failed to pull image");
        return ResponseEntity.ok(response);
    }
}
