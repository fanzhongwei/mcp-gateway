package com.mmyf.commons.model.query.code;

import com.mmyf.commons.model.entity.code.CodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 代码类型分页查询参数
 * @date 2024/03/08 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "代码类型分页查询参数")
public class CodeTypePageQuery extends CodeType {

    @Schema(description = "代码类型名称模糊查询")
    private String nameLike;
}
