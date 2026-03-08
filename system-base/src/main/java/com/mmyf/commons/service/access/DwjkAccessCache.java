package com.mmyf.commons.service.access;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.mmyf.commons.mapper.access.DwjkAccessMapper;
import com.mmyf.commons.model.entity.access.DwjkAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * dwjk授权缓存
 *
 * @author fanzhongwei
 * @date 2024/02/28 18:04
 **/
@Component
@Slf4j
public class DwjkAccessCache {

    @Resource
    private DwjkAccessMapper dwjkAccessMapper;

    /**
     * 重新加载dwjk授权缓存
     *
     * @return void
     * @author fanzhongwei
     * @date 2024/2/28 下午6:17
     **/
    @CacheInvalidate(name = "DwjkAccess.", key = "#result", multi = true)
    public List<Object> reload(){
        log.info("失效缓存：DwjkAccess.*");
        List<Object> accessTokenList = dwjkAccessMapper.selectObjs(Wrappers.<DwjkAccess>lambdaQuery().select(DwjkAccess::getAccessToken));
        return accessTokenList;
    }

    /**
     * 更新授权缓存
     *
     * @param dwjkAccess 授权
     * @return void
     * @author fanzhongwei
     * @date 2024/2/28 下午6:19
     **/
    @CacheInvalidate(name = "DwjkAccess.", key = "#dwjkAccess.accessToken")
    public void update(DwjkAccess dwjkAccess) {
        log.info("失效缓存：DwjkAccess.{}", dwjkAccess.getAccessToken());
    }

    /**
     * 删除授权缓存
     *
     * @param accessToken 授权token
     * @return void
     * @author fanzhongwei
     * @date 2024/2/28 下午6:21
     **/
    @CacheInvalidate(name = "DwjkAccess.", key = "#accessToken")
    public void delete(String accessToken) {
        log.info("失效缓存：DwjkAccess.{}", accessToken);
    }

    /**
     * 获取系统授权，两级缓存：本地(5分钟) + redis（不失效，如果启用）
     *
     * @param accessToken 系统授权token
     * @return com.mmyf.commons.model.entity.access.DwjkAccess
     * @author fanzhongwei
     * @date 2024/2/28 下午6:38
     **/
    @Cached(name = "DwjkAccess.", key = "#accessToken", cacheType = CacheType.BOTH, localExpire = 5, timeUnit = TimeUnit.MINUTES)
    @CachePenetrationProtect
    public DwjkAccess getAccess(String accessToken) {
        DwjkAccess dwjkAccess = dwjkAccessMapper.selectOne(Wrappers.<DwjkAccess>lambdaQuery().eq(DwjkAccess::getAccessToken, accessToken));
        return dwjkAccess;
    }
}
