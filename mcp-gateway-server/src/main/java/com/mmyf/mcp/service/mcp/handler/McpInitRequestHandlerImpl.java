package com.mmyf.mcp.service.mcp.handler;

import com.mmyf.mcp.model.entity.mcp.McpService;
import com.mmyf.mcp.service.mcp.McpProtocolService;
import io.modelcontextprotocol.server.McpInitRequestHandler;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP初始化请求处理器工厂
 *
 * @author Teddy
 * @date 2026-01-29
 */
@Component
@Slf4j
public class McpInitRequestHandlerImpl {

    @Autowired
    private McpProtocolService mcpProtocolService;

    /**
     * 创建初始化请求处理器
     *
     * @param service 服务对象
     * @param requestedProtocolVersion 请求的协议版本
     * @return 初始化请求处理器
     */
    public McpInitRequestHandler create(McpService service, String requestedProtocolVersion) {
        return initializeRequest -> {
            log.debug("Handling initialize request: {}", initializeRequest);
            
            // 从 InitializeRequest 中提取客户端参数
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
            
            // 使用请求中的协议版本，如果没有则使用传入的 requestedProtocolVersion
            String protocolVersion = initializeRequest.protocolVersion() != null 
                    ? initializeRequest.protocolVersion() 
                    : requestedProtocolVersion;
            
            McpSchema.InitializeResult result = mcpProtocolService.initialize(
                service, 
                clientParams, 
                protocolVersion
            );
            
            return Mono.just(result);
        };
    }
}
