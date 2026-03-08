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

import com.mmyf.mcp.model.entity.system.SystemEnv;
import com.mmyf.mcp.service.system.SystemEnvService;

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
 * 系统环境 前端控制器
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-23 10:49:25
 */
@RestController
@Tag(name = "系统环境", description = "业务接口-系统环境")
@Slf4j
public class SystemEnvController {


    @Autowired
    private SystemEnvService systemEnvService;

    @Autowired
    private SecurityService securityService;

    /**
     * 系统环境--新增接口
     *
     * @param systemEnv 系统环境
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-23 10:49:25
     **/
    @Operation(summary = "系统环境--新增接口")
    @PostMapping(value = "/api/v1/systemEnv", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated({Insert.class, Default.class}) SystemEnv systemEnv) {
        if (StringUtils.isBlank(systemEnv.getId())) {
            systemEnv.setId(UUIDHelper.getUuid());
        }
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            systemEnv.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(systemEnvService.save(systemEnv), HttpStatus.OK);
    }

    /**
     * 系统环境--根据系统环境主键删除接口
     *
     * @param id 系统环境ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-23 10:49:25
     **/
    @Operation(summary = "系统环境--根据系统环境主键删除接口")
    @DeleteMapping(value = "/api/v1/systemEnv/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", description="系统环境主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(systemEnvService.removeById(id), HttpStatus.OK);
    }

    /**
     * 系统环境--修改接口
     *
     * @param systemEnv 系统环境
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-23 10:49:25
     **/
    @Operation(summary = "系统环境--修改接口")
    @PutMapping(value = "/api/v1/systemEnv", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated({Update.class, Default.class}) SystemEnv systemEnv) {
        Assert.hasText(systemEnv.getId(), "主键id不能为空");
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            systemEnv.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(systemEnvService.updateById(systemEnv), HttpStatus.OK);
    }

    /**
     * 系统环境--根据系统环境主键查询接口
     *
     * @param id 系统环境ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-23 10:49:25
     **/
    @Operation(summary = "系统环境--根据系统环境主键查询接口")
    @GetMapping(value = "/api/v1/systemEnv/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<SystemEnv> select(@Parameter(name = "id", description="系统环境主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(systemEnvService.getById(id), HttpStatus.OK);
    }

    /**
     * 系统环境--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-23 10:49:25
     **/
    @Operation(summary = "系统环境--分页查询接口")
    @PostMapping(value = "/api/v1/systemEnv/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<SystemEnv>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<SystemEnv> pageParam) {
        User currUser = securityService.getCurrUserInfo();
        SystemEnv entityParam = pageParam.getEntityParam(SystemEnv.class);
        if (entityParam != null && currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            entityParam.setTenant(currUser.getTenant());
        }
        QueryWrapper<SystemEnv> wrapper = Wrappers.query(entityParam);
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, SystemEnv.class);
        return new ResponseEntity<>(systemEnvService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "系统环境--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/api/v1/systemEnv/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(SystemEnv.class));
    }

    /**
     * 系统环境--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author 超级管理员
     * @date 2026-01-23 10:49:25
     **/
    @Operation(summary = "系统环境--导出Excel")
    @PostMapping(value = "/api/v1/systemEnv/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<SystemEnv> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
