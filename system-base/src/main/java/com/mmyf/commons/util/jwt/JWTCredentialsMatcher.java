package com.mmyf.commons.util.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.entity.access.DwjkAccess;
import com.mmyf.commons.model.entity.organ.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

/**
 * package com.mmyf.commons.util.jwt <br/>
 * description: JWT 验证 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Slf4j
public class JWTCredentialsMatcher implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        JwtToken jwtToken = (JwtToken)token;
        if (jwtToken.getType() == SystemCodeEnum.JwtTokenType.WEB_TOKEN) {
            return doWebVerify(info, jwtToken);
        }
        if (jwtToken.getType() == SystemCodeEnum.JwtTokenType.DWJK_TOKEN) {
            return doDwjkVerify(info, jwtToken);
        }
        return false;
    }

    private boolean doDwjkVerify(AuthenticationInfo info, JwtToken jwtToken) {
        String jwt = (String) jwtToken.getCredentials();
        DwjkAccess dwjkAccess = (DwjkAccess) info.getPrincipals().getPrimaryPrincipal();
        try {
            JwtUtils.verify(jwt, dwjkAccess.getAccessToken(), JwtUtils.generateSalt(dwjkAccess.getId()));
            return true;
        } catch (JWTVerificationException e) {
            log.error("Token Error:{}", e.getMessage(), e);
            return false;
        }
    }

    private static boolean doWebVerify(AuthenticationInfo info, JwtToken jwtToken) {
        String jwt = (String) jwtToken.getCredentials();
        User user = (User) info.getPrincipals().getPrimaryPrincipal();
        try {
            JwtUtils.verify(jwt, user.getId(), JwtUtils.generateSalt(user.getId()));
            return true;
        } catch (JWTVerificationException e) {
            log.error("Token Error:{}", e.getMessage(), e);
            return false;
        }
    }

}
