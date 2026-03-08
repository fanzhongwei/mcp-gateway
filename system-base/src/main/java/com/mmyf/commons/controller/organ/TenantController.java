package com.mmyf.commons.controller.organ;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.util.excel.ExportExcelUtils;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
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
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;

import com.mmyf.commons.model.entity.organ.Tenant;
import com.mmyf.commons.service.organ.TenantService;

import java.util.Set;

/**
 * <p>
 * 租户 前端控制器
 * </p>
 *
 * @author mmyf
 * @since 2024-02-18
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "租户", description = "system_base-租户")
@Slf4j
public class TenantController {


    @Autowired
    private TenantService tenantService;

    @Autowired
    private OrganService organService;

    /**
     * 租户--新增接口
     *
     * @param tenant 租户
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "租户--新增接口")
    @PostMapping(value = "/tenant", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated Tenant tenant) {
        if (StringUtils.isBlank(tenant.getId())) {
            tenant.setId(UUIDHelper.getUuid());
        }
        organService.insertTenant(tenant);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 租户--删除接口
     *
     * @param id 租户ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "租户--删除接口")
    @DeleteMapping(value = "/tenant/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        organService.deleteTenant(id);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 租户--修改接口
     *
     * @param tenant 租户
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "租户--修改接口")
    @PutMapping(value = "/tenant", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated Tenant tenant) {
        Assert.hasText(tenant.getId(), "主键id不能为空");
        organService.updateTenant(tenant);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 租户--查询接口
     *
     * @param id 租户ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "租户--查询接口")
    @GetMapping(value = "/tenant/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Tenant> select(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(tenantService.getById(id), HttpStatus.OK);
    }

    /**
     * 租户--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "租户--分页查询接口")
    @PostMapping(value = "/tenant/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<Tenant>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Tenant> pageParam) {
        QueryWrapper<Tenant> wrapper = Wrappers.query(pageParam.getEntityParam(Tenant.class));
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, Tenant.class);
        return new ResponseEntity<>(tenantService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "租户--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/tenant/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(Tenant.class));
    }

    /**
     * 租户--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "租户--导出Excel")
    @PostMapping(value = "/tenant/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Tenant> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
