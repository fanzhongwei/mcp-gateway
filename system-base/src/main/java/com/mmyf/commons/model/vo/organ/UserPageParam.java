package com.mmyf.commons.model.vo.organ;

import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.validator.CodeValidate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.groups.Default;

/**
 * package com.mmyf.commons.model.vo.organ
 * description: 用户分页查询参数
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-02-28 22:36:49
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户分页查询参数")
public class UserPageParam {

    @Schema(description = "用户名或登录名模糊查询")
    private String nameOrLoginIdLike;

    @Schema(description = "用户类型，代码类型：100002")
    @CodeValidate(message = "用户类型，代码类型：100002，码值不合法", groups = {PageParam.ValidateGroup.class})
    private String userType;

    @Schema(description = "所属租户")
    private String tenant;

    @Schema(description = "所属部门")
    private String dept;
}
