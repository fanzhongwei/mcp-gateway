package com.mmyf.mcp.service.mcp.handler;

import com.mmyf.mcp.service.mcp.McpProtocolService;
import io.modelcontextprotocol.server.McpNotificationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * MCP通知处理器工厂
 *
 * @author Teddy
 * @date 2026-01-29
 */
@Component
@Slf4j
public class McpNotificationHandlers {

    @Autowired
    private McpProtocolService mcpProtocolService;

    /**
     * 创建Initialized通知处理器
     *
     * @param serviceId 服务ID
     */
    public McpNotificationHandler initializedHandler(String serviceId) {
        return (exchange, params) -> {
            log.debug("Handling initialized notification: serviceId={}", serviceId);
            // 从exchange中获取sessionId
            String sessionId = exchange.sessionId();
            mcpProtocolService.handleInitialized(serviceId, sessionId);
            return Mono.empty();
        };
    }
}
