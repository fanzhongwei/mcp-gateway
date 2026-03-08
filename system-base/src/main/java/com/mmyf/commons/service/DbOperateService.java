package com.mmyf.commons.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.collect.Lists;
import com.mmyf.commons.constant.SystemBaseConstant;
import com.mmyf.commons.exception.ResourceCreatedFailedException;
import com.mmyf.commons.exception.ResourceReadFailedException;
import com.mmyf.commons.exception.ResourceUpdateFailedException;
import com.mmyf.commons.util.SysUtils;
import com.mmyf.commons.util.lang.PropertyExtUtils;
import com.mmyf.commons.model.entity.IEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * package com.mmyf.commons.service
 * description: 数据库基础操作
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 17:42:30
 */
@Service
public class DbOperateService {
    private Log log = LogFactory.getLog(getClass());

    @Autowired
    private List<BaseMapper> mapperList;

    private Map<Class<? extends BaseMapper>, BaseMapper> mapperMap;

    private Map<Class, BaseMapper> entityMapperMap;

    /**
     * 初始化所有的mapper
     */
    @PostConstruct
    public void init() {
        mapperMap = new HashMap<>(mapperList.size());
        entityMapperMap = new HashMap<>(mapperList.size());
        mapperList.forEach(
                baseMapper -> {
                    Class mapperInterface = (Class) PropertyExtUtils.getProperty(Proxy.getInvocationHandler(baseMapper), "mapperInterface");
                    mapperMap.put(mapperInterface, baseMapper);
                    Class entityClass = getEntityClass(mapperInterface);
                    if (null != entityClass) {
                        entityMapperMap.put(entityClass, baseMapper);
                    }
                });
        mapperMap = Collections.unmodifiableMap(mapperMap);
        entityMapperMap = Collections.unmodifiableMap(entityMapperMap);
    }

    private Class getEntityClass(Class mapperClass) {
        // 获取父类的Type对象
        Type[] genericInterfaces = mapperClass.getGenericInterfaces();

        if (genericInterfaces == null || genericInterfaces.length <= 0) {
            return null;
        }

        Type baseMapperType = genericInterfaces[0];
        if (baseMapperType instanceof ParameterizedType) {
            // 将Type对象转换为ParameterizedType对象
            ParameterizedType parameterizedType = (ParameterizedType) baseMapperType;

            // 获取所有的泛型参数列表
            Type[] actualTypes = parameterizedType.getActualTypeArguments();
            if (actualTypes != null && actualTypes.length > 0) {
                return (Class) actualTypes[0];
            }
        }
        return null;
    }

    /**
     * 插入一个对象
     *
     * @param record *DO
     * @return boolean
     */
    public void insert(Object record) {
        boolean success = getMapperByEntityClass(record.getClass()).insert(record) == 1 ? true : false;
        if (!success) {
            throw new ResourceCreatedFailedException(record.getClass());
        }
    }

    /**
     * 批量插入多条记录
     *
     * @param recordList DO对象
     * @return 是否插入成功
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(Collection<? extends IEntity> recordList) {
        if (CollectionUtils.isEmpty(recordList)) {
            return;
        }
        Map<Class<? extends IEntity>, List<IEntity>> recordMap = groupDo(recordList);

        //调用相应mapper的方法
        recordMap.forEach((clazz, list) -> {
            batchInsert(getMapperByEntityClass(clazz), list);
        });
    }

    /**
     * 批量插入
     *
     * @param mapper     BaseMapper
     * @param recordList 数据
     * @param <T>        数据类型
     */
    private <T> void batchInsert(BaseMapper mapper, List<T> recordList) {
        if (CollectionUtils.isNotEmpty(recordList)) {
            Class<? extends BaseMapper> mapperClass = (Class) PropertyExtUtils.getProperty(Proxy.getInvocationHandler(mapper), "mapperInterface");
            String sqlStatement = SqlHelper.getSqlStatement(mapperClass, SqlMethod.INSERT_ONE);
            SqlHelper.executeBatch(recordList.get(0).getClass(), this.log, recordList, SystemBaseConstant.BATCH_LIMIT,
                    (sqlSession, entity) -> sqlSession.insert(sqlStatement, entity));
        }
    }

    /**
     * 批量插入多条记录
     *
     * @param recordList DO对象
     * @return 是否插入成功
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateByPrimaryKeySelective(Collection<? extends IEntity> recordList) {
        if (CollectionUtils.isEmpty(recordList)) {
            return;
        }
        Map<Class<? extends IEntity>, List<IEntity>> recordMap = groupDo(recordList);

        //调用相应mapper的方法
        recordMap.forEach((clazz, list) -> {
            batchUpdateByPrimaryKeySelective(getMapperByEntityClass(clazz), list);
        });
    }

    /**
     * 批量更新
     *
     * @param mapper     BaseMapper
     * @param recordList 数据
     * @param <T>        数据类型
     */
    private <T> void batchUpdateByPrimaryKeySelective(BaseMapper mapper, List<T> recordList) {
        if (CollectionUtils.isNotEmpty(recordList)) {
            Class<? extends BaseMapper> mapperClass = (Class) PropertyExtUtils.getProperty(Proxy.getInvocationHandler(mapper), "mapperInterface");
            String sqlStatement = SqlHelper.getSqlStatement(mapperClass, SqlMethod.UPDATE_BY_ID);
            SqlHelper.executeBatch(recordList.get(0).getClass(), this.log, recordList, SystemBaseConstant.BATCH_LIMIT, (sqlSession, entity) -> {
                MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
                param.put(Constants.ENTITY, entity);
                sqlSession.update(sqlStatement, param);
            });
        }
    }

    /**
     * 更新一条记录
     *
     * @param record DO对象
     * @return 是否插入成功
     */
    public void updateByPrimaryKeySelective(Object record) {
        boolean success = getMapperByEntityClass(record.getClass()).updateById(record) == 1 ? true : false;
        if (!success) {
            throw new ResourceUpdateFailedException(record.getClass());
        }
    }

    /**
     * 全量更新记录，慎用<br/>
     * 使用不当可能导致数据丢失
     *
     * @param recordList 全量更新记录，慎用
     * @return void
     * @author Teddy
     * @date 2023/4/28 下午2:30
     **/
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateByPrimaryKey(Collection<? extends IEntity> recordList) {
        if (CollectionUtils.isEmpty(recordList)) {
            return;
        }
        Map<Class<? extends IEntity>, List<IEntity>> recordMap = groupDo(recordList);

        //调用相应mapper的方法

        recordMap.forEach((clazz, list) -> {
            batchUpdateByPrimaryKey(getMapperByEntityClass(clazz), list);
        });
    }

    /**
     * 全量更新记录，慎用
     *
     * @param mapper     BaseMapper
     * @param recordList 数据 全量更新记录，慎用
     * @param <T>        数据类型
     */
    private <T> void batchUpdateByPrimaryKey(BaseMapper mapper, List<IEntity> recordList) {
        IEntity firstEntity = recordList.get(0);
        TableInfo tableInfo = TableInfoHelper.getTableInfo(firstEntity.getClass());
        if (StringUtils.isBlank(tableInfo.getKeyColumn())) {
            throw new ResourceUpdateFailedException("实体{}不存在主键，请检查", firstEntity.getClass().getName());
        }

        List<List<IEntity>> lists = Lists.partition(recordList, SystemBaseConstant.BATCH_LIMIT);
        if (CollectionUtils.isNotEmpty(lists)) {
            lists.forEach(list -> {
                Class<? extends BaseMapper> mapperClass = (Class) PropertyExtUtils.getProperty(Proxy.getInvocationHandler(mapper), "mapperInterface");
                String sqlStatement = SqlHelper.getSqlStatement(mapperClass, SqlMethod.UPDATE);
                SqlHelper.executeBatch(firstEntity.getClass(), this.log, recordList, SystemBaseConstant.BATCH_LIMIT, (sqlSession, entity) -> {
                    MapperMethod.ParamMap param = new MapperMethod.ParamMap<>();
                    UpdateWrapper updateWrapper = new UpdateWrapper();
                    List<TableFieldInfo> fieldList = tableInfo.getFieldList();
                    fieldList.forEach(field -> {
                        updateWrapper.set(field.getColumn(), PropertyExtUtils.getProperty(entity, field.getProperty()));
                    });
                    updateWrapper.eq(tableInfo.getKeyColumn(), entity.getId());
                    param.put(Constants.ENTITY, null);
                    param.put(Constants.WRAPPER, updateWrapper);
                    sqlSession.update(sqlStatement, param);
                });
            });
        }
    }


    /**
     * 删除一条记录
     *
     * @param record DO对象
     * @return 是否插入成功
     */
    public void delete(Object record) {
        getMapperByEntityClass(record.getClass()).deleteById(record);
    }

    /**
     * 批量删除多条记录
     *
     * @param recordList DO对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Collection<? extends IEntity> recordList) {
        if (CollectionUtils.isEmpty(recordList)) {
            return;
        }
        Map<Class<? extends IEntity>, List<IEntity>> recordMap = groupDo(recordList);

        //调用相应mapper的方法

        recordMap.forEach((clazz, list) -> {
            List<String> idList = list.stream()
                    .map(entity -> (String)entity.getId())
                    .collect(toList());
            batchDelete(getMapperByEntityClass(clazz), idList);
        });
    }

    /**
     * 批量更新
     *
     * @param mapper       BaseMapper
     * @param recordIdList 数据
     */
    private void batchDelete(BaseMapper mapper, List<String> recordIdList) {
        List<List<String>> lists = Lists.partition(recordIdList, SystemBaseConstant.BATCH_LIMIT);
        if (CollectionUtils.isNotEmpty(lists)) {
            lists.forEach(list -> {
                mapper.deleteBatchIds(list);
            });
        }
    }

    public Map<Class<? extends IEntity>, List<IEntity>> groupDo(Iterable<? extends IEntity> recordList) {
        Map<Class<? extends IEntity>, List<IEntity>> recordMap = new HashMap<>();
        //为不同的mapper分组
        for (IEntity record : recordList) {
            Class clazz = record.getClass();
            List<IEntity> objectList = recordMap.computeIfAbsent(clazz, k -> new ArrayList<>());
            objectList.add(record);
        }
        return recordMap;
    }

    /**
     * 根据DO.class获取mapper
     *
     * @param clazz DO.class
     * @return BaseMapper
     */
    public BaseMapper getMapperByEntityClass(Class<?> clazz) {
        if (!entityMapperMap.containsKey(clazz)) {
            throw new IllegalArgumentException(SysUtils.stringFormat("未找到实体类【】对应的Mapper", clazz));
        }
        return entityMapperMap.get(clazz);
    }

    /**
     * 根据Mapper.class获取mapper
     *
     * @param clazz Mapper.class
     * @return BaseMapper
     */
    public BaseMapper getMapperByMapperClass(Class<?> clazz) {
        return mapperMap.get(clazz);
    }

    @FunctionalInterface
    public interface DbOperateFunction{
        /**
         * 执行数据库操作
         */
        void execute();
    }

    /**
     * 多步数据操作执行方法，不要在service内部加一个调用加@Transactional的方法，这样会导致事务加不上
     * <p>
     * Spring之所以可以对开启@Transactional的方法进行事务管理，是因为Spring为当前类生成了一个代理类，
     * 然后在执行相关方法时，会判断这个方法有没有@Transactional注解，如果有的话，则会开启一个事务。
     *
     * @param function DbOperateFunction
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeInTransaction(DbOperateFunction function) {
        function.execute();
    }
}
