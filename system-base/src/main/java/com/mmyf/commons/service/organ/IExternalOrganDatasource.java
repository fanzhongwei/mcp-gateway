package com.mmyf.commons.service.organ;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mmyf.commons.model.entity.organ.Tenant;
import com.mmyf.commons.model.entity.organ.Dept;
import com.mmyf.commons.model.entity.organ.User;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * package com.mmyf.commons.service.organ
 * description: 外部租户数据源
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-02-27 21:36:02
 */
public interface IExternalOrganDatasource {

    /**
     * 刷新外部租户
     */
    void refreshOrgan();

    /**
     * 外部Tenant数据源
     * @return 所有Tenant
     */
    List<? extends Tenant> loadAllTenant();

    /**
     * 外部Dept数据源
     * @return 所有Dept
     */
    List<? extends Dept> loadAllDept();

    /**
     * 外部User数据源
     * @return 所有User
     */
    List<? extends User> loadAllUser();
}
