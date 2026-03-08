package com.mmyf.commons.translate.service.translator.impl;

import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.service.organ.OrganService;
import com.mmyf.commons.translate.annotation.TranslateUser;
import com.mmyf.commons.translate.model.TranslationParameter;
import com.mmyf.commons.translate.service.translator.ITranslator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * package com.mmyf.commons.translate.service.translator.impl <br/>
 * description: User翻译 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Slf4j
@Component
public class UserTranslator implements ITranslator<TranslateUser> {

    @Autowired
    private OrganService organService;

    @Override
    public String translate(TranslationParameter translationParameter) {
        Object fieldValue = translationParameter.getFieldValue();
        if (fieldValue == null) {
            return "";
        }
        return getUserName(fieldValue.toString());
    }

    private String getUserName(String value) {
        if (value == null) {
            return "";
        }
        User user = organService.getUser(value);
        return user == null ? value : user.getName();
    }

}
