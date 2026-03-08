package com.mmyf.commons.controller.role;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mmyf.commons.model.query.role.RolePageQuery;
import com.mmyf.commons.model.vo.organ.RoleVo;
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
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;

import com.mmyf.commons.model.entity.role.Role;
import com.mmyf.commons.service.role.RoleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mmyf.commons.util.excel.ExportExcelUtils;

import java.util.Set;

/**
 * <p>
 * 角色定义 前端控制器
 * </p>
 *
 * @author mmyf
 * @since 2024-02-26
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "角色定义", description = "system_base-角色定义")
@Slf4j
public class RoleController {


    @Autowired
    private RoleService roleService;

    /**
     * 角色定义--新增接口
     *
     * @param role 角色定义
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色定义--新增接口")
    @PostMapping(value = "/role", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated RoleVo role) {
        return new ResponseEntity<>(roleService.saveRole(role), HttpStatus.OK);
    }

    /**
     * 角色定义--删除接口
     *
     * @param id 角色定义ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色定义--删除接口")
    @DeleteMapping(value = "/role/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(roleService.deleteRole(id), HttpStatus.OK);
    }

    /**
     * 角色定义--修改接口
     *
     * @param role 角色定义
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色定义--修改接口")
    @PutMapping(value = "/role", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated RoleVo role) {
        Assert.hasText(role.getId(), "主键id不能为空");
        return new ResponseEntity<>(roleService.updateRole(role), HttpStatus.OK);
    }

    /**
     * 角色定义--查询接口
     *
     * @param id 角色定义ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色定义--查询接口")
    @GetMapping(value = "/role/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Role> select(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(roleService.getById(id), HttpStatus.OK);
    }

    /**
     * 角色定义--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色定义--分页查询接口")
    @PostMapping(value = "/role/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<RoleVo>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<RolePageQuery> pageParam) {
        RolePageQuery entityParam = pageParam.getEntityParam(RolePageQuery.class);
        QueryWrapper<Role> wrapper = Wrappers.query(entityParam);
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, Role.class);
        LambdaQueryWrapper<Role> lambdaQueryWrapper = wrapper.lambda()
                                               .like(StringUtils.isNotBlank(entityParam.getNameLike()), Role::getName, entityParam.getNameLike());
        Page page = roleService.page(pageParam.getPageParam(), lambdaQueryWrapper);
        page.setRecords(roleService.selectRoleRight(page.getRecords()));
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @Operation(summary = "角色定义--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/role/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(Role.class));
    }

    /**
     * 角色定义--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色定义--导出Excel")
    @PostMapping(value = "/role/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<RolePageQuery> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
