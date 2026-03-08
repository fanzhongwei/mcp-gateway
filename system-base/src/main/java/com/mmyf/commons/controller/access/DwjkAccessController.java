package com.mmyf.commons.controller.access;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mmyf.commons.exception.OperationNotAllowedException;
import com.mmyf.commons.exception.ResourceCreatedFailedException;
import com.mmyf.commons.log.ApiLog;
import com.mmyf.commons.service.access.DwjkAccessCache;
import com.mmyf.commons.util.jwt.JwtUtils;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import com.mmyf.commons.util.uuid.MD5Helper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
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

import com.mmyf.commons.model.entity.access.DwjkAccess;
import com.mmyf.commons.service.access.DwjkAccessService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mmyf.commons.util.excel.ExportExcelUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * <p>
 * 对外接口授权 前端控制器
 * </p>
 *
 * @author mmyf
 * @since 2024-02-28
 */
@RestController
@Tag(name = "对外接口授权", description = "system_base-对外接口授权")
@Slf4j
public class DwjkAccessController {


    @Autowired
    private DwjkAccessService dwjkAccessService;

    @Autowired
    private DwjkAccessCache dwjkAccessCache;

    @Value("${login.jwtDwjkAuthExpire:120}")
    private Long jwtDwjkAuthExpire;

    /**
     * 对外接口授权--新增接口
     *
     * @param dwjkAccess 对外接口授权
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-28
     **/
    @Operation(summary = "对外接口授权--新增接口")
    @PostMapping(value = "/api/v1/dwjkAccess", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiLog(module = "对外接口授权", name = "新增授权")
    public ResponseEntity<DwjkAccess> insert(@RequestBody @Validated DwjkAccess dwjkAccess) {
        if (StringUtils.isBlank(dwjkAccess.getId())) {
            dwjkAccess.setId(UUIDHelper.getUuid());
        }
        try {
            dwjkAccess.setAccessToken(MD5Helper.encrypt(dwjkAccess.getId()));
        } catch (NoSuchAlgorithmException e) {
            log.error("加密失败：{}", dwjkAccess.getId(), e);
            throw new ResourceCreatedFailedException("对外接口授权创建失败，请稍候再试");
        }
        boolean result = dwjkAccessService.save(dwjkAccess);
        if (result) {
            dwjkAccessCache.update(dwjkAccess);
        }
        return new ResponseEntity<>(dwjkAccess, HttpStatus.OK);
    }

    /**
     * 对外接口授权--删除接口
     *
     * @param id 对外接口授权ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-28
     **/
    @Operation(summary = "对外接口授权--删除接口")
    @ApiLog(module = "对外接口授权", name = "删除授权")
    @DeleteMapping(value = "/api/v1/dwjkAccess/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        DwjkAccess dwjkAccess = dwjkAccessService.getById(id);
        boolean result = dwjkAccessService.removeById(id);
        if (result) {
            dwjkAccessCache.delete(dwjkAccess.getAccessToken());
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 对外接口授权--修改接口
     *
     * @param dwjkAccess 对外接口授权
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-28
     **/
    @Operation(summary = "对外接口授权--修改接口")
    @ApiLog(module = "对外接口授权", name = "修改授权")
    @PutMapping(value = "/api/v1/dwjkAccess", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated DwjkAccess dwjkAccess) {
        Assert.hasText(dwjkAccess.getId(), "主键id不能为空");
        try {
            dwjkAccess.setAccessToken(MD5Helper.encrypt(dwjkAccess.getId()));
        } catch (NoSuchAlgorithmException e) {
            log.error("加密失败：{}", dwjkAccess.getId(), e);
            throw new ResourceCreatedFailedException("对外接口授权创建失败，请稍候再试");
        }
        boolean result = dwjkAccessService.updateById(dwjkAccess);
        if (result) {
            DwjkAccess access = dwjkAccessService.getById(dwjkAccess.getId());
            dwjkAccessCache.update(access);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 对外接口授权--查询接口
     *
     * @param id 对外接口授权ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-28
     **/
    @Operation(summary = "对外接口授权--查询接口")
    @GetMapping(value = "/api/v1/dwjkAccess/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<DwjkAccess> select(@Parameter(name = "id", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(dwjkAccessService.getById(id), HttpStatus.OK);
    }

    @Operation(summary = "对外接口获取访问授权")
    @GetMapping("/dwjk/api/v1/access/{systemId}/token/{systemToken}")
    public ResponseEntity<String> getAccessToken(@Parameter(name = "systemId", description = "系统ID", required = true) @PathVariable("systemId") String systemId,
        @Parameter(name = "systemToken", description = "系统token", required = true) @PathVariable("systemToken") String systemToken
    ) {
        DwjkAccess dwjkAccess = dwjkAccessCache.getAccess(systemToken);
        if (null == dwjkAccess) {
            throw new OperationNotAllowedException("systemId或systemToken错误，请联系厂商获取授权");
        }
        if (!StringUtils.equals(dwjkAccess.getId(), systemId)) {
            throw new OperationNotAllowedException("systemId或systemToken错误，请联系厂商获取授权");
        }
        return ResponseEntity.ok(JwtUtils.sign(dwjkAccess.getAccessToken(), JwtUtils.generateSalt(dwjkAccess.getId()), jwtDwjkAuthExpire));
    }

    @Operation(summary = "对外接口授权测试")
    @GetMapping("/dwjk/api/v1/test")
    public ResponseEntity<String> dwjkTokenTest() {
        return ResponseEntity.ok("test ok");
    }

    /**
     * 对外接口授权--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-28
     **/
    @Operation(summary = "对外接口授权--分页查询接口")
    @PostMapping(value = "/api/v1/dwjkAccess/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<DwjkAccess>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<DwjkAccess> pageParam) {
        QueryWrapper<DwjkAccess> wrapper = Wrappers.query(pageParam.getEntityParam(DwjkAccess.class));
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, DwjkAccess.class);
        return new ResponseEntity<>(dwjkAccessService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "对外接口授权--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/api/v1/dwjkAccess/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(DwjkAccess.class));
    }

    /**
     * 对外接口授权--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-28
     **/
    @Operation(summary = "对外接口授权--导出Excel")
    @PostMapping(value = "/api/v1/dwjkAccess/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<DwjkAccess> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
