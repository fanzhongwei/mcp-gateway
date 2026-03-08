package com.mmyf.commons.translate.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.*;
import com.alibaba.fastjson.util.FieldInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmyf.commons.configuration.MvcConfiguration;
import com.mmyf.commons.constant.AbstractEnum;
import com.mmyf.commons.translate.annotation.DeepTranslate;
import com.mmyf.commons.translate.annotation.TranslateToString;
import com.mmyf.commons.translate.model.TranslationParameter;
import com.mmyf.commons.translate.service.translator.ITranslator;
import com.mmyf.commons.translate.service.translator.impl.BatchIdTranslator;
import com.mmyf.commons.util.json.OrderedJSON;
import com.mmyf.commons.util.lang.ClassUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;

import com.mmyf.commons.util.lang.PropertyExtUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * package com.mmyf.commons.translate.service <br/>
 * description: 数据翻译 <br/>
 *
 * @author Teddy
 * @date 2022/5/17
 */
@Service
@Slf4j
public class DataTranslateService {

    /**
     * 存储翻译器bean实例
     */
    @Autowired(required = false)
    private List<ITranslator> registeredTranslators;

    @Autowired
    private BatchIdTranslator batchIdTranslator;

    public static final String TranslatePropSuffix = "TranslateText";

    /**
     * key为该翻译器对应注解类型，value为翻译器实例
     */
    private Map<Class, ITranslator> translatorMap = new HashMap<>(16);

    @PostConstruct
    private void init() {
        if (CollectionUtils.isNotEmpty(registeredTranslators)) {
            registeredTranslators.forEach(e -> translatorMap.put(((Class) (((ParameterizedType) e.getClass()
                    .getGenericInterfaces()[0]).getActualTypeArguments()[0])), e));
        }
    }

    /**
     * 转换值
     *
     * @param obj 任意对象
     * @return 翻译后的对象
     */
    public Object translateValue(Object obj) {
        try {
            // 如果是分页对象,则只把其中的数据对象翻译
            if (obj instanceof Page) {
                Page page = (Page) obj;
                Collection<?> data = (Collection<?>) page.getRecords();
                page.setRecords(translateCollection(data));
                return page;
            }
            // 列表的话,转发至handleCollection
            if (obj instanceof Collection<?>) {
                return translateCollection((Collection<?>) obj);
            }

            if (obj instanceof Object[]) {
                return translateArray((Object[]) obj);
            }

            // Model 转发到handleOne
            return translateObject(obj);
        } finally {
            batchIdTranslator.fillBatchTranslate();
        }
    }

    /**
     * 转换集合
     *
     * @param collection 集合对象
     * @return 翻译后的集合对象
     */
    private List translateCollection(Collection<?> collection) {
        return collection.stream().map(this::translateObject).collect(Collectors.toList());
    }

    /**
     * 转换数组
     *
     * @param collection 数组对象
     * @return 翻译后的集合对象
     */
    private Object translateArray(Object[] collection) {
        return Arrays.stream(collection).map(this::translateObject).collect(Collectors.toList());
    }

    /**
     * 转换单个对象
     *
     * @param obj 单个对象
     * @return 翻译后的单个对象
     */
    private Object translateObject(Object obj) {
        if (!isParseableObject(obj)) {
            return obj;
        }
        final boolean deep = obj.getClass().getAnnotation(DeepTranslate.class) != null;
        final Object rawResult = OrderedJSON.toJSON(obj, MvcConfiguration.getGlobalFastJsonSerializeConfig());

        if (!(rawResult instanceof JSONObject)) {
            return obj;
        }
        final JSONObject result = (JSONObject) rawResult;
        Deque<ValueWrapper> allStack = new ArrayDeque<>();
        allStack.push(new ValueWrapper(null, obj, result, ParseType.OBJECT, obj));
        while (!allStack.isEmpty()) {
            ValueWrapper wrapper = allStack.removeFirst();
            try {
                switch (wrapper.getParseType()) {
                    case OBJECT:
                        translateObject(wrapper, allStack);
                        break;
                    case COLLECTION_FIELD:
                        translateCollectionField(wrapper, allStack, deep);
                        break;
                    case OBJECT_FIELD:
                        translateObjectField(wrapper, allStack, deep);
                        break;
                }
            } catch (Exception e) {
                log.error("翻译出错！字段：{};值：{}。", wrapper.getField(), wrapper.getValue(), e);
            }
        }

        return result;
    }

    private void translateObjectField(ValueWrapper valueWrapper, Deque<ValueWrapper> allStatck, boolean deep) {
        Field field = valueWrapper.getField();
        Object value = valueWrapper.getValue();
        JSONObject result = valueWrapper.getResult();
        final String fieldName = field.getName();
        if (value instanceof AbstractEnum) {
            // 替换枚举翻译的tostring
            AbstractEnum e = (AbstractEnum) value;
            result.put(fieldName, e.getCode());
            // 添加翻译值
            result.put(fieldName + TranslatePropSuffix, e.getDesc());
        }

        Annotation translatorAnnotation = getTranslatorAnnotation(field);
        if (translatorAnnotation != null) {
            ITranslator translator = translatorMap.get(translatorAnnotation.annotationType());
            String translateText = translator.translate(new TranslationParameter(translatorAnnotation, value, valueWrapper.getObj(), field, result));
            result.put(fieldName + TranslatePropSuffix, translateText);
        } else {
            if (deep && isParseableObject(value)) {
                Object subResult = OrderedJSON.toJSON(value, MvcConfiguration.getGlobalFastJsonSerializeConfig());
                if (subResult instanceof JSONObject) {
                    result.put(fieldName, subResult);
                    allStatck.addFirst(new ValueWrapper(null, value, (JSONObject) subResult, ParseType.OBJECT, null));
                }
            }
        }
    }

    private void translateCollectionField(ValueWrapper valueWrapper, Deque<ValueWrapper> allStatck, boolean deep) {
        Field field = valueWrapper.getField();
        Object value = valueWrapper.getValue();
        JSONObject result = valueWrapper.getResult();
        final String fieldName = field.getName();

        TranslateToString translateToStringAnnotation = field.getAnnotation(TranslateToString.class);
        Collector collector = Objects.nonNull(translateToStringAnnotation) ?
                Collectors.joining(translateToStringAnnotation.separator()) : Collectors.toList();
        if (isEnum(field)) {
            // 翻译枚举值
            Object valueList = compatibleStream(value).map(e -> ((AbstractEnum) e).getCode()).collect(collector);
            result.put(fieldName, valueList);
            // 翻译枚举名称
            Object textList = compatibleStream(value).map(e -> ((AbstractEnum) e).getDesc()).collect(collector);
            result.put(fieldName + TranslatePropSuffix, textList);
        }

        Annotation translatorAnnotation = getTranslatorAnnotation(field);
        if (translatorAnnotation != null) {
            ITranslator translator = translatorMap.get(translatorAnnotation.annotationType());
            Object resultList =
                    compatibleStream(value).map(e -> translator.translate(new TranslationParameter(translatorAnnotation, e, valueWrapper.getObj(), field, result)))
                            .collect(collector);
            result.put(fieldName + TranslatePropSuffix, resultList);
        } else {
            if (deep && !(value instanceof JSONArray)) {
                final Object collFirstData = getCollectionFirstData(value);// 根据集合中的第一个数据来判断集合中存储的数据类型
                if (!isParseableObject(collFirstData)) {
                    return;
                }
                JSONArray array = new JSONArray();
                compatibleStream(value).forEach(obj -> {
                    Object subResult = OrderedJSON.toJSON(obj, MvcConfiguration.getGlobalFastJsonSerializeConfig());
                    if (subResult instanceof JSONObject) {
                        array.add(subResult);
                        allStatck.addFirst(new ValueWrapper(null, obj, (JSONObject) subResult, ParseType.OBJECT, null));
                    }
                });
                result.put(fieldName, array);
            }
        }
    }

    private Stream<?> compatibleStream(Object value) {
        Stream<?> stream = null;
        if (value instanceof Collection) {
            stream = ((Collection) value).stream();
        } else if (value.getClass().isArray()) {
            stream = Arrays.stream((Object[]) value);
        } else {
            stream = ((JSONArray) OrderedJSON.toJSON(value, MvcConfiguration.getGlobalFastJsonSerializeConfig())).stream();
        }
        return stream;
    }

    private Object getCollectionFirstData(Object coll) {
        // 集合有两种情况：Collection和Array，且handleObject中只对这两种类型设置ParseType.COLLECTION_FIELD进而调用到此方法
        if (coll instanceof Collection<?>) {
            return !((Collection<?>) coll).isEmpty() ? ((Collection<?>) coll).iterator().next() : null;
        } else if (coll.getClass().isArray()) {
            return Array.getLength(coll) > 0 ? Array.get(coll, 0) : null;
        }
        return null;
    }

    private Annotation getTranslatorAnnotation(Field field) {
        // 判断翻译注解
        for (Class c : translatorMap.keySet()) {
            Annotation translatorAnnotation = field.getAnnotation(c);
            if (Objects.nonNull(translatorAnnotation)) {
                return translatorAnnotation;
            }
        }
        return null;
    }

    /**
     * 判断是否是AbstractEnum，AbstractEnum也是支持翻译的
     *
     * @param field Field
     * @return 是否是AbstractEnum
     */
    private boolean isEnum(Field field) {
        Class<?> type = field.getType();
        if (type.isArray()) {
            type = type.getComponentType();
        }
        return type.isEnum() && AbstractEnum.class.isAssignableFrom(type);
    }

    private void translateObject(ValueWrapper valueWrapper, Deque<ValueWrapper> allStatck) {
        Object obj = valueWrapper.getValue();
        try {
            final Field[] fields = ClassUtil.getModelFields(obj.getClass());
            for (final Field field : fields) {
                final Object fieldValue = ClassUtil.getModelFieldValue(field, obj);
                if (fieldValue == null) {
                    // 如果为空也进行翻译，不然前端不知道哪些字段有翻译值
                    allStatck.addFirst(new ValueWrapper(field, fieldValue, valueWrapper.getResult(),
                            ParseType.OBJECT_FIELD, obj));
                    continue;
                }
                boolean isStrMultiVal = fieldValue instanceof String && fieldValue.toString().contains(";");
                boolean isStrMultiVal2 = fieldValue instanceof String && fieldValue.toString().contains("[");
                if (fieldValue instanceof Collection<?> || fieldValue.getClass().isArray() || isStrMultiVal || isStrMultiVal2) {
                    Object value;
                    if (isStrMultiVal2) {
                        value = ((String) fieldValue).replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]",
                                "").replaceAll(" ", "").split(",");
                    } else if (isStrMultiVal) {
                        value = ((String) fieldValue).replaceAll(" ", "").split(";");
                    } else {
                        value = fieldValue;
                    }
                    allStatck.addFirst(new ValueWrapper(field, value, valueWrapper.getResult(),
                            ParseType.COLLECTION_FIELD, obj));
                } else {
                    allStatck.addFirst(new ValueWrapper(field, fieldValue, valueWrapper.getResult(),
                            ParseType.OBJECT_FIELD, obj));
                }
            }
        } catch (Exception e) {
            log.error("处理代码或者租户翻译出错！" + obj.getClass(), e);
        }
    }

    private boolean isParseableObject(Object obj) {
        return obj != null
                && !isPrimitiveObject(obj.getClass())
                && !(obj instanceof JSON)
                && !(obj instanceof Map || obj instanceof Collection)
                && !(obj instanceof IPage);
    }

    /**
     * Class<?>是否是基础类型
     *
     * @param clazz Class<?>
     * @return 是否是基础类型
     */
    private boolean isPrimitiveObject(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == Boolean.class
                || clazz == Character.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class
                || clazz == BigInteger.class
                || clazz == BigDecimal.class
                || clazz == String.class
                || clazz == java.util.Date.class
                || clazz == java.sql.Date.class
                || clazz == java.sql.Time.class
                || clazz == java.sql.Timestamp.class
                || clazz == java.time.LocalDate.class
                || clazz == java.time.LocalTime.class
                || clazz == java.time.LocalDateTime.class;
    }

    enum ParseType {
        OBJECT, OBJECT_FIELD, COLLECTION_FIELD
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ValueWrapper {

        Field field;

        Object value;

        JSONObject result;

        ParseType parseType;

        Object obj;

    }

}
