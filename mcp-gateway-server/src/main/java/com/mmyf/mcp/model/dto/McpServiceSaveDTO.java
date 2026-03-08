package com.mmyf.mcp.model.dto;

import lombok.Data;

import java.util.List;

/**
 * MCP服务保存DTO
 * 
 * @author Teddy
 * @date 2026-01-26
 */
@Data
public class McpServiceSaveDTO {
    
    /**
     * 服务ID（更新时必填）
     */
    private String id;
    
    /**
     * 服务名称
     */
    private String name;
    
    /**
     * 服务描述
     */
    private String desc;
    
    /**
     * 服务状态
     */
    private String status;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 服务端点
     */
    private String endpoint;
    
    /**
     * 配置选择列表
     */
    private Config config;
    
    @Data
    public static class Config {
        /**
         * 选择列表
         */
        private List<Selection> selections;
    }
    
    @Data
    public static class Selection {
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
         * API ID列表
         */
        private List<String> apiIds;
        
        /**
         * 系统级别的自定义MCP名字
         */
        private String customMcpName;
        
        /**
         * 接口ID到自定义MCP名字的映射
         */
        private java.util.Map<String, String> apiCustomMcpNames;
    }
}
