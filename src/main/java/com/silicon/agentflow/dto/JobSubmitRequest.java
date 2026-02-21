package com.silicon.agentflow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class JobSubmitRequest {
    private Map<String, Object> parameters;
    private Boolean autoOptimize = false;  // 是否启用自动优化
}
