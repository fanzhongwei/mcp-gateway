package com.mmyf.commons.shiro.realm;

import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.exception.OperationNotAllowedException;
import com.mmyf.commons.model.entity.access.DwjkAccess;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.service.access.DwjkAccessCache;
import com.mmyf.commons.service.organ.OrganCache;
import com.mmyf.commons.util.jwt.JWTCredentialsMatcher;
import com.mmyf.commons.util.jwt.JwtToken;
import com.mmyf.commons.util.jwt.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * package com.mmyf.commons.shiro.realm <br/>
 * description: jwt shiro realm <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Slf4j
public class JWTShiroRealm extends UsernamePasswordRealm {

    @Autowired
    private OrganCache organCache;

    @Autowired
    private DwjkAccessCache dwjkAccessCache;

    public JWTShiroRealm() {
        this.setCredentialsMatcher(new JWTCredentialsMatcher());
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        JwtToken jwtToken = (JwtToken)token;

        if (jwtToken.getType() == SystemCodeEnum.JwtTokenType.WEB_TOKEN) {
            return doGetWebAuthorizationInfo(jwtToken);
        }
        if (jwtToken.getType() == SystemCodeEnum.JwtTokenType.DWJK_TOKEN) {
            return doGetDwjkAuthorizationInfo(jwtToken);
        }
        throw new OperationNotAllowedException("禁止访问，请联系厂商获取授权");
    }

    private AuthenticationInfo doGetDwjkAuthorizationInfo(JwtToken jwtToken) {
        String accessToken = JwtUtils.getUserId(jwtToken.getToken());
        DwjkAccess dwjkAccess = dwjkAccessCache.getAccess(accessToken);
        return new SimpleAuthenticationInfo(dwjkAccess, dwjkAccess.getId(), dwjkAccess.getAccessToken());
    }

    private AuthenticationInfo doGetWebAuthorizationInfo(JwtToken jwtToken) {
        String userId = JwtUtils.getUserId(jwtToken.getToken());
        if (StringUtils.isBlank(userId)) {
            throw new UnknownAccountException("用户不存在！");
        }
        User user = organCache.getUser(userId);
        if (null == user) {
            throw new UnknownAccountException("用户不存在！");
        }
        return new SimpleAuthenticationInfo(user, user.getId(), user.getPassword());
    }

}
