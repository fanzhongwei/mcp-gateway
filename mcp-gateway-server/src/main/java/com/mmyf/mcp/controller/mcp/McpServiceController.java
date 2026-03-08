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

import com.mmyf.mcp.model.dto.McpServiceSaveDTO;
import com.mmyf.mcp.model.entity.mcp.McpService;
import com.mmyf.mcp.model.vo.McpServiceDetailVO;
import com.mmyf.mcp.service.mcp.McpServiceService;

import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.shiro.security.SecurityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.mmyf.commons.util.excel.ExportExcelUtils;
import org.springframework.util.Assert;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import com.mmyf.commons.validator.groups.*;
import jakarta.validation.groups.Default;

import java.util.List;
import java.util.Set;


/**
 * <p>
 * mcp服务 前端控制器
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-26 15:01:45
 */
@RestController
@Tag(name = "mcp服务", description = "业务接口-mcp服务")
@Slf4j
public class McpServiceController {


    @Autowired
    private McpServiceService mcpServiceService;

    @Autowired
    private SecurityService securityService;

    /**
     * mcp服务--新增接口（已废弃，使用saveOrUpdate接口）
     *
     * @param mcpService mcp服务
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 15:01:45
     **/
    @Operation(summary = "mcp服务--新增接口（已废弃，使用saveOrUpdate接口）")
    @PostMapping(value = "/api/v1/mcpService", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<Boolean> insert(@RequestBody @Validated({Insert.class, Default.class}) McpService mcpService) {
        if (StringUtils.isBlank(mcpService.getId())) {
            mcpService.setId(UUIDHelper.getUuid());
        }
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            mcpService.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(mcpServiceService.save(mcpService), HttpStatus.OK);
    }

    /**
     * mcp服务--保存或更新接口（包含配置API）
     *
     * @param saveDTO 保存DTO
     * @return org.springframework.http.ResponseEntity<java.lang.String> 返回服务ID
     * @author 超级管理员
     * @date 2026-01-26
     **/
    @Operation(summary = "mcp服务--保存或更新接口（包含配置API）")
    @PostMapping(value = "/api/v1/mcpService/saveOrUpdate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveOrUpdate(@RequestBody McpServiceSaveDTO saveDTO) {
        McpService mcpService = new McpService();
        mcpService.setId(saveDTO.getId());
        mcpService.setName(saveDTO.getName());
        mcpService.setDesc(saveDTO.getDesc());
        mcpService.setStatus(saveDTO.getStatus());
        mcpService.setAccessToken(saveDTO.getAccessToken());
        mcpService.setEndpoint(saveDTO.getEndpoint());
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            mcpService.setTenant(currUser.getTenant());
        }
        
        List<McpServiceService.SelectionData> selections = new java.util.ArrayList<>();
        if (saveDTO.getConfig() != null && saveDTO.getConfig().getSelections() != null) {
            for (McpServiceSaveDTO.Selection selection : saveDTO.getConfig().getSelections()) {
                McpServiceService.SelectionData selectionData = new McpServiceService.SelectionData();
                selectionData.setSystemId(selection.getSystemId());
                selectionData.setSystemName(selection.getSystemName());
                selectionData.setEnvId(selection.getEnvId());
                selectionData.setEnvName(selection.getEnvName());
                selectionData.setEnvBaseUrl(selection.getEnvBaseUrl());
                selectionData.setApiIds(selection.getApiIds());
                selectionData.setCustomMcpName(selection.getCustomMcpName());
                selectionData.setApiCustomMcpNames(selection.getApiCustomMcpNames());
                selections.add(selectionData);
            }
        }
        
        String serviceId = mcpServiceService.saveOrUpdateService(mcpService, selections);
        return new ResponseEntity<>(serviceId, HttpStatus.OK);
    }

    /**
     * mcp服务--根据mcp服务主键删除接口
     *
     * @param id mcp服务ID
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 15:01:45
     **/
    @Operation(summary = "mcp服务--根据mcp服务主键删除接口")
    @DeleteMapping(value = "/api/v1/mcpService/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> delete(@Parameter(name = "id", description="mcp服务主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(mcpServiceService.removeById(id), HttpStatus.OK);
    }

    /**
     * mcp服务--修改接口
     *
     * @param mcpService mcp服务
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author 超级管理员
     * @date 2026-01-26 15:01:45
     **/
    @Operation(summary = "mcp服务--修改接口")
    @PutMapping(value = "/api/v1/mcpService", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> update(@RequestBody @Validated({Update.class, Default.class}) McpService mcpService) {
        Assert.hasText(mcpService.getId(), "主键id不能为空");
        User currUser = securityService.getCurrUserInfo();
        if (currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            mcpService.setTenant(currUser.getTenant());
        }
        return new ResponseEntity<>(mcpServiceService.updateById(mcpService), HttpStatus.OK);
    }

    /**
     * mcp服务--根据mcp服务主键查询接口
     *
     * @param id mcp服务ID
     * @return org.springframework.http.ResponseEntity<McpService>
     * @author 超级管理员
     * @date 2026-01-26 15:01:45
     **/
    @Operation(summary = "mcp服务--根据mcp服务主键查询接口")
    @GetMapping(value = "/api/v1/mcpService/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<McpService> select(@Parameter(name = "id", description="mcp服务主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(mcpServiceService.getById(id), HttpStatus.OK);
    }

    /**
     * mcp服务--查询详情接口（包含关联的系统、环境、API信息）
     *
     * @param id mcp服务ID
     * @return org.springframework.http.ResponseEntity<McpServiceDetailVO>
     * @author 超级管理员
     * @date 2026-01-26
     **/
    @Operation(summary = "mcp服务--查询详情接口（包含关联的系统、环境、API信息）")
    @GetMapping(value = "/api/v1/mcpService/{id}/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<McpServiceDetailVO> selectDetail(@Parameter(name = "id", description="mcp服务主键", required = true) @PathVariable("id") String id) {
        McpServiceDetailVO detail = mcpServiceService.getServiceDetail(id);
        if (detail == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(detail, HttpStatus.OK);
    }

    /**
     * mcp服务--发布接口
     *
     * @param id mcp服务ID
     * @return org.springframework.http.ResponseEntity<Boolean>
     * @author 超级管理员
     * @date 2026-01-26
     **/
    @Operation(summary = "mcp服务--发布接口")
    @PostMapping(value = "/api/v1/mcpService/{id}/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> publish(@Parameter(name = "id", description="mcp服务主键", required = true) @PathVariable("id") String id) {
        try {
            boolean result = mcpServiceService.publishService(id);
            if (result) {
                return new ResponseEntity<>(result, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("发布失败", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IllegalArgumentException e) {
            log.error("发布服务失败: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("发布服务异常", e);
            return new ResponseEntity<>("发布服务时发生异常: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * mcp服务--停止接口
     *
     * @param id mcp服务ID
     * @return org.springframework.http.ResponseEntity<Boolean>
     * @author 超级管理员
     * @date 2026-01-26
     **/
    @Operation(summary = "mcp服务--停止接口")
    @PostMapping(value = "/api/v1/mcpService/{id}/stop", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> stop(@Parameter(name = "id", description="mcp服务主键", required = true) @PathVariable("id") String id) {
        return new ResponseEntity<>(mcpServiceService.stopService(id), HttpStatus.OK);
    }

    /**
     * mcp服务--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<Page<McpService>>
     * @author 超级管理员
     * @date 2026-01-26 15:01:45
     **/
    @Operation(summary = "mcp服务--分页查询接口")
    @PostMapping(value = "/api/v1/mcpService/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<McpService>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<McpService> pageParam) {
        User currUser = securityService.getCurrUserInfo();
        McpService entityParam = pageParam.getEntityParam(McpService.class);
        if (entityParam != null && currUser != null && StringUtils.isNotBlank(currUser.getTenant())) {
            entityParam.setTenant(currUser.getTenant());
        }
        QueryWrapper<McpService> wrapper = Wrappers.query(entityParam);
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, McpService.class);
        Page<McpService> page = mcpServiceService.page(pageParam.getPageParam(), wrapper);
        
        // 批量计算每个服务的组合数（接口数已在实体类中）
        // 注意：由于实体类没有 comboCount 字段，这里无法直接设置
        // 前端可以通过 apiCount 字段获取接口总数，组合数可以通过详情接口获取
        // 或者前端可以在需要时单独查询统计信息
        
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @Operation(summary = "mcp服务--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/api/v1/mcpService/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(McpService.class));
    }

    /**
     * mcp服务--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author 超级管理员
     * @date 2026-01-26 15:01:45
     **/
    @Operation(summary = "mcp服务--导出Excel")
    @PostMapping(value = "/api/v1/mcpService/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<McpService> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
