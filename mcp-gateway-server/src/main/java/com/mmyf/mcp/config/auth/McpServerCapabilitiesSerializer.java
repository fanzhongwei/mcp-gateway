package com.mmyf.mcp.config.auth;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * 在 initialize 响应的 capabilities 中补充 extensions（OAuth client credentials），
 * 以适配 MCP 扩展发现；仅在配置要求时输出。
 */
public class McpServerCapabilitiesSerializer extends JsonSerializer<McpSchema.ServerCapabilities> {

    private static final String OAUTH_CC_EXT = "io.modelcontextprotocol/oauth-client-credentials";

    private final McpPublishedAuthProperties properties;

    public McpServerCapabilitiesSerializer(McpPublishedAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void serialize(McpSchema.ServerCapabilities value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        writeIfPresent(gen, serializers, "completions", value.completions());
        writeIfPresent(gen, serializers, "experimental", value.experimental());
        writeIfPresent(gen, serializers, "logging", value.logging());
        writeIfPresent(gen, serializers, "prompts", value.prompts());
        writeIfPresent(gen, serializers, "resources", value.resources());
        writeIfPresent(gen, serializers, "tools", value.tools());
        if (properties.shouldAdvertiseOAuthExtension()) {
            gen.writeObjectField("extensions", Map.of(OAUTH_CC_EXT, Collections.emptyMap()));
        }
        gen.writeEndObject();
    }

    private static void writeIfPresent(JsonGenerator gen, SerializerProvider serializers, String field, Object val)
            throws IOException {
        if (val != null) {
            gen.writeFieldName(field);
            serializers.defaultSerializeValue(val, gen);
        }
    }
}
