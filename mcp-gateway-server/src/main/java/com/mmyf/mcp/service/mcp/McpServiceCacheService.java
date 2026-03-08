package com.mmyf.mcp.service.mcp;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.mmyf.mcp.model.entity.mcp.McpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * MCP服务缓存管理Service
 * 用于管理McpProtocolService中的validateService方法缓存
 *
 * @author 超级管理员
 * @since 2026-01-29
 */
@Service
@Slf4j
public class McpServiceCacheService {

    /**
     * 清除MCP服务缓存
     * 用于在发布、停止、删除服务时清除McpProtocolService中的缓存
     *
     * @param serviceId 服务ID
     * @param accessToken 访问令牌
     */
    @CacheInvalidate(name = "mcpServiceCache:", key = "#serviceId + ':' + #accessToken")
    public void invalidateCache(String serviceId, String accessToken) {
        if (StringUtils.hasText(serviceId) && StringUtils.hasText(accessToken)) {
            log.debug("已清除MCP服务缓存: {}:{}", serviceId, accessToken);
        }
    }
}
