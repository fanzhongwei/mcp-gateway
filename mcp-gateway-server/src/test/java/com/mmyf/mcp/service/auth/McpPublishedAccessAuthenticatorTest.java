package com.mmyf.mcp.service.auth;

import com.mmyf.mcp.config.auth.McpPublishedAuthProperties;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.PublishedAuthMode;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.PublishedAuthOrder;
import com.mmyf.mcp.model.entity.mcp.McpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpPublishedAccessAuthenticatorTest {

    @Mock
    private McpOAuthJwtService oauthJwtService;

    private McpPublishedAuthProperties props;
    private McpPublishedAccessAuthenticator authenticator;

    private McpService service;

    @BeforeEach
    void setUp() {
        props = new McpPublishedAuthProperties();
        authenticator = new McpPublishedAccessAuthenticator(props, oauthJwtService);
        service = new McpService();
        service.setAccessToken("legacy-secret");
    }

    @Test
    void legacyOnlyUsesStaticToken() {
        props.setMode(PublishedAuthMode.LEGACY_ONLY);
        assertTrue(authenticator.allowAccess(service, "sid", "legacy-secret"));
        assertFalse(authenticator.allowAccess(service, "sid", "other"));
    }

    @Test
    void oauthOnlyUsesJwt() {
        props.setMode(PublishedAuthMode.OAUTH_ONLY);
        when(oauthJwtService.isOAuthEnabled()).thenReturn(true);
        when(oauthJwtService.isTokenValidForService("jwt", "sid")).thenReturn(true);
        assertTrue(authenticator.allowAccess(service, "sid", "jwt"));
        assertFalse(authenticator.allowAccess(service, "sid", "legacy-secret"));
    }

    @Test
    void bothOAuthFirstWithFallback() {
        props.setMode(PublishedAuthMode.BOTH);
        props.setOrder(PublishedAuthOrder.OAUTH_FIRST);
        props.setFallbackEnabled(true);
        when(oauthJwtService.isOAuthEnabled()).thenReturn(true);
        when(oauthJwtService.isTokenValidForService("legacy-secret", "sid")).thenReturn(false);
        assertTrue(authenticator.allowAccess(service, "sid", "legacy-secret"));
    }

    @Test
    void bothLegacyFirstWithFallback() {
        props.setMode(PublishedAuthMode.BOTH);
        props.setOrder(PublishedAuthOrder.LEGACY_FIRST);
        props.setFallbackEnabled(true);
        when(oauthJwtService.isOAuthEnabled()).thenReturn(true);
        when(oauthJwtService.isTokenValidForService("wrong-legacy", "sid")).thenReturn(false);
        when(oauthJwtService.isTokenValidForService("jwt", "sid")).thenReturn(true);
        assertFalse(authenticator.allowAccess(service, "sid", "wrong-legacy"));
        assertTrue(authenticator.allowAccess(service, "sid", "jwt"));
    }

    @Test
    void bothNoFallbackOAuthFirst() {
        props.setMode(PublishedAuthMode.BOTH);
        props.setOrder(PublishedAuthOrder.OAUTH_FIRST);
        props.setFallbackEnabled(false);
        when(oauthJwtService.isOAuthEnabled()).thenReturn(true);
        when(oauthJwtService.isTokenValidForService("legacy-secret", "sid")).thenReturn(false);
        assertFalse(authenticator.allowAccess(service, "sid", "legacy-secret"));
    }
}
