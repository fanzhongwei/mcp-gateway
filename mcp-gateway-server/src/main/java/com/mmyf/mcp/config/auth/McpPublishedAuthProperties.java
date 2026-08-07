package com.mmyf.mcp.config.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 已发布 MCP 的双认证与内置 OAuth（client_credentials）配置。
 */
@Data
@ConfigurationProperties(prefix = "mcp.published-auth")
public class McpPublishedAuthProperties {

    /**
     * 认证模式：默认仅沿用发布服务上的静态 accessToken，行为与升级前一致。
     */
    private PublishedAuthMode mode = PublishedAuthMode.LEGACY_ONLY;

    /**
     * 并行模式下优先尝试的分支（仅在 mode=BOTH 且开启回退时与单次尝试策略共同生效）。
     */
    private PublishedAuthOrder order = PublishedAuthOrder.OAUTH_FIRST;

    /**
     * BOTH 模式下，首选分支失败是否尝试另一分支。
     */
    private boolean fallbackEnabled = true;

    @NestedConfigurationProperty
    private OAuthServer oauthServer = new OAuthServer();

    public boolean shouldAdvertiseOAuthExtension() {
        return oauthServer.isEnabled() && mode != PublishedAuthMode.LEGACY_ONLY;
    }

    public enum PublishedAuthMode {
        LEGACY_ONLY,
        OAUTH_ONLY,
        BOTH
    }

    public enum PublishedAuthOrder {
        OAUTH_FIRST,
        LEGACY_FIRST
    }

    @Data
    public static class OAuthServer {

        /**
         * 是否启用内置授权服务器（/oauth/token、/oauth/jwks）及 JWT 校验分支。
         */
        private boolean enabled = false;

        private String issuer = "";

        private String audience = "mcp-gateway";

        private long accessTokenTtlSeconds = 3600L;

        private String keyId = "mcp-oauth-key-1";

        /**
         * PKCS#8 PEM，RSA 私钥（BEGIN PRIVATE KEY）。
         */
        private String privateKeyPem = "";

        private List<OAuthClient> clients = new ArrayList<>();
    }

    @Data
    public static class OAuthClient {
        private String clientId = "";
        private String clientSecret = "";
        private List<String> serviceIds = new ArrayList<>();
    }

    public void validateOrThrow() {
        if (mode == PublishedAuthMode.OAUTH_ONLY && !oauthServer.isEnabled()) {
            throw new IllegalStateException(
                    "mcp.published-auth.mode=OAUTH_ONLY 需要 mcp.published-auth.oauth-server.enabled=true");
        }
        if (!oauthServer.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(oauthServer.getIssuer())) {
            throw new IllegalStateException("启用内置 OAuth 时必须配置 mcp.published-auth.oauth-server.issuer");
        }
        if (!StringUtils.hasText(oauthServer.getAudience())) {
            throw new IllegalStateException("启用内置 OAuth 时必须配置 mcp.published-auth.oauth-server.audience");
        }
        if (!StringUtils.hasText(oauthServer.getPrivateKeyPem())) {
            throw new IllegalStateException("启用内置 OAuth 时必须配置 mcp.published-auth.oauth-server.private-key-pem");
        }
        if (oauthServer.getClients() == null || oauthServer.getClients().isEmpty()) {
            throw new IllegalStateException("启用内置 OAuth 时必须配置至少一个 mcp.published-auth.oauth-server.clients");
        }
        for (OAuthClient c : oauthServer.getClients()) {
            if (!StringUtils.hasText(c.getClientId())) {
                throw new IllegalStateException("OAuth client 缺少 client-id");
            }
            if (!StringUtils.hasText(c.getClientSecret())) {
                throw new IllegalStateException("OAuth client 缺少 client-secret: " + c.getClientId());
            }
            if (c.getServiceIds() == null || c.getServiceIds().isEmpty()) {
                throw new IllegalStateException("OAuth client 必须配置 service-ids: " + c.getClientId());
            }
        }
    }
}
