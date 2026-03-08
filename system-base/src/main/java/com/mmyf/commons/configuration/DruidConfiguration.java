package com.mmyf.commons.configuration;

import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * package com.mmyf.configuration <br/>
 * description: TODO <br/>
 *
 * @author Teddy
 * @date 2022/5/12
 */
@Configuration
public class DruidConfiguration {

    @Bean(name = "wallFilter")
    @DependsOn("wallConfig")
    public WallFilter wallFilter(WallConfig wallConfig) {
        WallFilter wallFilter = new WallFilter();
        wallFilter.setConfig(wallConfig);
        return wallFilter;
    }

    @Bean(name = "wallConfig")
    public WallConfig wallConfig() {
        WallConfig wallConfig = new WallConfig();
        wallConfig.setMultiStatementAllow(Boolean.TRUE);
        wallConfig.setCommentAllow(Boolean.TRUE);
        wallConfig.setNoneBaseStatementAllow(Boolean.TRUE);
        wallConfig.setStrictSyntaxCheck(Boolean.FALSE);
        return wallConfig;
    }
}
