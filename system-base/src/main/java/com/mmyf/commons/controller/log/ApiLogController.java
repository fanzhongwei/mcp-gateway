package com.mmyf.commons.controller.log;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmyf.commons.model.entity.log.ApiLog;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.service.log.ApiLogService;
import com.mmyf.commons.translate.annotation.DataTranslate;
import com.mmyf.commons.util.excel.ExportExcelUtils;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * <p>
 * 用户权限 前端控制器
 * </p>
 *
 * @author Teddy
 * @since 2022-05-29
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "system_base-日志接口")
public class ApiLogController {

    @Autowired
    private ApiLogService apiLogService;

    /**
     * 审计日志--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "审计日志--分页查询接口")
    @PostMapping(value = "/apiLog/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<ApiLog>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<ApiLog> pageParam) {
        QueryWrapper<ApiLog> wrapper = Wrappers.query(pageParam.getEntityParam(ApiLog.class));
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, ApiLog.class);
        return new ResponseEntity<>(apiLogService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "审计日志--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/apiLog/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(ApiLog.class));
    }

    /**
     * 审计日志--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "审计日志--导出Excel")
    @PostMapping(value = "/apiLog/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<ApiLog> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
