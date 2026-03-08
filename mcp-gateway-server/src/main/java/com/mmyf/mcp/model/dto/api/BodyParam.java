package com.mmyf.mcp.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body参数结构
 * 对应前端保存的 bodyParam 对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Body参数")
public class BodyParam {

    @JsonProperty("type")
    @Schema(description = "Body类型：json, form-data, x-www-form-urlencoded, raw")
    private String type;

    @JsonProperty("jsonParams")
    @Schema(description = "JSON参数列表（树形结构），当 type=json 时使用")
    private List<JsonParamNode> jsonParams;

    @JsonProperty("formDataParams")
    @Schema(description = "Form-data参数列表，当 type=form-data 时使用")
    private List<FormDataParam> formDataParams;

    @JsonProperty("urlEncodedParams")
    @Schema(description = "URL编码参数列表，当 type=x-www-form-urlencoded 时使用")
    private List<UrlEncodedParam> urlEncodedParams;

    @JsonProperty("raw")
    @Schema(description = "原始数据，当 type=raw 时使用")
    private String raw;
}
