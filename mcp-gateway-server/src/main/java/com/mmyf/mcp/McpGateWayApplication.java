package com.mmyf.mcp;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * package com.mmyf.mcp
 * description: DbdOnlineApplication
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-07 23:52:12
 */
@SpringBootApplication(
        scanBasePackages = {"com.mmyf"},
        exclude = {
//                FlywayAutoConfiguration.class
                RedissonAutoConfigurationV2.class
                // 如果 Spring AI MCP 的自动配置与我们的实现冲突，可以在这里排除
                // org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration.class
        }
)
@EnableMethodCache(basePackages = "com.mmyf")
@EnableCreateCacheAnnotation
public class McpGateWayApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpGateWayApplication.class, args);
    }

}
