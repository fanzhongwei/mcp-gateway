package com.mmyf.commons.translate.service.translator.impl;

import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.translate.annotation.TranslateOrgan;
import com.mmyf.commons.translate.model.TranslationParameter;
import com.mmyf.commons.translate.service.translator.ITranslator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * package com.mmyf.commons.translate.service.translator.impl <br/>
 * description: Tenant翻译 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Component
@Slf4j
public class OrganTranslator implements ITranslator<TranslateOrgan> {

    @Autowired
    private OrganService organService;

    @Resource
    private TenantTranslator tenantTranslator;
    @Resource
    private DeptTranslator deptTranslator;
    @Resource
    private UserTranslator userTranslator;

    @Override
    public String translate(TranslationParameter translationParameter) {
        Object fieldValue = translationParameter.getFieldValue();
        if (fieldValue == null) {
            return "";
        }
        // ID都是唯一的
        // 先查询是不是tenant
        String translateText = tenantTranslator.translate(translationParameter);
        if (!StringUtils.equals(translateText, fieldValue.toString())) {
            return translateText;
        }
        // 再查询是不是dept
        translateText = deptTranslator.translate(translationParameter);
        if (!StringUtils.equals(translateText, fieldValue.toString())) {
            return translateText;
        }
        // 最后再查询是不是user
        return userTranslator.translate(translationParameter);
    }



}
