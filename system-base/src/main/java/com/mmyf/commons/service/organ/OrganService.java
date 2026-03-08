package com.mmyf.commons.service.organ;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.entity.organ.Tenant;
import com.mmyf.commons.model.entity.organ.Dept;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.model.entity.organ.UserRight;
import com.mmyf.commons.model.vo.organ.*;
import com.mmyf.commons.service.DbOperateService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import com.mmyf.commons.service.right.RightService;
import com.mmyf.commons.util.uuid.UUIDHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * package com.mmyf.service.organ
 * description: OrganService
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 15:33:02
 */
@Service
@Slf4j
public class OrganService {

    @Resource
    private OrganCache organCache;

    @Autowired
    private RightService rightService;

    @Autowired
    private UserService userService;

    @Autowired
    private DbOperateService dbOperateService;

    @Resource
    private UserRightService userRightService;

    /**
     * 获取租户根节点
     *
     * @return java.lang.String
     * @date 2024/3/5 下午5:00
     **/
    public String getRootTenantId() {
        return organCache.getTreeRoot().getId();
    }

    /**
     * 根据id获取tenant
     *
     * @param id tenant id
     * @return tenant
     */
    public Tenant getTenant(@NotNull String id) {
        return organCache.getTenant(id);
    }

    /**
     * 根据diexternalNid获取tenant
     * @param externalNid externalNid
     * @return tenant
     */
    public Tenant getTenantByExternalNid(@NotNull Long externalNid) {
        return organCache.getTenantByExternalNid(externalNid);
    }

    /**
     * 根据id获取tenant树
     * @param id tenant id
     * @return tenant tree
     */
    public TenantVo getTreeTenant(@NotNull String id) {
        return organCache.getTreeTenant(id);
    }

    /**
     * 根据tenant获取tenant树
     * @param tenant tenant do
     * @return tenant tree
     */
    public TenantVo getTreeTenant(@NotNull Tenant tenant) {
        return organCache.getTreeTenant(tenant.getId());
    }

    /**
     * 根据id获取dept
     * @param id dept id
     * @return dept
     */
    public Dept getDept(@NotNull String id) {
        return organCache.getDept(id);
    }

    /**
     * 根据id获取Dept树
     * @param id Dept id
     * @return Dept tree
     */
    public DeptVo getTreeDept(@NotNull String id) {
        return organCache.getTreeDept(id);
    }

    /**
     * 根据Dept获取Dept树
     * @param dept Dept do
     * @return Dept tree
     */
    public DeptVo getTreeDept(@NotNull Dept dept) {
        return organCache.getTreeDept(dept.getId());
    }

    /**
     * 根据id获取用户
     *
     * @param id user id
     * @return user
     */
    public User getUser(@NotNull String id) {
        return organCache.getUser(id);
    }

    /**
     * 根据登陆ID获取用户
     *
     * @param loginId 登陆ID
     * @return User
     */
    public User getUserByLoginId(@NotNull String loginId) {
        return organCache.getUserByLoginId(loginId);
    }

    /**
     * 根据user id 获取所属部门
     *
     * @param id user id
     * @return 所属部门
     */
    public Dept getUserDept(@NotNull String id) {
        return getDept(getUser(id).getDept());
    }

    /**
     * 根据user 获取所属部门
     * @param user user
     * @return 所属部门
     */
    public Dept getUserDept(@NotNull User user) {
        return getDept(user.getDept());
    }

    /**
     * 根据user id 获取所属租户
     * @param id user id
     * @return 所属租户
     */
    public Tenant getUserTenant(@NotNull String id) {
        return getTenant(getUser(id).getTenant());
    }

    /**
     * 根据user 获取所属租户
     * @param user user
     * @return 所属租户
     */
    public Tenant getUserTenant(@NotNull User user) {
        return getTenant(user.getTenant());
    }

    /**
     * 更新tenant
     *
     * @param tenant tenant
     */
    public void updateTenant(@NotNull Tenant tenant) {
        organCache.updateTenant(tenant);
    }

    /**
     * 新增tenant
     * @param tenant tenant
     */
    public void insertTenant(@NotNull Tenant tenant) {
        Assert.hasLength(tenant.getPid(), "租户必须有父节点，请检查");
        organCache.insertTenant(tenant);
    }

    /**
     * 删除tenant，同时删除tenant下的dept和user
     * @param tenantId tenantId
     */
    public void deleteTenant(@NotNull String tenantId) {
        organCache.deleteTenant(tenantId);
    }

    /**
     * 更新dept
     * @param dept Dept
     */
    public void updateDept(@NotNull Dept dept) {
        organCache.updateDept(dept);
    }

    /**
     * 添加dept
     *
     * @param dept Dept
     */
    public void insertDept(@NotNull Dept dept) {
        Assert.hasLength(dept.getTenant(), "部门必须有所属租户，请检查");
        organCache.insertDept(dept);
    }

    /**
     * 删除部门，同时删除部门下的user
     * @param id dept id
     */
    public void deleteDept(@NotNull String id) {
        organCache.deleteDept(id);
    }

    /**
     * 更新user（同步用户权限：按用户角色同步 t_user_right）
     * @param user User
     */
    public void updateUser(@NotNull User user) {
        organCache.updateUser(user);
        syncUserRoleToRight(user.getId(), user.getRoleId() != null ? user.getRoleId() : Collections.emptySet());
    }

    /**
     * 新增user（同步用户权限：按用户角色同步 t_user_right）
     * @param user User
     */
    public void insertUser(@NotNull User user) {
        Assert.hasLength(user.getTenant(), "用户必须有所属租户，请检查");
        organCache.insertUser(user);
        syncUserRoleToRight(user.getId(), user.getRoleId() != null ? user.getRoleId() : Collections.emptySet());
    }

    /**
     * 批量添加用户
     * @param userList 用户集合
     */
    public void insertUser(@NotNull List<User> userList) {
        userService.saveBatch(userList);
        organCache.reload();
    }

    /** mcpAdmin 账号不允许删除 */
    private static final String LOGIN_ID_MCP_ADMIN = "mcpAdmin";

    /**
     * 删除user（mcpAdmin 用户不允许删除）
     *
     * @param id User id
     */
    public void deleteUser(@NotNull String id) {
        User user = getUser(id);
        if (user != null && LOGIN_ID_MCP_ADMIN.equals(user.getLoginid())) {
            throw new IllegalArgumentException("mcpAdmin 用户不允许删除");
        }
        dbOperateService.executeInTransaction(() -> {
            organCache.deleteUser(id);
            rightService.deleteUserRightAndRole(id);
        });
    }

    /**
     * 获取所有子tenantId
     *
     * @param tenantId 根节点tenantId
     * @return java.util.List<java.lang.String>
     * @date 2024/3/5 上午11:44
     **/
    public List<String> getAllSubTenantId(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return Collections.emptyList();
        }
        TenantVo treeTenant = getTreeTenant(tenantId);
        List<BaseOrgan> allSubTenant = getAllSubOrganByOrganType(treeTenant, SystemCodeEnum.OrganType.TENANT);
        return allSubTenant.stream().map(BaseOrgan::getId).collect(Collectors.toList());
    }

    /**
     * 获取所有子tenantId和当前tenantId
     *
     * @param tenantId 根节点tenantId
     * @return java.util.List<java.lang.String>
     * @date 2024/3/5 上午11:44
     **/
    public List<String> getTenantAndAllSubTenantId(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return Collections.emptyList();
        }
        List<String> tenantIdList = new ArrayList<>();
        tenantIdList.add(tenantId);
        tenantIdList.addAll(getAllSubTenantId(tenantId));
        return tenantIdList;
    }

    /**
     * 获取所有子deptId
     *
     * @param deptId 根节点deptId
     * @return java.util.List<java.lang.String>
     * @date 2024/3/5 上午11:44
     **/
    public List<String> getAllSubDeptId(String deptId) {
        if (StringUtils.isBlank(deptId)) {
            return Collections.emptyList();
        }
        DeptVo treeDept = getTreeDept(deptId);
        List<BaseOrgan> allSubTenant = getAllSubOrganByOrganType(treeDept, SystemCodeEnum.OrganType.DEPT);
        return allSubTenant.stream().map(BaseOrgan::getId).collect(Collectors.toList());
    }

    /**
     * 获取所有子deptId和当前deptId
     *
     * @param deptId 根节点deptId
     * @return java.util.List<java.lang.String>
     * @date 2024/3/5 上午11:44
     **/
    public List<String> getDeptAndAllSubDeptId(String deptId) {
        if (StringUtils.isBlank(deptId)) {
            return Collections.emptyList();
        }
        List<String> deptIdList = new ArrayList<>();
        deptIdList.add(deptId);
        deptIdList.addAll(getAllSubDeptId(deptId));
        return deptIdList;
    }

    /**
     * 获取所有子deptId
     *
     * @param tenantId 根节点tenantId
     * @return java.util.List<java.lang.String>
     * @date 2024/3/5 上午11:44
     **/
    public List<String> getAllSubDeptIdByTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return Collections.emptyList();
        }
        TenantVo treeTenant = getTreeTenant(tenantId);
        List<BaseOrgan> allSubTenant = getAllSubOrganByOrganType(treeTenant, SystemCodeEnum.OrganType.DEPT);
        return allSubTenant.stream().map(BaseOrgan::getId).collect(Collectors.toList());
    }

    private List<BaseOrgan> getAllSubOrganByOrganType(BaseOrgan baseOrgan, SystemCodeEnum.OrganType organType) {
        if (baseOrgan == null || baseOrgan.getType() != organType) {
            return Collections.emptyList();
        }
        List<BaseOrgan> subOrganList = new ArrayList<>();
        // 当前层级比目标层级高，继续找子节点
        if (baseOrgan.getType().ordinal() <= organType.ordinal()) {
            if (CollectionUtils.isNotEmpty(baseOrgan.getChildren())) {
                baseOrgan.getChildren().forEach(currOrgan -> {
                    // 当前层级比目标层级高，加入结果集，继续找子节点
                    if (currOrgan.getType().ordinal() <= organType.ordinal()) {
                        subOrganList.add(currOrgan);
                        subOrganList.addAll(getAllSubOrganByOrganType(currOrgan, organType));
                    }
                });
            }
        }
        return subOrganList;
    }

    /**
     * 根据用户租户权限获取租户树
     *
     * @param user User
     * @return BaseOrgan
     */
    public BaseOrgan getOrganTreeByUserRight(User user) {
        TenantVo treeTenant = getTreeTenant(user.getTenant());
        // 如果用户直属于Tenant，那么直接返回TenantTree
        if (StringUtils.isNotBlank(user.getTenant()) && StringUtils.isBlank(user.getDept())) {
            return treeTenant;
        }

        // 否则需要进行过滤，仅保留当前部门及其子节点
        // 先拷贝一份
        DeptVo treeDept = getTreeDept(user.getDept());

        while (StringUtils.isNotBlank(treeDept.getPid())) {
            // 组装父dept
            DeptVo deptVo = new DeptVo(getDept(treeDept.getPid()));
            // 只保留一个节点
            deptVo.setChildren(Collections.singletonList(treeDept));
            treeDept = deptVo;
        }
        TenantVo tenantVo = new TenantVo(getTenant(user.getTenant()));
        // 保留一个节点
        tenantVo.setChildren(Collections.singletonList(treeDept));
        return tenantVo;
    }

    /**
     * 获取当前传入的用户拥有哪些租户的操作权限
     *
     * @param userInfo 用户信息
     * @return 拥有的租户操作权限 Map<name, BaseOrgan>
     */
    public Map<String, BaseOrgan> getHasRightOrganNameMap(User userInfo) {
        BaseOrgan organTreeByUserRight = this.getOrganTreeByUserRight(userInfo);
        return this.getHasRightOrganNameMap(organTreeByUserRight, userInfo, false);
    }

    /**
     * 获取当前传入的用户拥有哪些租户的操作权限
     *
     * @param userInfo 用户信息
     * @return 拥有的租户操作权限 Map<id, BaseOrgan>
     */
    public Map<String, BaseOrgan> getHasRightOrganIdMap(User userInfo) {
        Map<String, BaseOrgan> hasRightOrganNameMap = this.getHasRightOrganNameMap(userInfo);
        return hasRightOrganNameMap.values().stream().collect(Collectors.toMap(BaseOrgan::getId, Function.identity()));
    }

    /**
     * 获取当前传入的用户拥有哪些租户的操作权限
     *
     * @param userInfo 用户信息
     * @return 拥有的租户操作权限  List<organ id>
     */
    public List<String> getHasRightOrganId(User userInfo) {
        Map<String, BaseOrgan> hasRightOrganNameMap = this.getHasRightOrganNameMap(userInfo);
        return hasRightOrganNameMap.values().stream().map(BaseOrgan::getId).collect(Collectors.toList());
    }

    private Map<String, BaseOrgan> getHasRightOrganNameMap(BaseOrgan organTreeByUserRight, User userInfo,
            boolean parentHashRight) {
        Map<String, BaseOrgan> organMap = Maps.newHashMap();
        if (organTreeByUserRight instanceof TenantVo) {
            TenantVo tenantVo = (TenantVo) organTreeByUserRight;
            // 父级具有权限，子节点也就具有权限了
            // 如果用户直属于tenant，那么当前tenant具有操作权限
            if (parentHashRight || StringUtils.isNotBlank(userInfo.getTenant()) && StringUtils.isBlank(userInfo.getDept())
                    && StringUtils.equals(tenantVo.getId(), userInfo.getTenant())) {
                // 添加当前节点
                organMap.put(tenantVo.getName(), tenantVo);
                // 当前的子节点也具有操作权限
                if (CollectionUtils.isNotEmpty(tenantVo.getChildren())) {
                    tenantVo.getChildren().forEach(vo -> organMap.putAll(getHasRightOrganNameMap(vo, userInfo, true)));
                }
                // 当前节点下的所有节点都添加完成，直接返回
                return organMap;
            }
            // 寻找有权限的子节点
            if (CollectionUtils.isNotEmpty(tenantVo.getChildren())) {
                tenantVo.getChildren().forEach(vo -> organMap.putAll(getHasRightOrganNameMap(vo, userInfo, false)));
            }
        }
        if (organTreeByUserRight instanceof DeptVo) {
            DeptVo deptVo =  (DeptVo)organTreeByUserRight;
            // 父级具有权限，子节点也就具有权限了
            // 用户直属与dept，那么当前dept具有操作权限
            if (parentHashRight || StringUtils.equals(deptVo.getId(), userInfo.getDept())) {
                // 添加当前节点
                organMap.put(deptVo.getName(), deptVo);
                // 当前的子节点也具有操作权限
                if (CollectionUtils.isNotEmpty(deptVo.getChildren())) {
                    deptVo.getChildren().forEach(vo -> organMap.putAll(getHasRightOrganNameMap(vo, userInfo, true)));
                }
                // 当前节点下的所有节点都添加完成，直接返回
                return organMap;
            }
            // 寻找有权限的子节点
            if (CollectionUtils.isNotEmpty(deptVo.getChildren())) {
                deptVo.getChildren().forEach(vo -> organMap.putAll(getHasRightOrganNameMap(vo, userInfo, false)));
            }
        }
        return organMap;
    }

    /**
     * 获取当前租户及其下辖租户ID
     *
     * @param organ id
     * @return 当前租户及其下辖租户ID
     */
    public List<String> getHasRightOrganIdByOrganId(String organ) {
        return new ArrayList<>(getHasRightOrganIdMapByOrganId(organ).keySet());
    }

    /**
     * 获取当前租户及其下辖租户
     *
     * @param organ id
     * @return 当前租户及其下辖租户
     */
    public List<BaseOrgan> getHasRightOrganByOrganId(String organ) {
        return new ArrayList<>(getHasRightOrganIdMapByOrganId(organ).values());
    }

    /**
     * 获取当前租户及其下辖租户
     *
     * @param organ id
     * @return 当前租户及其下辖租户 Map<id, BaseOrgan>
     */
    public Map<String, BaseOrgan> getHasRightOrganIdMapByOrganId(String organ) {
        TenantVo treeTenant = this.getTreeTenant(organ);
        if (null != treeTenant) {
            return getOrganIdMapFromTree(treeTenant);
        }
        DeptVo treeDept = this.getTreeDept(organ);
        if (null != treeDept) {
            return getOrganIdMapFromTree(treeDept);
        }
        return Maps.newHashMap();
    }

    private Map<String, BaseOrgan> getOrganIdMapFromTree(BaseOrgan tree) {
        Map<String, BaseOrgan> result = Maps.newHashMap();
        if (tree instanceof TenantVo) {
            TenantVo tenantVo = (TenantVo) tree;
            result.put(tree.getId(), tree);
            if (CollectionUtils.isNotEmpty(tenantVo.getChildren())) {
                tenantVo.getChildren().forEach(vo -> result.putAll(getOrganIdMapFromTree(vo)));
            }
        }
        if (tree instanceof DeptVo) {
            DeptVo deptVo = (DeptVo) tree;
            result.put(tree.getId(), tree);
            if (CollectionUtils.isNotEmpty(deptVo.getChildren())) {
                deptVo.getChildren().forEach(vo -> result.putAll(getOrganIdMapFromTree(vo)));
            }
        }
        return result;
    }


    public BaseOrgan getOrganById(String organId) {
        TenantVo treeTenant = this.getTreeTenant(organId);
        if (null != treeTenant) {
            return treeTenant;
        }
        DeptVo treeDept = this.getTreeDept(organId);
        if (null != treeDept) {
            return treeDept;
        }
        return null;
    }

    public List<User> listOrganUserByUserType(String organId, String userType) {
        TenantVo treeTenant = this.getTreeTenant(organId);
        if (null != treeTenant) {
            return getUserFromTree(treeTenant, userType);
        }
        DeptVo treeDept = this.getTreeDept(organId);
        if (null != treeDept) {
            return getUserFromTree(treeDept, userType);
        }
        return Collections.emptyList();
    }

    private List<User> getUserFromTree(BaseOrgan tree, String userType) {
        List<User> userList = new ArrayList<>();
        if (tree instanceof TenantVo) {
            TenantVo tenantVo = (TenantVo) tree;
            if (CollectionUtils.isNotEmpty(tenantVo.getChildren())) {
                tenantVo.getChildren().forEach(vo -> userList.addAll(getUserFromTree(vo, userType)));
            }
        }
        if (tree instanceof DeptVo) {
            DeptVo deptVo = (DeptVo) tree;
            if (CollectionUtils.isNotEmpty(deptVo.getChildren())) {
                deptVo.getChildren().forEach(vo -> userList.addAll(getUserFromTree(vo, userType)));
            }
        }
        if (tree instanceof UserVo && StringUtils.equals(((UserVo)tree).getUserType(), userType)) {
            userList.add((UserVo)tree);
        }
        return userList;
    }

    public List<UserVo> searchUser(String searchKey, String hasNotRight) {
        List<UserVo> userVos = organCache.searchUser(searchKey);
        if (StringUtils.isNotBlank(hasNotRight)) {
            RightVo rightVo = rightService.getRightInfo(hasNotRight);
            if (CollectionUtils.isNotEmpty(rightVo.getUserIds())) {
                Set<String> userIdSet = new HashSet<>(rightVo.getUserIds());
                return userVos.stream().filter(user -> !userIdSet.contains(user.getId())).collect(Collectors.toList());
            }
        }
        return userVos;
    }

    /**
     * 按用户角色同步 t_user_right（与「用户-修改用户拥有的角色」一致）
     */
    private void syncUserRoleToRight(String userId, Set<String> roleIds) {
        userRightService.remove(Wrappers.<UserRight>lambdaQuery()
                .eq(UserRight::getUserid, userId)
                .eq(UserRight::getType, SystemCodeEnum.UserRigtType.ROLE.getCode()));
        if (CollectionUtils.isNotEmpty(roleIds)) {
            List<UserRight> userRoles = new ArrayList<>();
            roleIds.forEach(roleId -> {
                UserRight role = new UserRight();
                role.setId(UUIDHelper.getUuid());
                role.setType(SystemCodeEnum.UserRigtType.ROLE.getCode());
                role.setUserid(userId);
                role.setRoleid(roleId);
                userRoles.add(role);
            });
            dbOperateService.batchInsert(userRoles);
        }
    }

    public void updateUserRole(UserRoleVo userRoleVo) {
        User user = organCache.getUser(userRoleVo.getUserId());
        if (null == user) {
            return;
        }
        Set<String> roleIds = userRoleVo.getRoleId();
        if (CollectionUtils.isEmpty(roleIds)) {
            dbOperateService.executeInTransaction(() -> {
                userService.update(Wrappers.<User>lambdaUpdate().set(User::getRoleId, null).eq(User::getId, user.getId()));
                syncUserRoleToRight(user.getId(), Collections.emptySet());
            });
        } else {
            dbOperateService.executeInTransaction(() -> {
                userService.update(Wrappers.<User>lambdaUpdate().set(User::getRoleId, roleIds, "javaType=String,jdbcType=VARCHAR,typeHandler=com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler")
                        .eq(User::getId, user.getId()));
                syncUserRoleToRight(user.getId(), roleIds);
            });
        }
        user.setRoleId(roleIds);
    }
}
