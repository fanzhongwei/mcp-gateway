package com.mmyf.commons.configuration;

import com.mmyf.commons.util.config.ConfigUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * package com.mmyf.commons.configuration
 * description: 系统配置加载类
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-03-24 16:57:09
 */
@Configuration
public class ConfigAutoConfiguration {

    @Autowired
    private Environment environment;


    @Bean
    public ConfigUtils configUtils() {
        return new ConfigUtils(environment);
    }
}
