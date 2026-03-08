package com.mmyf.mcp.service.mcp;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmyf.mcp.model.dto.mcp.*;
import com.mmyf.mcp.model.dto.api.*;
import com.mmyf.mcp.model.entity.mcp.McpConfigApi;
import com.mmyf.mcp.model.entity.mcp.McpService;
import com.mmyf.mcp.model.entity.system.Api;
import com.mmyf.mcp.model.entity.system.System;
import com.mmyf.mcp.model.entity.system.SystemEnv;
import com.mmyf.mcp.service.converter.ToolConverter;
import com.mmyf.mcp.service.system.ApiService;
import com.mmyf.mcp.service.system.SystemEnvService;
import com.mmyf.mcp.service.system.SystemService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

/**
 * MCP协议服务层
 * 处理MCP协议的initialize、ping、tools/list和tools/call请求
 *
 * @author Teddy
 * @date 2026-01-26
 */
@Service
@Slf4j
public class McpProtocolService {

    @Autowired
    private McpServiceService mcpServiceService;

    @Autowired
    private McpConfigApiService mcpConfigApiService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private SystemEnvService systemEnvService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ToolConverter toolConverter;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    // ThreadLocal用于保存form-data的boundary，以便在设置Content-Type时使用
    private static final ThreadLocal<String> FORM_DATA_BOUNDARY = new ThreadLocal<>();

    /**
     * 验证服务访问权限
     *
     * @param serviceId 服务ID
     * @param accessToken 访问令牌
     * @return 服务对象，如果验证失败返回null
     */
    @Cached(name = "mcpServiceCache:", key = "#serviceId + ':' + #accessToken", cacheType = CacheType.BOTH, localExpire = 5, expire = 5, timeUnit = TimeUnit.MINUTES)
    public McpService validateService(String serviceId, String accessToken) {
        McpService service = mcpServiceService.getById(serviceId);
        if (service == null) {
            return null;
        }

        // 验证服务状态
        if (!"published".equals(service.getStatus())) {
            log.warn("服务未发布，状态: {}", service.getStatus());
            return null;
        }

        // 验证访问令牌
        if (!StringUtils.hasText(accessToken) || !accessToken.equals(service.getAccessToken())) {
            log.warn("访问令牌验证失败");
            return null;
        }

        return service;
    }

    /**
     * 协议初始化（initialize）
     * 根据MCP 2025-11-25规范实现版本协商和能力声明
     *
     * @param service 已验证的MCP服务
     * @param clientParams 客户端传入的参数（协议版本、能力等）
     * @param requestedProtocolVersion 客户端请求的协议版本（从HTTP头获取）
     * @return 初始化结果
     */
    @SuppressWarnings("unchecked")
    public McpSchema.InitializeResult initialize(McpService service, Map<String, Object> clientParams,
                                         String requestedProtocolVersion) {
        if (service == null) {
            throw new IllegalArgumentException("服务不能为空");
        }

        // 版本协商
        String protocolVersion = negotiateProtocolVersion(
                requestedProtocolVersion != null ? requestedProtocolVersion : 
                (clientParams != null ? (String) clientParams.get("protocolVersion") : null)
        );

        // 声明服务端能力：当前仅支持tools能力
        McpSchema.ServerCapabilities capabilities = new McpSchema.ServerCapabilities(
                new McpSchema.ServerCapabilities.CompletionCapabilities(),
                null,
                new McpSchema.ServerCapabilities.LoggingCapabilities(),
                new McpSchema.ServerCapabilities.PromptCapabilities(false),
                new McpSchema.ServerCapabilities.ResourceCapabilities(false, false),
                new McpSchema.ServerCapabilities.ToolCapabilities(false));

        Map<String, Object> toolsCapability = new HashMap<>();

        // 构建serverInfo
        McpSchema.Implementation serverInfo = new McpSchema.Implementation(StringUtils.hasText(service.getName()) ? service.getName() : "MCP Gateway Service", "1.0.0");


        McpSchema.InitializeResult initializeResult = new McpSchema.InitializeResult(protocolVersion, capabilities,
                serverInfo, null);
        log.debug("Initialized MCP service: initializeResult={}", initializeResult);
        return initializeResult;
    }

    /**
     * 版本协商
     * 如果客户端请求的版本服务器支持，返回该版本；否则返回服务器支持的最新版本
     *
     * @param requestedVersion 客户端请求的版本
     * @return 协商后的协议版本
     */
    private String negotiateProtocolVersion(String requestedVersion) {
        // 支持的版本列表（按优先级排序）
        List<String> supportedVersions = Arrays.asList("2025-11-25", "2024-11-05");

        if (requestedVersion != null && supportedVersions.contains(requestedVersion)) {
            log.debug("Protocol version negotiated: {}", requestedVersion);
            return requestedVersion;
        }

        // 返回服务器支持的最新版本
        String latestVersion = supportedVersions.get(0);
        log.debug("Protocol version negotiated (server latest): {} (requested: {})", 
                latestVersion, requestedVersion);
        return latestVersion;
    }

    /**
     * 处理initialized通知
     * 客户端在收到initialize响应后必须发送此通知
     *
     * @param serviceId 服务ID
     * @param sessionId Session ID
     */
    public void handleInitialized(String serviceId, String sessionId) {
        log.debug("Received initialized notification: serviceId={}, sessionId={}", serviceId, sessionId);
        // 这里可以执行初始化后的操作，比如注册监听器等
    }

    /**
     * 连接健康检查（ping）
     *
     * @return 空对象，表示成功
     */
    public Map<String, Object> ping() {
        // 按规范返回空对象即可
        return Collections.emptyMap();
    }

    /**
     * 处理MCP协议方法请求
     *
     * @param method 方法名
     * @param serviceId 服务ID
     * @param service 已验证的MCP服务
     * @param params 请求参数
     * @param protocolVersion 协议版本（用于版本特定的处理）
     * @return 方法执行结果
     */
    @SuppressWarnings("unchecked")
    public Object handleMethod(String method, String serviceId, McpService service,
                                  Map<String, Object> params, String protocolVersion) {
        if (method == null) {
            throw new IllegalArgumentException("method is required");
        }
        log.debug("Handling MCP method: method={}, serviceId={}, protocolVersion={}, params={}", method, serviceId, protocolVersion, params);
        switch (method) {
            case "initialize":
                return initialize(service, params, protocolVersion);
            case "ping":
                return ping();
            case "tools/list":
                return listTools(serviceId);
            case "tools/call":
                return callTool(serviceId, params);
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    /**
     * 获取工具列表（tools/list）
     *
     * @param serviceId 服务ID
     * @return 工具列表结果（MCP协议格式）
     */
    public McpSchema.ListToolsResult listTools(String serviceId) {
        McpSchema.ListToolsResult toolsResult = listToolsInternal(serviceId, false);
        return toolsResult;
    }

    /**
     * 获取工具列表内部实现
     *
     * @param serviceId 服务ID
     * @return 工具列表
     */
    public McpSchema.ListToolsResult listToolsInternal(String serviceId, boolean getServiceMetaInfo) {
        McpService service = mcpServiceService.getById(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("服务不存在");
        }

        // 查询服务配置
        List<McpConfigApi> configApis = mcpConfigApiService.list(
                new LambdaQueryWrapper<McpConfigApi>()
                        .eq(McpConfigApi::getMcpServiceId, serviceId)
        );

        if (configApis.isEmpty()) {
            return new McpSchema.ListToolsResult(new ArrayList<>(), null);
        }

        // 收集所有需要查询的ID
        Set<String> systemIds = new HashSet<>();
        Set<String> envIds = new HashSet<>();
        Set<String> apiIds = new HashSet<>();

        for (McpConfigApi configApi : configApis) {
            if (StringUtils.hasText(configApi.getSystemId())) {
                systemIds.add(configApi.getSystemId());
            }
            if (StringUtils.hasText(configApi.getEnvId())) {
                envIds.add(configApi.getEnvId());
            }
            // 从 apiConfig 读取接口ID
            List<McpApiConfigItem> apiConfigList =
                    Optional.ofNullable(configApi.getApiConfig()).orElseGet(Collections::emptyList);
            for (McpApiConfigItem item : apiConfigList) {
                if (StringUtils.hasText(item.getApiId())) {
                    apiIds.add(item.getApiId());
                }
            }
        }

        // 批量查询
        Map<String, System> systemMap = new HashMap<>();
        if (!systemIds.isEmpty()) {
            List<System> systems = systemService.listByIds(systemIds);
            systemMap = systems.stream().collect(Collectors.toMap(System::getId, s -> s));
        }

        Map<String, SystemEnv> envMap = new HashMap<>();
        if (!envIds.isEmpty()) {
            List<SystemEnv> envs = systemEnvService.listByIds(envIds);
            envMap = envs.stream().collect(Collectors.toMap(SystemEnv::getId, e -> e));
        }

        Map<String, Api> apiMap = new HashMap<>();
        if (!apiIds.isEmpty()) {
            List<Api> apis = apiService.listByIds(apiIds);
            apiMap = apis.stream().collect(Collectors.toMap(Api::getId, a -> a));
        }

        // 转换为MCP工具
        List<McpSchema.Tool> tools = new ArrayList<>();
        for (McpConfigApi configApi : configApis) {
            System system = systemMap.get(configApi.getSystemId());
            SystemEnv env = envMap.get(configApi.getEnvId());
            String systemName = system != null ? system.getName() : "Unknown";
            String envBaseUrl = env != null ? env.getBaseUrl() : "";
            // 获取系统级别的自定义MCP名字
            String systemMcpName = configApi.getSystemMcpName();

            // 从 apiConfig 读取接口信息
            List<McpApiConfigItem> apiConfigList =
                    Optional.ofNullable(configApi.getApiConfig()).orElseGet(Collections::emptyList);
            for (McpApiConfigItem apiConfigItem : apiConfigList) {
                String apiId = apiConfigItem.getApiId();
                String apiCustomMcpName = apiConfigItem.getCustomMcpName();
                Api api = apiMap.get(apiId);
                if (api != null) {
                    McpSchema.Tool.Builder toolBuilder = toolConverter.convertToTool(api, systemName, systemMcpName, apiCustomMcpName);
                    if (toolBuilder != null) {
                        if (getServiceMetaInfo) {
                            McpToolMeta toolMeta = new McpToolMeta();
                            // 设置内部字段
                            toolMeta.setApi(api);
                            toolMeta.setApiId(api.getId());
                            toolMeta.setSystemId(configApi.getSystemId());
                            toolMeta.setSystemName(systemName);
                            toolMeta.setEnvId(configApi.getEnvId());
                            toolMeta.setEnvBaseUrl(envBaseUrl);
                            toolMeta.setMethod(api.getMethod());
                            toolMeta.setPath(api.getPath());
                            toolBuilder.meta(Map.of("toolMeta", toolMeta));
                        }
                        tools.add(toolBuilder.build());
                    }
                }
            }
        }

        return new McpSchema.ListToolsResult(tools, null);
    }

    // 之前的 ApiConfigItem 和 JSON 解析方法已被移除，
    // 现在直接使用实体上的结构化字段 McpApiConfigItem 列表。

    /**
     * 调用工具（tools/call）
     *
     * @param serviceId 服务ID
     * @param params 请求参数，包含name和arguments
     * @return 工具调用结果
     */
    @SuppressWarnings("unchecked")
    public McpSchema.CallToolResult callTool(String serviceId, Map<String, Object> params) {
        if (params == null) {
            throw new IllegalArgumentException("params is required");
        }

        String toolName = (String) params.get("name");
        if (!StringUtils.hasText(toolName)) {
            throw new IllegalArgumentException("tool name is required");
        }

        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        return callToolInternal(serviceId, toolName, arguments);
    }

    /**
     * 调用工具内部实现
     *
     * @param serviceId 服务ID
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 工具调用结果
     */
    public McpSchema.CallToolResult callToolInternal(String serviceId, String toolName, Map<String, Object> arguments) {
        // 获取工具列表以查找对应的工具
        List<McpSchema.Tool> tools = listToolsInternal(serviceId, true).tools();
        McpSchema.Tool tool = tools.stream()
                .filter(t -> toolName.equals(t.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));

        // 构建API请求URL
        McpToolMeta toolMeta = (McpToolMeta) MapUtils.getObject(tool.meta(), "toolMeta");
        String url = buildApiUrl(toolMeta, arguments);

        // 构建HTTP请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120));

        // 设置HTTP方法
        String method = toolMeta.getMethod() != null ? toolMeta.getMethod().toUpperCase() : "GET";
        
        // 先构建请求体（form-data需要boundary信息）
        HttpRequest.BodyPublisher bodyPublisher = buildRequestBody(toolMeta, arguments);
        
        // 设置请求头（需要知道body类型以设置正确的Content-Type）
        setRequestHeaders(requestBuilder, toolMeta, arguments, bodyPublisher);
        
        requestBuilder.method(method, bodyPublisher);

        try {
            // 发送请求
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            // 构建响应
            return McpSchema.CallToolResult.builder()
                                           .addTextContent(response.body())
                                           .isError(response.statusCode() >= 400)
                                           .build();
        } catch (Exception e) {
            log.error("调用API失败", e);
            return McpSchema.CallToolResult.builder()
                                           .isError(true)
                                           .addTextContent("API调用失败: " + e.getMessage())
                                           .build();
        } finally {
            // 清理ThreadLocal
            FORM_DATA_BOUNDARY.remove();
        }
    }

    /**
     * 构建API请求URL
     */
    private String buildApiUrl(McpToolMeta tool, Map<String, Object> arguments) {
        String baseUrl = tool.getEnvBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("环境基础URL不能为空");
        }

        // 确保baseUrl不以/结尾
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String path = tool.getPath();
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("API路径不能为空");
        }

        // 替换路径参数
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = arguments.get(paramName);
            if (value == null) {
                throw new IllegalArgumentException("缺少必需的路径参数: " + paramName);
            }
            matcher.appendReplacement(sb, value.toString());
        }
        matcher.appendTail(sb);
        path = sb.toString();

        // 确保path以/开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // 构建完整URL
        StringBuilder urlBuilder = new StringBuilder(baseUrl).append(path);

        // 添加Query参数（根据API配置）
        List<String> queryParamPairs = buildQueryParams(tool, arguments);
        if (!queryParamPairs.isEmpty()) {
            urlBuilder.append("?").append(String.join("&", queryParamPairs));
        }

        return urlBuilder.toString();
    }

    /**
     * 构建Query参数列表（根据API配置）
     */
    @SuppressWarnings("unchecked")
    private List<String> buildQueryParams(McpToolMeta tool, Map<String, Object> arguments) {
        List<String> queryParamPairs = new ArrayList<>();
        
        if (tool.getApi() == null) {
            return queryParamPairs;
        }

        try {
            Api api = tool.getApi();
            if (api == null || api.getQueryParams() == null || api.getQueryParams().isEmpty()) {
                return queryParamPairs;
            }

            List<QueryParam> queryParams = api.getQueryParams();

            // 获取需要排除的参数名集合（路径参数、body参数、header参数）
            Set<String> excludedParamNames = getExcludedParamNames(tool, api);

            // 遍历配置的query参数
            for (QueryParam param : queryParams) {
                String key = param.getKey();
                if (!StringUtils.hasText(key)) {
                    continue;
                }

                // 跳过被排除的参数
                if (excludedParamNames.contains(key)) {
                    continue;
                }

                // 从arguments中获取参数值
                Object value = arguments.get(key);
                if (value == null) {
                    // 如果arguments中没有值，使用配置中的默认值
                    value = param.getValue();
                }

                // 只有当值不为null时才添加到URL中
                if (value != null) {
                    queryParamPairs.add(key + "=" + encodeValue(value));
                }
            }
        } catch (Exception e) {
            log.error("构建Query参数失败: apiId={}", tool.getApiId(), e);
        }

        return queryParamPairs;
    }

    /**
     * 获取需要排除的参数名集合（路径参数、body参数、header参数）
     */
    private Set<String> getExcludedParamNames(McpToolMeta tool, Api api) {
        Set<String> excludedNames = new HashSet<>();

        // 1. 路径参数
        Set<String> pathParamNames = extractPathParamNames(tool.getPath());
        excludedNames.addAll(pathParamNames);

        // 2. Body参数
        BodyParam bodyParam = api.getBodyParam();
        if (bodyParam != null) {
            String type = bodyParam.getType();
            if (type == null) {
                type = "json";
            }

            switch (type) {
                case "json":
                    // json类型使用jsonParam参数
                    excludedNames.add("jsonParam");
                    // 兼容旧格式：排除所有body中的字段
                    // 这里不排除，因为旧格式可能将body字段作为单独参数
                    break;
                case "form-data":
                    // form-data类型的参数
                    if (bodyParam.getFormDataParams() != null) {
                        for (FormDataParam param : bodyParam.getFormDataParams()) {
                            String key = param.getKey();
                            if (StringUtils.hasText(key)) {
                                excludedNames.add(key);
                            }
                        }
                    }
                    break;
                case "x-www-form-urlencoded":
                    // x-www-form-urlencoded类型的参数
                    if (bodyParam.getUrlEncodedParams() != null) {
                        for (UrlEncodedParam param : bodyParam.getUrlEncodedParams()) {
                            String key = param.getKey();
                            if (StringUtils.hasText(key)) {
                                excludedNames.add(key);
                            }
                        }
                    }
                    break;
                case "raw":
                    // raw类型使用body参数
                    excludedNames.add("body");
                    break;
            }
        }

        // 3. Header参数
        if (api.getHeaders() != null && !api.getHeaders().isEmpty()) {
            for (Header header : api.getHeaders()) {
                String key = header.getKey();
                if (StringUtils.hasText(key)) {
                    excludedNames.add(key);
                }
            }
        }

        return excludedNames;
    }

    /**
     * 提取路径参数名
     */
    private Set<String> extractPathParamNames(String path) {
        Set<String> paramNames = new HashSet<>();
        if (!StringUtils.hasText(path)) {
            return paramNames;
        }
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }
        return paramNames;
    }

    /**
     * URL编码值
     */
    private String encodeValue(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value.toString(), "UTF-8");
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 构建请求体
     */
    private HttpRequest.BodyPublisher buildRequestBody(McpToolMeta tool, Map<String, Object> arguments) {
        String method = tool.getMethod() != null ? tool.getMethod().toUpperCase() : "GET";
        if ("GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method)) {
            return HttpRequest.BodyPublishers.noBody();
        }

        // 获取API配置以确定bodyParam类型
        if (!StringUtils.hasText(tool.getApiId())) {
            return HttpRequest.BodyPublishers.noBody();
        }

        try {
            Api api = apiService.getById(tool.getApiId());
            if (api == null || api.getBodyParam() == null) {
                return HttpRequest.BodyPublishers.noBody();
            }

            BodyParam bodyParam = api.getBodyParam();
            String type = bodyParam.getType();
            if (type == null) {
                type = "json";
            }

            switch (type) {
                case "json":
                    return buildJsonRequestBody(tool, bodyParam, arguments);
                case "form-data":
                    return buildFormDataRequestBody(bodyParam, arguments);
                case "x-www-form-urlencoded":
                    return buildUrlEncodedRequestBody(bodyParam, arguments);
                case "raw":
                    return buildRawRequestBody(bodyParam, arguments);
                default:
                    // 默认使用JSON格式
                    return buildJsonRequestBody(tool, bodyParam, arguments);
            }
        } catch (Exception e) {
            log.error("构建请求体失败", e);
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    /**
     * 构建JSON格式的请求体
     */
    private HttpRequest.BodyPublisher buildJsonRequestBody(McpToolMeta tool, BodyParam bodyParam, Map<String, Object> arguments) {
        try {
            // 检查是否有jsonParam参数（统一后的JSON body参数）
            Object jsonParam = arguments.get("jsonParam");
            if (jsonParam != null) {
                // 如果jsonParam是字符串，直接使用；否则转换为JSON字符串
                String jsonBody;
                if (jsonParam instanceof String) {
                    jsonBody = (String) jsonParam;
                } else {
                    jsonBody = objectMapper.writeValueAsString(jsonParam);
                }
                return HttpRequest.BodyPublishers.ofString(jsonBody);
            }

            // 兼容旧格式：构建JSON body对象
            Map<String, Object> body = new HashMap<>();
            Set<String> pathParamNames = extractPathParamNames(tool.getPath());
            Set<String> queryParamNames = extractQueryParamNames(bodyParam);
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String key = entry.getKey();
                // 排除路径参数和查询参数，其他参数作为body参数
                if (!pathParamNames.contains(key) && !queryParamNames.contains(key) && !"jsonParam".equals(key)) {
                    body.put(key, entry.getValue());
                }
            }
            if (!body.isEmpty()) {
                String jsonBody = objectMapper.writeValueAsString(body);
                return HttpRequest.BodyPublishers.ofString(jsonBody);
            }
        } catch (Exception e) {
            log.error("构建JSON请求体失败", e);
        }
        return HttpRequest.BodyPublishers.noBody();
    }

    /**
     * 构建form-data格式的请求体
     */
    private HttpRequest.BodyPublisher buildFormDataRequestBody(BodyParam bodyParam, Map<String, Object> arguments) {
        try {
            List<FormDataParam> formDataParams = bodyParam.getFormDataParams();
            if (formDataParams == null || formDataParams.isEmpty()) {
                return HttpRequest.BodyPublishers.noBody();
            }

            // 构建multipart/form-data格式的请求体
            StringBuilder bodyBuilder = new StringBuilder();
            long timestamp = java.lang.System.currentTimeMillis();
            String boundary = "----WebKitFormBoundary" + timestamp;
            
            // 保存boundary到ThreadLocal，以便在设置Content-Type时使用
            FORM_DATA_BOUNDARY.set(boundary);

            for (FormDataParam param : formDataParams) {
                String key = param.getKey();
                if (!StringUtils.hasText(key)) {
                    continue;
                }

                // 从arguments中获取参数值
                Object value = arguments.get(key);
                if (value == null) {
                    // 如果arguments中没有值，使用配置中的默认值
                    value = param.getValue();
                    if (value == null) {
                        value = "";
                    }
                }

                bodyBuilder.append("--").append(boundary).append("\r\n");
                bodyBuilder.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n\r\n");
                bodyBuilder.append(value.toString()).append("\r\n");
            }
            bodyBuilder.append("--").append(boundary).append("--\r\n");

            return HttpRequest.BodyPublishers.ofString(bodyBuilder.toString());
        } catch (Exception e) {
            log.error("构建form-data请求体失败", e);
        }
        return HttpRequest.BodyPublishers.noBody();
    }

    /**
     * 构建x-www-form-urlencoded格式的请求体
     */
    private HttpRequest.BodyPublisher buildUrlEncodedRequestBody(BodyParam bodyParam, Map<String, Object> arguments) {
        try {
            List<UrlEncodedParam> urlEncodedParams = bodyParam.getUrlEncodedParams();
            if (urlEncodedParams == null || urlEncodedParams.isEmpty()) {
                return HttpRequest.BodyPublishers.noBody();
            }

            // 构建URL编码格式的请求体
            List<String> pairs = new ArrayList<>();
            for (UrlEncodedParam param : urlEncodedParams) {
                String key = param.getKey();
                if (!StringUtils.hasText(key)) {
                    continue;
                }

                // 从arguments中获取参数值
                Object value = arguments.get(key);
                if (value == null) {
                    // 如果arguments中没有值，使用配置中的默认值
                    value = param.getValue();
                    if (value == null) {
                        value = "";
                    }
                }

                pairs.add(encodeValue(key) + "=" + encodeValue(value.toString()));
            }

            if (pairs.isEmpty()) {
                return HttpRequest.BodyPublishers.noBody();
            }

            return HttpRequest.BodyPublishers.ofString(String.join("&", pairs));
        } catch (Exception e) {
            log.error("构建x-www-form-urlencoded请求体失败", e);
        }
        return HttpRequest.BodyPublishers.noBody();
    }

    /**
     * 构建raw格式的请求体
     */
    private HttpRequest.BodyPublisher buildRawRequestBody(BodyParam bodyParam, Map<String, Object> arguments) {
        try {
            // 从arguments中获取body参数
            Object body = arguments.get("body");
            if (body != null) {
                return HttpRequest.BodyPublishers.ofString(body.toString());
            }

            // 如果arguments中没有body，使用配置中的raw值
            String raw = bodyParam.getRaw();
            if (StringUtils.hasText(raw)) {
                return HttpRequest.BodyPublishers.ofString(raw);
            }
        } catch (Exception e) {
            log.error("构建raw请求体失败", e);
        }
        return HttpRequest.BodyPublishers.noBody();
    }

    /**
     * 提取查询参数名（从bodyParam配置中）
     */
    private Set<String> extractQueryParamNames(BodyParam bodyParam) {
        Set<String> paramNames = new HashSet<>();
        // 这个方法主要用于排除查询参数，但查询参数实际上是从API的queryParams字段中获取的
        // 这里返回空集合，因为查询参数已经在buildApiUrl中处理了
        return paramNames;
    }

    /**
     * 设置请求头
     */
    private void setRequestHeaders(HttpRequest.Builder requestBuilder, McpToolMeta tool, Map<String, Object> arguments, HttpRequest.BodyPublisher bodyPublisher) {
        // 先收集所有自定义headers，以便判断是否需要设置默认Content-Type
        Set<String> customHeaderKeys = new HashSet<>();
        boolean hasCustomContentType = false;

        // 从API配置中获取headers配置，并从arguments中提取对应的值设置到请求头
        if (StringUtils.hasText(tool.getApiId())) {
            try {
                Api api = apiService.getById(tool.getApiId());
                if (api != null && api.getHeaders() != null && !api.getHeaders().isEmpty()) {
                    List<Header> headers = api.getHeaders();

                    for (Header header : headers) {
                        String key = header.getKey();
                        if (!StringUtils.hasText(key)) {
                            continue;
                        }

                        // 检查是否是Content-Type相关的header
                        if ("Content-Type".equalsIgnoreCase(key) || "content-type".equalsIgnoreCase(key)) {
                            hasCustomContentType = true;
                        }

                        // 从arguments中获取header参数值
                        Object value = arguments.get(key);
                        if (value != null) {
                            // 如果arguments中有值，使用arguments中的值
                            requestBuilder.header(key, value.toString());
                            customHeaderKeys.add(key.toLowerCase());
                        } else if (header.getValue() != null) {
                            // 如果arguments中没有值，但配置中有默认值，使用默认值
                            requestBuilder.header(key, header.getValue());
                            customHeaderKeys.add(key.toLowerCase());
                        }
                        // 如果既没有arguments值也没有默认值，则不设置该header
                    }
                }
            } catch (Exception e) {
                log.error("解析Header配置失败: apiId={}", tool.getApiId(), e);
            }
        }

        // 根据bodyParam.type设置Content-Type（如果没有自定义的Content-Type）
        String method = tool.getMethod() != null ? tool.getMethod().toUpperCase() : "GET";
        if (!"GET".equals(method) && !"HEAD".equals(method) && !"DELETE".equals(method)) {
            if (!hasCustomContentType && !customHeaderKeys.contains("content-type")) {
                // 根据API配置的bodyParam.type设置Content-Type
                String contentType = determineContentType(tool.getApiId(), bodyPublisher);
                if (StringUtils.hasText(contentType)) {
                    requestBuilder.header("Content-Type", contentType);
                } else {
                    // 默认使用application/json
                    requestBuilder.header("Content-Type", "application/json;charset=UTF-8");
                }
            }
        }
        
        // 设置默认的Accept（如果没有自定义的Accept）
        if (!customHeaderKeys.contains("accept")) {
            requestBuilder.header("Accept", "application/json, */*");
        }
    }

    /**
     * 根据API配置的bodyParam.type确定Content-Type
     */
    private String determineContentType(String apiId, HttpRequest.BodyPublisher bodyPublisher) {
        if (!StringUtils.hasText(apiId)) {
            return null;
        }
        try {
            Api api = apiService.getById(apiId);
            if (api != null && api.getBodyParam() != null) {
                BodyParam bodyParam = api.getBodyParam();
                String type = bodyParam.getType();
                if (type == null) {
                    type = "json";
                }
                switch (type) {
                    case "json":
                        return "application/json;charset=UTF-8";
                    case "x-www-form-urlencoded":
                        return "application/x-www-form-urlencoded";
                    case "form-data":
                        // form-data 需要从ThreadLocal中获取boundary
                        String boundary = FORM_DATA_BOUNDARY.get();
                        if (StringUtils.hasText(boundary)) {
                            return "multipart/form-data; boundary=" + boundary;
                        }
                        // 如果ThreadLocal中没有boundary，使用默认值（虽然不应该发生）
                        long timestamp = java.lang.System.currentTimeMillis();
                        return "multipart/form-data; boundary=----WebKitFormBoundary" + timestamp;
                    case "raw":
                        return "text/plain";
                    default:
                        return "application/json;charset=UTF-8";
                }
            }
        } catch (Exception e) {
            log.error("确定Content-Type失败: apiId={}", apiId, e);
        }
        return null;
    }

}
