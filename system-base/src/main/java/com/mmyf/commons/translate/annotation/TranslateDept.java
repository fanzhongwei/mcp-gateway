package com.mmyf.commons.translate.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * package com.mmyf.commons.translate.annotation <br/>
 * description: Dept翻译标注 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Documented
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface TranslateDept {

}
