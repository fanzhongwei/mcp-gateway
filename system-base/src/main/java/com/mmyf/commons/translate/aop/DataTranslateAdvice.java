package com.mmyf.commons.translate.aop;

import com.mmyf.commons.translate.annotation.DataTranslate;
import com.mmyf.commons.translate.service.DataTranslateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * package com.mmyf.commons.translate.service <br/>
 * description: 数据翻译功能 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@ControllerAdvice
public class DataTranslateAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private DataTranslateService dataTranslateService;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 有翻译注解 && 是ajax restController
        return returnType.hasMethodAnnotation(DataTranslate.class)
                && (returnType.hasMethodAnnotation(ResponseBody.class)
                || returnType.getDeclaringClass().getAnnotation(RestController.class) != null);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<?
            extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        return body == null ? null : dataTranslateService.translateValue(body);
    }

}
