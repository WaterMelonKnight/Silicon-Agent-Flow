package com.silicon.agentflow.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.optimization.model:gpt-4}")
    private String model;

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi, OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(0.7)
                .withMaxTokens(2000)
                .build());
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
