package com.mmyf.commons.util.mybatis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * mybatis plus 动态更新：接口传入key为null表示为清空，没传key表示不更新
 *
 * @date 2024/04/15 11:18
 **/
@UtilityClass
@Slf4j
public class DynamicUpdateUtils {

    private static final String DEFAULT_KEY = "root";

    /**
     * 存放http put接口传入的需要update的json key
     *
     * @date 2024/4/15 上午11:21
     **/
    private static final ThreadLocal<Map<String, Set<String>>> updateFieldMap = new InheritableThreadLocal<>();

    /**
     * 清理动态更新记录的字段信息
     *
     * @return void
     * @date 2024/4/15 上午11:36
     **/
    public static void clearUpdateKeys() {
        updateFieldMap.remove();
    }

    /**
     * 判断传入的属性是否需要强制更新，接口传入key为null表示为清空，没传key表示不更新
     *
     * @param property 更新的属性
     * @return boolean
     * @date 2024/4/15 下午2:11
     **/
    public static boolean needForceUpdateToNull(String property) {
        Map<String, Set<String>> fieldMap = DynamicUpdateUtils.updateFieldMap.get();
        if (MapUtils.isEmpty(fieldMap)) {
            return false;
        }
        Set<String> fieldSet = fieldMap.get("root");
        if (null == fieldSet) {
            return false;
        }
        return fieldSet.contains(property);
    }

    /**
     * 设置PUT请求要更新的数据，从中解析key
     *
     * @param data 要更新的数据
     * @return void
     * @date 2024/4/15 上午11:49
     **/
    public static void setUpdateData(String data) {
        if (StringUtils.isBlank(data)) {
            return;
        }
        Object dataObject = JSON.parse(data);
        // 解析前端传入的key
        Map<String, Set<String>> fieldMap = Maps.newHashMap();
        if (dataObject instanceof JSONArray) {
            JSONArray array = (JSONArray)dataObject;
            Object obj = array.get(0);
            if (obj instanceof JSONObject) {
                int index = 0;
                for (Object item : array.toArray()) {
                    fieldMap.putAll(getObjectKey(DEFAULT_KEY + "-" + index, (JSONObject) item));
                    index++;
                }
            }
        } else {
            Set<String> fields = new HashSet<>();
            ((JSONObject)dataObject).forEach((key, value) -> {
                if (value instanceof JSONObject) {
                    fieldMap.putAll(getObjectKey(key, (JSONObject) value));
                } else if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    // 集合为空设置key到root上
                    if (array.isEmpty()) {
                        fields.add(key);
                    } else {
                        Object obj = array.get(0);
                        if (obj instanceof JSONObject) {
                            int index = 0;
                            for (Object item : array.toArray()) {
                                fieldMap.putAll(getObjectKey(key + "-" + index, (JSONObject) item));
                                index++;
                            }
                        } else {
                            // 集合元素不是Object的设置key到root上
                            fields.add(key);
                        }
                    }
                } else {
                    fields.add(key);
                }
            });
            fieldMap.put(DEFAULT_KEY, fields);
        }
        DynamicUpdateUtils.updateFieldMap.set(fieldMap);
    }

    private Map<String, Set<String>> getObjectKey(String parentKey, JSONObject jsonObject) {
        Map<String, Set<String>> updateFieldMap = Maps.newHashMap();
        Set<String> fields = new HashSet<>();
        jsonObject.forEach((key, value) -> {
            if (value instanceof JSONObject) {
                updateFieldMap.putAll(getObjectKey(parentKey + "." + key, (JSONObject) value));
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                // 集合为空设置key到root上
                if (array.isEmpty()) {
                    fields.add(key);
                } else {
                    Object obj = array.get(0);
                    if (obj instanceof JSONObject) {
                        int index = 0;
                        for (Object item : array.toArray()) {
                            updateFieldMap.putAll(getObjectKey(parentKey + "." + key + "-" + index, (JSONObject) item));
                            index++;
                        }
                    } else {
                        // 集合元素不是Object的设置key到root上
                        fields.add(key);
                    }
                }
            } else {
                fields.add(key);
            }
        });
        updateFieldMap.put(parentKey, fields);
        return updateFieldMap;
    }
}
