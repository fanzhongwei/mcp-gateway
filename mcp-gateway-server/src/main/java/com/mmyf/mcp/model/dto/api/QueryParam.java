package com.mmyf.mcp.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query参数结构
 * 对应前端保存的 queryParams 数组中的每个元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Query参数")
public class QueryParam {

    @JsonProperty("key")
    @Schema(description = "参数名")
    private String key;

    @JsonProperty("value")
    @Schema(description = "参数值")
    private String value;

    @JsonProperty("type")
    @Schema(description = "参数类型：string, number, array, object, boolean")
    private String type;

    @JsonProperty("description")
    @Schema(description = "参数说明")
    private String description;

    @JsonProperty("required")
    @Schema(description = "是否必填")
    private Boolean required;
}
