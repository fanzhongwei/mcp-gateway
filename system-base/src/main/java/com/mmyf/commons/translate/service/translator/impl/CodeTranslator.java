package com.mmyf.commons.translate.service.translator.impl;

import com.mmyf.commons.service.code.CodeCache;
import com.mmyf.commons.translate.annotation.TranslateCode;
import com.mmyf.commons.translate.model.TranslationParameter;
import com.mmyf.commons.translate.service.translator.ITranslator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * package com.mmyf.commons.translate.service.translator.impl
 * description: 码值翻译
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-01-14 22:56:10
 */
@Component
public class CodeTranslator implements ITranslator<TranslateCode> {

    @Autowired
    private CodeCache codeCache;

    @Override
    public String translate(TranslationParameter translationParameter) {
        Object fieldValue = translationParameter.getFieldValue();
        if (fieldValue == null) {
            return "";
        }
        TranslateCode annotation = (TranslateCode)translationParameter.getAnnotation();
        return StringUtils.defaultIfBlank(codeCache.getCodeName(annotation.codeType(), fieldValue.toString()), fieldValue.toString());
    }
}
