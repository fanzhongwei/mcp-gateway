package com.mmyf.commons.translate.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * package com.mmyf.commons.translate.model <br/>
 * description: 封装翻译用到的参数 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranslationParameter {

    /**
     * 翻译注解
     */
    private Annotation annotation;

    /**
     * 字段值
     */
    private Object fieldValue;

    /**
     * 翻译字段所在对象
     */
    private Object obj;

    /** 翻译字段定义 */
    private Field field;

    /** 翻译结果对象 */
    private JSONObject result;

}
