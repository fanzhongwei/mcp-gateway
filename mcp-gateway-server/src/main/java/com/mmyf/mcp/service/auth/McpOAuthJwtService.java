package com.mmyf.mcp.service.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.OAuthClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.*;

/**
 * 内置 OAuth：签发与校验访问令牌（RS256），以及 JWKS 公钥导出。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpOAuthJwtService {

    public static final String CLAIM_MCP_SERVICES = "mcp_services";

    private final McpPublishedAuthProperties properties;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private Algorithm signingAlgorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void initialize() {
        if (!properties.getOauthServer().isEnabled()) {
            return;
        }
        try {
            this.privateKey = loadPkcs8PrivateKey(properties.getOauthServer().getPrivateKeyPem());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            if (!(privateKey instanceof java.security.interfaces.RSAPrivateCrtKey crt)) {
                throw new IllegalStateException(
                        "RSA 私钥需为含 CRT 系数的 PKCS#8 格式（openssl genrsa 后 pkcs8 导出）");
            }
            this.publicKey = (RSAPublicKey) kf.generatePublic(
                    new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
            this.signingAlgorithm = Algorithm.RSA256(publicKey, privateKey);
            this.verifier = JWT.require(signingAlgorithm)
                    .withIssuer(properties.getOauthServer().getIssuer())
                    .withAudience(properties.getOauthServer().getAudience())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("初始化内置 OAuth 签名密钥失败", e);
        }
    }

    public boolean isOAuthEnabled() {
        return properties.getOauthServer().isEnabled();
    }

    public Optional<OAuthClient> authenticateClient(String clientId, String clientSecret) {
        if (!isOAuthEnabled() || !StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            return Optional.empty();
        }
        return properties.getOauthServer().getClients().stream()
                .filter(c -> clientId.equals(c.getClientId()) && clientSecret.equals(c.getClientSecret()))
                .findFirst();
    }

    public String issueAccessToken(OAuthClient client) {
        if (!isOAuthEnabled()) {
            throw new IllegalStateException("内置 OAuth 未启用");
        }
        Instant now = Instant.now();
        long ttl = properties.getOauthServer().getAccessTokenTtlSeconds();
        Instant exp = now.plusSeconds(ttl);
        List<String> sids = new ArrayList<>(client.getServiceIds());
        return JWT.create()
                .withKeyId(properties.getOauthServer().getKeyId())
                .withIssuer(properties.getOauthServer().getIssuer())
                .withAudience(properties.getOauthServer().getAudience())
                .withSubject(client.getClientId())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .withClaim(CLAIM_MCP_SERVICES, sids)
                .sign(signingAlgorithm);
    }

    /**
     * @return true 表示 JWT 合法且允许访问指定已发布服务
     */
    public boolean isTokenValidForService(String token, String serviceId) {
        if (!isOAuthEnabled() || !StringUtils.hasText(token) || !StringUtils.hasText(serviceId)) {
            return false;
        }
        try {
            DecodedJWT jwt = verifier.verify(token);
            List<String> services = jwt.getClaim(CLAIM_MCP_SERVICES).asList(String.class);
            return services != null && services.contains(serviceId);
        } catch (Exception e) {
            log.debug("OAuth JWT 校验失败 serviceId={}: {}", serviceId, e.getMessage());
            return false;
        }
    }

    public Map<String, Object> jwksDocument() {
        if (!isOAuthEnabled()) {
            return Map.of("keys", List.of());
        }
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("kty", "RSA");
        key.put("kid", properties.getOauthServer().getKeyId());
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("n", base64UrlUnsigned(publicKey.getModulus()));
        key.put("e", base64UrlUnsigned(publicKey.getPublicExponent()));
        return Map.of("keys", List.of(key));
    }

    private static RSAPrivateKey loadPkcs8PrivateKey(String pem) throws Exception {
        String normalized = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static String base64UrlUnsigned(java.math.BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
