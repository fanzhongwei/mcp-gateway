package com.mmyf.commons.configuration;

import org.redisson.Redisson;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisOperations;

/**
 * package com.mmyf.commons.configuration
 * description: 自定义Redisson配置，根据system.redis.enabled配置判断是否启用Redisson
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-03-23 21:44:37
 */
@Configuration
@AutoConfiguration(
        before = {RedisAutoConfiguration.class}
)
@ConditionalOnClass({Redisson.class, RedisOperations.class})
@EnableConfigurationProperties({RedissonProperties.class, RedisProperties.class})
@ConditionalOnProperty(value = "system.redis.enabled", havingValue = "true")
public class RedissonConfiguration extends RedissonAutoConfiguration {

}
