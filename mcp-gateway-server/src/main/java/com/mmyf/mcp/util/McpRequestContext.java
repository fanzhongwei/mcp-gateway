package com.mmyf.mcp.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * MCP请求上下文工具类
 * 用于在多租户场景中传递serviceId等信息
 *
 * @author Teddy
 * @date 2026-01-26
 */
public class McpRequestContext {
    
    private static final String SERVICE_ID_KEY = "mcp.serviceId";
    private static final String SERVICE_KEY = "mcp.service";
    
    /**
     * 设置服务ID到请求上下文
     */
    public static void setServiceId(String serviceId) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(SERVICE_ID_KEY, serviceId, RequestAttributes.SCOPE_REQUEST);
        }
    }
    
    /**
     * 从请求上下文获取服务ID
     */
    public static String getServiceId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return (String) attributes.getAttribute(SERVICE_ID_KEY, RequestAttributes.SCOPE_REQUEST);
        }
        return null;
    }
    
    /**
     * 设置服务对象到请求上下文
     */
    public static void setService(Object service) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(SERVICE_KEY, service, RequestAttributes.SCOPE_REQUEST);
        }
    }
    
    /**
     * 从请求上下文获取服务对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> serviceClass) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return (T) attributes.getAttribute(SERVICE_KEY, RequestAttributes.SCOPE_REQUEST);
        }
        return null;
    }
    
    /**
     * 清除请求上下文
     */
    public static void clear() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.removeAttribute(SERVICE_ID_KEY, RequestAttributes.SCOPE_REQUEST);
            attributes.removeAttribute(SERVICE_KEY, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
