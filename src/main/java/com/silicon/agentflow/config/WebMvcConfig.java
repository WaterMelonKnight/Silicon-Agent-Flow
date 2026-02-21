package com.silicon.agentflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源映射
     * 将本地工作目录映射到 /files/** URL 路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录下的 workspaces 目录
        String workspacePath = Paths.get("workspaces").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/files/**")
                .addResourceLocations(workspacePath);
    }

    /**
     * 配置 Swagger/OpenAPI 文档
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Silicon-Agent-Flow API")
                        .version("1.0.0")
                        .description("AI-Driven Distributed EDA Scheduler API Documentation")
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
