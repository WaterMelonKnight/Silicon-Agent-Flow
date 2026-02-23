package com.silicon.agentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API 自定义客户端
 * 直接使用 RestTemplate 调用 DeepSeek API，绕过 Spring AI 的兼容性问题
 */
@Slf4j
@Service
public class DeepSeekClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key:${ai.openai.api-key}}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:${ai.openai.base-url}}")
    private String baseUrl;

    @Value("${ai.optimization.model:deepseek-chat}")
    private String model;

    public DeepSeekClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 DeepSeek Chat Completion API
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 响应内容
     */
    public String chatCompletion(String systemPrompt, String userPrompt) {
        try {
            // 构建请求 URL
            String url = baseUrl + "/v1/chat/completions";
            log.debug("Calling DeepSeek API: {}", url);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();

            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 创建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            log.debug("Request body: {}", objectMapper.writeValueAsString(requestBody));
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                log.debug("Response: ", responseBody);

                // 解析 JSON 响应
                JsonNode root = objectMapper.readTree(responseBody);
                String content = root.path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();

                log.info("DeepSeek API call successful, response length: {}", content.length());
                return content;
            } else {
                log.error("DeepSeek API returned non-200 status: {}", response.getStatusCode());
                throw new RuntimeException("DeepSeek API call failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to call DeepSeek API", e);
            throw new RuntimeException("DeepSeek API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 测试 API 连接
     */
    public boolean testConnection() {
        try {
            String response = chatCompletion(
                    "You are a helpful assistant.",
                    "Say 'Hello' in one word."
            );
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.error("DeepSeek API connection test failed", e);
            return false;
        }
    }
}
