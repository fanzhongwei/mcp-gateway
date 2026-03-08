package com.mmyf.commons.controller.organ;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mmyf.commons.service.organ.OrganService;
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
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;

import com.mmyf.commons.model.entity.organ.Dept;
import com.mmyf.commons.service.organ.DeptService;

import java.util.Set;

/**
 * <p>
 * 部门 前端控制器
 * </p>
 *
 * @author mmyf
 * @since 2024-02-18
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "部门", description = "system_base-部门")
@Slf4j
public class DeptController {


    @Autowired
    private DeptService deptService;

    @Autowired
    private OrganService organService;

    /**
     * 部门--新增接口
     *
     * @param dept 部门
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "部门--新增接口")
    @PostMapping(value = "/dept", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> insert(@RequestBody @Validated Dept dept) {
        if (StringUtils.isBlank(dept.getId())) {
            dept.setId(UUIDHelper.getUuid());
        }
        organService.insertDept(dept);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 部门--删除接口
     *
     * @param id 部门ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "部门--删除接口")
    @DeleteMapping(value = "/dept/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        organService.deleteDept(id);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 部门--修改接口
     *
     * @param dept 部门
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "部门--修改接口")
    @PutMapping(value = "/dept", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated Dept dept) {
        Assert.hasText(dept.getId(), "主键id不能为空");
        organService.updateDept(dept);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * 部门--查询接口
     *
     * @param id 部门ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "部门--查询接口")
    @GetMapping(value = "/dept/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Dept> select(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(deptService.getById(id), HttpStatus.OK);
    }

    /**
     * 部门--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "部门--分页查询接口")
    @PostMapping(value = "/dept/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<Dept>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Dept> pageParam) {
        QueryWrapper<Dept> wrapper = Wrappers.query(pageParam.getEntityParam(Dept.class));
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, Dept.class);
        return new ResponseEntity<>(deptService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "部门--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/dept/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(Dept.class));
    }

    /**
     * 部门--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "部门--导出Excel")
    @PostMapping(value = "/dept/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Dept> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
