package com.mmyf.commons.model.vo.organ;

import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.entity.organ.Tenant;
import com.mmyf.commons.translate.annotation.DeepTranslate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

/**
 * package com.mmyf.model.vo
 * description: TenantVo
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 15:06:31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "租户")
@DeepTranslate
public class TenantVo extends Tenant implements BaseOrgan {

    private SystemCodeEnum.OrganType type = SystemCodeEnum.OrganType.TENANT;

    @Schema(description = "子节点")
    private List<BaseOrgan> children = new ArrayList<>();

    public TenantVo(Tenant tenant) {
        BeanUtils.copyProperties(tenant, this);
    }

    public void addChild(BaseOrgan organ) {
        children.add(organ);
    }

}
