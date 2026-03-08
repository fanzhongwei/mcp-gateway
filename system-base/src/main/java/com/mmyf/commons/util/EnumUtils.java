package com.mmyf.commons.util;

import com.google.common.collect.Maps;
import com.mmyf.commons.constant.AbstractEnum;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;

import java.util.*;

/**
 * 枚举工具类
 *
 * @date 2024/03/06 14:43
 **/
@UtilityClass
public class EnumUtils {

    private static final Map<Class<? extends AbstractEnum>, Map<String, AbstractEnum>> CODE_ENUM_MAP;
    static {
        Map<Class<? extends AbstractEnum>, Map<String, AbstractEnum>> codeEnumMap = Maps.newHashMap();
        Set<Class<? extends AbstractEnum>> enumSet = new Reflections("com.mmyf").getSubTypesOf(AbstractEnum.class);
        enumSet.forEach(enumClass -> {
            AbstractEnum[] enumConstants = enumClass.getEnumConstants();
            Map<String, AbstractEnum> enumMap = Maps.newHashMap();
            Arrays.stream(enumConstants).forEach(value -> {
                enumMap.put(value.getCode(), value);
            });
            codeEnumMap.put(enumClass, Collections.unmodifiableMap(enumMap));
        });
        CODE_ENUM_MAP = Collections.unmodifiableMap(codeEnumMap);
    }

    /**
     * 根据码值获取枚举对象
     *
     * @param enumClass AbstractEnum子类
     * @param code 码值
     * @return com.mmyf.commons.constant.AbstractEnum
     * @date 2024/3/6 下午2:57
     **/
    public static <T extends AbstractEnum> T getEnumByCode(Class<T> enumClass, String code) {
        return (T) CODE_ENUM_MAP.get(enumClass).get(code);
    }

    /**
     * 获取实现了指定接口的枚举类型
     *
     * @param interfaceClass 接口
     * @return java.util.List<java.lang.Class<? extends java.lang.Enum<?>>>
     * @date 2024/3/6 下午2:38
     **/
    public static List<Class<? extends Enum<?>>> getEnumsImplementingInterface(Class<?> interfaceClass) {
        List<Class<? extends Enum<?>>> result = new ArrayList<>();
        for (Class<?> enumClass : interfaceClass.getDeclaringClass().getDeclaredClasses()) {
            if (Enum.class.isAssignableFrom(enumClass)) {
                for (Class<?> interfaceType : enumClass.getInterfaces()) {
                    if (interfaceType.equals(interfaceClass)) {
                        result.add((Class<? extends Enum<?>>) enumClass);
                        break;
                    }
                }
            }
        }
        return result;
    }
}
