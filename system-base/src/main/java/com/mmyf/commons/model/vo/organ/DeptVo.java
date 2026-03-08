package com.mmyf.commons.model.vo.organ;

import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.entity.organ.Dept;
import com.mmyf.commons.translate.annotation.DeepTranslate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

/**
 * package com.mmyf.model.vo
 * description: dept vo
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 15:10:32
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "租户--部门")
@DeepTranslate
public class DeptVo extends Dept implements BaseOrgan {

    private SystemCodeEnum.OrganType type = SystemCodeEnum.OrganType.DEPT;

    @Schema(description = "子节点，可能是dept也可能是user，根据type进行区分")
    private List<BaseOrgan> children = new ArrayList<>();

    public DeptVo(Dept dept) {
        BeanUtils.copyProperties(dept, this);
    }

    public void addChild(BaseOrgan organ) {
        children.add(organ);
    }

}
