package com.mmyf.mcp.config.auth;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpPublishedAuthProperties.class)
public class McpPublishedAuthConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer mcpPublishedAuthJacksonCustomizer(McpPublishedAuthProperties properties) {
        return builder -> builder.modulesToInstall(new McpPublishedAuthJacksonModule(properties));
    }
}
