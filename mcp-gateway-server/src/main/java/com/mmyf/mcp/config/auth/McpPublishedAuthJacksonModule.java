package com.mmyf.mcp.config.auth;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.modelcontextprotocol.spec.McpSchema;

public class McpPublishedAuthJacksonModule extends SimpleModule {

    public McpPublishedAuthJacksonModule(McpPublishedAuthProperties properties) {
        super("mcp-published-auth-capabilities");
        addSerializer(McpSchema.ServerCapabilities.class, new McpServerCapabilitiesSerializer(properties));
    }
}
