package com.mmyf.commons.validator.impl;

import com.mmyf.commons.service.code.CodeCache;
import com.mmyf.commons.validator.CodeValidate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Array;
import java.util.*;

/**
 * 自定义码值校验器
 *
 * @author fanzhongwei
 **/
public class CodeValidator implements ConstraintValidator<CodeValidate, Object> {

    private CodeValidate codeValidate;

    @Autowired
    private CodeCache codeCache;

    @Override
    public void initialize(CodeValidate codeValidate) {
        this.codeValidate = codeValidate;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        // 为空和值不对应该单独分开校验
        if (null == value || ObjectUtils.isEmpty(value)) {
            return true;
        }
        List<String> arr = new ArrayList<>();
        if (value instanceof String) {
            String sValue = value.toString();
            if (StringUtils.isNotBlank(codeValidate.split())) {
                arr.addAll(Arrays.asList(StringUtils.split(sValue, codeValidate.split())));
            } else {
                arr.add(sValue);
            }
        }
        if (value instanceof List) {
            ((List) value).forEach(s -> arr.add(Objects.toString(s, "")));
        }
        if (value instanceof String[]) {
            arr.addAll(Arrays.asList((String[])value));
        }
        if (ArrayUtils.isNotEmpty(codeValidate.range())) {
            for (String s : arr) {
                if (!ArrayUtils.contains(codeValidate.range(), s)) {
                    return false;
                }
            }
        }
        if (StringUtils.isNotBlank(codeValidate.codeType())) {
            for (String s : arr) {
                boolean hasCode = StringUtils.isNotBlank(codeCache.getCodeName(codeValidate.codeType(), s));
                if (!hasCode) {
                    return false;
                }
            }
        }
        if (StringUtils.isNotBlank(codeValidate.codeNameType())) {
            for (String s : arr) {
                boolean hasCode = StringUtils.isNotBlank(codeCache.getCodeByName(codeValidate.codeNameType(), s));
                if (!hasCode) {
                    return false;
                }
            }
        }
        return true;
    }
}
