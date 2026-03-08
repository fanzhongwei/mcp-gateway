package com.mmyf.mcp.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JSON参数节点结构（树形结构）
 * 对应前端保存的 jsonParams 数组中的每个元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON参数节点")
public class JsonParamNode {

    @JsonProperty("id")
    @Schema(description = "节点ID")
    private String id;

    @JsonProperty("name")
    @Schema(description = "参数名")
    private String name;

    @JsonProperty("required")
    @Schema(description = "是否必填")
    private Boolean required;

    @JsonProperty("type")
    @Schema(description = "参数类型：string, number, boolean, object, array")
    private String type;

    @JsonProperty("example")
    @Schema(description = "示例值")
    private String example;

    @JsonProperty("description")
    @Schema(description = "参数说明")
    private String description;

    @JsonProperty("children")
    @Schema(description = "子节点（用于 object 和 array 类型）")
    private List<JsonParamNode> children;
}
