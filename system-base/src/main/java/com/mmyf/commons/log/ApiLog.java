package com.mmyf.commons.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * package com.mmyf.commons.log <br/>
 * description: 自定义操作日志注解 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/5/30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    /**
     * 操作模块
     * @return
     */
    String module() default "";

    /**
     * 接口名
     * @return
     */
    String name() default "";
}
