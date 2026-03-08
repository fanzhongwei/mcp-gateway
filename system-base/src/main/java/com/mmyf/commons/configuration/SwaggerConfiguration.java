package com.mmyf.commons.configuration;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Teddy
 * @since 2021.03.22
 * <p>
 * swagger 配置
 */
@Configuration
@Profile({"dev", "test"})
@Slf4j
public class SwaggerConfiguration {

    private OperationCustomizer authHeaderCustomizer(String authHeaderName, String authHeaderDescription, boolean required) {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .name(authHeaderName)
                    .description(authHeaderDescription)
                    .in("header")
                    .required(required));
            return operation;
        };
    }

    @Bean
    public GroupedOpenApi createSystemApi() {
        return GroupedOpenApi.builder()
                             .group("business")
                             .pathsToMatch("/api/**")
                             .packagesToScan("com.mmyf")
                             .packagesToExclude("com.mmyf.commons")
                             .addOpenApiCustomizer(openApi -> openApi
                                     .info(new Info().title("系统业务接口").version("1.0.0"))
                             )
                             .addOperationCustomizer(authHeaderCustomizer("Authorization", "接口身份认证", false))
                            .build();
    }

    @Bean
    public GroupedOpenApi createWebApi() {
        return GroupedOpenApi.builder()
                             .group("system")
                             .pathsToMatch("/api/**")
                             .packagesToScan("com.mmyf.commons")
                             .addOpenApiCustomizer(openApi -> openApi
                                     .info(new Info().title("系统基础接口").version("1.0.0"))
                             )
                             .addOperationCustomizer(authHeaderCustomizer("Authorization", "接口身份认证", false))
                             .build();
    }

    @Bean
    public GroupedOpenApi createDwjkApi() {
        return GroupedOpenApi.builder()
                             .group("dwjk")
                             .pathsToMatch("/dwjk/api/**")
                             .packagesToScan("com.mmyf")
                             .addOpenApiCustomizer(openApi -> openApi
                                     .info(new Info().title("系统对外接口").version("1.0.0"))
                             )
                             .addOperationCustomizer(authHeaderCustomizer("AccessToken", "对外接口身份认证", true))
                             .build();
    }

    @Bean
    public GroupedOpenApi createMcpProtocolApi() {
        return GroupedOpenApi.builder()
                             .group("mcp-protocol")
                             .pathsToMatch("/mcp/**")
                             .packagesToScan("com.mmyf.mcp.controller.mcp")
                             .addOpenApiCustomizer(openApi -> openApi
                                     .info(new Info().title("MCP协议接口").version("1.0.0")
                                             .description("Model Context Protocol (MCP) 协议接口，支持tools/list和tools/call方法"))
                             )
                             // 不添加OperationCustomizer，因为Controller中已经通过@Parameter注解定义了Authorization参数
                             .build();
    }
}
