package com.mmyf.commons.model.vo.organ;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * 用户角色VO
 *
 * @author fanzhongwei
 * @date 2024/03/01 16:03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户角色VO")
public class UserRoleVo {

    @Schema(description = "用户ID")
    @NotEmpty(message = "用户ID不能为空")
    private String userId;

    @Schema(description = "用户拥有的角色，为空时表示清空用户拥有的角色")
    private Set<String> roleId;
}
