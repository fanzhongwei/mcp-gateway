package com.mmyf.commons.controller.code;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mmyf.commons.model.query.code.CodeTypePageQuery;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
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

import com.mmyf.commons.model.entity.code.CodeType;
import com.mmyf.commons.service.code.CodeTypeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mmyf.commons.util.excel.ExportExcelUtils;

import java.util.Set;

/**
 * <p>
 * 代码类型 前端控制器
 * </p>
 *
 * @author mmyf
 * @since 2024-02-19
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "代码类型", description = "system_base-代码类型")
@Slf4j
public class CodeTypeController {


    @Autowired
    private CodeTypeService codeTypeService;

    /**
     * 代码类型--新增接口
     *
     * @param codeType 代码类型
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码类型--新增接口")
    // @PostMapping(value = "/codeType", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated CodeType codeType) {
        if (StringUtils.isBlank(codeType.getId())) {
            codeType.setId(UUIDHelper.getUuid());
        }
        return new ResponseEntity<>(codeTypeService.save(codeType), HttpStatus.OK);
    }

    /**
     * 代码类型--删除接口
     *
     * @param id 代码类型ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码类型--删除接口")
    // @DeleteMapping(value = "/codeType/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(codeTypeService.removeById(id), HttpStatus.OK);
    }

    /**
     * 代码类型--修改接口
     *
     * @param codeType 代码类型
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码类型--修改接口")
    // @PutMapping(value = "/codeType", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated CodeType codeType) {
        return new ResponseEntity<>(codeTypeService.updateById(codeType), HttpStatus.OK);
    }

    /**
     * 代码类型--查询接口
     *
     * @param id 代码类型ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码类型--查询接口")
    @GetMapping(value = "/codeType/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<CodeType> select(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(codeTypeService.getById(id), HttpStatus.OK);
    }

    /**
     * 代码类型--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码类型--分页查询接口")
    @PostMapping(value = "/codeType/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<CodeType>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<CodeTypePageQuery> pageParam) {
        CodeTypePageQuery entityParam = pageParam.getEntityParam(CodeTypePageQuery.class);
        QueryWrapper<CodeType> wrapper = Wrappers.query(entityParam);
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, CodeType.class);
        LambdaQueryWrapper<CodeType> lambdaQueryWrapper = wrapper.lambda()
                                                   .like(StringUtils.isNotBlank(entityParam.getNameLike()), CodeType::getName, entityParam.getNameLike());
        return new ResponseEntity<>(codeTypeService.page(pageParam.getPageParam(), lambdaQueryWrapper), HttpStatus.OK);
    }

    @Operation(summary = "代码类型--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/codeType/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(CodeType.class));
    }

    /**
     * 代码类型--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码类型--导出Excel")
    @PostMapping(value = "/codeType/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<CodeTypePageQuery> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
