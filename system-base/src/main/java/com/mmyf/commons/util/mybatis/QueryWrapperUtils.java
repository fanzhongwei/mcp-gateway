package com.mmyf.commons.util.mybatis;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.util.SysUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * QueryWrapper 查询条件处理
 *
 * @date 2024/3/5 下午6:17
 **/
@UtilityClass
@Slf4j
public class QueryWrapperUtils {

    /**
     * 添加排序字段到warpper中，不支持自定义sql
     *
     * @param pageParam  分页参数
     * @param wrapper 查询参数
     * @param entityClass 查询实体类
     * @param tableAlias 实体类表别名
     * @return void
     * @date 2024/3/5 下午6:27
     **/
    public static <T> void appendOrderItem(PageParam pageParam, QueryWrapper<T> wrapper, Class<T> entityClass, String tableAlias) {
        appendOrderItemToWrapper(pageParam, wrapper, entityClass, tableAlias);
    }

    /**
     * 添加排序字段到warpper中，不支持自定义sql
     *
     * @param pageParam  分页参数
     * @param wrapper 查询参数
     * @param entityClass 查询实体类
     * @return void
     * @date 2024/3/5 下午6:27
     **/
    public static <T> void appendOrderItem(PageParam pageParam, QueryWrapper<T> wrapper, Class<T> entityClass) {
        appendOrderItemToWrapper(pageParam, wrapper, entityClass, null);
    }

    private static <T> void appendOrderItemToWrapper(PageParam pageParam, QueryWrapper<T> wrapper, Class<T> entityClass, String tableAlias) {
        List<OrderItem> orders = pageParam.getPage().getOrders();
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        Map<String, String> fieldColumnMap = getEntityPropertyColumnMap(entityClass);
        orders.forEach(orderItem -> {
            String sqlColumn = fieldColumnMap.get(orderItem.getColumn());
            Assert.hasText(sqlColumn, SysUtils.stringFormat("字段{}不支持排序", orderItem.getColumn()));
            if (StringUtils.isNotBlank(tableAlias)) {
                sqlColumn = tableAlias + "." + sqlColumn;
            }
            if (orderItem.isAsc()) {
                wrapper.orderByAsc(sqlColumn);
            } else {
                wrapper.orderByDesc(sqlColumn);
            }
        });
        // 然后清空page对象中的排序字段
        pageParam.getPage().setOrders(Collections.emptyList());
    }

    /**
     * 添加查询条件到warpper中
     * @param entityParam 查询参数
     * @param wrapper 查询条件
     * @param entityClass 查询实体类
     * @param tableAlias 查询实体类别名
     * @param <T> 查询实体类
     */
    public static <T> void appendEntityParamToWrapper(Object entityParam, QueryWrapper<T> wrapper, Class<T> entityClass, String tableAlias) {
        Map<String, String> fieldColumnMap = getEntityPropertyColumnMap(entityClass);
        fieldColumnMap.forEach((property, sqlColumn) -> {
            Object fieldValue = ReflectUtil.getFieldValue(entityParam, property);
            if (StringUtils.isNotBlank(tableAlias)) {
                sqlColumn = tableAlias + "." + sqlColumn;
            }
            wrapper.eq(null != fieldValue, sqlColumn, fieldValue);
        });
    }

    @NotNull
    private static <T> Map<String, String> getEntityPropertyColumnMap(Class<T> entityClass) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        Map<String, String> fieldColumnMap = new LinkedHashMap<>();
        if (StringUtils.isNoneBlank(tableInfo.getKeyColumn(), tableInfo.getKeyProperty())) {
            fieldColumnMap.put(tableInfo.getKeyProperty(), tableInfo.getKeyColumn());
        }
        tableInfo.getFieldList().forEach(field -> fieldColumnMap.put(field.getProperty(), field.getColumn()));
        return fieldColumnMap;
    }

    /**
     * 根据实体类获取哪些实体属性支持排序
     * @param entityClass 实体类
     * @param <T> entity
     * @return 支持排序的实体类
     */
    public <T> Set<String> entityAllowOrderColumns(Class<T> entityClass) {
        return getEntityPropertyColumnMap(entityClass).keySet();
    }

    /**
     * 根据VO类获取哪些实体属性支持排序，仅支持sql查询字段都设置别名与VO字段一一对应
     *
     * @param voClass VO Class
     * @param <T> VO
     * @return
     */
    public <T> Set<String> voAllowOrderColumns(Class<T> voClass) {
        Field[] declaredFields = voClass.getDeclaredFields();
        return Stream.of(declaredFields).filter(field -> field.getAnnotation(Schema.class) != null)
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
