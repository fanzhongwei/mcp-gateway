package com.mmyf.commons.shiro.realm;

import com.mmyf.commons.model.entity.organ.Right;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.service.organ.OrganCache;
import com.mmyf.commons.service.right.RightService;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mmyf.commons.util.uuid.MD5Helper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * package com.mmyf.commons.shiro.realm <br/>
 * description: 用户名密码验证 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Slf4j
public class UsernamePasswordRealm extends AuthorizingRealm {

    @Autowired
    private RightService rightService;

    @Autowired
    private OrganCache organCache;

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        User account = (User)super.getAvailablePrincipal(principals);
        log.debug("授权查询回调函数, 进行鉴权但缓存中无用户的授权信息时调用: {}---AccountId:" + account.getLoginid());
        List<String> roles = new ArrayList();
        // TODO 从数据库查询角色

        if (!roles.contains("一般用户")) {
            roles.add("一般用户");
        }

        Set<String> perms = rightService.getRightsByUserId(account.getId());

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRoles(roles);
        info.addStringPermissions(perms);
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken userToken = (UsernamePasswordToken)token;
        User user = organCache.getUserByLoginId(userToken.getUsername());
        String password = user.getPassword();
        // 支持直接使用MD5加密后的密码登录
        if (StringUtils.equals(new String(userToken.getPassword()), user.getPassword())) {
            try {
                password = MD5Helper.encrypt(password);
            } catch (NoSuchAlgorithmException e) {
                log.warn("密码MD5加密失败", e);
            }
        }
        return new SimpleAuthenticationInfo(user, password, user.getName());
    }

}
