package com.mmyf.mcp.config.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时校验已发布 MCP 认证相关配置。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@Slf4j
public class McpPublishedAuthStartupValidator implements ApplicationRunner {

    private final McpPublishedAuthProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        properties.validateOrThrow();
        log.info("mcp.published-auth 配置校验通过: mode={}, oauthEnabled={}",
                properties.getMode(), properties.getOauthServer().isEnabled());
    }
}
