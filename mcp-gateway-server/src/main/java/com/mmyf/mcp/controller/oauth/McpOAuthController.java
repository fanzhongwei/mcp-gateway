package com.mmyf.mcp.controller.oauth;

import com.mmyf.mcp.config.auth.McpPublishedAuthProperties;
import com.mmyf.mcp.config.auth.McpPublishedAuthProperties.OAuthClient;
import com.mmyf.mcp.service.auth.McpOAuthJwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 内置 OAuth2 client_credentials 最小端点与 JWKS。
 */
@RestController
@RequestMapping("/oauth")
@Tag(name = "内置 OAuth", description = "Client Credentials 与 JWKS")
@RequiredArgsConstructor
@Slf4j
public class McpOAuthController {

    private final McpPublishedAuthProperties authProperties;
    private final McpOAuthJwtService oauthJwtService;

    @Operation(summary = "获取访问令牌", description = "grant_type=client_credentials，application/x-www-form-urlencoded")
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret) {

        if (!authProperties.getOauthServer().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OAuth token endpoint disabled");
        }
        if (!"client_credentials".equals(grantType)) {
            return oauthError(HttpStatus.BAD_REQUEST, "unsupported_grant_type", "grant_type must be client_credentials");
        }
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            return oauthError(HttpStatus.UNAUTHORIZED, "invalid_client", "client_id and client_secret required");
        }
        Optional<OAuthClient> client = oauthJwtService.authenticateClient(clientId, clientSecret);
        if (client.isEmpty()) {
            log.warn("mcp oauth token rejected invalid_client clientId={}", clientId);
            return oauthError(HttpStatus.UNAUTHORIZED, "invalid_client", "invalid client credentials");
        }
        String accessToken = oauthJwtService.issueAccessToken(client.get());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", accessToken);
        body.put("token_type", "Bearer");
        body.put("expires_in", authProperties.getOauthServer().getAccessTokenTtlSeconds());
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "JWKS 公钥")
    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        if (!authProperties.getOauthServer().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "JWKS endpoint disabled");
        }
        return oauthJwtService.jwksDocument();
    }

    private static ResponseEntity<Map<String, Object>> oauthError(HttpStatus status, String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.status(status).body(body);
    }
}
