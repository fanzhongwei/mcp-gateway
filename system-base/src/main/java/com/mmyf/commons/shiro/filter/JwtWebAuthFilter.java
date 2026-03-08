package com.mmyf.commons.shiro.filter;

import com.alibaba.fastjson.JSON;
import com.mmyf.commons.advice.ExceptionBody;
import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.util.jwt.JwtToken;
import com.mmyf.commons.util.jwt.JwtUtils;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.http.HttpStatus;

/**
 * package com.mmyf.commons.shiro.filter <br/>
 * description: web页面登陆同时支持用户名密码+token登录 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Slf4j
public class JwtWebAuthFilter extends FormAuthenticationFilter {

    /** token 过期时间 秒 */
    private final Long expireSeconds;

    public JwtWebAuthFilter(Long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // 支持RESTFul无状态，请求时在Header中放一个Authorization，值就是对应的Token
        HttpServletRequest req = (HttpServletRequest) request;
        String jwtToken = req.getHeader(JwtUtils.AUTH_HEADER);

        if (StringUtils.isNotBlank(jwtToken)) {
            // 验证token是否过期
            try {
                if (JwtUtils.isTokenExpired(jwtToken)) {
                    writeUnAuthorized((HttpServletResponse) response);
                    return false;
                }
            } catch (Exception e) {
                writeUnAuthorized((HttpServletResponse) response);
                return false;
            }

            // 验证JWT token
            if(!isLoginRequest(request, response)) {
                return executeLogin(request, response);
            }
        }

        // 没有token走用户名密码登录
        writeUnAuthorized((HttpServletResponse) response);
        return false;
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        String jwtToken = req.getHeader(JwtUtils.AUTH_HEADER);
        return new JwtToken(jwtToken, SystemCodeEnum.JwtTokenType.WEB_TOKEN);
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        String jwtTokenNew = null;
        if (token instanceof JwtToken) {
            String jwtToken = token.getCredentials().toString();
            jwtTokenNew = JwtUtils.refreshTokenExpired(jwtToken, JwtUtils.generateSalt(JwtUtils.getUserId(jwtToken)), expireSeconds);
        }
        if (null != jwtTokenNew) {
            httpResponse.setHeader(JwtUtils.AUTH_HEADER, jwtTokenNew);
        }
        return true;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        log.error("token：{}，验证失败", token.getCredentials().toString() ,e);
        writeUnAuthorized((HttpServletResponse) response);
        return false;
    }

    private void writeUnAuthorized(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSON.toJSONString(new ExceptionBody(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED")));
        } catch (IOException e1) {
            log.error("输入错误信息失败", e1);
        }
    }
}
