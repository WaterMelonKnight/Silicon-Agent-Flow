package com.silicon.agentflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步配置
 * 启用 @Async 注解支持
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
