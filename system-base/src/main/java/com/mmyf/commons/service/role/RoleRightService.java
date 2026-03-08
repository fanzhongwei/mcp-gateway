package com.mmyf.commons.service.role;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mmyf.commons.model.entity.organ.Right;
import com.mmyf.commons.model.entity.role.RoleRight;
import com.mmyf.commons.mapper.role.RoleRightMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mmyf.commons.service.right.RightService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色权限映射 服务实现类
 * </p>
 *
 * @author mmyf
 * @since 2024-02-26
 */
@Service
@Slf4j
public class RoleRightService extends ServiceImpl<RoleRightMapper, RoleRight> implements IService<RoleRight> {

    @Autowired
    private RightService rightService;

    public List<Right> selectRightByRoleId(String roleId) {
        if (StringUtils.isBlank(roleId)) {
            return Collections.emptyList();
        }
        List<RoleRight> roleRights = this.list(Wrappers.<RoleRight>lambdaQuery().eq(RoleRight::getRoleId, roleId));
        if (CollectionUtils.isEmpty(roleRights)) {
            return Collections.emptyList();
        }
        List<String> rightKeys = roleRights.stream().map(RoleRight::getRightkey).collect(Collectors.toList());
        return rightService.listByIds(rightKeys);
    }
}
