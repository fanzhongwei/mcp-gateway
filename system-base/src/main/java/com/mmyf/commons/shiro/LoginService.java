package com.mmyf.commons.shiro;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.service.organ.UserService;
import com.mmyf.commons.shiro.security.SecurityService;
import javax.security.auth.login.LoginException;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * package com.mmyf.commons.shiro <br/>
 * description: 登陆服务 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Service
@Slf4j
public class LoginService {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganService organService;

    /**
     * 登陆
     *
     * @param usernamePasswordToken UsernamePasswordToken
     * @throws LoginException 登陆异常
     */
    public void login(UsernamePasswordToken usernamePasswordToken) {
        if (this.securityService.isLogin()) {
            this.logout();
        }
        // 甲方要求以用户名+租户ID（数字类型）进行登录
        String password = new String(usernamePasswordToken.getPassword());
        if (NumberUtil.isInteger(password)) {
            QueryWrapper<User> wrapper = Wrappers.<User>query()
                                                  .eq("c_loginid", usernamePasswordToken.getUsername())
                                                  .exists("select 1 from system_base.t_tenant c where c.n_external_nid = {0} and c.c_id = system_base.t_user.c_tenant", Integer.parseInt(password));
            List<User> userList = userService.list(wrapper);
            if (CollectionUtils.isNotEmpty(userList)) {
                User user = organService.getUser(userList.get(0).getId());
                if (null != user.getPassword()) {
                    usernamePasswordToken.setPassword(user.getPassword().toCharArray());
                }
            }
        }


        Subject subject = SecurityUtils.getSubject();
        usernamePasswordToken.setRememberMe(securityService.isEnableRememberMe());
        try {
            subject.login(usernamePasswordToken);
        } catch (Exception e) {
            log.error("login fail!", e);
            throw new AuthenticationException("用户名或密码错误，请检查");
        }
    }

    public void logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null && (subject.isAuthenticated() || subject.isRemembered())) {
            subject.logout();
        }
        afterLogout();
    }

    protected void afterLogout() {

    }

}
