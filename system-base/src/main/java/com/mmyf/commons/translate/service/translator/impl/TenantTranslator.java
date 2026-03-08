package com.mmyf.commons.translate.service.translator.impl;

import com.mmyf.commons.model.entity.organ.Tenant;
import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.translate.annotation.TranslateTenant;
import com.mmyf.commons.translate.model.TranslationParameter;
import com.mmyf.commons.translate.service.translator.ITranslator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * package com.mmyf.commons.translate.service.translator.impl <br/>
 * description: Tenant翻译 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Component
@Slf4j
public class TenantTranslator implements ITranslator<TranslateTenant> {

    @Autowired
    private OrganService organService;

    @Override
    public String translate(TranslationParameter translationParameter) {
        Object fieldValue = translationParameter.getFieldValue();
        if (fieldValue == null) {
            return "";
        }
        return getTenantName(fieldValue.toString());
    }

    private String getTenantName(String value) {
        if (value == null) {
            return "";
        }
        Tenant tenant = organService.getTenant(value);
        return tenant == null ? value : tenant.getName();
    }

}
