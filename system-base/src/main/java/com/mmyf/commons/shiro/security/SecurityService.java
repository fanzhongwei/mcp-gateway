package com.mmyf.commons.shiro.security;

import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.service.organ.OrganCache;
import com.mmyf.commons.service.right.RightService;
import com.mmyf.commons.util.lang.ExpressionUtils;
import java.util.Set;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * package com.mmyf.commons.shiro.security <br/>
 * description: 安全框架封装服务 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Component
public class SecurityService {

    /**
     * 是否记住登陆
     */
    @Value("${login.remember:false}")
    private boolean isEnableRememberMe;

    @Autowired
    private RightService rightService;

    @Resource
    private OrganCache organCache;

    /**
     * 是否记住登陆
     *
     * @return
     */
    public boolean isEnableRememberMe() {
        return isEnableRememberMe;
    }

    private Subject getSubject() {
        try {
            return SecurityUtils.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 是否已登录
     * @return
     */
    public boolean isLogin() {
        Subject subject = getSubject();
        if (subject == null) {
            return false;
        }
        if (subject.isAuthenticated()) {
            return true;
        }
        return isEnableRememberMe && subject.isRemembered();
    }

    /**
     * 获取当前用户信息
     * @return
     */
    public User getCurrUserInfo() {
        Subject subject = getSubject();
        if (isLogin()) {
            User user = (User) subject.getPrincipal();
            user.setRightList(getUserRights(user.getId()));
            return user;
        } else {
            return null;
        }
    }

    public Set<String> getUserRights(String userId) {
        return rightService.getRightsByUserId(userId);
    }

    private boolean isSuperAdmin() {
        Subject subject = getSubject();
        User user = null;
        if (isLogin()) {
            user = (User) subject.getPrincipal();
        }
        if (user == null) {
            return false;
        }
        return StringUtils.equalsIgnoreCase("admin", user.getLoginid());
    }

    /**
     * 当前用户是否有指定角色
     * @param role
     * @return
     */
    public boolean hasRole(String role) {
        if (isSuperAdmin()) {
            return true;
        }
        Subject subject = getSubject();
        if (isLogin()) {
            return subject.hasRole(role);
        }
        return false;
    }

    /**
     * 当前用户是否有指定权限，可传入表达式
     * @param rightExpr
     * @return
     */
    public boolean hasRight(String rightExpr) {
        if (isSuperAdmin()) {
            return true;
        }
        String expRight = parseRights2Exp(rightExpr);
        return ExpressionUtils.evaluateExpression(expRight);
    }

    /**
     * 当前用户是否有指定权限，可传入表达式
     * @param rightExpr
     * @param organRight
     * @param customDataRight
     * @return
     */
    public boolean hasRight(String rightExpr, String organRight, String customDataRight) {
        if (isSuperAdmin()) {
            return true;
        }
        if (StringUtils.isNoneBlank(organRight) || StringUtils.isNoneBlank(customDataRight)) {
            return false;
        }
        String expRight = parseRights2Exp(rightExpr);
        return ExpressionUtils.evaluateExpression(expRight);
    }

    protected boolean hasRightInner(String right) {
        if (isSuperAdmin()) {
            return true;
        }
        Subject subject = getSubject();
        if (isLogin()) {
            return subject.isPermitted(right);
        }
        return false;
    }

    private String parseRights2Exp(String right) {
        if (null == right) {
            return "1";
        }

        StringBuffer exp = new StringBuffer();
        int head = 0;
        boolean isLetter = false;
        for (int i = 0; i < right.length(); i++) {
            char ch = right.charAt(i);
            if (ch != '(' && ch != ')' && ch != ' ') {
                if (!isLetter) {
                    head = i;
                    isLetter = true;
                }
            } else {
                if (isLetter) {
                    String str = right.substring(head, i);
                    if ("or".equalsIgnoreCase(str) || "|".equals(str)) {
                        exp.append("|");
                    } else if ("and".equalsIgnoreCase(str) || "&".equals(str)) {
                        exp.append("&");
                    } else {
                        boolean isTrue = this.hasRightInner(str);
                        // boolean isTrue = false;
                        exp.append(isTrue ? 1 : 0);
                    }
                    isLetter = false;
                }
                exp.append(ch);
            }
        }

        if (isLetter) {
            String str = right.substring(head, right.length());
            boolean isTrue = this.hasRightInner(str);
            // boolean isTrue = true;
            exp.append(isTrue ? 1 : 0);
        }

        return exp.toString();
    }

    /**
     * 获取当前用户权限（包含功能权限以及功能权限对应的数据权限），可传入表达式
     * @param rightExpt 权限表达式为空时返回所有权限
     * @return
     */
    public Object getRight(String rightExpt) {
        return null;
    }

    /**
     * 获取当前用户权限（包含功能权限以及功能权限对应的数据权限）
     *
     * @param filterSet
     * @return
     */
    public Object getRight(Set<String> filterSet) {
        return null;
    }


    /**
     * 是否是rememberMe登录的
     * @since 6.1.4
     * @return
     */
    public boolean isRemembered() {
        Subject subject = getSubject();
        return subject == null ? false : subject.isRemembered();
    }
}
