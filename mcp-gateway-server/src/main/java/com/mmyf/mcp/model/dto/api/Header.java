package com.mmyf.mcp.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求头结构
 * 对应前端保存的 headers 数组中的每个元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "请求头")
public class Header {

    @JsonProperty("key")
    @Schema(description = "请求头名称")
    private String key;

    @JsonProperty("value")
    @Schema(description = "请求头值")
    private String value;

    @JsonProperty("description")
    @Schema(description = "请求头说明")
    private String description;

    @JsonProperty("required")
    @Schema(description = "是否必填")
    private Boolean required;
}
