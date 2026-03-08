package com.mmyf.commons.model.vo.organ;

import com.mmyf.commons.model.entity.organ.Right;
import com.mmyf.commons.model.entity.organ.User;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

/**
 * package com.mmyf.commons.model.vo <br/>
 * description: 权限VO <br/>
 *
 * @author Teddy
 * @date 2022/5/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户权限VO")
public class RightVo extends Right {

    public RightVo(Right right) {
        BeanUtils.copyProperties(right, this);
    }

    /**
     * 拥有该权限的人员ID
     */
    @Schema(description = "拥有该权限的人员ID")
    private Set<String> userIds;

}
