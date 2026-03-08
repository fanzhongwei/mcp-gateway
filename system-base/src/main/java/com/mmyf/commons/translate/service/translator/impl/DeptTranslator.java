package com.mmyf.commons.translate.service.translator.impl;

import com.mmyf.commons.model.entity.organ.Dept;
import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.translate.annotation.TranslateDept;
import com.mmyf.commons.translate.model.TranslationParameter;
import com.mmyf.commons.translate.service.translator.ITranslator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * package com.mmyf.commons.translate.service.translator.impl <br/>
 * description: Dept 翻译 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Slf4j
@Component
public class DeptTranslator implements ITranslator<TranslateDept> {

    @Autowired
    private OrganService organService;

    @Override
    public String translate(TranslationParameter translationParameter) {
        Object fieldValue = translationParameter.getFieldValue();
        if (fieldValue == null) {
            return "";
        }
        return getDeptName(fieldValue.toString());
    }

    private String getDeptName(String value) {
        if (value == null) {
            return "";
        }
        Dept dept = organService.getDept(value);
        return dept == null ? value : dept.getName();
    }

}
