package com.mmyf.commons.translate.service.translator.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.service.DbOperateService;
import com.mmyf.commons.translate.model.TranslationParameter;
import com.mmyf.commons.translate.service.DataTranslateService;
import com.mmyf.commons.translate.service.translator.ITranslator;
import com.mmyf.commons.translate.annotation.TranslateBatchById;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据ID批量翻译
 *
 * @date 2024/04/30 18:27
 **/
@Component
@Slf4j
public class BatchIdTranslator implements ITranslator<TranslateBatchById> {

    @Autowired
    private DbOperateService dbOperateService;

    private ThreadLocal<Map<TranslateBatchById, List<TranslationParameter>>> batchTranslateParams = new ThreadLocal<>();

    /**
     * 获取翻译内容
     *
     * @param translationParameter 翻译用到的参数
     * @return 翻译值
     */
    @Override
    public String translate(TranslationParameter translationParameter) {
        Map<TranslateBatchById, List<TranslationParameter>> paramsMap = batchTranslateParams.get();
        if (null == paramsMap) {
            paramsMap = Maps.newHashMap();
            batchTranslateParams.set(paramsMap);
        }
        paramsMap.computeIfAbsent((TranslateBatchById) translationParameter.getAnnotation(), k -> new ArrayList<>()).add(translationParameter);
        return "";
    }

    /**
     * 填充所有的批量翻译字段
     */
    public void fillBatchTranslate() {
        try {
            // 填充当前线程的批量翻译字段
            Map<TranslateBatchById, List<TranslationParameter>> paramsMap = batchTranslateParams.get();
            if (MapUtils.isEmpty(paramsMap)) {
                return;
            }
            paramsMap.forEach(this::fill);
        } finally {
            // 清理当前线程的批量翻译参数信息
            batchTranslateParams.remove();
        }
    }

    private void fill(TranslateBatchById translateBatchById, List<TranslationParameter> translationParameters) {
        try {
            if (CollectionUtils.isEmpty(translationParameters)) {
                return;
            }
            Class<? extends IEntity> entityClass = translateBatchById.entityClass();
            TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
            if (null == tableInfo) {
                log.error("批量翻译配置实体【{}】错误，无法翻译", entityClass);
                return;
            }
            BaseMapper mapper = dbOperateService.getMapperByEntityClass(entityClass);
            Set<Object> keyValues = translationParameters.stream()
                    .filter(p -> null != p.getFieldValue())
                    .map(TranslationParameter::getFieldValue).collect(Collectors.toSet());
            String keyColumn = tableInfo.getKeyColumn();
            QueryWrapper<Object> queryWrapper = Wrappers.query().select(keyColumn, translateBatchById.columnName()).in(keyColumn, keyValues);
            List<Map<String, Object>> translateValueList = mapper.selectMaps(queryWrapper);
            if (CollectionUtils.isEmpty(translateValueList)) {
                return;
            }
            Map<Object, Object> translateValueMap = translateValueList.stream().collect(Collectors.toMap(map -> MapUtils.getObject(map, keyColumn), map -> MapUtils.getObject(map, translateBatchById.columnName())));
            translationParameters.forEach(param -> {
                param.getResult().put(param.getField().getName() + DataTranslateService.TranslatePropSuffix, translateValueMap.get(param.getFieldValue()));
            });
        } catch (Exception e) {
            log.error("批量翻译{}失败", translateBatchById, e);
        }
    }
}
