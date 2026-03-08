package com.mmyf.commons.controller.organ;

import com.mmyf.commons.log.ApiLog;
import com.mmyf.commons.model.entity.organ.Tenant;
import com.mmyf.commons.model.entity.organ.Dept;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.model.vo.organ.BaseOrgan;
import com.mmyf.commons.model.vo.organ.TenantVo;
import com.mmyf.commons.model.vo.organ.DeptVo;
import com.mmyf.commons.model.vo.organ.UserVo;
import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.shiro.security.SecurityService;
import com.mmyf.commons.translate.annotation.DataTranslate;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * package com.mmyf.controller.organ
 * description: 租户管理
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 18:26:20
 */
@RestController
@RequestMapping("/api/v1/organ")
@Tag(name = "system_base-租户树")
@Slf4j
public class OrganController {

    @Autowired
    private OrganService organService;

    @Autowired
    private SecurityService securityService;

    @Operation(summary = "根据用户租户权限获取租户树")
    @GetMapping("/tree")
    @DataTranslate
    public ResponseEntity<BaseOrgan> getOrganTreeByUserRight() {
        User user = securityService.getCurrUserInfo();
        Assert.notNull(user, "未授权的访问");
        return ResponseEntity.ok(organService.getOrganTreeByUserRight(user));
    }

    @Operation(summary = "获取租户树--organ")
    @GetMapping("/tree/{organId}")
    @DataTranslate
    public ResponseEntity<BaseOrgan> getOrganTree(@Parameter(description = "租户树根节点ID") @PathVariable("organId") String organId) {
        TenantVo treeTenant = organService.getTreeTenant(organId);
        if (null != treeTenant) {
            return  ResponseEntity.ok(treeTenant);
        }
        return ResponseEntity.ok(organService.getTreeDept(organId));
    }

    @Operation(summary = "获取租户树--tenant")
    @GetMapping("/tree/tenant/{tenantId}")
    @DataTranslate
    public ResponseEntity<TenantVo> getOrganTreeTenant(
            @Parameter(description = "租户树根节点ID") @PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(organService.getTreeTenant(tenantId));
    }

    @Operation(summary = "获取租户树--dept")
    @GetMapping("/tree/dept/{deptId}")
    public ResponseEntity<DeptVo> getOrganTreeDept(
            @Parameter(description = "租户树根节点ID") @PathVariable("deptId") String deptId) {
        return ResponseEntity.ok(organService.getTreeDept(deptId));
    }

    @Operation(summary = "获取租户树--查询用户")
    @GetMapping("/user/list")
    public ResponseEntity<List<UserVo>> getUserList(@RequestParam("key") @Parameter(description = "查询参数：登录名或者用户名", required = true) String searchKey,
                                                    @RequestParam(value = "hasNotRight", required = false) @Parameter(description = "权限点") String hasNotRight) {
        return ResponseEntity.ok(organService.searchUser(searchKey, hasNotRight));
    }
}
