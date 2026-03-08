package com.mmyf.mcp.model.dto.mcp;

import com.mmyf.mcp.model.entity.system.Api;
import lombok.Data;
import java.util.Map;

/**
 * MCP工具定义
 *
 * @author Teddy
 * @date 2026-01-26
 */
@Data
public class McpToolMeta {
    /**
     * 工具名称，唯一标识
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 关联的API
     */
    private Api api;

    /**
     * 关联的API ID
     */
    private String apiId;

    /**
     * 系统ID
     */
    private String systemId;

    /**
     * 系统名称
     */
    private String systemName;

    /**
     * 环境ID
     */
    private String envId;

    /**
     * 环境基础URL
     */
    private String envBaseUrl;

    /**
     * API的HTTP方法
     */
    private String method;

    /**
     * API的路径
     */
    private String path;
}
