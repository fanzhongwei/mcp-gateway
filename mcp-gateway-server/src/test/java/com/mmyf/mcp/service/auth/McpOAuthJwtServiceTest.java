package com.mmyf.mcp.service.auth;

import com.mmyf.mcp.config.auth.McpPublishedAuthProperties;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.OAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class McpOAuthJwtServiceTest {

    private McpPublishedAuthProperties props;
    private McpOAuthJwtService service;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        String pem = toPkcs8Pem(kp.getPrivate().getEncoded());

        props = new McpPublishedAuthProperties();
        props.getOauthServer().setEnabled(true);
        props.getOauthServer().setIssuer("https://issuer.test/mcp-gateway");
        props.getOauthServer().setAudience("mcp-gateway");
        props.getOauthServer().setAccessTokenTtlSeconds(120);
        props.getOauthServer().setKeyId("kid-1");
        props.getOauthServer().setPrivateKeyPem(pem);
        OAuthClient c = new OAuthClient();
        c.setClientId("c1");
        c.setClientSecret("s1");
        c.setServiceIds(List.of("svc-a", "svc-b"));
        props.getOauthServer().setClients(List.of(c));

        service = new McpOAuthJwtService(props);
        service.initialize();
    }

    @Test
    void issueAndVerifyTokenForAllowedService() {
        OAuthClient c = props.getOauthServer().getClients().get(0);
        String jwt = service.issueAccessToken(c);
        assertTrue(service.isTokenValidForService(jwt, "svc-a"));
        assertTrue(service.isTokenValidForService(jwt, "svc-b"));
        assertFalse(service.isTokenValidForService(jwt, "svc-other"));
    }

    @Test
    void rejectWrongSecret() {
        assertTrue(service.authenticateClient("c1", "s1").isPresent());
        assertTrue(service.authenticateClient("c1", "wrong").isEmpty());
    }

    @Test
    void jwksContainsKey() {
        var doc = service.jwksDocument();
        assertNotNull(doc.get("keys"));
        assertEquals(1, ((List<?>) doc.get("keys")).size());
    }

    private static String toPkcs8Pem(byte[] pkcs8) {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pkcs8);
        return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----\n";
    }
}
