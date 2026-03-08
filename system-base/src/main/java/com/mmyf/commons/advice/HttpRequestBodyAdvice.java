package com.mmyf.commons.advice;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.mmyf.commons.util.mybatis.DynamicUpdateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * http请求处理
 *
 * @author fanzhongwei
 * @date 2024/04/03 14:11
 **/
@ControllerAdvice
@Slf4j
public class HttpRequestBodyAdvice extends RequestBodyAdviceAdapter {
    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> converterType) {
        // 只处理Put请求，规范约定Put请求为修改数据
        return FastJsonHttpMessageConverter.class.isAssignableFrom(converterType) && null != methodParameter.getExecutable().getDeclaredAnnotation(PutMapping.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        byte[] body = IOUtils.toByteArray(inputMessage.getBody());

        DynamicUpdateUtils.setUpdateData(new String(body, StandardCharsets.UTF_8));

        return new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage httpInputMessage, MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

}
