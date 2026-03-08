package com.mmyf.mcp.model.dto.mcp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 服务中关联的 API 配置项
 * <p>
 * 与 {@code t_mcp_config_api.j_api_config} JSON 字段对应，结构示例：
 * [{ "apiId": "xxx", "customMcpName": "yyy" }, ...]
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MCP 服务 API 配置项")
public class McpApiConfigItem {

    /**
     * API 接口 ID
     */
    @Schema(description = "API 接口 ID")
    private String apiId;

    /**
     * 针对该 API 的自定义 MCP 名称（可选）
     */
    @Schema(description = "自定义 MCP 名称（可选）")
    private String customMcpName;
}

