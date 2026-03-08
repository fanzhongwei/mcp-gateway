package com.mmyf.mcp.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cookie参数结构
 * 对应前端保存的 cookies 数组中的每个元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cookie参数")
public class Cookie {

    @JsonProperty("key")
    @Schema(description = "Cookie名称")
    private String key;

    @JsonProperty("value")
    @Schema(description = "Cookie值")
    private String value;

    @JsonProperty("description")
    @Schema(description = "Cookie说明")
    private String description;

    @JsonProperty("required")
    @Schema(description = "是否必填")
    private Boolean required;
}
