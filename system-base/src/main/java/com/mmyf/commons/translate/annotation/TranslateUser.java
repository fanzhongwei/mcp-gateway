package com.mmyf.commons.translate.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * package com.mmyf.commons.translate.annotation <br/>
 * description: User 翻译注解 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Documented
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface TranslateUser {

}
