package com.mmyf.mcp.service.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP传输上下文提取器实现
 * 从HTTP请求中提取上下文信息（如认证信息、服务ID等）
 *
 * @author Teddy
 * @date 2026-01-29
 */
@Component
@Slf4j
public class McpTransportContextExtractorImpl implements McpTransportContextExtractor<HttpServletRequest> {

    @Override
    public McpTransportContext extract(HttpServletRequest request) {
        return McpTransportContext.create(buildMetadata(request));
    }

    /**
     * 提取传输上下文并合并额外元数据（如无状态场景下的 serviceId、service）
     */
    public McpTransportContext extractWithExtra(HttpServletRequest request, Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = buildMetadata(request);
        if (extraMetadata != null && !extraMetadata.isEmpty()) {
            metadata.putAll(extraMetadata);
        }
        return McpTransportContext.create(metadata);
    }

    private Map<String, Object> buildMetadata(HttpServletRequest request) {
        Map<String, Object> metadata = new HashMap<>();

        // 提取Authorization头
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            metadata.put("Authorization", authHeader);
            // 提取token
            String token = extractAccessToken(authHeader);
            if (StringUtils.hasText(token)) {
                metadata.put("accessToken", token);
            }
        }

        // 提取协议版本
        String protocolVersion = request.getHeader("MCP-Protocol-Version");
        if (StringUtils.hasText(protocolVersion)) {
            metadata.put("protocolVersion", protocolVersion);
        }

        // 提取Session ID
        String sessionId = request.getHeader("MCP-Session-Id");
        if (StringUtils.hasText(sessionId)) {
            metadata.put("sessionId", sessionId);
        }

        // 提取Origin
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            metadata.put("Origin", origin);
        }

        return metadata;
    }

    /**
     * 从Authorization请求头中提取访问令牌
     */
    private String extractAccessToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        // 支持 "Bearer token" 格式
        if (authHeader.startsWith("Bearer ") || authHeader.startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 也支持直接传递token
        return authHeader.trim();
    }
}
