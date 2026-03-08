package com.mmyf.commons.translate.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * package com.mmyf.commons.translate.annotation <br/>
 * description: 标识接口返回值需要翻译 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface DataTranslate {

}
