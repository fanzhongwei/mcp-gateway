package com.mmyf.commons.controller.right;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmyf.commons.log.ApiLog;
import com.mmyf.commons.model.entity.organ.Right;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.model.vo.organ.RightVo;
import com.mmyf.commons.service.right.RightService;
import com.mmyf.commons.shiro.security.SecurityService;
import com.mmyf.commons.translate.annotation.DataTranslate;
import com.mmyf.commons.util.excel.ExportExcelUtils;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;

/**
 * <p>
 * 权限定义 前端控制器
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "system_base-权限管理")
@Tag(name = "system_base-权限管理")
public class RightController {

    @Autowired
    private RightService rightService;

    @Autowired
    private SecurityService securityService;

    /**
     * 权限定义--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-18
     **/
    @Operation(summary = "权限定义--分页查询接口")
    @PostMapping(value = "/right/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<Right>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Right> pageParam) {
        QueryWrapper<Right> wrapper = Wrappers.query(pageParam.getEntityParam(Right.class));
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, Right.class);
        return new ResponseEntity<>(rightService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "权限定义--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/right/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(Right.class));
    }

    /**
     * 权限定义--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "权限定义--导出Excel")
    @PostMapping(value = "/right/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Right> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }

    @GetMapping("/rights/all")
    @Operation(summary = "获取所有权限点")
    public ResponseEntity<List<Right>> allRightKeys() {
        return ResponseEntity.ok(rightService.allRight());
    }

    /**
     * 获取权限信息
     *
     * @param key 权限字
     * @return RightVo
     */
    @GetMapping("/right/{key}")
    @Operation(summary = "获取权限信息")
    public ResponseEntity<RightVo> getRightInfo(
            @Parameter(name = "key", description = "权限字", required = true)
            @PathVariable("key") String key) {
        return ResponseEntity.ok(rightService.getRightInfo(key));
    }

    /**
     * 添加权限点
     *
     * @param rightVo 权限信息
     */
    @PostMapping("/right")
    @Operation(summary = "添加权限点")
    @ApiLog(module = "权限管理", name = "添加权限点")
    public ResponseEntity<Void> addRight(@RequestBody RightVo rightVo) {
        Assert.hasText(rightVo.getRightkey(), "权限字不能为空");
        Assert.hasText(rightVo.getName(), "权限名称不能为空");
        Assert.hasText(rightVo.getDescript(), "权限描述不能为空");

        rightService.addRight(rightVo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rights/user/{userId}")
    @Operation(summary = "获取用户所拥有的权限")
    public ResponseEntity<Set<String>> getUserRights(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(securityService.getUserRights(userId));
    }

    /**
     * 更新权限
     *
     * @param rightVo 权限信息
     */
    @Operation(summary = "更新权限点")
    @PutMapping("/right")
    @ApiLog(module = "权限管理", name = "更新权限点")
    public ResponseEntity<Void> updateRight(@RequestBody RightVo rightVo) {
        Assert.hasText(rightVo.getRightkey(), "权限字不能为空");
        Assert.hasText(rightVo.getName(), "权限名称不能为空");
        Assert.hasText(rightVo.getDescript(), "权限描述不能为空");
        rightService.updateRight(rightVo);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除权限
     *
     * @param rightKey 权限key
     */
    @DeleteMapping("/right/{key}")
    @Operation(summary = "删除权限点")
    @ApiLog(module = "权限管理", name = "删除权限点")
    public ResponseEntity<Void> deleteRight(
            @Parameter(name = "权限key", required = true)
            @PathVariable("key") String rightKey) {
        rightService.deleteRight(rightKey);
        return ResponseEntity.noContent().build();
    }

}
