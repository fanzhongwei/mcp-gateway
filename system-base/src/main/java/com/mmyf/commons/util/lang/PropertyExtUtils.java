package com.mmyf.commons.util.lang;

import com.mmyf.commons.exception.OperationNotAllowedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Property ext utils.
 *
 * @author Teddy
 * @Description: 属性工具
 * @date 2018 -5-18 11:21
 */
@Slf4j
public class PropertyExtUtils extends org.apache.commons.beanutils.PropertyUtils {

    /**
     * 设置bean的属性，解决PropertyUtils无法处理lombok的链式问题
     *
     * @param bean  the bean
     * @param name  the name
     * @param value the value
     */
    public static void setProperty(Object bean, String name, Object value) {
        try {
            Field field = getField(bean.getClass(), name);
            field.setAccessible(true);
            field.set(bean, value);
        } catch (Exception e) {
            throw new OperationNotAllowedException("设置" + bean + "的属性：" + name + "失败", e);
        }
    }

    /**
     * 获取Field
     *
     * @param clazz     类
     * @param fieldName 属性名
     * @return Field
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        Assert.notNull(clazz, "Class required");
        Assert.hasText(fieldName, "Field name required");

        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException var3) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            } else {
                throw new IllegalStateException("Could not locate field '" + fieldName + "' on class " + clazz);
            }
        }
    }

    /**
     * Get property object.
     *
     * @param bean the bean
     * @param name the name
     * @return the object
     */
    public static Object getProperty(Object bean, String name) {
        try {
            Field field = getField(bean.getClass(), name);
            field.setAccessible(true);
            return field.get(bean);
        } catch (Exception e) {
            log.error("获取{}的属性：{}失败", bean, name, e);
            throw new OperationNotAllowedException("获取" + bean + "的属性：" + name + "失败", e);
        }
    }

    /**
     * 判断class中是否有某个属性
     *
     * @param clazz Class
     * @param name  String
     * @return boolean boolean
     */
    public static boolean hasProperty(Class<?> clazz, String name) {
        try {
            clazz.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * 拷贝对象属性
     *
     * @param source     源bean
     * @param targetType bean类型
     * @param <T>        bean泛型
     * @return 新bean
     */
    public static <T> T copy(Object source, Class<T> targetType) {
        Object target = BeanUtils.instantiateClass(targetType);
        BeanUtils.copyProperties(source, target);
        return (T)target;
    }

    /**
     * 拷贝对象属性
     *
     * @param sourceList 源bean
     * @param targetType bean类型
     * @param <T>        bean泛型
     * @return 新bean
     */
    public static <T> List<T> copy(List<?> sourceList, Class<T> targetType) {
        List<T> collect = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(sourceList)) {
            for (Object n : sourceList) {
                collect.add(copy(n, targetType));
            }
        }
        return collect;
    }
}
