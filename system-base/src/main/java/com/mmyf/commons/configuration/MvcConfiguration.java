package com.mmyf.commons.configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.mmyf.commons.translate.deserialzer.LocalDateTimeSerialzer;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mmyf.commons.util.lang.PropertyExtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * package com.mmyf.configuration
 * description: mmyf
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-14 23:08:07
 */
@Configuration
@Slf4j
public class MvcConfiguration extends WebMvcConfigurationSupport {

    private static volatile FastJsonHttpMessageConverter GLOBAL_FASTJSON_CONVERTER;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/*", "/", "").addResourceLocations("classpath:/static/index.html");
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/swagger-ui/swagger-initializer.js").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/swagger-ui/**").addResourceLocations("classpath:META-INF/resources/webjars/swagger-ui/5.21.0");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:META-INF/resources/webjars/swagger-ui/5.21.0");
        super.addResourceHandlers(registry);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/mobile").setViewName("mobile/index");
    }

    @Bean
    public InternalResourceViewResolver viewResolver() {

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/static/");
        viewResolver.setSuffix(".html");

        return viewResolver;
    }

    /**
     * 配置fastjson方式一：(map集合)保留非实体构造方法的null，构造方法中的null格式化为——""
     * 配置fastjson方式二：在SpringBoot启动类中(@SpringBootApplication)，注入Bean: HttpMessageConverters
     * (list集合+单个对象实例) 中的null，均能够格式为——""
     *
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ByteArrayHttpMessageConverter byteArrayConverter = getByteArrayHttpMessageConverter();
        converters.add(byteArrayConverter);

        FastJsonHttpMessageConverter converter = getGlobalFastJsonHttpMessageConverter();
        //添加到会话中
        converters.add(converter);
    }

    @NotNull
    private static ByteArrayHttpMessageConverter getByteArrayHttpMessageConverter() {
        final ByteArrayHttpMessageConverter byteArrayConverter = new ByteArrayHttpMessageConverter();
        final List<MediaType> list = new ArrayList<>();
        list.add(MediaType.IMAGE_JPEG);
        list.add(MediaType.IMAGE_PNG);
        list.add(MediaType.APPLICATION_OCTET_STREAM);
        list.add(MediaType.APPLICATION_JSON);
        byteArrayConverter.setSupportedMediaTypes(list);
        return byteArrayConverter;
    }

    public static FastJsonHttpMessageConverter getGlobalFastJsonHttpMessageConverter() {
        if (null == GLOBAL_FASTJSON_CONVERTER) {
            synchronized (MvcConfiguration.class) {
                if (null == GLOBAL_FASTJSON_CONVERTER) {
                    GLOBAL_FASTJSON_CONVERTER = getFastJsonHttpMessageConverter();
                }
            }
        }
        return GLOBAL_FASTJSON_CONVERTER;
    }

    public static SerializeConfig getGlobalFastJsonSerializeConfig() {
        return getGlobalFastJsonHttpMessageConverter().getFastJsonConfig()
                                                      .getSerializeConfig();
    }

    @NotNull
    private static FastJsonHttpMessageConverter getFastJsonHttpMessageConverter() {
        //创建会话消息实例容器
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        //创建fastJson配置实例
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
                //全局修改日期格式,如果时间是data、时间戳类型，按照这种格式初始化时间 "yyyy-MM-dd HH:mm:ss"
                SerializerFeature.WriteDateUseDateFormat,
                // 保留 Map 空的字段
                SerializerFeature.WriteMapNullValue,
                // 将 String 类型的 null 转成""
                SerializerFeature.WriteNullStringAsEmpty,
                // 将 List 类型的 null 转成 []
                SerializerFeature.WriteNullListAsEmpty,
                // 将 Boolean 类型的 null 转成 false
                SerializerFeature.WriteNullBooleanAsFalse,
                // 避免循环引用
                SerializerFeature.DisableCircularReferenceDetect,
                //返回Json数据排版格式
                SerializerFeature.PrettyFormat,
                // 去除循环引用
                SerializerFeature.DisableCircularReferenceDetect,
                // 序列化枚举时使用toString方法
                SerializerFeature.WriteEnumUsingToString
        );

        //按字段名称排序后输出-SerializerFeature.SortField
        JSON.DEFAULT_GENERATE_FEATURE &= ~SerializerFeature.SortField.getMask();
        // 使用基于字段的序列化方式可以避免 getter 方法名与 JSON 字段名不一致的问题，提高代码的可读性和可维护性。
        // 因此没有定义字段，只有getXXX，那么XXX是不会被序列化的
        SerializeConfig serializeConfig = new SerializeConfig(true);
        serializeConfig.put(LocalDateTime.class, new LocalDateTimeSerialzer());
        serializeConfig.put(Date.class, new LocalDateTimeSerialzer());
        SerializeConfig.getGlobalInstance().put(LocalDateTime.class, new LocalDateTimeSerialzer());
        SerializeConfig.getGlobalInstance().put(Date.class, new LocalDateTimeSerialzer());
        config.setSerializeConfig(serializeConfig);


        //设置配置实例
        converter.setFastJsonConfig(config);
        //设置默认编码方式
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        //集合填入媒介类型
        List<MediaType> mediaTypeList = new ArrayList<>();
        // 解决中文乱码问题，相当于在 Controller 上的 @RequestMapping 中加了个属性 produces = "application/json"
        mediaTypeList.add(MediaType.APPLICATION_JSON);
        //设置支持媒介——装载了解决中文乱码参数
        converter.setSupportedMediaTypes(mediaTypeList);
        return converter;
    }
}
