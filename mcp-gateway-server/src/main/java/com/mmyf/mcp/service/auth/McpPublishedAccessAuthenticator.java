package com.mmyf.mcp.service.auth;

import com.mmyf.mcp.config.auth.McpPublishedAuthProperties;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.PublishedAuthMode;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.PublishedAuthOrder;
import com.mmyf.mcp.model.entity.mcp.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 已发布 MCP 访问校验：静态 accessToken 与内置 OAuth JWT 双轨并行。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpPublishedAccessAuthenticator {

    private final McpPublishedAuthProperties properties;
    private final McpOAuthJwtService oauthJwtService;

    public boolean legacyTokenMatches(McpService service, String accessToken) {
        return StringUtils.hasText(accessToken) && accessToken.equals(service.getAccessToken());
    }

    public boolean oauthTokenMatchesService(String serviceId, String accessToken) {
        if (!oauthJwtService.isOAuthEnabled()) {
            return false;
        }
        return oauthJwtService.isTokenValidForService(accessToken, serviceId);
    }

    /**
     * @return true 表示允许访问该已发布服务
     */
    public boolean allowAccess(McpService service, String serviceId, String accessToken) {
        PublishedAuthMode mode = properties.getMode();
        if (mode == PublishedAuthMode.LEGACY_ONLY) {
            boolean ok = legacyTokenMatches(service, accessToken);
            if (!ok) {
                log.warn("mcp published auth failed branch=LEGACY serviceId={}", serviceId);
            }
            return ok;
        }
        if (mode == PublishedAuthMode.OAUTH_ONLY) {
            boolean ok = oauthTokenMatchesService(serviceId, accessToken);
            if (!ok) {
                log.warn("mcp published auth failed branch=OAUTH serviceId={}", serviceId);
            }
            return ok;
        }

        boolean oauthFirst = properties.getOrder() == PublishedAuthOrder.OAUTH_FIRST;
        if (properties.isFallbackEnabled()) {
            if (oauthFirst) {
                if (oauthTokenMatchesService(serviceId, accessToken)) {
                    return true;
                }
                if (legacyTokenMatches(service, accessToken)) {
                    return true;
                }
            } else {
                if (legacyTokenMatches(service, accessToken)) {
                    return true;
                }
                if (oauthTokenMatchesService(serviceId, accessToken)) {
                    return true;
                }
            }
        } else {
            if (oauthFirst) {
                return oauthTokenMatchesService(serviceId, accessToken);
            }
            return legacyTokenMatches(service, accessToken);
        }
        log.warn("mcp published auth failed branch=BOTH serviceId={} oauthFirst={} fallback={}",
                serviceId, oauthFirst, properties.isFallbackEnabled());
        return false;
    }
}
