package com.mmyf.commons.validator;


import com.mmyf.commons.validator.impl.CodeValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import java.lang.annotation.*;

/**
 * 自定义码值校验，目前支持int、String的码值校验<br/>
 *
 * 优先级：range | rangeStr > codeType<br/>
 * 校验逻辑详见: {@link CodeValidator#isValid}<br/>
 *
 * 校验示例：<br/>
 * <li> @CodeValidate(codeType="10100001", message="单值代码错误,请检查")<br/>
 * <li> @CodeValidate(rangeStr={"1","2"}, message="单值代码错误,请检查")<br/>
 *
 *
 * @author fanzhongwei
 **/
@Documented
@Constraint(
        validatedBy = {CodeValidator.class}
)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@ReportAsSingleViolation
public @interface CodeValidate {

    String message();

    /** 校验代码值 */
    String codeType() default "";

    /** 校验代码名称 */
    String codeNameType() default "";


    String[] range() default {};

    /** String类型的码值校验，可能会有传多个的情况，如果需要分割后校验单个码值是否正确，可以配置该参数 */
    String split() default "";


    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        CodeValidate[] value();
    }
}
