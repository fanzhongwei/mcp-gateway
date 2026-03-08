package com.mmyf.commons.controller.role;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mmyf.commons.model.entity.organ.Right;
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

import com.mmyf.commons.model.entity.role.RoleRight;
import com.mmyf.commons.service.role.RoleRightService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mmyf.commons.util.excel.ExportExcelUtils;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 角色权限映射 前端控制器
 * </p>
 *
 * @author mmyf
 * @since 2024-02-26
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "角色权限映射", description = "system_base-角色权限映射")
@Slf4j
public class RoleRightController {


    @Autowired
    private RoleRightService roleRightService;

    /**
     * 角色权限映射--新增接口
     *
     * @param roleRight 角色权限映射
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色权限映射--新增接口")
    @PostMapping(value = "/roleRight", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated RoleRight roleRight) {
        if (StringUtils.isBlank(roleRight.getId())) {
            roleRight.setId(UUIDHelper.getUuid());
        }
        return new ResponseEntity<>(roleRightService.save(roleRight), HttpStatus.OK);
    }

    /**
     * 角色权限映射--删除接口
     *
     * @param id 角色权限映射ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色权限映射--删除接口")
    @DeleteMapping(value = "/roleRight/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(roleRightService.removeById(id), HttpStatus.OK);
    }

    /**
     * 角色权限映射--修改接口
     *
     * @param roleRight 角色权限映射
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色权限映射--修改接口")
    @PutMapping(value = "/roleRight", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated RoleRight roleRight) {
        Assert.hasText(roleRight.getId(), "主键id不能为空");
        return new ResponseEntity<>(roleRightService.updateById(roleRight), HttpStatus.OK);
    }

    /**
     * 角色权限映射--查询接口
     *
     * @param id 角色权限映射ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色权限映射--查询接口")
    @GetMapping(value = "/roleRight/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<RoleRight> select(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(roleRightService.getById(id), HttpStatus.OK);
    }

    /**
     * 角色权限映射--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色权限映射--分页查询接口")
    @PostMapping(value = "/roleRight/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<RoleRight>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<RoleRight> pageParam) {
        QueryWrapper<RoleRight> wrapper = Wrappers.query(pageParam.getEntityParam(RoleRight.class));
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, RoleRight.class);
        return new ResponseEntity<>(roleRightService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "角色权限映射--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/roleRight/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(RoleRight.class));
    }

    /**
     * 角色权限映射--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-26
     **/
    @Operation(summary = "角色权限映射--导出Excel")
    @PostMapping(value = "/roleRight/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<RoleRight> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }

    @Operation(summary = "根据角色ID查询关联的权限")
    @GetMapping(value = "/roleRight/{roleId}/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Right>> selectRightByRoleId(@Parameter(name = "roleId", description = "角色ID", required = true) @PathVariable("roleId") String roleId) {
        return ResponseEntity.ok(roleRightService.selectRightByRoleId(roleId));
    }
}
