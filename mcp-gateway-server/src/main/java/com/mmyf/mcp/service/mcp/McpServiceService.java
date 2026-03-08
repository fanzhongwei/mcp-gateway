package com.mmyf.mcp.service.mcp;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmyf.commons.util.uuid.UUIDHelper;
import com.mmyf.mcp.mapper.mcp.McpServiceMapper;
import com.mmyf.mcp.model.dto.mcp.McpApiConfigItem;
import com.mmyf.mcp.model.entity.mcp.McpConfigApi;
import com.mmyf.mcp.model.entity.mcp.McpService;
import com.mmyf.mcp.model.entity.system.Api;
import com.mmyf.mcp.model.entity.system.System;
import com.mmyf.mcp.model.entity.system.SystemEnv;
import com.mmyf.mcp.model.vo.McpServiceDetailVO;
import com.mmyf.mcp.service.mcp.McpConfigApiService;
import com.mmyf.mcp.service.system.ApiService;
import com.mmyf.mcp.service.system.SystemEnvService;
import com.mmyf.mcp.service.system.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * mcp服务 服务实现类
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-26 15:01:45
 */
@Service
@Slf4j
public class McpServiceService extends ServiceImpl<McpServiceMapper, McpService> implements IService<McpService> {

    @Autowired
    private McpConfigApiService mcpConfigApiService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private SystemEnvService systemEnvService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private McpServiceCacheService mcpServiceCacheService;

    /**
     * 保存或更新MCP服务（包含配置API）
     *
     * @param mcpService 服务信息
     * @param selections  配置选择列表
     * @return 保存后的服务ID
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = "mcpServiceCache", key = "#mcpService.id + ':' + #mcpService.accessToken")
    public String saveOrUpdateService(McpService mcpService, List<SelectionData> selections) {
        boolean isUpdate = StringUtils.hasText(mcpService.getId());
        
        if (!isUpdate) {
            // 新增
            if (!StringUtils.hasText(mcpService.getId())) {
                mcpService.setId(UUIDHelper.getUuid());
            }
            if (!StringUtils.hasText(mcpService.getAccessToken())) {
                mcpService.setAccessToken(UUIDHelper.getUuid());
            }
            mcpService.setEndpoint("/mcp/service/" + mcpService.getId() + "/stateless");
            // 计算接口数量
            int totalApiCount = selections.stream()
                                          .filter(s -> s != null && s.getApiIds() != null)
                                          .mapToInt(s -> s.getApiIds().size())
                    .sum();
            mcpService.setApiCount(totalApiCount);
            this.save(mcpService);
        } else {
            // 更新
            // 计算接口数量
            int totalApiCount = selections.stream()
                    .filter(s -> s != null && s.getApiIds() != null)
                    .mapToInt(s -> s.getApiIds().size())
                    .sum();
            mcpService.setApiCount(totalApiCount);
            this.updateById(mcpService);
            
            // 删除旧的配置API
            LambdaQueryWrapper<McpConfigApi> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(McpConfigApi::getMcpServiceId, mcpService.getId());
            mcpConfigApiService.remove(wrapper);
        }
        
        // 保存配置API
        for (SelectionData selection : selections) {
            McpConfigApi configApi = new McpConfigApi();
            configApi.setId(UUIDHelper.getUuid());
            configApi.setMcpServiceId(mcpService.getId());
            configApi.setTenant(mcpService.getTenant());
            configApi.setSystemId(selection.getSystemId());
            configApi.setEnvId(selection.getEnvId());
            // 设置系统级别的自定义MCP名字
            configApi.setSystemMcpName(selection.getCustomMcpName());
            // 构建 apiConfig：包含接口ID和自定义MCP名字的列表
            List<McpApiConfigItem> apiConfigItems =
                    buildApiConfig(selection.getApiIds(), selection.getApiCustomMcpNames());
            configApi.setApiConfig(apiConfigItems);
            mcpConfigApiService.save(configApi);
        }
        
        return mcpService.getId();
    }

    /**
     * 查询服务详情（包含关联的系统、环境、API信息）
     *
     * @param id 服务ID
     * @return 服务详情VO
     */
    public McpServiceDetailVO getServiceDetail(String id) {
        McpService service = this.getById(id);
        if (service == null) {
            return null;
        }
        
        McpServiceDetailVO vo = new McpServiceDetailVO();
        // 复制基本属性
        vo.setId(service.getId());
        vo.setName(service.getName());
        vo.setDesc(service.getDesc());
        vo.setEndpoint(service.getEndpoint());
        vo.setApiCount(service.getApiCount());
        vo.setStatus(service.getStatus());
        vo.setAccessToken(service.getAccessToken());
        vo.setPublishTime(service.getPublishTime());
        vo.setCreateTime(service.getCreateTime());
        vo.setModifyTime(service.getModifyTime());
        
        // 查询配置API列表
        LambdaQueryWrapper<McpConfigApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpConfigApi::getMcpServiceId, id);
        List<McpConfigApi> configApis = mcpConfigApiService.list(wrapper);
        
        // 收集所有需要查询的ID
        Set<String> systemIds = new HashSet<>();
        Set<String> envIds = new HashSet<>();
        Set<String> apiIds = new HashSet<>();
        
        for (McpConfigApi configApi : configApis) {
            if (StringUtils.hasText(configApi.getSystemId())) {
                systemIds.add(configApi.getSystemId());
            }
            if (StringUtils.hasText(configApi.getEnvId())) {
                envIds.add(configApi.getEnvId());
            }
            // 从 apiConfig 读取接口ID
            List<McpApiConfigItem> apiConfigList = safeApiConfigList(configApi);
            for (McpApiConfigItem item : apiConfigList) {
                String apiId = item.getApiId();
                if (StringUtils.hasText(apiId)) {
                    apiIds.add(apiId);
                }
            }
        }
        
        // 批量查询系统、环境、API
        Map<String, System> systemMap = new HashMap<>();
        if (!systemIds.isEmpty()) {
            List<System> systems = systemService.listByIds(systemIds);
            systemMap = systems.stream().collect(Collectors.toMap(System::getId, s -> s));
        }
        
        Map<String, SystemEnv> envMap = new HashMap<>();
        if (!envIds.isEmpty()) {
            List<SystemEnv> envs = systemEnvService.listByIds(envIds);
            envMap = envs.stream().collect(Collectors.toMap(SystemEnv::getId, e -> e));
        }
        
        Map<String, Api> apiMap = new HashMap<>();
        if (!apiIds.isEmpty()) {
            List<Api> apis = apiService.listByIds(apiIds);
            apiMap = apis.stream().collect(Collectors.toMap(Api::getId, a -> a));
        }
        
        // 构建配置API详情列表
        List<McpServiceDetailVO.ConfigApiDetail> configApiDetails = new ArrayList<>();
        for (McpConfigApi configApi : configApis) {
            McpServiceDetailVO.ConfigApiDetail detail = new McpServiceDetailVO.ConfigApiDetail();
            detail.setId(configApi.getId());
            detail.setSystemId(configApi.getSystemId());
            detail.setEnvId(configApi.getEnvId());
            // 设置系统级别的自定义MCP名字
            detail.setCustomMcpName(configApi.getSystemMcpName());
            
            // 设置系统信息
            System system = systemMap.get(configApi.getSystemId());
            if (system != null) {
                detail.setSystemName(system.getName());
            }
            
            // 设置环境信息
            SystemEnv env = envMap.get(configApi.getEnvId());
            if (env != null) {
                detail.setEnvName(env.getName());
                detail.setEnvBaseUrl(env.getBaseUrl());
            }
            
            // 解析 apiConfig，获取接口信息和自定义MCP名字
            List<McpApiConfigItem> apiConfigList = safeApiConfigList(configApi);

            // 设置API详情列表
            List<McpServiceDetailVO.ApiDetail> apiDetails = new ArrayList<>();
            for (McpApiConfigItem item : apiConfigList) {
                String apiId = item.getApiId();
                String customMcpName = item.getCustomMcpName();
                Api api = apiMap.get(apiId);
                if (api != null) {
                    McpServiceDetailVO.ApiDetail apiDetail = new McpServiceDetailVO.ApiDetail();
                    apiDetail.setId(api.getId());
                    apiDetail.setName(api.getName());
                    apiDetail.setMethod(api.getMethod());
                    apiDetail.setPath(api.getPath());
                    // 设置接口级别的自定义MCP名字
                    apiDetail.setCustomMcpName(customMcpName);
                    apiDetails.add(apiDetail);
                }
            }
            detail.setApis(apiDetails);
            
            configApiDetails.add(detail);
        }
        
        vo.setConfigApis(configApiDetails);
        
        return vo;
    }

    /**
     * 发布服务
     *
     * @param id 服务ID
     * @return 是否成功
     * @throws IllegalArgumentException 如果服务配置不完整
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean publishService(String id) {
        McpService service = this.getById(id);
        if (service == null) {
            throw new IllegalArgumentException("服务不存在");
        }
        
        // 验证服务状态
        String currentStatus = service.getStatus();
        if (!"draft".equals(currentStatus) && !"stopped".equals(currentStatus)) {
            throw new IllegalArgumentException("服务状态不允许发布，当前状态: " + currentStatus);
        }
        
        // 验证基本信息
        if (!StringUtils.hasText(service.getName())) {
            throw new IllegalArgumentException("服务名称不能为空");
        }
        if (!StringUtils.hasText(service.getEndpoint())) {
            throw new IllegalArgumentException("服务端点不能为空");
        }
        if (!StringUtils.hasText(service.getAccessToken())) {
            throw new IllegalArgumentException("访问令牌不能为空");
        }
        
        // 验证服务配置
        LambdaQueryWrapper<McpConfigApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpConfigApi::getMcpServiceId, id);
        List<McpConfigApi> configApis = mcpConfigApiService.list(wrapper);
        
        if (configApis.isEmpty()) {
            throw new IllegalArgumentException("服务必须至少配置一个资源组合");
        }
        
        // 验证每个资源组合
        Set<String> systemIds = new HashSet<>();
        Set<String> envIds = new HashSet<>();
        Set<String> apiIds = new HashSet<>();
        
        for (McpConfigApi configApi : configApis) {
            if (!StringUtils.hasText(configApi.getSystemId())) {
                throw new IllegalArgumentException("资源组合必须包含系统ID");
            }
            systemIds.add(configApi.getSystemId());

            if (!StringUtils.hasText(configApi.getEnvId())) {
                log.warn("资源组合缺少环境ID，建议配置环境以确定baseUrl");
            } else {
                envIds.add(configApi.getEnvId());
            }

            // 从 apiConfig 读取接口ID
            List<McpApiConfigItem> apiConfigList = safeApiConfigList(configApi);
            if (apiConfigList.isEmpty()) {
                throw new IllegalArgumentException("资源组合必须至少包含一个API接口");
            }
            for (McpApiConfigItem item : apiConfigList) {
                String apiId = item.getApiId();
                if (StringUtils.hasText(apiId)) {
                    apiIds.add(apiId);
                }
            }
        }
        
        // 验证关联数据是否存在
        if (!systemIds.isEmpty()) {
            List<System> systems = systemService.listByIds(systemIds);
            if (systems.size() != systemIds.size()) {
                throw new IllegalArgumentException("部分系统不存在或已被删除");
            }
        }
        
        if (!envIds.isEmpty()) {
            List<SystemEnv> envs = systemEnvService.listByIds(envIds);
            if (envs.size() != envIds.size()) {
                throw new IllegalArgumentException("部分环境不存在或已被删除");
            }
        }
        
        if (!apiIds.isEmpty()) {
            List<Api> apis = apiService.listByIds(apiIds);
            if (apis.size() != apiIds.size()) {
                throw new IllegalArgumentException("部分API接口不存在或已被删除");
            }
        }
        
        // 验证通过，更新状态
        service.setStatus("published");
        service.setPublishTime(LocalDateTime.now());
        boolean result = this.updateById(service);
        
        // 清除缓存（使用服务ID和访问令牌）
        if (StringUtils.hasText(service.getAccessToken())) {
            invalidateMcpServiceCache(service.getId(), service.getAccessToken());
        }
        
        return result;
    }

    /**
     * 停止服务
     *
     * @param id 服务ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean stopService(String id) {
        McpService service = this.getById(id);
        if (service == null) {
            return false;
        }
        service.setStatus("stopped");
        boolean result = this.updateById(service);
        
        // 清除缓存（使用服务ID和访问令牌）
        if (StringUtils.hasText(service.getAccessToken())) {
            invalidateMcpServiceCache(service.getId(), service.getAccessToken());
        }
        
        return result;
    }


    /**
     * 配置选择数据
     */
    @lombok.Data
    public static class SelectionData {
        private String systemId;
        private String systemName;
        private String envId;
        private String envName;
        private String envBaseUrl;
        private List<String> apiIds;
        private String customMcpName;
        private java.util.Map<String, String> apiCustomMcpNames;
    }

    /**
     * 构建 apiConfig 列表
     * 结构：[{"apiId": "xxx", "customMcpName": "yyy"}, ...]
     *
     * @param apiIds 接口ID列表
     * @param apiCustomMcpNames 接口ID到自定义MCP名字的映射
     * @return 接口配置列表
     */
    private List<McpApiConfigItem> buildApiConfig(List<String> apiIds,
                                                  java.util.Map<String, String> apiCustomMcpNames) {
        List<McpApiConfigItem> apiConfigList = new ArrayList<>();
        if (apiIds == null || apiIds.isEmpty()) {
            return apiConfigList;
        }
        for (String apiId : apiIds) {
            McpApiConfigItem item = new McpApiConfigItem();
            item.setApiId(apiId);
            if (apiCustomMcpNames != null && apiCustomMcpNames.containsKey(apiId)) {
                String customName = apiCustomMcpNames.get(apiId);
                if (StringUtils.hasText(customName)) {
                    item.setCustomMcpName(customName);
                }
            }
            apiConfigList.add(item);
        }
        return apiConfigList;
    }

    /**
     * 安全获取配置中的 API 列表（避免 null）
     */
    private List<McpApiConfigItem> safeApiConfigList(McpConfigApi configApi) {
        List<McpApiConfigItem> list = configApi.getApiConfig();
        return list != null ? list : Collections.emptyList();
    }

    /**
     * 计算服务的组合数和接口数（用于列表展示）
     *
     * @param serviceId 服务ID
     * @return [组合数, 接口数]
     */
    public int[] calculateServiceStats(String serviceId) {
        LambdaQueryWrapper<McpConfigApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpConfigApi::getMcpServiceId, serviceId);
        List<McpConfigApi> configApis = mcpConfigApiService.list(wrapper);
        
        int comboCount = configApis.size();
        int apiCount = 0;
        for (McpConfigApi configApi : configApis) {
            List<McpApiConfigItem> apiConfigList = safeApiConfigList(configApi);
            apiCount += apiConfigList.size();
        }

        return new int[]{comboCount, apiCount};
    }

    /**
     * 清除MCP服务缓存
     * 用于在发布、停止、删除服务时清除McpProtocolService中的缓存
     *
     * @param serviceId 服务ID
     * @param accessToken 访问令牌
     */
    private void invalidateMcpServiceCache(String serviceId, String accessToken) {
        if (StringUtils.hasText(serviceId) && StringUtils.hasText(accessToken)) {
            try {
                mcpServiceCacheService.invalidateCache(serviceId, accessToken);
            } catch (Exception e) {
                log.warn("清除MCP服务缓存失败: {}:{}", serviceId, accessToken, e);
            }
        }
    }

    /**
     * 根据ID删除服务（重写以清除缓存）
     *
     * @param id 服务ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        // 先获取服务信息，以便清除缓存
        if (id instanceof String) {
            McpService service = this.getById((String) id);
            if (service != null && StringUtils.hasText(service.getAccessToken())) {
                // 清除缓存
                invalidateMcpServiceCache(service.getId(), service.getAccessToken());
            }
        }
        // 执行删除操作
        return super.removeById(id);
    }
}
