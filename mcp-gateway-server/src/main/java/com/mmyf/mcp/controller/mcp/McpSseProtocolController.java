package com.mmyf.mcp.controller.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmyf.mcp.model.entity.mcp.McpService;
import com.mmyf.mcp.service.mcp.McpProtocolService;
import com.mmyf.mcp.service.mcp.McpTransportContextExtractorImpl;
import com.mmyf.mcp.service.mcp.handler.McpSessionFactory;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MCP协议控制器（基于2025-11-25规范）
 * 实现SSE传输层，支持POST和GET请求
 *
 * 不建议使用有状态的会话管理，会给服务端带来巨大压力，建议使用无状态的 {@link McpStatelessProtocolController}
 *
 * @author Teddy
 * @date 2026-01-26
 */
@RestController
//@RequestMapping("/mcp/service")
@Tag(name = "MCP协议（SSE）", description = "MCP协议接口（SSE传输层）")
@Slf4j
@Deprecated
public class McpSseProtocolController {

    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";
    public static final String SESSION_ID = "sessionId";

    @Autowired
    private McpProtocolService mcpProtocolService;

    @Autowired
    private McpSessionFactory sessionFactory;

    @Autowired
    private McpTransportContextExtractorImpl contextExtractor;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * JSON mapper for serialization/deserialization
     */
    private final McpJsonMapper jsonMapper;

    /**
     * Map of active client sessions, keyed by session ID
     */
    private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    /**
     * 支持的协议版本列表
     */
    private static final List<String> SUPPORTED_PROTOCOL_VERSIONS = Arrays.asList("2025-11-25", "2024-11-05");
    
    /**
     * 默认协议版本（向后兼容）
     */
    private static final String DEFAULT_PROTOCOL_VERSION = "2024-11-05";

    /**
     * 构造函数，初始化JSON mapper
     */
    public McpSseProtocolController(ObjectMapper objectMapper) {
        this.jsonMapper = new JacksonMcpJsonMapper(objectMapper);
    }


    /**
     * MCP协议端点（GET方法）
     * 用于建立SSE流，允许服务器主动向客户端发送消息
     * 根据MCP 2025-11-25规范：客户端可以通过GET请求建立SSE流
     *
     * @param serviceId 服务ID
     * @param authHeader Authorization请求头
     * @param acceptHeader Accept请求头
     * @return SSE流或405 Method Not Allowed
     */
    @Operation(
            summary = "MCP协议SSE端点（GET）",
            description = "建立Server-Sent Events流，允许服务器主动向客户端发送消息。\n\n" +
                    "客户端可以通过GET请求建立SSE连接，服务器可以通过此连接推送通知消息。\n" +
                    "支持使用Last-Event-ID头恢复之前的流。"
    )
    @GetMapping(value = "/{serviceId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> handleCreateSession(
            @Parameter(description = "MCP服务ID", required = true, example = "1234567890abcdef")
            @PathVariable(value = "serviceId") String serviceId,
            @Parameter(description = "访问令牌，格式：Bearer {token}", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Accept头，必须包含text/event-stream")
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @Parameter(description = "MCP-Protocol-Version")
            @RequestHeader(value = "MCP-Protocol-Version", required = false) String protocolVersionHeader,
            HttpServletRequest httpRequest
    ) {
        log.debug("Received MCP Create Session request: serviceId={}", serviceId);
        
        // 验证Accept头
        if (acceptHeader == null || !acceptHeader.contains("text/event-stream")) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

        // 提取访问令牌
        String accessToken = extractAccessToken(authHeader);
        if (!StringUtils.hasText(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 验证服务
        McpService service = mcpProtocolService.validateService(serviceId, accessToken);
        if (service == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 验证协议版本
        String protocolVersion = protocolVersionHeader != null ? protocolVersionHeader : DEFAULT_PROTOCOL_VERSION;
        if (!isValidProtocolVersion(protocolVersion)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // 创建session factory
        McpServerSession.Factory factory = sessionFactory.createFactory(serviceId, service, protocolVersion);

        // 构建base URL和message endpoint
        String baseUrl = buildBaseUrl(httpRequest);
        String messageEndpoint = "/mcp/service/" + serviceId + "/sse/message";

        // 创建SSE连接
        SseEmitter emitter = createSseConnection(factory, baseUrl, messageEndpoint);

        return ResponseEntity.ok(emitter);
    }

    /**
     * MCP协议端点（POST方法）
     * 根据MCP 2025-11-25规范实现Streamable HTTP传输层
     * 处理JSON-RPC 2.0格式的请求、通知和响应
     *
     * @param serviceId 服务ID
     * @param authHeader Authorization请求头
     * @param httpRequest HTTP请求对象
     * @return JSON-RPC响应、SSE流或202 Accepted
     */
    @Operation(
            summary = "MCP协议端点（POST）",
            description = "处理MCP协议的请求，支持Streamable HTTP传输层。\n\n" +
                    "**JSON-RPC Request**: 返回SSE流或JSON响应\n" +
                    "**JSON-RPC Notification/Response**: 返回202 Accepted\n" +
                    "**initialize**: 协议初始化，协商协议版本和能力\n" +
                    "**ping**: 连接健康检查\n" +
                    "**tools/list**: 获取服务中所有可用的工具列表\n" +
                    "**tools/call**: 调用指定的工具"
    )
    @PostMapping(value = "/{serviceId}/sse/message",
            produces = MediaType.APPLICATION_JSON_VALUE, 
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handlePostRequestMessage(
            @Parameter(description = "MCP服务ID", required = true, example = "1234567890abcdef")
            @PathVariable(value = "serviceId") String serviceId,
            @Parameter(description = "访问令牌，格式：Bearer {token}", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "sessionId", required = true) String sessionId,
            HttpServletRequest httpRequest
    ) {
        try {
            log.debug("Received MCP POST request: serviceId={}, sessionId={}", serviceId, sessionId);

            // 验证认证
            String accessToken = extractAccessToken(authHeader);
            if (!StringUtils.hasText(accessToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(McpError.builder(401)
                                .message("Invalid Access Token: Authorization header is required (Bearer token)")
                                .build());
            }

            // 验证服务
            McpService service = mcpProtocolService.validateService(serviceId, accessToken);
            if (service == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(McpError.builder(403)
                                .message("Service Not Found: Service does not exist or is not published or access token is invalid")
                                .build());
            }

            // 读取请求体
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = httpRequest.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            if (body.length() == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(McpError.builder(400)
                                .message("Invalid Request: Request body is required and must be valid JSON")
                                .build());
            }

            // 处理消息
            handleMessage(sessionId, body.toString(), httpRequest);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("处理MCP请求异常", e);
            if (e.getMessage() != null && e.getMessage().contains("Session not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(McpError.builder(404)
                                .message("Session not found: " + sessionId)
                                .build());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(McpError.builder(500)
                            .message("Internal Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"))
                            .build());
        } catch (Exception e) {
            log.error("处理MCP请求异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(McpError.builder(500)
                            .message("Internal Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"))
                            .build());
        }
    }

    /**
     * 构建基础URL
     */
    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String path = contextPath;

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        if (path != null && !path.isEmpty()) {
            url.append(path);
        }
        return url.toString();
    }

    /**
     * 验证协议版本
     */
    private boolean isValidProtocolVersion(String version) {
        if (version == null) {
            return true; // 使用默认版本
        }
        return SUPPORTED_PROTOCOL_VERSIONS.contains(version) || 
               DEFAULT_PROTOCOL_VERSION.equals(version);
    }

    /**
     * 验证Origin头（防止DNS重绑定攻击）
     * 简化实现：允许localhost和同源请求
     */
    private boolean isValidOrigin(String origin) {
        // 兼容旧签名：保持默认更严格的行为（仅允许localhost）
        return isValidOrigin(origin, null);
    }

    /**
     * 验证Origin头（防止DNS重绑定攻击）
     * 允许：
     * - localhost/127.0.0.1
     * - 同源（基于Host/X-Forwarded-Host/X-Forwarded-Proto）
     */
    private boolean isValidOrigin(String origin, HttpServletRequest request) {
        if (origin == null) {
            return true;
        }

        // 允许localhost
        if (origin.startsWith("http://localhost")
                || origin.startsWith("https://localhost")
                || origin.startsWith("http://127.0.0.1")
                || origin.startsWith("https://127.0.0.1")) {
            return true;
        }

        if (request == null) {
            return false;
        }

        try {
            URI originUri = URI.create(origin);
            String originScheme = originUri.getScheme();
            String originHost = originUri.getHost();
            int originPort = originUri.getPort();

            if (!StringUtils.hasText(originScheme) || !StringUtils.hasText(originHost)) {
                return false;
            }

            String forwardedHost = request.getHeader("X-Forwarded-Host");
            String forwardedProto = request.getHeader("X-Forwarded-Proto");
            String hostHeader = StringUtils.hasText(forwardedHost) ? forwardedHost : request.getHeader("Host");
            if (!StringUtils.hasText(hostHeader)) {
                return false;
            }

            // X-Forwarded-Host / Host 可能包含端口
            String hostOnly = hostHeader;
            int serverPort = -1;
            int colonIdx = hostHeader.lastIndexOf(':');
            if (colonIdx > 0 && hostHeader.indexOf(']') < 0) { // 简单排除IPv6格式
                hostOnly = hostHeader.substring(0, colonIdx);
                try {
                    serverPort = Integer.parseInt(hostHeader.substring(colonIdx + 1));
                } catch (Exception ignore) {
                    serverPort = -1;
                }
            }

            String serverScheme = StringUtils.hasText(forwardedProto) ? forwardedProto : request.getScheme();

            if (!originScheme.equalsIgnoreCase(serverScheme)) {
                return false;
            }
            if (!originHost.equalsIgnoreCase(hostOnly)) {
                return false;
            }
            // 如果Origin显式带端口，则必须匹配服务端端口（能解析到的话）
            if (originPort != -1 && serverPort != -1 && originPort != serverPort) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Failed to validate Origin: {}", origin, e);
            return false;
        }
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

    /**
     * 处理DELETE请求（终止Session）
     */
    @Operation(summary = "终止MCP Session", description = "客户端可以发送DELETE请求来终止Session")
    @DeleteMapping(value = "/{serviceId}")
    public ResponseEntity<?> handleDeleteRequest(
            @PathVariable String serviceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "MCP-Session-Id", required = false) String sessionId
    ) {
        // 验证认证
        String accessToken = extractAccessToken(authHeader);
        if (!StringUtils.hasText(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 验证服务
        McpService service = mcpProtocolService.validateService(serviceId, accessToken);
        if (service == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 终止Session
        if (sessionId != null) {
            McpServerSession session = sessions.get(sessionId);
            if (session != null) {
                session.closeGracefully().block();
                sessions.remove(sessionId);
                return ResponseEntity.ok().build();
            }
        }

        // 如果服务器不允许客户端终止Session，返回405
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    /**
     * 创建SSE连接并返回SseEmitter
     * 参考WebMvcSseServerTransportProvider的实现
     *
     * @param sessionFactory Session工厂
     * @param baseUrl 基础URL
     * @param messageEndpoint 消息端点
     * @return SseEmitter
     */
    private SseEmitter createSseConnection(McpServerSession.Factory sessionFactory,
                                          String baseUrl, String messageEndpoint) {
        SseEmitter emitter = new SseEmitter(0L); // 0表示不超时

        // 创建session transport
        GatewayMcpSessionTransport sessionTransport = new GatewayMcpSessionTransport(emitter);

        // 创建session
        McpServerSession session = sessionFactory.create(sessionTransport);
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        log.debug("Creating new SSE connection for session: {}", sessionId);

        // 注册完成和错误回调
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for session: {}", sessionId);
            sessions.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out for session: {}", sessionId);
            sessions.remove(sessionId);
        });
        emitter.onError((ex) -> {
            log.error("SSE connection error for session: {}", sessionId, ex);
            sessions.remove(sessionId);
        });

        // 发送初始endpoint事件
        try {
            String endpointUrl = buildEndpointUrl(baseUrl, messageEndpoint, sessionId);
            emitter.send(SseEmitter.event()
                    .name(ENDPOINT_EVENT_TYPE)
                    .data(endpointUrl));
        } catch (IOException e) {
            log.error("Failed to send initial endpoint event: {}", e.getMessage());
            sessions.remove(sessionId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 处理消息请求
     * 参考WebMvcSseServerTransportProvider的实现
     *
     * @param sessionId Session ID
     * @param requestBody 请求体
     * @param httpRequest HTTP请求
     */
    private void handleMessage(String sessionId, String requestBody, HttpServletRequest httpRequest) {
        McpServerSession session = sessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        try {
            // 提取transport context
            McpTransportContext transportContext = contextExtractor.extract(httpRequest);

            // 反序列化JSON-RPC消息
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, requestBody);

            // 处理消息
            session.handle(message)
                    .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                    .block(); // 阻塞以兼容同步处理
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to deserialize message: {}", e.getMessage());
            throw new RuntimeException("Invalid message format", e);
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
            throw new RuntimeException("Error handling message", e);
        }
    }

    /**
     * 构建endpoint URL
     * 参考WebMvcSseServerTransportProvider的实现
     */
    private String buildEndpointUrl(String baseUrl, String messageEndpoint, String sessionId) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + messageEndpoint + "?sessionId=" + sessionId;
        }
        return baseUrl + messageEndpoint + "?sessionId=" + sessionId;
    }

    /**
     * Gateway MCP Session Transport实现
     * 参考WebMvcSseServerTransportProvider中的WebMvcMcpSessionTransport实现
     */
    private class GatewayMcpSessionTransport implements McpServerTransport {

        private final SseEmitter emitter;
        private final ReentrantLock emitterLock = new ReentrantLock();

        GatewayMcpSessionTransport(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 发送JSON-RPC消息到客户端
         */
        @Override
        public reactor.core.publisher.Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return reactor.core.publisher.Mono.fromRunnable(() -> {
                emitterLock.lock();
                try {
                    String jsonText = jsonMapper.writeValueAsString(message);
                    emitter.send(SseEmitter.event()
                            .name(MESSAGE_EVENT_TYPE)
                            .data(jsonText));
                } catch (Exception e) {
                    log.error("Failed to send message: {}", e.getMessage());
                    emitter.completeWithError(e);
                } finally {
                    emitterLock.unlock();
                }
            });
        }

        /**
         * 转换数据类型
         */
        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }

        /**
         * 优雅关闭transport
         */
        @Override
        public reactor.core.publisher.Mono<Void> closeGracefully() {
            return reactor.core.publisher.Mono.fromRunnable(() -> {
                emitterLock.lock();
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("Failed to complete SSE emitter: {}", e.getMessage());
                } finally {
                    emitterLock.unlock();
                }
            });
        }

        /**
         * 立即关闭transport
         */
        @Override
        public void close() {
            emitterLock.lock();
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Failed to complete SSE emitter: {}", e.getMessage());
            } finally {
                emitterLock.unlock();
            }
        }
    }

}
