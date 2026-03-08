package com.mmyf.commons.service.code;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.mmyf.commons.mapper.code.CodeMapper;
import com.mmyf.commons.mapper.code.CodeTypeMapper;
import com.mmyf.commons.model.entity.code.Code;
import com.mmyf.commons.model.entity.code.CodeType;
import com.mmyf.commons.util.config.ConfigUtils;
import com.mmyf.commons.util.lock.LockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 代码缓存
 *
 * @author mmyf
 * @date 2024/01/09 14:24
 **/
@Service
@Slf4j
public class CodeCache {

    @Resource
    private CodeMapper codeMapper;

    @Resource
    private CodeTypeMapper codeTypeMapper;

    /**
     * 代码类型缓存
     */
    @CreateCache(name = "codeTypeCache.", cacheType = CacheType.BOTH, localExpire = 5, timeUnit = TimeUnit.MINUTES)
    @CachePenetrationProtect
    private Cache<String, CodeCacheModel> codeTypeCache;

    @PostConstruct
    public void init() {
        // 主节点才加载缓存
        if (!ConfigUtils.isMasterSystem()) {
            return;
        }

        loadCache();
    }

    private void loadCache() {
        List<CodeType> codeTypeList = codeTypeMapper.selectList(Wrappers.emptyWrapper());
        log.info("单值代码缓存加载--查询到【{}】个代码类型", codeTypeList.size());
        Map<String, CodeCacheModel> codeModelMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(codeTypeList)) {
            codeTypeList.forEach(type -> {
                CodeCacheModel codeCacheModel = new CodeCacheModel();
                codeCacheModel.setCodeType(type);
                codeModelMap.put(type.getId(), codeCacheModel);
            });
        } else {
            // 代码类型为空，则不加载缓存
            return;
        }

        List<Code> codeList = codeMapper.selectList(Wrappers.<Code>lambdaQuery().orderByAsc(Code::getPid).orderByAsc(Code::getNOrder));
        if (CollectionUtils.isEmpty(codeList)) {
            // 代码为空，则不加载缓存
            return;
        }
        codeList.forEach(code -> {
            CodeCacheModel codeCacheModel = codeModelMap.get(code.getPid());
            if (null == codeCacheModel) {
                log.warn("代码类型【{}】不存在，不加载码值【{}】", code.getPid(), code.getPid());
                return;
            }
            codeCacheModel.getCodeMap().put(code.getCode(), code);
            codeCacheModel.getCodeNameMap().put(code.getName(), code);
        });
        log.info("单值代码缓存加载--缓存模型组装完成");
        // codeCacheModel加载到缓存中
        try {
            // 多个节点不能同时加载
            LockUtils.lock("CodeCacheInit");
            log.info("单值代码缓存加载--缓存加载开始");
            codeTypeCache.putAll(codeModelMap);
            log.info("单值代码缓存加载--缓存加载完成");
        } finally {
            LockUtils.unlock("CodeCacheInit");
        }
    }

    public void reload() {
        loadCache();
    }

    /**
     * 获取代码类型
     *
     * @param codeType 代码类型
     * @return com.mmyf.commons.model.entity.code.CodeType
     * @date 2024/3/8 下午2:10
     **/
    public CodeType getCodeType(String codeType) {
        CodeCacheModel codeCacheModel = codeTypeCache.computeIfAbsent(codeType, this::loadFromDb);
        if (null == codeCacheModel) {
            return null;
        }
        return codeCacheModel.getCodeType();
    }

    /**
     * 根据代码类型获取代码集合
     * 
     * @param codeType 代码类型
     * @return java.util.Collection<com.mmyf.commons.model.entity.code.Code>
     * @author mmyf
     * @date 2024/1/9 下午3:04     
     **/
    public Collection<Code> getCodeList(String codeType) {
        CodeCacheModel codeCacheModel = codeTypeCache.computeIfAbsent(codeType, this::loadFromDb);
        if (null == codeCacheModel) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(codeCacheModel.getCodeMap().values());
    }

    /**
     * 根据代码类型和码值获取码值名称
     *
     * @param codeType 代码类型
     * @param code 码值
     * @return 代码名称
     */
    public String getCodeName(String codeType, String code) {
        CodeCacheModel codeCacheModel = codeTypeCache.computeIfAbsent(codeType, this::loadFromDb);
        if (null == codeCacheModel) {
            return null;
        }
        return Optional.ofNullable(codeCacheModel.getCodeMap().get(code))
                .map(Code::getName)
                .orElse(null);
    }

    /**
     * 根据代码类型和码值名称获取码值
     *
     * @param codeType 代码类型
     * @param name 码值名称
     * @return 代码值
     */
    public String getCodeByName(String codeType, String name) {
        CodeCacheModel codeCacheModel = codeTypeCache.computeIfAbsent(codeType, this::loadFromDb);
        if (null == codeCacheModel) {
            return null;
        }
        return Optional.ofNullable(codeCacheModel.getCodeNameMap().get(name))
                .map(Code::getCode)
                .orElse(null);
    }

    private CodeCacheModel loadFromDb(String codeType) {
        CodeCacheModel codeCacheModel = new CodeCacheModel();
        codeCacheModel.setCodeType(codeTypeMapper.selectById(codeType));
        List<Code> codeList = codeMapper.selectList(Wrappers.<Code>lambdaQuery().eq(Code::getPid, codeType));
        codeList.forEach(code -> {
            codeCacheModel.getCodeMap().put(code.getCode(), code);
            codeCacheModel.getCodeNameMap().put(code.getName(), code);
        });
        return codeCacheModel;
    }
}
