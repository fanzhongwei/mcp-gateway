package com.mmyf.commons.service.code;

import com.mmyf.commons.model.entity.code.Code;
import com.mmyf.commons.model.entity.code.CodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * package com.mmyf.commons.service.code
 * description: 单值代码缓存模型
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-03-24 16:00:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单值代码缓存模型")
public class CodeCacheModel implements Serializable {
    private static final long serialVersionUID = -3112739069342907921L;

    @Schema(description = "代码类型")
    private CodeType codeType;

    @Schema(description = "码值-对象映射")
    private Map<String, Code> codeMap = new LinkedHashMap<>();

    @Schema(description = "码值名称-对象映射")
    private Map<String, Code> codeNameMap = new LinkedHashMap<>();
}
