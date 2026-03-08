package com.mmyf.mcp.controller.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 接口转发控制器
 * 用于解决前端跨域问题，通过后端转发请求
 * 
 * @author Teddy
 * @date 2026-01-23
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/proxy")
public class ProxyController {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * 构造函数，初始化 HttpClient 并配置超时
     */
    public ProxyController() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 转发HTTP请求
     * 直接使用原始HTTP响应流输出，不进行任何包装
     * 
     * @param request 转发请求参数
     * @param httpResponse HTTP响应对象，用于直接输出原始响应
     */
    @PostMapping("/request")
    public void proxyRequest(@RequestBody ProxyRequest request, HttpServletResponse httpResponse) {
        try {
            // 验证URL
            if (!StringUtils.hasText(request.getUrl())) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.setContentType("text/plain;charset=UTF-8");
                httpResponse.getWriter().write("URL不能为空");
                return;
            }
            
            // 验证URL格式
            URI uri;
            try {
                uri = new URI(request.getUrl());
            } catch (Exception e) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.setContentType("text/plain;charset=UTF-8");
                httpResponse.getWriter().write("URL格式不正确: " + e.getMessage());
                return;
            }
            
            // 确定HTTP方法
            String method = StringUtils.hasText(request.getMethod()) 
                ? request.getMethod().toUpperCase() 
                : "GET";
            
            log.info("转发请求: {} {}", method, request.getUrl());
            
            // 构建请求头
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(120)); // 读取超时：120秒
            
            // 设置请求头
            if (request.getHeaders() != null) {
                request.getHeaders().forEach((key, value) -> {
                    if (StringUtils.hasText(key) && value != null) {
                        // 排除一些不应该转发的头
                        String lowerKey = key.toLowerCase();
                        if (!lowerKey.equals("host") && 
                            !lowerKey.equals("connection") && 
                            !lowerKey.equals("content-length") &&
                            !lowerKey.equals("transfer-encoding")) {
                            requestBuilder.header(key, value.toString());
                        }
                    }
                });
            }
            
            // 处理请求体
            if (request.getBody() != null && !"GET".equals(method) && !"HEAD".equals(method)) {
                byte[] bodyBytes;
                String contentType = null;
                
                // 确定 Content-Type
                if (request.getHeaders() != null) {
                    Object contentTypeObj = request.getHeaders().get("Content-Type");
                    if (contentTypeObj != null) {
                        contentType = contentTypeObj.toString();
                    }
                }
                
                // 处理请求体
                if (request.getBody() instanceof String) {
                    String bodyStr = (String) request.getBody();
                    bodyBytes = bodyStr.getBytes("UTF-8");
                    if (contentType == null) {
                        // 尝试判断是否为JSON
                        if (bodyStr.trim().startsWith("{") || bodyStr.trim().startsWith("[")) {
                            contentType = "application/json;charset=UTF-8";
                        } else {
                            contentType = "text/plain;charset=UTF-8";
                        }
                    }
                } else {
                    // 对于对象，序列化为 JSON
                    bodyBytes = objectMapper.writeValueAsBytes(request.getBody());
                    if (contentType == null) {
                        contentType = "application/json;charset=UTF-8";
                    }
                }
                
                // 设置 Content-Type
                if (contentType != null) {
                    requestBuilder.header("Content-Type", contentType);
                }
                
                // 设置请求体
                requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
            } else {
                // 没有请求体，使用 GET 等方法
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            
            // 如果没有设置 Accept 头，根据 Content-Type 设置合适的 Accept 头
            boolean hasAccept = false;
            if (request.getHeaders() != null) {
                hasAccept = request.getHeaders().containsKey("Accept") || 
                           request.getHeaders().containsKey("accept");
            }
            
            if (!hasAccept) {
                String contentType = null;
                if (request.getHeaders() != null) {
                    Object contentTypeObj = request.getHeaders().get("Content-Type");
                    if (contentTypeObj != null) {
                        contentType = contentTypeObj.toString().toLowerCase();
                    }
                }
                
                // 如果请求体是 JSON，也检查 Content-Type
                if (contentType == null && request.getBody() != null && 
                    !(request.getBody() instanceof String)) {
                    contentType = "application/json";
                }
                
                if (contentType != null && contentType.contains("application/json")) {
                    // 如果发送的是JSON，期望接收的也是JSON
                    requestBuilder.header("Accept", "application/json, */*");
                } else {
                    // 其他情况，接受所有类型
                    requestBuilder.header("Accept", "*/*");
                }
            }
            
            // 构建请求
            HttpRequest httpRequest = requestBuilder.build();
            
            // 发送请求，接收字节数组响应
            HttpResponse<byte[]> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofByteArray()
            );
            
            // 设置响应状态码
            httpResponse.setStatus(response.statusCode());
            
            // 复制所有原始响应头（排除一些不应该转发的头）
            response.headers().map().forEach((key, values) -> {
                // 排除一些控制头，避免影响代理行为
                if (!key.equalsIgnoreCase("connection") && 
                    !key.equalsIgnoreCase("transfer-encoding") &&
                    !key.equalsIgnoreCase("content-encoding") &&
                    !key.equalsIgnoreCase("content-length")) {
                    // 设置响应头
                    for (String value : values) {
                        httpResponse.addHeader(key, value);
                    }
                }
            });
            
            // 直接输出原始响应体
            if (response.body() != null && response.body().length > 0) {
                httpResponse.getOutputStream().write(response.body());
                httpResponse.getOutputStream().flush();
            }
            
        } catch (Exception e) {
            log.error("转发请求失败", e);
            try {
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                httpResponse.setContentType("text/plain;charset=UTF-8");
                httpResponse.getWriter().write("转发请求失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }
    
    
    /**
     * 转发请求参数
     */
    public static class ProxyRequest {
        private String url;
        private String method = "GET";
        private Map<String, Object> headers;
        private Object body;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public Map<String, Object> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, Object> headers) {
            this.headers = headers;
        }
        
        public Object getBody() {
            return body;
        }
        
        public void setBody(Object body) {
            this.body = body;
        }
    }
}

