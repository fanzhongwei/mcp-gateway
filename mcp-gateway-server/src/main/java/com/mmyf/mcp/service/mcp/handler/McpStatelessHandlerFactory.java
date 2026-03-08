package com.mmyf.mcp.service.mcp.handler;

import com.mmyf.mcp.model.entity.mcp.McpService;
import com.mmyf.mcp.service.mcp.McpProtocolService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessNotificationHandler;
import io.modelcontextprotocol.server.McpStatelessRequestHandler;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 无状态 MCP 服务端 Handler 工厂
 * 构建 McpStatelessServerHandler，各 handler 从 McpTransportContext 中读取 serviceId、service
 *
 * @author Teddy
 * @date 2026-01-29
 */
@Component
@Slf4j
public class McpStatelessHandlerFactory {

    private static final String CTX_SERVICE_ID = "serviceId";
    private static final String CTX_SERVICE = "service";

    @Autowired
    private McpProtocolService mcpProtocolService;

    /**
     * 创建无状态服务端 Handler（单例复用，从 context 读取 serviceId/service）
     */
    public McpStatelessServerHandler createHandler(McpJsonMapper jsonMapper) {
        Map<String, McpStatelessRequestHandler<?>> requestHandlers = new HashMap<>();
        requestHandlers.put(McpSchema.METHOD_INITIALIZE, initializeRequestHandler(jsonMapper));
        requestHandlers.put(McpSchema.METHOD_PING, pingRequestHandler());
        requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
        requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());

        Map<String, McpStatelessNotificationHandler> notificationHandlers = new HashMap<>();
        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED, initializedNotificationHandler());

        return new GatewayMcpStatelessServerHandler(requestHandlers, notificationHandlers);
    }

    private McpStatelessRequestHandler<McpSchema.InitializeResult> initializeRequestHandler(McpJsonMapper jsonMapper) {
        return (transportContext, params) -> {
            McpService service = (McpService) transportContext.get(CTX_SERVICE);
            String requestedProtocolVersion = transportContext.get("protocolVersion") != null
                    ? transportContext.get("protocolVersion").toString()
                    : null;

            McpSchema.InitializeRequest initializeRequest = jsonMapper.convertValue(params,
                    McpSchema.InitializeRequest.class);

            Map<String, Object> clientParams = new HashMap<>();
            if (initializeRequest.protocolVersion() != null) {
                clientParams.put("protocolVersion", initializeRequest.protocolVersion());
            }
            if (initializeRequest.capabilities() != null) {
                clientParams.put("capabilities", initializeRequest.capabilities());
            }
            if (initializeRequest.clientInfo() != null) {
                clientParams.put("clientInfo", initializeRequest.clientInfo());
            }
            if (initializeRequest.meta() != null) {
                clientParams.put("_meta", initializeRequest.meta());
            }

            String protocolVersion = initializeRequest.protocolVersion() != null
                    ? initializeRequest.protocolVersion()
                    : requestedProtocolVersion;

            McpSchema.InitializeResult result = mcpProtocolService.initialize(service, clientParams, protocolVersion);
            return Mono.just(result);
        };
    }

    private McpStatelessRequestHandler<Map<String, Object>> pingRequestHandler() {
        return (transportContext, params) -> {
            Map<String, Object> result = mcpProtocolService.ping();
            return Mono.just(result);
        };
    }

    private McpStatelessRequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
        return (transportContext, params) -> {
            String serviceId = (String) transportContext.get(CTX_SERVICE_ID);
            McpSchema.ListToolsResult result = mcpProtocolService.listTools(serviceId);
            return Mono.just(result);
        };
    }

    @SuppressWarnings("unchecked")
    private McpStatelessRequestHandler<McpSchema.CallToolResult> toolsCallRequestHandler() {
        return (transportContext, params) -> {
            String serviceId = (String) transportContext.get(CTX_SERVICE_ID);
            Map<String, Object> paramsMap = params instanceof Map ? (Map<String, Object>) params : new HashMap<>();
            McpSchema.CallToolResult result = mcpProtocolService.callTool(serviceId, paramsMap);
            return Mono.just(result);
        };
    }

    private McpStatelessNotificationHandler initializedNotificationHandler() {
        return (transportContext, params) -> {
            String serviceId = (String) transportContext.get(CTX_SERVICE_ID);
            log.debug("Handling initialized notification: serviceId={} (stateless, no session)", serviceId);
            mcpProtocolService.handleInitialized(serviceId, null);
            return Mono.empty();
        };
    }

    /**
     * 无状态服务端 Handler 实现，委托给 request/notification handlers
     */
    private static class GatewayMcpStatelessServerHandler implements McpStatelessServerHandler {

        private final Map<String, McpStatelessRequestHandler<?>> requestHandlers;
        private final Map<String, McpStatelessNotificationHandler> notificationHandlers;

        GatewayMcpStatelessServerHandler(
                Map<String, McpStatelessRequestHandler<?>> requestHandlers,
                Map<String, McpStatelessNotificationHandler> notificationHandlers) {
            this.requestHandlers = requestHandlers;
            this.notificationHandlers = notificationHandlers;
        }

        @Override
        public Mono<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext transportContext,
                McpSchema.JSONRPCRequest request) {
            McpStatelessRequestHandler<?> requestHandler = requestHandlers.get(request.method());
            if (requestHandler == null) {
                return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                        new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
                                "Missing handler for request type: " + request.method(), null)));
            }
            return requestHandler.handle(transportContext, request.params())
                    .map(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), result, null))
                    .onErrorResume(t -> {
                        McpSchema.JSONRPCResponse.JSONRPCError error;
                        if (t instanceof McpError mcpError && mcpError.getJsonRpcError() != null) {
                            error = mcpError.getJsonRpcError();
                        } else {
                            error = new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                                    t.getMessage(), null);
                        }
                        return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null, error));
                    });
        }

        @Override
        public Mono<Void> handleNotification(McpTransportContext transportContext,
                McpSchema.JSONRPCNotification notification) {
            McpStatelessNotificationHandler notificationHandler = notificationHandlers.get(notification.method());
            if (notificationHandler == null) {
                log.debug("Missing handler for notification type: {}", notification.method());
                return Mono.empty();
            }
            return notificationHandler.handle(transportContext, notification.params());
        }
    }
}
