package com.mmyf.commons.service.organ;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.mmyf.commons.mapper.organ.TenantMapper;
import com.mmyf.commons.mapper.organ.DeptMapper;
import com.mmyf.commons.mapper.organ.UserMapper;
import com.mmyf.commons.model.entity.organ.Tenant;
import com.mmyf.commons.model.entity.organ.Dept;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.model.vo.organ.BaseOrgan;
import com.mmyf.commons.model.vo.organ.TenantVo;
import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.vo.organ.DeptVo;
import com.mmyf.commons.model.vo.organ.UserVo;
import com.mmyf.commons.service.DbOperateService;
import com.mmyf.commons.util.config.ConfigUtils;
import com.mmyf.commons.util.lock.LockUtils;
import com.mmyf.commons.util.uuid.MD5Helper;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * package com.mmyf.service.organ
 * description: organ cache
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 15:36:23
 */
@Component
@Slf4j
public class OrganCache {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private UserMapper userMapper;

    @Autowired
    private DbOperateService dbOperateService;

    @Autowired(required = false)
    private IExternalOrganDatasource externalOrganDatasource;

    @Autowired
    private ConfigUtils configUtils;

    /**
     * 租户根节点
     */
    @Value("${root.tenant}")
    private String rootTenantId;

    @CreateCache(name = "organCache.", cacheType = CacheType.BOTH, localExpire = 2, timeUnit = TimeUnit.HOURS)
    @CachePenetrationProtect
    private Cache<String, Map> organCache;

    @PostConstruct
    public void init() {
        // 主节点才加载缓存，其他节点从远端缓存中获取吧
        if (!ConfigUtils.isMasterSystem()) {
            return;
        }
        loadCache();
    }

    private void loadCache() {
        log.info("租户缓存加载--开始");
        try {
            LockUtils.lock("OrganCacheInit");
            Map<String, Tenant> tenantMap = Maps.newHashMap();
            Map<Long, Tenant> externalNidTenantMap = Maps.newHashMap();

            Map<String, Dept> deptMap = Maps.newHashMap();

            Map<String, User> userMap = Maps.newHashMap();
            Map<String, User> userLoginIdMap = Maps.newHashMap();
            Map<String, List<User>> externalCidUsersMap = Maps.newHashMap();

            Map<String, TenantVo> tenantVoMap = Maps.newHashMap();
            Map<String, DeptVo> deptVoMap = Maps.newHashMap();

            // 主节点才刷新外部租户
            if (ConfigUtils.isMasterSystem() && null != externalOrganDatasource) {
                externalOrganDatasource.refreshOrgan();
            }
            log.info("租户缓存加载--tenant加载开始");
            loadTenant(tenantMap, externalNidTenantMap);
            log.info("租户缓存加载--tenant加载结束");

            log.info("租户缓存加载--dept加载开始");
            loadDept(deptMap);
            log.info("租户缓存加载--dept加载结束");

            log.info("租户缓存加载--user加载开始");
            loadUser(userMap, userLoginIdMap, externalCidUsersMap);
            log.info("租户缓存加载--user加载结束");

            log.info("租户缓存加载--租户树加载开始");
            loadOrganTree(tenantMap, deptMap, userMap, tenantVoMap, deptVoMap);
            log.info("租户缓存加载--租户树加载结束");

            log.info("租户缓存加载--缓存数据组装完成，写入缓存开始");
            organCache.put("tenantMap", tenantMap);
            organCache.put("externalNidTenantMap", externalNidTenantMap);
            organCache.put("deptMap", deptMap);
            organCache.put("userMap", userMap);
            organCache.put("userLoginIdMap", userLoginIdMap);
            organCache.put("externalCidUsersMap", externalCidUsersMap);
            organCache.put("tenantVoMap", tenantVoMap);
            organCache.put("deptVoMap", deptVoMap);

            log.info("租户缓存加载--缓存数据组装完成，写入缓存完成");
        } finally {
            LockUtils.unlock("OrganCacheInit");
        }
    }

    public TenantVo getTreeRoot() {
        return (TenantVo) organCache.get("tenantVoMap").get(rootTenantId);
    }

    private void loadTenant(Map<String, Tenant> tenantMap, Map<Long, Tenant> externalNidTenantMap) {
        List<? extends Tenant> tenants;
        if (externalOrganDatasource == null) {
            tenants = tenantMapper.selectList(Wrappers.emptyWrapper());
        } else {
            tenants = externalOrganDatasource.loadAllTenant();
        }
        tenants.forEach(tenant -> {
            tenantMap.put(tenant.getId(), tenant);
            if (null != tenant.getExternalNid()) {
                externalNidTenantMap.put(tenant.getExternalNid(), tenant);
            }
        });
    }

    private void loadDept(Map<String, Dept> deptMap) {
        List<? extends Dept> depts;
        if (externalOrganDatasource == null) {
            depts = deptMapper.selectList(Wrappers.emptyWrapper());
        } else {
            depts = externalOrganDatasource.loadAllDept();
        }
        deptMap.putAll(depts.stream().collect(Collectors.toMap(Dept::getId, Function.identity())));
    }

    private void loadUser(Map<String, User> userMap, Map<String, User> userLoginIdMap, Map<String, List<User>> externalCidUsersMap) {
        List<? extends User> users;
        if (externalOrganDatasource == null) {
            users = userMapper.selectList(Wrappers.emptyWrapper());
        } else {
            users = externalOrganDatasource.loadAllUser();
        }
        users.forEach(user -> {
            userMap.put(user.getId(), user);
            if (StringUtils.isNotBlank(user.getLoginid())) {
                userLoginIdMap.put(user.getLoginid(), user);
            }
            String externalCid = user.getExternalCid();
            if (StringUtils.isNotBlank(externalCid)) {
                externalCidUsersMap.computeIfAbsent(externalCid, k -> new ArrayList<>()).add(user);
            }
        });
    }

    private void loadOrganTree(Map<String, Tenant> tenantMap, Map<String, Dept> deptMap, Map<String, User> userMap, Map<String, TenantVo> tenantVoMap, Map<String, DeptVo> deptVoMap) {
        Tenant rootTenant = tenantMap.get(rootTenantId);
        Assert.notNull(rootTenant, "租户根节点为空，这是初始化数据，不应该为空。");
        // 自底向上，组装user节点
        userMap.forEach((id, user) -> {
            buildUserCache(user, tenantMap, tenantVoMap, deptMap, deptVoMap);
        });

        // 自底向上，组装dept节点
        deptMap.forEach((id, dept) -> {
            buildDeptCache(dept, tenantMap, tenantVoMap, deptMap, deptVoMap);
        });

        // 顶层tenant组装
        tenantMap.forEach((id, tenant) -> {
            if (StringUtils.isNotBlank(tenant.getPid())) {
                buildTenantCache(tenant, tenantMap, tenantVoMap);
            }
        });
        // 获取根节点
        BaseOrgan organTree = tenantVoMap.computeIfAbsent(rootTenantId, tenantId -> new TenantVo(rootTenant));
        sortOrganTree(organTree);
    }

    private void sortOrganTree(BaseOrgan organ) {
        switch (organ.getType()) {
            case TENANT:
                TenantVo tenantVo = (TenantVo) organ;
                if(CollectionUtils.isNotEmpty(tenantVo.getChildren())) {
                    tenantVo.getChildren().sort(this::compareOrgan);
                    tenantVo.getChildren().forEach(this::sortOrganTree);
                }
                break;
            case DEPT:
                DeptVo deptVo = (DeptVo) organ;
                if(CollectionUtils.isNotEmpty(deptVo.getChildren())) {
                    deptVo.getChildren().sort(this::compareOrgan);
                    deptVo.getChildren().forEach(this::sortOrganTree);
                }
                break;
            default:
                // user节点没有子节点，不用继续排序
                return;
        }
    }

    private int compareOrgan(BaseOrgan o1, BaseOrgan o2) {
        if (o1.getType() != o2.getType()) {
            return o1.getType().ordinal() - o2.getType().ordinal();
        } else if (o1.getCreateTime() == null || o2.getCreateTime() == null) {
            return 0;
        } else {
            return o1.getCreateTime().compareTo(o2.getCreateTime());
        }
    }

    private void buildTenantCache(Tenant tenant, Map<String, Tenant> tenantMap, Map<String, TenantVo> tenantVoMap) {
        TenantVo tenantVoParent = tenantVoMap.computeIfAbsent(tenant.getPid(), tenantId -> {
            Assert.notNull(tenantMap.get(tenantId), "未知的tenantId: " + tenantId);
            return new TenantVo(tenantMap.get(tenantId));
        });
        TenantVo tenantVo = tenantVoMap.computeIfAbsent(tenant.getId(), tenantId -> {
            Assert.notNull(tenantMap.get(tenantId), "未知的tenantId: " + tenantId);
            return new TenantVo(tenantMap.get(tenantId));
        });
        tenantVoParent.addChild(tenantVo);
    }

    private void buildDeptCache(Dept dept, Map<String, Tenant> tenantMap, Map<String, TenantVo> tenantVoMap, Map<String, Dept> deptMap, Map<String, DeptVo> deptVoMap) {
        DeptVo deptVo = deptVoMap.computeIfAbsent(dept.getId(), deptId -> new DeptVo(dept));
        // 关联tenant节点
        TenantVo tenantVoParent = tenantVoMap.computeIfAbsent(dept.getTenant(), tenantId -> {
            Assert.notNull(tenantMap.get(tenantId), "未知的tenantId: " + tenantId);
            return new TenantVo(tenantMap.get(tenantId));
        });

        // 存在部门嵌套的情况，挂在部门下
        if (StringUtils.isNotBlank(dept.getPid())) {
            DeptVo deptVoParent = deptVoMap.computeIfAbsent(dept.getPid(), deptId -> {
                Assert.notNull(deptMap.get(deptId), "未知的deptId: " + deptId);
                return new DeptVo(deptMap.get(deptId));
            });
            deptVoParent.addChild(deptVo);
        } else {
            // 直接挂在Tenant节点下
            tenantVoParent.addChild(deptVo);
        }
    }

    private void buildUserCache(User user, Map<String, Tenant> tenantMap, Map<String, TenantVo> tenantVoMap, Map<String, Dept> deptMap, Map<String, DeptVo> deptVoMap) {
        // 关联tenant节点
        TenantVo tenantVoParent = tenantVoMap.computeIfAbsent(user.getTenant(), tenantId -> {
            Assert.notNull(tenantMap.get(tenantId), "未知的tenantId: " + tenantId);
            return new TenantVo(tenantMap.get(tenantId));
        });
        UserVo userVo = new UserVo(user);

        if (StringUtils.isNotBlank(user.getDept())) {
            // 关联dept节点
            DeptVo deptVoParent = deptVoMap.computeIfAbsent(user.getDept(), deptId -> {
                Assert.notNull(deptMap.get(deptId), "未知的deptId: " + deptId);
                return new DeptVo(deptMap.get(deptId));
            });
            deptVoParent.addChild(userVo);
        } else {
            // 关联父子节点，父节点为tenant
            tenantVoParent.addChild(userVo);
        }
    }

    /**
     * 重新加载租户缓存
     */
    public void reload(){
        loadCache();
    }

    /**
     * 根据id获取tenant
     * @param id tenant id
     * @return tenant
     */
    public Tenant getTenant(@NotNull String id) {
        return (Tenant) organCache.get("tenantMap").get(id);
    }

    /**
     * 根据diexternalNid获取tenant
     * @param externalNid externalNid
     * @return tenant
     */
    public Tenant getTenantByExternalNid(@NotNull Long externalNid) {
        return (Tenant) organCache.get("externalNidTenantMap").get(externalNid);
    }

    /**
     * 根据externalCid获取用户
     * @param externalCid 外部userId
     * @return 用户可能有多个，需要上层自己判断
     */
    public List<User> getUserListByExternalCid(String externalCid) {
        return (List<User>) organCache.get("externalCidUsersMap").get(externalCid);
    }

    /**
     * 根据id获取tenant树
     * @param id tenant id
     * @return tenant tree
     */
    public TenantVo getTreeTenant(@NotNull String id) {
        return (TenantVo) organCache.get("tenantVoMap").get(id);
    }

    /**
     * 根据id获取dept
     * @param id dept id
     * @return dept
     */
    public Dept getDept(@NotNull String id) {
        return (Dept) organCache.get("deptMap").get(id);
    }

    /**
     * 根据id获取Dept树
     * @param id Dept id
     * @return Dept tree
     */
    public DeptVo getTreeDept(@NotNull String id) {
        return (DeptVo) organCache.get("deptVoMap").get(id);
    }

    /**
     * 根据id获取用户
     * @param id user id
     * @return user
     */
    public User getUser(@NotNull String id) {
        return (User) organCache.get("userMap").get(id);
    }

    /**
     * 根据登陆名获取用户
     *
     * @param loginId User login id
     * @return User
     */
    public User getUserByLoginId(String loginId) {
        return (User) organCache.get("userLoginIdMap").get(loginId);
    }

    /**
     * 更新tenant，同时更新缓存，如果变更层级关系需要手动刷新租户缓存
     *
     * @param tenant tenant
     */
    public void updateTenant(Tenant tenant) {
        tenantMapper.updateById(tenant);

        Map<String, Tenant> tenantMap = organCache.get("tenantMap");
        tenantMap.put(tenant.getId(), tenant);

        Map<Long, Tenant> externalNidTenantMap = organCache.get("externalNidTenantMap");
        externalNidTenantMap.put(tenant.getExternalNid(), tenant);

        Map<String, TenantVo> tenantVoMap = organCache.get("tenantVoMap");
        TenantVo newVo = new TenantVo(tenant);
        newVo.setChildren(tenantVoMap.get(tenant.getId()).getChildren());
        tenantVoMap.put(tenant.getId(), newVo);

        organCache.put("tenantMap", tenantMap);
        organCache.put("externalNidTenantMap", externalNidTenantMap);
        organCache.put("tenantVoMap", tenantVoMap);
    }

    /**
     * 新增tenant
     * @param tenant tenant
     */
    public void insertTenant(Tenant tenant) {
        tenantMapper.insert(tenant);

        Map<String, Tenant> tenantMap = organCache.get("tenantMap");
        tenantMap.put(tenant.getId(), tenant);

        Map<Long, Tenant> externalNidTenantMap = organCache.get("externalNidTenantMap");
        externalNidTenantMap.put(tenant.getExternalNid(), tenant);

        Map<String, TenantVo> tenantVoMap = organCache.get("tenantVoMap");
        TenantVo newVo = new TenantVo(tenant);
        tenantVoMap.put(tenant.getId(), newVo);

        organCache.put("tenantMap", tenantMap);
        organCache.put("externalNidTenantMap", externalNidTenantMap);
        organCache.put("tenantVoMap", tenantVoMap);
    }

    /**
     * 删除tenant，级联删除其下所有子租户、部门及用户（从缓存递归收集所有级联ID后批量删除）
     * @param tenantId tenantId
     */
    public void deleteTenant(String tenantId) {
        TenantVo tree = getTreeTenant(tenantId);
        if (tree == null) {
            log.warn("删除租户时缓存未命中 tenantId={}，跳过", tenantId);
            return;
        }
        List<String> tenantIds = new ArrayList<>();
        List<String> deptIds = new ArrayList<>();
        collectCascadeIdsFromTenant(tree, tenantIds, deptIds);

        dbOperateService.executeInTransaction(() -> {
            tenantMapper.deleteById(tenantId);
            tenantMapper.delete(Wrappers.<Tenant>lambdaUpdate().in(Tenant::getPid, tenantIds));
            deptMapper.delete(Wrappers.<Dept>lambdaUpdate().in(Dept::getTenant, tenantIds));
            userMapper.delete(Wrappers.<User>lambdaUpdate().in(User::getTenant, tenantIds));
        });
        // 删除变动比较大，直接重新加载租户缓存
        this.reload();
    }

    /**
     * 从缓存中的租户树递归收集所有级联 id：租户 id 后序（先子后父），部门 id
     */
    private void collectCascadeIdsFromTenant(BaseOrgan node, List<String> tenantIds, List<String> deptIds) {
        if (node == null) {
            return;
        }
        switch (node.getType()) {
            case TENANT:
                TenantVo tenantVo = (TenantVo) node;
                if (CollectionUtils.isNotEmpty(tenantVo.getChildren())) {
                    for (BaseOrgan child : tenantVo.getChildren()) {
                        collectCascadeIdsFromTenant(child, tenantIds, deptIds);
                    }
                }
                tenantIds.add(node.getId());
                break;
            case DEPT:
                collectCascadeIdsFromDept((DeptVo) node, deptIds);
                break;
            case USER:
//                userIds.add(node.getId());
                break;
            default:
                break;
        }
    }

    /**
     * 更新dept
     * @param dept Dept
     */
    public void updateDept(Dept dept) {
       deptMapper.updateById(dept);

        Map<String, Dept> deptMap = organCache.get("deptMap");
        deptMap.put(dept.getId(), dept);

        Map<String, DeptVo> deptVoMap = organCache.get("deptVoMap");
        DeptVo newVo = new DeptVo(dept);
        newVo.setChildren(deptVoMap.get(dept.getId()).getChildren());
        deptVoMap.put(dept.getId(), newVo);

        organCache.put("deptMap", deptMap);
        organCache.put("deptVoMap", deptVoMap);
    }

    /**
     * 添加dept
     *
     * @param dept Dept
     */
    public void insertDept(Dept dept) {
        deptMapper.insert(dept);

        Map<String, Dept> deptMap = organCache.get("deptMap");
        deptMap.put(dept.getId(), dept);

        Map<String, DeptVo> deptVoMap = organCache.get("deptVoMap");
        DeptVo newVo = new DeptVo(dept);
        deptVoMap.put(dept.getId(), newVo);

        organCache.put("deptMap", deptMap);
        organCache.put("deptVoMap", deptVoMap);
    }

    /**
     * 删除部门，级联删除其下所有子部门及用户（从缓存递归收集所有级联ID后批量删除）
     * @param id dept id
     */
    public void deleteDept(String id) {
        DeptVo tree = getTreeDept(id);
        if (tree == null) {
            log.warn("删除部门时缓存未命中 deptId={}，跳过", id);
            return;
        }
        List<String> deptIds = new ArrayList<>();
        collectCascadeIdsFromDept(tree, deptIds);

        dbOperateService.executeInTransaction(() -> {
            if (!deptIds.isEmpty()) {
                userMapper.delete(Wrappers.<User>lambdaQuery().in(User::getDept, deptIds));
                deptMapper.delete(Wrappers.<Dept>lambdaQuery().in(Dept::getId, deptIds));
            }
        });
        this.reload();
    }

    /**
     * 从缓存中的部门树递归收集所有级联部门 id
     */
    private void collectCascadeIdsFromDept(DeptVo node, List<String> deptIds) {
        if (node == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(node.getChildren())) {
            for (BaseOrgan child : node.getChildren()) {
                if (child.getType() == SystemCodeEnum.OrganType.DEPT) {
                    collectCascadeIdsFromDept((DeptVo) child, deptIds);
                }
            }
        }
        deptIds.add(node.getId());
    }

    /**
     * 更新user，租户树上的用户信息需要手动自己刷新
     * @param user User
     */
    public void updateUser(User user) {
        try {
            if (StringUtils.isNotBlank(user.getPassword())) {
                user.setPassword(MD5Helper.encrypt(user.getPassword()));
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("加密失败：{}", user.getPassword(), e);
        }
        userMapper.updateById(user);

        Map<String, User> userMap = organCache.get("userMap");
        User oldUser = userMap.get(user.getId());
        User newUser = userMapper.selectById(user.getId());
        userMap.put(user.getId(), newUser);
        organCache.put("userMap", userMap);

        // 可能修改用户名
        Map<String, User> userLoginIdMap = organCache.get("userLoginIdMap");
        userLoginIdMap.remove(oldUser.getLoginid());
        userLoginIdMap.put(newUser.getLoginid(), newUser);
        organCache.put("userLoginIdMap", userLoginIdMap);

        Map<String, List<User>> externalCidUsersMap = organCache.get("externalCidUsersMap");
        List<User> userList = externalCidUsersMap.remove(oldUser.getExternalCid());
        if (null != userList) {
            for (User u : userList ) {
                if (StringUtils.equals(u.getId(), newUser.getId())) {
                    BeanUtils.copyProperties(newUser, u);
                    break;
                }
            }
            externalCidUsersMap.put(newUser.getExternalCid(), userList);
            organCache.put("externalCidUsersMap", externalCidUsersMap);
        }
    }

    /**
     * 新增user，租户树上的用户信息需要手动自己刷新
     * @param user User
     */
    public void insertUser(User user) {
        try {
            user.setPassword(MD5Helper.encrypt(user.getPassword()));
        } catch (NoSuchAlgorithmException e) {
            log.error("加密失败：{}", user.getPassword(), e);
        }
        userMapper.insert(user);

        Map<String, User> userMap = organCache.get("userMap");
        User oldUser = userMap.get(user.getId());
        userMap.put(user.getId(), user);
        organCache.put("userMap", userMap);

        Map<String, User> userLoginIdMap = organCache.get("userLoginIdMap");
        userLoginIdMap.put(user.getLoginid(), user);
        organCache.put("userLoginIdMap", userLoginIdMap);

        if (null != oldUser && StringUtils.isNotBlank(oldUser.getExternalCid())) {
            Map<String, List<User>> externalCidUsersMap = organCache.get("externalCidUsersMap");
            List<User> userList = externalCidUsersMap.get(oldUser.getExternalCid());
            if (null == userList) {
                userList = new ArrayList<>();
            }
            userList.add(user);
            externalCidUsersMap.put(user.getExternalCid(), userList);
            organCache.put("externalCidUsersMap", externalCidUsersMap);
        }
    }

    /**
     * 删除user
     * @param id User id
     */
    public void deleteUser(String id) {
        userMapper.deleteById(id);
        Map<String, User> userMap = organCache.get("userMap");
        User deletedUser = userMap.remove(id);
        organCache.put("userMap", userMap);

        Map<String, User> userLoginIdMap = organCache.get("userLoginIdMap");
        userLoginIdMap.remove(deletedUser.getLoginid());
        organCache.put("userLoginIdMap", userLoginIdMap);

        Map<String, List<User>> externalCidUsersMap = organCache.get("externalCidUsersMap");
        List<User> userList = externalCidUsersMap.remove(deletedUser.getExternalCid());
        if (null != userList) {
            List<User> newUserList = userList.stream().filter(user -> !StringUtils.equals(user.getId(), id)).collect(Collectors.toList());
            externalCidUsersMap.put(deletedUser.getExternalCid(), newUserList);
        }
    }

    public List<UserVo> searchUser(String searchKey) {
        Map<String, User> userLoginIdMap = organCache.get("userLoginIdMap");
        return userLoginIdMap.values()
                .stream()
                .filter(user -> StringUtils.containsIgnoreCase(user.getLoginid(), searchKey) || StringUtils.containsIgnoreCase(user.getName(), searchKey))
                .map(user -> new UserVo(user))
                .collect(Collectors.toList());
    }
}
