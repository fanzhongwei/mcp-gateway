package com.mmyf.commons.service.role;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.injector.methods.UpdateById;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.mmyf.commons.mapper.role.RoleRightMapper;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.model.entity.role.Role;
import com.mmyf.commons.mapper.role.RoleMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mmyf.commons.model.entity.role.RoleRight;
import com.mmyf.commons.model.vo.organ.RoleVo;
import com.mmyf.commons.service.DbOperateService;
import com.mmyf.commons.service.organ.OrganCache;
import com.mmyf.commons.service.organ.UserService;
import com.mmyf.commons.util.SysUtils;
import com.mmyf.commons.util.uuid.UUIDHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色定义 服务实现类
 * </p>
 *
 * @author mmyf
 * @since 2024-02-26
 */
@Service
@Slf4j
public class RoleService extends ServiceImpl<RoleMapper, Role> implements IService<Role> {

    @Autowired
    private DbOperateService dbOperateService;

    @Resource
    private RoleRightMapper roleRightMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganCache organCache;

    /**
     * 保存角色信息
     *
     * @param roleVo 角色信息
     * @return
     */
    public boolean saveRole(RoleVo roleVo) {
        List<IEntity> insertList = new ArrayList<>();
        if (StringUtils.isBlank(roleVo.getId())) {
            roleVo.setId(UUIDHelper.getUuid());
        }
        Set<String> roleRightKeySet = roleVo.getRoleRightKeyList();
        if (CollectionUtils.isNotEmpty(roleRightKeySet)) {
            roleRightKeySet = roleRightKeySet.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (CollectionUtils.isNotEmpty(roleRightKeySet)) {
            roleRightKeySet.forEach(rightKey -> {
                RoleRight roleRight = new RoleRight();
                roleRight.setId(UUIDHelper.getUuid());
                roleRight.setRoleId(roleVo.getId());
                roleRight.setRightkey(rightKey);
                insertList.add(roleRight);
            });
        }
        dbOperateService.executeInTransaction(() -> {
            this.save(roleVo);
            dbOperateService.batchInsert(insertList);
        });
        return true;
    }

    /**
     * 更新角色信息
     *
     * @param roleVo 角色信息
     * @return
     */
    public boolean updateRole(RoleVo roleVo) {
        Set<String> roleRightKeySet = roleVo.getRoleRightKeyList();
        if (CollectionUtils.isNotEmpty(roleRightKeySet)) {
            roleRightKeySet = roleRightKeySet.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        List<RoleRight> existRoleRights = roleRightMapper.selectList(Wrappers.<RoleRight>lambdaQuery().eq(RoleRight::getRoleId, roleVo.getId()));
        Set<String> existRightKeys = existRoleRights.stream().map(RoleRight::getRightkey).collect(Collectors.toSet());
        Set<String> targetRightKeys = CollectionUtils.isNotEmpty(roleRightKeySet) ? roleRightKeySet : Collections.emptySet();

        Set<String> insertRightKeys = new LinkedHashSet<>(targetRightKeys);
        insertRightKeys.removeAll(existRightKeys);

        Set<String> deleteRightKeys = new LinkedHashSet<>(existRightKeys);
        deleteRightKeys.removeAll(targetRightKeys);

        List<IEntity> insertList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(insertRightKeys)) {
            insertRightKeys.forEach(rightKey -> {
                RoleRight roleRight = new RoleRight();
                roleRight.setId(UUIDHelper.getUuid());
                roleRight.setRoleId(roleVo.getId());
                roleRight.setRightkey(rightKey);
                insertList.add(roleRight);
            });
        }

        dbOperateService.executeInTransaction(() -> {
            this.updateById(roleVo);
            if (CollectionUtils.isNotEmpty(deleteRightKeys)) {
                roleRightMapper.delete(Wrappers.<RoleRight>lambdaQuery()
                        .eq(RoleRight::getRoleId, roleVo.getId())
                        .in(RoleRight::getRightkey, deleteRightKeys));
            }
            if (CollectionUtils.isNotEmpty(insertList)) {
                dbOperateService.batchInsert(insertList);
            }
        });
        return true;
    }

    /**
     * 删除角色，同步删除角色关联的权限
     *
     * @param id
     * @return
     */
    public boolean deleteRole(String id) {
        dbOperateService.executeInTransaction(() -> {
            this.removeById(id);
            roleRightMapper.delete(Wrappers.<RoleRight>lambdaQuery().eq(RoleRight::getRoleId, id));
            UpdateWrapper<User> userUpdateWrapper = Wrappers.<User>update()
                                                .setSql(SysUtils.stringFormat("j_role_id = JSON_REMOVE(j_role_id, JSON_UNQUOTE(JSON_SEARCH(j_role_id, 'one', '{}', NULL, '$[*]')))", id))
                                                .apply(SysUtils.stringFormat("JSON_CONTAINS(j_role_id, '\"{}\"')", id));
            userService.update(userUpdateWrapper);
        });
        organCache.reload();
        return true;
    }

    /**
     * 查询role对应的角色
     *
     * @param roleList role集合
     * @return com.mmyf.commons.model.vo.organ.RoleVo
     * @author fanzhongwei
     * @date 2024/3/4 下午5:42
     **/
    public List<RoleVo> selectRoleRight(List<Role> roleList) {
        if (CollectionUtils.isEmpty(roleList)) {
            return Collections.emptyList();
        }
        List<String> roleIdList = roleList.stream().map(Role::getId).collect(Collectors.toList());
        List<RoleRight> roleRightList = roleRightMapper.selectList(Wrappers.<RoleRight>lambdaQuery().in(RoleRight::getRoleId, roleIdList));
        Map<String, Set<String>> roleRightKeyMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(roleRightList)) {
            roleRightList.forEach(roleRight -> roleRightKeyMap.computeIfAbsent(roleRight.getRoleId(), k -> new LinkedHashSet<>()).add(roleRight.getRightkey()));
        }
        return roleList.stream().map(role -> {
            RoleVo vo = new RoleVo(role);
            vo.setRoleRightKeyList(roleRightKeyMap.getOrDefault(role.getId(), Collections.emptySet()));
            return vo;
        }).collect(Collectors.toList());
    }
}
