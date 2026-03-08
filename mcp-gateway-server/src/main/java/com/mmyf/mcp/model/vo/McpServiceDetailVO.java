package com.mmyf.mcp.model.vo;

import com.mmyf.mcp.model.entity.mcp.McpService;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MCP服务详情VO
 * 
 * @author Teddy
 * @date 2026-01-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServiceDetailVO extends McpService {
    
    /**
     * 配置API列表（包含关联的系统、环境信息）
     */
    private List<ConfigApiDetail> configApis;
    
    /**
     * 配置API详情
     */
    @Data
    public static class ConfigApiDetail {
        /**
         * 配置API ID
         */
        private String id;
        
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
         * 环境名称
         */
        private String envName;
        
        /**
         * 环境基础URL
         */
        private String envBaseUrl;
        
        /**
         * 系统级别的自定义MCP名字
         */
        private String customMcpName;
        
        /**
         * API详情列表
         */
        private List<ApiDetail> apis;
    }
    
    /**
     * API详情
     */
    @Data
    public static class ApiDetail {
        /**
         * API ID
         */
        private String id;
        
        /**
         * API名称
         */
        private String name;
        
        /**
         * HTTP方法
         */
        private String method;
        
        /**
         * 接口路径
         */
        private String path;
        
        /**
         * 接口级别的自定义MCP名字
         */
        private String customMcpName;
    }
}
