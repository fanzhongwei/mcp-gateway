package com.mmyf.commons.model.vo.organ;

import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.entity.organ.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;

/**
 * package com.mmyf.model.vo
 * description: user vo
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 15:30:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class UserVo extends User implements BaseOrgan {

    private SystemCodeEnum.OrganType type = SystemCodeEnum.OrganType.USER;

    public UserVo(User user) {
        BeanUtils.copyProperties(user, this);
    }


    @Override
    public List<BaseOrgan> getChildren() {
        return Collections.emptyList();
    }
}
