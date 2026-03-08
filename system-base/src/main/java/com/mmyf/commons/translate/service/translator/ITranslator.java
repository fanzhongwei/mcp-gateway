package com.mmyf.commons.translate.service.translator;

import com.mmyf.commons.translate.model.TranslationParameter;
import java.lang.annotation.Annotation;

/**
 * package com.mmyf.commons.translate.service <br/>
 * description: 翻译器接口 <br/>
 *
 * @param <T> 翻译的注解类型
 * @author Teddy
 * @date 2022/5/17
 */
public interface ITranslator<T extends Annotation> {

    /**
     * 获取翻译内容
     *
     * @param translationParameter 翻译用到的参数
     * @return 翻译值
     */
    String translate(TranslationParameter translationParameter);

}
