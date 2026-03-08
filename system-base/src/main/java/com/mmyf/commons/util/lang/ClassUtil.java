package com.mmyf.commons.util.lang;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * package com.mmyf.commons.util.lang <br/>
 * description: ClassUtil <br/>
 *
 * @author Teddy
 * @date 2022/5/18
 */
@UtilityClass
public class ClassUtil {

    /**
     * @description 获得pojo中以及父类中的字段
     * @param modelClass
     * @deprecated 因命名问题，推荐使用getModelFields，暂保留兼容
     * @return
     */
    public static Field[] getModelField(Class<?> modelClass) {
        return getModelFields(modelClass);
    }

    /**
     * ClassUtil
     *
     * @param modelClass
     * @return
     * @description 获得pojo中以及父类中的字段
     * @author Teddy
     * @date 2018年7月19日 下午3:34:31
     */
    public static Field[] getModelFields(Class<?> modelClass) {
        List<Field> allFields = new ArrayList<>();
        Class<?> cls = modelClass;
        do {
            Field[] fields = getModelFieldsInCache(cls);
            allFields.addAll(Arrays.asList(fields));
            cls = cls.getSuperclass();
        } while (cls != null && !Object.class.equals(cls));
        return allFields.toArray(new Field[] {});
    }

    private static final Map<Class<?>, Field[]> modelFieldsCache = new ConcurrentReferenceHashMap<>(1024);
    /**
     * @param modelClass
     * @return
     */
    private static Field[] getModelFieldsInCache(Class<?> modelClass) {
        Field[] fields = modelFieldsCache.get(modelClass);
        if (fields == null) {
            fields = modelClass.getDeclaredFields();
            fields = Arrays.stream(fields).filter(f -> !f.isSynthetic()).filter((f) -> {
                return !"serialVersionUID".equals(f.getName());
            }).toArray(Field[]::new);
            modelFieldsCache.put(modelClass, fields);
        }
        return fields;
    }

    /**
     * @description 在pojo中以及父类中查找字段
     * @param modelClass
     * @param fieldName
     * @return
     */
    public static Field getModelField(Class<?> modelClass, String fieldName) {
        Field field = null;
        Class<?> cls = modelClass;
        do {
            Field[] fields = getModelFieldsInCache(cls);
            field = Arrays.stream(fields).filter(f -> f.getName().equals(fieldName)).findAny().orElse(null);
            if (field != null) {
                break;
            }
            cls = cls.getSuperclass();
        } while (cls != null && !Object.class.equals(cls));
        return field;
    }

    /**
     * 指定字段名一次获取多个字段
     * @param modelClass
     * @param fieldNames
     * @return
     */
    public static Field[] getModelFields(Class<?> modelClass, String[] fieldNames) {
        Field[] fields = getModelFields(modelClass);
        fields = Arrays.stream(fields).filter(f -> {
            return fieldNames == null || fieldNames.length == 0 || Arrays.binarySearch(fieldNames, f.getName()) >= 0;
        }).toArray(Field[]::new);
        return fields;
    }

    /**
     * 获得model中的字段值
     * @param field
     * @param model
     * @return
     */
    public static Object getModelFieldValue(Field field, Object model){
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(model);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 指定属性范围，将pojo类的一些字段转成数据库表的列名
     * @param modelClass pojo类
     * @param props 属性名称数组，对应pojo类的field的名称
     * @author Teddy
     * @return
     */
    public static String[] getModelColumns(Class<?> modelClass, String[] props) {
        Field[] fields = getModelFields(modelClass);
        return Arrays.stream(fields).filter((f)->{
            return props == null || props.length == 0 || Arrays.binarySearch(props, f.getName()) >= 0;
        }).map(ClassUtil::getColumnName).toArray(String[]::new);
    }

    /**
     * 获得pojo类一个字段对应的数据库表的列名
     * @param field
     * @return
     */
    public static String getColumnName(Field field) {
        TableField column = field.getAnnotation(TableField.class);
        if (column != null) {
            return column.value();
        } else {
            return field.getName();
        }
    }

    /**
     * 获得pojo类对应的表名
     * @param modelClass
     * @return
     */
    public static String getTableName(Class<?> modelClass) {
        TableName table = modelClass.getAnnotation(TableName.class);
        if (table != null) {
            return table.value();
        }
        return modelClass.getName();
    }

    /**
     * @param name
     * @return
     */
    public static boolean hasClass(String name) {
        try {
            Class<?> clazz = ClassUtils.forName(name, ClassUtil.class.getClassLoader());
            return clazz != null;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
