package com.mmyf.commons.translate.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * package com.mmyf.commons.translate.annotation <br/>
 * description: 将数组或集合翻译成指定分隔符分隔的字符串 <br/>
 *
 * @author Teddy
 * @date 2022/5/18
 */
@Documented
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface TranslateToString {

    /**
     * 分隔符
     *
     * @return 分隔符，默认英文逗号
     */
    String separator() default ",";

}
