package com.mmyf.commons.shiro.filter;

import com.alibaba.fastjson.JSON;
import com.mmyf.commons.advice.ExceptionBody;
import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.entity.access.DwjkAccess;
import com.mmyf.commons.service.access.DwjkAccessCache;
import com.mmyf.commons.util.http.RequestUtils;
import com.mmyf.commons.util.jwt.JwtToken;
import com.mmyf.commons.util.jwt.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.http.HttpStatus;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * package com.mmyf.commons.shiro.filter <br/>
 * description: 对外接口只支持AccessToke方式请求 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Slf4j
public class JwtDwjkAuthFilter extends AuthenticatingFilter {

    /** token 过期时间 秒 */
    private final Long expireSeconds;

    private final DwjkAccessCache dwjkAccessCache;

    public JwtDwjkAuthFilter(Long expireSeconds, DwjkAccessCache dwjkAccessCache) {
        this.expireSeconds = expireSeconds;
        this.dwjkAccessCache = dwjkAccessCache;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) {
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // 支持RESTFul无状态，请求时在Header中放一个Authorization，值就是对应的Token
        HttpServletRequest req = (HttpServletRequest) request;
        String jwtToken = req.getHeader(JwtUtils.AUTH_INTERFACE_HEADER);

        if (StringUtils.isNotBlank(jwtToken)) {
            // 验证token是否过期
            try {
                if (JwtUtils.isTokenExpired(jwtToken)) {
                    writeUnAuthorized((HttpServletResponse) response, "非法访问，请联系厂商获取授权");
                    return false;
                }
            } catch (Exception e) {
                writeUnAuthorized((HttpServletResponse) response, "非法访问，请联系厂商获取授权");
                return false;
            }

            // 验证JWT token
            if(!isLoginRequest(request, response)) {
                if (validateTokenParam(jwtToken, (HttpServletRequest) request, (HttpServletResponse) response)) {
                    return executeLogin(request, response);
                }
                return false;
            }
        }

        // 没有AccessToken不允许访问
        writeUnAuthorized((HttpServletResponse) response, "非法访问，请联系厂商获取授权");
        return false;
    }

    private boolean validateTokenParam(String jwtToken, HttpServletRequest request, HttpServletResponse response) {
        String accessToken = JwtUtils.getUserId(jwtToken);
        if (StringUtils.isBlank(accessToken)) {
            writeUnAuthorized(response, "非法访问，请联系厂商获取授权");
            return false;
        }
        DwjkAccess dwjkAccess = dwjkAccessCache.getAccess(accessToken);
        if (null == dwjkAccess) {
            writeUnAuthorized(response, "非法访问，请联系厂商获取授权");
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = dwjkAccess.getAccessTimeStart();
        if (null != start && now.isBefore(start)) {
            writeUnAuthorized(response, "非法访问，您的授权还未生效");
            return false;
        }
        LocalDateTime end = dwjkAccess.getAccessTimeEnd();
        if (null != end && now.isAfter(end)) {
            writeUnAuthorized(response, "非法访问，您的授权已过期");
            return false;
        }

        // 获取当前线程的请求属性
        String remoteIp = RequestUtils.getRemoteIp(request);
        String ipWhiteList = dwjkAccess.getIpWhitelist();
        if (StringUtils.isNotBlank(ipWhiteList) && StringUtils.isNotBlank(remoteIp)) {
            String[] ipList = StringUtils.split(ipWhiteList, ";");
            if (!StringUtils.equalsAny(remoteIp, ipList)) {
                writeUnAuthorized(response, "非法访问，请联系厂商获取授权");
                return false;
            }
        }
        String ipBlacklist = dwjkAccess.getIpBlacklist();
        if (StringUtils.isNotBlank(ipBlacklist) && StringUtils.isNotBlank(remoteIp)) {
            String[] ipList = StringUtils.split(ipBlacklist, ";");
            if (StringUtils.equalsAny(remoteIp, ipList)) {
                writeUnAuthorized(response, "非法访问，请联系厂商获取授权");
                return false;
            }
        }
        return true;
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        String jwtToken = req.getHeader(JwtUtils.AUTH_INTERFACE_HEADER);
        return new JwtToken(jwtToken, SystemCodeEnum.JwtTokenType.DWJK_TOKEN);
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
            httpResponse.setHeader(JwtUtils.AUTH_INTERFACE_HEADER, jwtTokenNew);
        }
        return true;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        log.error("token：{}，验证失败", token.getCredentials().toString() ,e);
        writeUnAuthorized((HttpServletResponse) response, "非法访问，请联系厂商获取授权");
        return false;
    }

    private void writeUnAuthorized(HttpServletResponse response, String message) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.println(JSON.toJSONString(new ExceptionBody(HttpStatus.UNAUTHORIZED, StringUtils.defaultIfBlank(message, "UNAUTHORIZED"))));
        } catch (IOException e1) {
            log.error("输入错误信息失败", e1);
        }
    }
}
