package com.mmyf.commons.model.vo.organ;

import com.mmyf.commons.constant.SystemCodeEnum;
import java.time.LocalDateTime;
import java.util.List;

/**
 * package com.mmyf.model.vo
 * description: BaseOrgan
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 15:00:15
 */
public interface BaseOrgan {

    /** 区分租户类型：tenant、dept、user */
    SystemCodeEnum.OrganType getType();

    String getId();

    String getName();

    Integer getNOrder();

    String getValid();

    LocalDateTime getCreateTime();

    LocalDateTime getModifyTime();

    List<BaseOrgan> getChildren();
}
