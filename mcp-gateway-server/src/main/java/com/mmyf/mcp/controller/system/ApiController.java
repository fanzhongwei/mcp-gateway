package com.mmyf.mcp.controller.system;

import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import com.mmyf.mcp.model.entity.system.Api;
import com.mmyf.mcp.service.system.ApiService;

import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.shiro.security.SecurityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mmyf.commons.util.excel.ExportExcelUtils;
import org.springframework.util.Assert;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import com.mmyf.commons.validator.groups.*;
import jakarta.validation.groups.Default;
import java.util.Set;


/**
 * <p>
 * API接口表 前端控制器
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-24 17:29:26
 */
@RestController
@Tag(name = "API接口表", description = "业务接口-API接口表")
@Slf4j
public class ApiController {


    @Autowired
    private ApiService apiService;

    @Autowired
    private SecurityService securityService;

    /**
     * API接口表--新增接口
     *
     * @param api API接口表
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-24 17:29:26
     **/
    @Operation(summary = "API接口表--新增接口")
    @PostMapping(value = "/api/v1/api", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated({Insert.class, Default.class}) Api api) {
        if (StringUtils.isBlank(api.getId())) {
            api.setId(UUIDHelper.getUuid());
        }
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            api.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(apiService.save(api), HttpStatus.OK);
    }

    /**
     * API接口表--根据API接口表主键删除接口
     *
     * @param id API接口表ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-24 17:29:26
     **/
    @Operation(summary = "API接口表--根据API接口表主键删除接口")
    @DeleteMapping(value = "/api/v1/api/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", description="API接口表主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(apiService.removeById(id), HttpStatus.OK);
    }

    /**
     * API接口表--修改接口
     *
     * @param api API接口表
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-24 17:29:26
     **/
    @Operation(summary = "API接口表--修改接口")
    @PutMapping(value = "/api/v1/api", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated({Update.class, Default.class}) Api api) {
        Assert.hasText(api.getId(), "主键id不能为空");
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            api.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(apiService.updateById(api), HttpStatus.OK);
    }

    /**
     * API接口表--根据API接口表主键查询接口
     *
     * @param id API接口表ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-24 17:29:26
     **/
    @Operation(summary = "API接口表--根据API接口表主键查询接口")
    @GetMapping(value = "/api/v1/api/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Api> select(@Parameter(name = "id", description="API接口表主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(apiService.getById(id), HttpStatus.OK);
    }

    /**
     * API接口表--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-24 17:29:26
     **/
    @Operation(summary = "API接口表--分页查询接口")
    @PostMapping(value = "/api/v1/api/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<Api>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Api> pageParam) {
        User currUser = securityService.getCurrUserInfo();
        Api entityParam = pageParam.getEntityParam(Api.class);
        if (entityParam != null && currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            entityParam.setTenant(currUser.getTenant());
        }
        QueryWrapper<Api> wrapper = Wrappers.query(entityParam);
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, Api.class);
        return new ResponseEntity<>(apiService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "API接口表--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/api/v1/api/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(Api.class));
    }

    /**
     * API接口表--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author 超级管理员
     * @date 2026-01-24 17:29:26
     **/
    @Operation(summary = "API接口表--导出Excel")
    @PostMapping(value = "/api/v1/api/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Api> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
