package com.mmyf.mcp.controller.mcp;

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

import com.mmyf.mcp.model.entity.mcp.McpConfigApi;
import com.mmyf.mcp.service.mcp.McpConfigApiService;

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
 * mcp服务配置API接口 前端控制器
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-26 14:56:34
 */
@RestController
@Tag(name = "mcp服务配置API接口", description = "业务接口-mcp服务配置API接口")
@Slf4j
public class McpConfigApiController {


    @Autowired
    private McpConfigApiService mcpConfigApiService;

    @Autowired
    private SecurityService securityService;

    /**
     * mcp服务配置API接口--新增接口
     *
     * @param mcpConfigApi mcp服务配置API接口
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 14:56:34
     **/
    @Operation(summary = "mcp服务配置API接口--新增接口")
    @PostMapping(value = "/api/v1/mcpConfigApi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated({Insert.class, Default.class}) McpConfigApi mcpConfigApi) {
        if (StringUtils.isBlank(mcpConfigApi.getId())) {
            mcpConfigApi.setId(UUIDHelper.getUuid());
        }
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            mcpConfigApi.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(mcpConfigApiService.save(mcpConfigApi), HttpStatus.OK);
    }

    /**
     * mcp服务配置API接口--根据mcp服务配置API接口主键删除接口
     *
     * @param id mcp服务配置API接口ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 14:56:34
     **/
    @Operation(summary = "mcp服务配置API接口--根据mcp服务配置API接口主键删除接口")
    @DeleteMapping(value = "/api/v1/mcpConfigApi/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", description="mcp服务配置API接口主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(mcpConfigApiService.removeById(id), HttpStatus.OK);
    }

    /**
     * mcp服务配置API接口--修改接口
     *
     * @param mcpConfigApi mcp服务配置API接口
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 14:56:34
     **/
    @Operation(summary = "mcp服务配置API接口--修改接口")
    @PutMapping(value = "/api/v1/mcpConfigApi", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated({Update.class, Default.class}) McpConfigApi mcpConfigApi) {
        Assert.hasText(mcpConfigApi.getId(), "主键id不能为空");
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            mcpConfigApi.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(mcpConfigApiService.updateById(mcpConfigApi), HttpStatus.OK);
    }

    /**
     * mcp服务配置API接口--根据mcp服务配置API接口主键查询接口
     *
     * @param id mcp服务配置API接口ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 14:56:34
     **/
    @Operation(summary = "mcp服务配置API接口--根据mcp服务配置API接口主键查询接口")
    @GetMapping(value = "/api/v1/mcpConfigApi/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<McpConfigApi> select(@Parameter(name = "id", description="mcp服务配置API接口主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(mcpConfigApiService.getById(id), HttpStatus.OK);
    }

    /**
     * mcp服务配置API接口--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 14:56:34
     **/
    @Operation(summary = "mcp服务配置API接口--分页查询接口")
    @PostMapping(value = "/api/v1/mcpConfigApi/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<McpConfigApi>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<McpConfigApi> pageParam) {
        User currUser = securityService.getCurrUserInfo();
        McpConfigApi entityParam = pageParam.getEntityParam(McpConfigApi.class);
        if (entityParam != null && currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            entityParam.setTenant(currUser.getTenant());
        }
        QueryWrapper<McpConfigApi> wrapper = Wrappers.query(entityParam);
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, McpConfigApi.class);
        return new ResponseEntity<>(mcpConfigApiService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "mcp服务配置API接口--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/api/v1/mcpConfigApi/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(McpConfigApi.class));
    }

    /**
     * mcp服务配置API接口--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author 超级管理员
     * @date 2026-01-26 14:56:34
     **/
    @Operation(summary = "mcp服务配置API接口--导出Excel")
    @PostMapping(value = "/api/v1/mcpConfigApi/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<McpConfigApi> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
