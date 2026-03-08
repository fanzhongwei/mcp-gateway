package com.mmyf.commons.translate.annotation;

import com.mmyf.commons.model.entity.IEntity;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 根据ID批量查询数据库进行翻译，适用于经常变化的业务数据
 *
 * @author teddy
 * @date 2024/04/30 18:26
 **/
@Documented
@Retention(RUNTIME)
@Target({FIELD})
public @interface TranslateBatchById {

    /**
     * 需要查询的实体class
     */
    Class<? extends IEntity> entityClass();

    /** 需要查询的字段 */
    String columnName();
}
