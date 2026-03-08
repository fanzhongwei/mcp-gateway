package com.mmyf.commons.util.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.*;
import com.alibaba.fastjson.util.FieldInfo;
import com.alibaba.fastjson.util.TypeUtils;
import com.mmyf.commons.util.lang.PropertyExtUtils;

import java.lang.reflect.Array;
import java.util.*;

/**
 * package com.mmyf.commons.util.json
 * description: 有序JSON
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-03-17 13:20:34
 */
public class OrderedJSON extends JSON {

    /**
     * 获取有序JSON
     * @param javaObject java对象
     * @param config 序列化配置
     * @return 有序JSON
     */
    public static Object toJSON(Object javaObject, SerializeConfig config) {
        if (javaObject == null) {
            return null;
        }

        if (javaObject instanceof JSON) {
            return javaObject;
        }

        if (javaObject instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) javaObject;

            int size = map.size();

            Map innerMap;
            if (map instanceof LinkedHashMap) {
                innerMap = new LinkedHashMap(size);
            } else if (map instanceof TreeMap) {
                innerMap = new TreeMap();
            } else {
                innerMap = new HashMap(size);
            }

            JSONObject json = new JSONObject(innerMap);

            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object key = entry.getKey();
                String jsonKey = TypeUtils.castToString(key);
                Object jsonValue = toJSON(entry.getValue(), config);
                json.put(jsonKey, jsonValue);
            }

            return json;
        }

        if (javaObject instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) javaObject;

            JSONArray array = new JSONArray(collection.size());

            for (Object item : collection) {
                Object jsonValue = toJSON(item, config);
                array.add(jsonValue);
            }

            return array;
        }

        if (javaObject instanceof JSONSerializable) {
            String json = JSON.toJSONString(javaObject);
            return JSON.parse(json);
        }

        Class<?> clazz = javaObject.getClass();

        if (clazz.isEnum()) {
            return ((Enum<?>) javaObject).name();
        }

        if (clazz.isArray()) {
            int len = Array.getLength(javaObject);

            JSONArray array = new JSONArray(len);

            for (int i = 0; i < len; ++i) {
                Object item = Array.get(javaObject, i);
                Object jsonValue = toJSON(item, config);
                array.add(jsonValue);
            }

            return array;
        }

        if (ParserConfig.isPrimitive2(clazz)) {
            return javaObject;
        }

        ObjectSerializer serializer = config.getObjectWriter(clazz);
        if (serializer instanceof JavaBeanSerializer) {
            JavaBeanSerializer javaBeanSerializer = (JavaBeanSerializer) serializer;

            JSONObject json = new JSONObject(true);
            try {
                Map<String, Object> values = getFieldValuesMap(javaObject, javaBeanSerializer);
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    json.put(entry.getKey(), toJSON(entry.getValue(), config));
                }
            } catch (Exception e) {
                throw new JSONException("toJSON error", e);
            }
            return json;
        }

        String text = JSON.toJSONString(javaObject, config);
        return JSON.parse(text);
    }

    private static Map<String, Object> getFieldValuesMap(Object javaObject, JavaBeanSerializer javaBeanSerializer) throws Exception {
        FieldSerializer[] getters = (FieldSerializer[]) PropertyExtUtils.getProperty(javaBeanSerializer, "getters");

        Map<String, Object> map = new LinkedHashMap<String, Object>(getters.length);
        boolean skipTransient = true;
        FieldInfo fieldInfo = null;

        for (FieldSerializer getter : getters) {
            skipTransient = SerializerFeature.isEnabled((Integer) PropertyExtUtils.getProperty(getter, "features"), SerializerFeature.SkipTransientField);
            fieldInfo = getter.fieldInfo;

            if (skipTransient && fieldInfo != null && fieldInfo.fieldTransient) {
                continue;
            }

            if (getter.fieldInfo.unwrapped) {
                Object unwrappedValue = getter.getPropertyValue(javaObject);
                Object map1 = JSON.toJSON(unwrappedValue);
                if (map1 instanceof Map) {
                    map.putAll((Map) map1);
                } else {
                    map.put(getter.fieldInfo.name, getter.getPropertyValue(javaObject));
                }
            } else {
                map.put(getter.fieldInfo.name, getter.getPropertyValue(javaObject));
            }
        }

        return map;
    }
}
