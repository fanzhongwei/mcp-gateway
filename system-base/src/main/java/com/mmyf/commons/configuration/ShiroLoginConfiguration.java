package com.mmyf.commons.configuration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.mmyf.commons.constant.SystemBaseConstant;
import com.mmyf.commons.service.access.DwjkAccessCache;
import com.mmyf.commons.shiro.filter.JwtDwjkAuthFilter;
import com.mmyf.commons.shiro.filter.JwtWebAuthFilter;
import com.mmyf.commons.shiro.realm.JWTShiroRealm;
import com.mmyf.commons.shiro.realm.UsernamePasswordRealm;
import com.mmyf.commons.shiro.security.SecurityService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.Filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * package com.mmyf.commons.shiro <br/>
 * description: 登陆配置 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Configuration
public class ShiroLoginConfiguration {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${login.permitUrl:}")
    private String permitUrl;

    // web前端请求有效时间，默认30分钟
    @Value("${login.jwtWebAuthExpire:1800}")
    private Long jwtWebAuthExpire;

    // 接口请求有效时间，默认2分钟
    @Value("${login.jwtDwjkAuthExpire:120}")
    private Long jwtDwjkAuthExpire;

    @Autowired
    private SecurityService securityService;

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager manager) {
        AuthorizationAttributeSourceAdvisor aasa = new AuthorizationAttributeSourceAdvisor();
        aasa.setSecurityManager(manager);
        return aasa;
    }

    @Bean
    @ConditionalOnMissingBean
    public CredentialsMatcher credentialsMatcher() {
        HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher();
        credentialsMatcher.setHashAlgorithmName("MD5");
        return credentialsMatcher;
    }

    @Bean
    public UsernamePasswordRealm usernamePasswordRealm(CredentialsMatcher matcher) {
        UsernamePasswordRealm realm = new UsernamePasswordRealm();
        realm.setCredentialsMatcher(matcher);
        realm.setCachingEnabled(true);
        realm.setAuthenticationCachingEnabled(false);
        realm.setAuthorizationCachingEnabled(true);
        return realm;
    }

    @Bean
    public JWTShiroRealm jwtRealm() {
        JWTShiroRealm realm = new JWTShiroRealm();
        realm.setCachingEnabled(false);
        realm.setAuthenticationCachingEnabled(false);
        realm.setAuthorizationCachingEnabled(false);
        return realm;
    }

    @Bean
    public SecurityManager securityManager(@Qualifier("usernamePasswordRealm") UsernamePasswordRealm usernamePasswordRealm,
                                           @Qualifier("jwtRealm") JWTShiroRealm jwtRealm,
                                           @Autowired(required = false) CacheManager shiroCacheManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        List<Realm> realms = new ArrayList<>();
        realms.add(jwtRealm);
        realms.add(usernamePasswordRealm);
        securityManager.setRealms(realms);
        securityManager.setCacheManager(shiroCacheManager);
        ((ModularRealmAuthenticator) securityManager.getAuthenticator()).setAuthenticationStrategy(new FirstSuccessfulStrategy());
        return securityManager;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager, DwjkAccessCache dwjkAccessCache) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        Map<String, Filter> filters = shiroFilterFactoryBean.getFilters();
        // 添加JwtWebAuthFilter，同时支持用户名密码+token登录
        filters.put("authc", new JwtWebAuthFilter(jwtWebAuthExpire));
        // 添加JwtInterfaceAuthFilter，用于接口调用身份认证
        filters.put("interfaceAuth", new JwtDwjkAuthFilter(jwtDwjkAuthExpire, dwjkAccessCache));
        filters.put("anon", new AnonymousFilter());

        // 拦截器.
        Map<String, String> filterChainDefinitionMap = getFilterChains();
        // 登录url
        shiroFilterFactoryBean.setLoginUrl("/api/v1/login");
        // 主页
        shiroFilterFactoryBean.setSuccessUrl("/");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return shiroFilterFactoryBean;
    }

    private Map<String, String> getFilterChains() {
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // 先添加自定义的

        // 外部接口认证
        filterChainDefinitionMap.put("/dwjk/api/v1/access/**", "anon");
        filterChainDefinitionMap.put("/dwjk/**", "interfaceAuth");

        filterChainDefinitionMap.put("/rights*", "authc,perms[" + SystemBaseConstant.SYSTEM_CONSOLE + "]");
        filterChainDefinitionMap.put("/organ*", "authc,perms[" + SystemBaseConstant.SYSTEM_CONSOLE + "]");
        filterChainDefinitionMap.put("/roles*", "authc,perms[" + SystemBaseConstant.SYSTEM_CONSOLE + "]");
        filterChainDefinitionMap.put("/actuator*", "authc,perms[" + SystemBaseConstant.SYSTEM_CONSOLE + "]");

        // 前端资源是vue打包，都定向到首页，前端判断是否登陆
        filterChainDefinitionMap.put("/", "anon");
        // 前端资源都定向到/mobile
        filterChainDefinitionMap.put("/mobile", "anon");

        filterChainDefinitionMap.put("/**/login", "anon");
        filterChainDefinitionMap.put("/**/logout", "anon");
        filterChainDefinitionMap.put("/**/*.js", "anon");
        filterChainDefinitionMap.put("/**/*.js.map", "anon");
        filterChainDefinitionMap.put("/**/*.json", "anon");
        filterChainDefinitionMap.put("/**/*.mp4", "anon");
        filterChainDefinitionMap.put("/**/*.css", "anon");
        filterChainDefinitionMap.put("/**/*.png", "anon");
        filterChainDefinitionMap.put("/**/*.jpg", "anon");
        filterChainDefinitionMap.put("/**/*.jpeg", "anon");
        filterChainDefinitionMap.put("/**/*.gif", "anon");
        filterChainDefinitionMap.put("/**/*.ico", "anon");
        filterChainDefinitionMap.put("/**/*.svg", "anon");
        filterChainDefinitionMap.put("/**/*.webp", "anon");
        filterChainDefinitionMap.put("/**/*.swf", "anon");
        filterChainDefinitionMap.put("/**/*.eot", "anon");
        filterChainDefinitionMap.put("/**/*.otf", "anon");
        filterChainDefinitionMap.put("/**/*.ttf", "anon");
        filterChainDefinitionMap.put("/**/*.woff", "anon");
        filterChainDefinitionMap.put("/**/*.woff2", "anon");
        // 微服务健康检查需要用到
        filterChainDefinitionMap.put("/actuator/health", "anon");

        // swagger
        filterChainDefinitionMap.put("/swagger-ui.html", "anon");
        filterChainDefinitionMap.put("/swagger-ui.html/**", "anon");
        filterChainDefinitionMap.put("/swagger-resources", "anon");
        filterChainDefinitionMap.put("/swagger-resources/**", "anon");
        filterChainDefinitionMap.put("/v2/api-docs", "anon");

        // 处理配置的例外url
        if (permitUrl != null) {
            Arrays.stream(StringUtils.split(permitUrl, ",")).map(s -> s.trim()).filter(s -> !StringUtils.isBlank(s)).forEach(urlPattern -> {
                filterChainDefinitionMap.put(urlPattern, "anon");
            });
        }

        if (securityService.isEnableRememberMe()) {
            filterChainDefinitionMap.put("/**", "user");
        } else {
            filterChainDefinitionMap.put("/**", "authc");
        }
        return filterChainDefinitionMap;
    }

}
