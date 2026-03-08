package com.mmyf.mcp.model.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 响应配置结构
 * 对应前端保存的 responseConfig 对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "响应配置")
public class ResponseConfig {

    @JsonProperty("validateResponse")
    @Schema(description = "是否校验响应")
    private Boolean validateResponse;

    @JsonProperty("successStatus")
    @Schema(description = "成功状态码（如：200, 201, 204）")
    private String successStatus;
}
