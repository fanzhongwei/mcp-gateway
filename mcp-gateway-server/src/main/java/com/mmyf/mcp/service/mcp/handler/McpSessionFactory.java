package com.mmyf.mcp.service.mcp.handler;

import com.mmyf.mcp.model.entity.mcp.McpService;
import io.modelcontextprotocol.server.McpInitRequestHandler;
import io.modelcontextprotocol.server.McpNotificationHandler;
import io.modelcontextprotocol.server.McpRequestHandler;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MCP Session工厂实现
 * 根据服务信息创建对应的session
 *
 * @author Teddy
 * @date 2026-01-29
 */
@Component
@Slf4j
public class McpSessionFactory {

    @Autowired
    private McpInitRequestHandlerImpl initRequestHandlerFactory;

    @Autowired
    private McpRequestHandlers requestHandlersFactory;

    @Autowired
    private McpNotificationHandlers notificationHandlersFactory;

    /**
     * 创建Session Factory
     *
     * @param serviceId 服务ID
     * @param service 服务对象
     * @param requestedProtocolVersion 请求的协议版本
     * @return Session Factory
     */
    public McpServerSession.Factory createFactory(String serviceId, McpService service, String requestedProtocolVersion) {
        // 为这个session创建handlers
        McpInitRequestHandler initHandler = initRequestHandlerFactory.create(service, requestedProtocolVersion);

        return transport -> {
            // 生成session ID
            String sessionId = UUID.randomUUID().toString();

            // 创建请求处理器映射
            Map<String, McpRequestHandler<?>> requestHandlerMap = new HashMap<>();
            requestHandlerMap.put("ping", requestHandlersFactory.pingHandler());
            requestHandlerMap.put("tools/list", requestHandlersFactory.listToolsHandler(serviceId));
            requestHandlerMap.put("tools/call", requestHandlersFactory.callToolHandler(serviceId));

            // 创建通知处理器映射
            Map<String, McpNotificationHandler> notificationHandlerMap = new HashMap<>();
            notificationHandlerMap.put("notifications/initialized", notificationHandlersFactory.initializedHandler(serviceId));

            // 创建session
            return new McpServerSession(
                sessionId,
                Duration.ofMinutes(30), // 请求超时时间
                transport,
                initHandler,
                requestHandlerMap,
                notificationHandlerMap
            );
        };
    }
}
