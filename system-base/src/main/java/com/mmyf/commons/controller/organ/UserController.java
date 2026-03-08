package com.mmyf.commons.controller.organ;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mmyf.commons.log.ApiLog;
import com.mmyf.commons.model.vo.organ.UserPageParam;
import com.mmyf.commons.model.vo.organ.UserRoleVo;
import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.shiro.security.SecurityService;
import com.mmyf.commons.util.excel.ExportExcelUtils;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.util.uuid.UUIDHelper;
import com.mmyf.commons.translate.annotation.DataTranslate;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;

import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.service.organ.UserService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author mmyf
 * @since 2024-02-18
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "用户", description = "system_base-用户")
@Slf4j
public class UserController {


    @Autowired
    private UserService userService;

    @Autowired
    private OrganService organService;

    @Autowired
    private SecurityService securityService;

    /**
     * 用户--新增接口
     *
     * @param user 用户
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "用户--新增接口")
    @PostMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    @Valid
    @ApiLog(module = "用户管理", name = "新增用户信息")
    public ResponseEntity<Boolean> insert(@RequestBody @Validated User user) {
        if (StringUtils.isBlank(user.getId())) {
            user.setId(UUIDHelper.getUuid());
        }
        organService.insertUser(user);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 用户--删除接口
     *
     * @param id 用户ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "用户--删除接口")
    @DeleteMapping(value = "/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiLog(module = "用户管理", name = "删除用户信息")
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        organService.deleteUser(id);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 用户--修改接口
     *
     * @param user 用户
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "用户--修改接口")
    @PutMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    @Valid
    @ApiLog(module = "用户管理", name = "修改用户信息")
    public ResponseEntity<Boolean> update(@RequestBody @Validated User user) {
        Assert.hasText(user.getId(), "主键id不能为空");
        organService.updateUser(user);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Operation(summary = "用户--修改用户拥有的角色")
    @PutMapping(value = "/user/role", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> updateUserRole(@RequestBody @Validated UserRoleVo userRoleVo) {
        organService.updateUserRole(userRoleVo);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 用户--查询接口
     *
     * @param id 用户ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "用户--查询接口")
    @GetMapping(value = "/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<User> select(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        User user = userService.getById(id);
        if (null == user) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        user.setRightList(securityService.getUserRights(id));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * 用户--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "用户--分页查询接口")
    @PostMapping(value = "/user/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<User>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<UserPageParam> pageParam) {
        UserPageParam entityParam = pageParam.getEntityParam(UserPageParam.class);
        QueryWrapper<User> wrapper = Wrappers.query();
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, User.class);
        if (null != entityParam) {
            LambdaQueryWrapper<User> queryWrapper = wrapper.lambda();
            if (StringUtils.isNotBlank(entityParam.getNameOrLoginIdLike())) {
                queryWrapper.and(qr -> qr.like(User::getName, entityParam.getNameOrLoginIdLike())
                        .or()
                        .like(User::getLoginid, entityParam.getNameOrLoginIdLike()));
            }
            // 租户：包含当前租户及所有子级租户下的用户
            if (StringUtils.isNotBlank(entityParam.getTenant())) {
                List<String> tenantIds = organService.getTenantAndAllSubTenantId(entityParam.getTenant());
                if (!tenantIds.isEmpty()) {
                    queryWrapper.in(User::getTenant, tenantIds);
                } else {
                    queryWrapper.eq(User::getTenant, entityParam.getTenant());
                }
            }
            // 部门：包含当前部门及所有子级部门下的用户
            if (StringUtils.isNotBlank(entityParam.getDept())) {
                List<String> deptIds = organService.getDeptAndAllSubDeptId(entityParam.getDept());
                if (!deptIds.isEmpty()) {
                    queryWrapper.in(User::getDept, deptIds);
                } else {
                    queryWrapper.eq(User::getDept, entityParam.getDept());
                }
            }
            if (StringUtils.isNotBlank(entityParam.getUserType())) {
                queryWrapper.eq(User::getUserType, entityParam.getUserType());
            }
            return new ResponseEntity<>(userService.page(pageParam.getPageParam(), queryWrapper), HttpStatus.OK);
        }

        return new ResponseEntity<>(userService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "用户--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/user/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(User.class));
    }

    /**
     * 用户--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "用户--导出Excel")
    @PostMapping(value = "/user/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<UserPageParam> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
