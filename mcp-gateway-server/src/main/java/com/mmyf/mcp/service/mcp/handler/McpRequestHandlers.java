package com.mmyf.mcp.service.mcp.handler;

import com.mmyf.mcp.service.mcp.McpProtocolService;
import io.modelcontextprotocol.server.McpRequestHandler;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP请求处理器工厂
 *
 * @author Teddy
 * @date 2026-01-29
 */
@Component
@Slf4j
public class McpRequestHandlers {

    @Autowired
    private McpProtocolService mcpProtocolService;

    /**
     * 创建Ping请求处理器
     */
    public McpRequestHandler<Map<String, Object>> pingHandler() {
        return (exchange, params) -> {
            log.debug("Handling ping request");
            Map<String, Object> result = mcpProtocolService.ping();
            return Mono.just(result);
        };
    }

    /**
     * 创建Tools/list请求处理器
     *
     * @param serviceId 服务ID
     */
    public McpRequestHandler<McpSchema.ListToolsResult> listToolsHandler(String serviceId) {
        return (exchange, params) -> {
            log.debug("Handling tools/list request: serviceId={}", serviceId);
            McpSchema.ListToolsResult result = mcpProtocolService.listTools(serviceId);
            return Mono.just(result);
        };
    }

    /**
     * 创建Tools/call请求处理器
     *
     * @param serviceId 服务ID
     */
    @SuppressWarnings("unchecked")
    public McpRequestHandler<McpSchema.CallToolResult> callToolHandler(String serviceId) {
        return (exchange, params) -> {
            log.debug("Handling tools/call request: serviceId={}, params={}", serviceId, params);
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            McpSchema.CallToolResult result = mcpProtocolService.callTool(serviceId, paramsMap);
            return Mono.just(result);
        };
    }
}
