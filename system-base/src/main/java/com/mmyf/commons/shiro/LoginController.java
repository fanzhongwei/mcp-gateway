package com.mmyf.commons.shiro;

import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.shiro.security.SecurityService;
import com.mmyf.commons.translate.annotation.DataTranslate;
import com.mmyf.commons.util.jwt.JwtUtils;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * package com.mmyf.commons.shiro <br/>
 * description: 登陆 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Controller
@Slf4j
@Tag(name = "用户登录相关操作", description = "system_base-用户登录")
public class LoginController {

    @Value("${login.jwtWebAuthExpire:1800}")
    private Long jwtWebAuthExpire;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private LoginService loginService;

    /**
     * 登陆认证
     *
     * @param usernamePasswordToken UsernamePasswordToken
     * @return 登陆结果
     */
    @PostMapping("/api/v1/login")
    @Operation(summary = "用户名密码登录",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "用户名密码，例如：{username:\"xxx\",password:\"xxx\"}",
                required = true,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(
                                implementation = UsernamePasswordToken.class,
                                example = "{\"username\":\"xxx\",\"password\":\"xxx\"}"
                        )
                )
        )
    )
    @ResponseBody
    @DataTranslate
    public ResponseEntity<User> login(@Parameter(hidden = false) @RequestBody UsernamePasswordToken usernamePasswordToken) {
        loginService.login(usernamePasswordToken);
        User currUserInfo = securityService.getCurrUserInfo();
        return ResponseEntity.ok().header(JwtUtils.AUTH_HEADER, JwtUtils.sign(currUserInfo.getId(), JwtUtils.generateSalt(currUserInfo.getId()), jwtWebAuthExpire)).body(currUserInfo);
    }

    /**
     * 登出
     * @return Void
     */
    @GetMapping("/api/v1/logout")
    @Operation(summary = "用户登出")
    public ResponseEntity<Void> logout() {
        loginService.logout();
        return ResponseEntity.noContent().build();
    }

}
