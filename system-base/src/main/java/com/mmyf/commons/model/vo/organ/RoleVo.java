package com.mmyf.commons.model.vo.organ;

import com.mmyf.commons.model.entity.role.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.Set;

/**
 * package com.mmyf.commons.model.vo.organ
 * description: TODO
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-02-28 23:08:57
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色VO")
public class RoleVo extends Role {

    public RoleVo(Role role) {
        BeanUtils.copyProperties(role, this);
    }

    @Schema(description = "角色关联的权限")
    private Set<String> roleRightKeyList;
}
