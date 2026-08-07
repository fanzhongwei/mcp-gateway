package com.mmyf.mcp.config.auth;

import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.OAuthClient;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpPublishedAuthPropertiesTest {

    @Test
    void oauthOnlyWithoutEnabledShouldFail() {
        McpPublishedAuthProperties props = new McpPublishedAuthProperties();
        props.setMode(McpPublishedAuthProperties.PublishedAuthMode.OAUTH_ONLY);
        assertThrows(IllegalStateException.class, props::validateOrThrow);
    }

    @Test
    void enabledWithoutIssuerShouldFail() {
        McpPublishedAuthProperties props = baseEnabledProps();
        props.getOauthServer().setIssuer("");
        assertThrows(IllegalStateException.class, props::validateOrThrow);
    }

    @Test
    void enabledWithValidConfigShouldPass() {
        McpPublishedAuthProperties props = baseEnabledProps();
        assertDoesNotThrow(props::validateOrThrow);
    }

    private static McpPublishedAuthProperties baseEnabledProps() {
        McpPublishedAuthProperties props = new McpPublishedAuthProperties();
        props.setMode(McpPublishedAuthProperties.PublishedAuthMode.BOTH);
        props.getOauthServer().setEnabled(true);
        props.getOauthServer().setIssuer("https://issuer.test/mcp-gateway");
        props.getOauthServer().setAudience("mcp-gateway");
        props.getOauthServer().setPrivateKeyPem(generatePrivateKeyPem());
        OAuthClient c = new OAuthClient();
        c.setClientId("cid");
        c.setClientSecret("secret");
        c.setServiceIds(List.of("s1"));
        props.getOauthServer().setClients(List.of(c));
        return props;
    }

    private static String generatePrivateKeyPem() {
        try {
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            byte[] pkcs8 = gen.generateKeyPair().getPrivate().getEncoded();
            String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pkcs8);
            return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----\n";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

