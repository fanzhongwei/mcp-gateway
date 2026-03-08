package com.mmyf.commons.util.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * package com.mmyf.commons.util.redis
 * description: Redis 工具类
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-03-24 11:25:51
 */
@Slf4j
public class ConfigUtils {

    private static Environment ENVIRONMENT;

    public ConfigUtils(Environment environment) {
        ENVIRONMENT = environment;
    }

    /**
     * 是否为主节点
     */
    public static boolean isMasterSystem() {
        return BooleanUtils.isTrue(ENVIRONMENT.getProperty("system.master", Boolean.class));
    }

    /**
     * 是否启用redis
     */
    public static boolean isRedisEnabled() {
        return BooleanUtils.isTrue(ENVIRONMENT.getProperty("system.redis.enabled", Boolean.class));
    }
}
