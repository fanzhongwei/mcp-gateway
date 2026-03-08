package com.mmyf.mcp.controller.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmyf.mcp.model.entity.mcp.McpService;
import com.mmyf.mcp.service.mcp.McpProtocolService;
import com.mmyf.mcp.service.mcp.McpTransportContextExtractorImpl;
import com.mmyf.mcp.service.mcp.handler.McpStatelessHandlerFactory;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 无状态 MCP 协议控制器
 * 参考 SDK 中 HttpServletStatelessServerTransport：不维护会话，每次请求独立处理。
 * - GET：返回 405 Method Not Allowed
 * - POST：处理 JSON-RPC 请求/通知，Request 返回 200+JSON，Notification 返回 202 Accepted
 *
 * @author Teddy
 * @date 2026-01-29
 */
@RestController
@RequestMapping("/mcp/service")
@Tag(name = "MCP协议（无状态）", description = "MCP协议接口（无状态 Streamable HTTP 传输层）")
@Slf4j
public class McpStatelessProtocolController {

    public static final String UTF_8 = "UTF-8";
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static final String ACCEPT = "Accept";

    @Autowired
    private McpProtocolService mcpProtocolService;

    @Autowired
    private McpTransportContextExtractorImpl contextExtractor;

    @Autowired
    private McpStatelessHandlerFactory statelessHandlerFactory;

    private final McpJsonMapper jsonMapper;

    public McpStatelessProtocolController(ObjectMapper objectMapper) {
        this.jsonMapper = new JacksonMcpJsonMapper(objectMapper);
    }

    private McpStatelessServerHandler getStatelessHandler() {
        return statelessHandlerFactory.createHandler(jsonMapper);
    }

    /**
     * GET 请求：无状态传输不支持 GET，返回 405
     */
    @Operation(summary = "无状态 MCP 端点（GET）", description = "无状态传输不支持 GET，返回 405 Method Not Allowed")
    @GetMapping(value = "/{serviceId}/stateless")
    public ResponseEntity<Void> handleGet(
            @Parameter(description = "MCP服务ID", required = true) @PathVariable(value = "serviceId") String serviceId) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    /**
     * POST 请求：处理 JSON-RPC 请求与通知
     * 规范要求 Accept 头同时包含 application/json 和 text/event-stream
     */
    @Operation(
            summary = "无状态 MCP 端点（POST）",
            description = "处理无状态 MCP 请求。\n\n" +
                    "**JSON-RPC Request**: 返回 200 + JSON 响应\n" +
                    "**JSON-RPC Notification**: 返回 202 Accepted\n" +
                    "请求头 Accept 必须同时包含 application/json 和 text/event-stream"
    )
    @PostMapping(
            value = "/{serviceId}/stateless",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public void handlePost(
            @Parameter(description = "MCP服务ID", required = true) @PathVariable(value = "serviceId") String serviceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 校验认证
        String accessToken = extractAccessToken(authHeader);
        if (!StringUtils.hasText(accessToken)) {
            responseError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    McpError.builder(401).message("Invalid Access Token: Authorization header is required (Bearer token)").build());
            return;
        }

        // 校验服务
        McpService service = mcpProtocolService.validateService(serviceId, accessToken);
        if (service == null) {
            responseError(response, HttpServletResponse.SC_FORBIDDEN,
                    McpError.builder(403).message("Service Not Found or access token is invalid").build());
            return;
        }

        // 校验 Accept 头（与 HttpServletStatelessServerTransport 一致）
        String accept = request.getHeader(ACCEPT);
        if (accept == null || !(accept.contains(APPLICATION_JSON) && accept.contains(TEXT_EVENT_STREAM))) {
            responseError(response, HttpServletResponse.SC_BAD_REQUEST,
                    McpError.builder(400).message("Both application/json and text/event-stream required in Accept header").build());
            return;
        }

        // 构建传输上下文（含 serviceId、service）
        McpTransportContext transportContext = contextExtractor.extractWithExtra(request,
                Map.of("serviceId", serviceId, "service", service));

        // 读取请求体
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        if (body.length() == 0) {
            responseError(response, HttpServletResponse.SC_BAD_REQUEST,
                    McpError.builder(400).message("Request body is required and must be valid JSON").build());
            return;
        }

        try {
            log.debug("Received MCP stateless request: {}", body);
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body.toString());

            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
                try {
                    McpSchema.JSONRPCResponse jsonrpcResponse = getStatelessHandler()
                            .handleRequest(transportContext, jsonrpcRequest)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();

                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.setStatus(HttpServletResponse.SC_OK);
                    String jsonResponseText = jsonMapper.writeValueAsString(jsonrpcResponse);
                    PrintWriter writer = response.getWriter();
                    writer.write(jsonResponseText);
                    writer.flush();
                } catch (Exception e) {
                    log.error("Failed to handle request: {}", e.getMessage());
                    responseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            McpError.builder(500).message("Failed to handle request: " + e.getMessage()).build());
                }
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                try {
                    getStatelessHandler().handleNotification(transportContext, jsonrpcNotification)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                    response.setStatus(HttpServletResponse.SC_ACCEPTED);
                } catch (Exception e) {
                    log.error("Failed to handle notification: {}", e.getMessage());
                    responseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            McpError.builder(500).message("Failed to handle notification: " + e.getMessage()).build());
                }
            } else {
                responseError(response, HttpServletResponse.SC_BAD_REQUEST,
                        McpError.builder(400).message("The server accepts either requests or notifications").build());
            }
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to deserialize message: {}", e.getMessage());
            responseError(response, HttpServletResponse.SC_BAD_REQUEST, McpError.builder(400).message("Invalid message format").build());
        } catch (Exception e) {
            log.error("Unexpected error handling message: {}", e.getMessage());
            responseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    McpError.builder(500).message("Unexpected error: " + e.getMessage()).build());
        }
    }

    private String extractAccessToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        if (authHeader.startsWith("Bearer ") || authHeader.startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader.trim();
    }

    private void responseError(HttpServletResponse response, int httpCode, McpError mcpError) throws IOException {
        response.setContentType(APPLICATION_JSON);
        response.setCharacterEncoding(UTF_8);
        response.setStatus(httpCode);
        String jsonError = mcpError.toString();
        PrintWriter writer = response.getWriter();
        writer.write(jsonError);
        writer.flush();
    }
}
