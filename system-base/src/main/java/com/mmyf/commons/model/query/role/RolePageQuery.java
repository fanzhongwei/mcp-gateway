package com.mmyf.commons.model.query.role;

import com.mmyf.commons.model.entity.role.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 角色定义分页参数
 *
 * @date 2024/03/08 13:59
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "代码类型分页查询参数")
public class RolePageQuery extends Role {

    @Schema(description = "角色名称模糊查询")
    private String nameLike;
}
