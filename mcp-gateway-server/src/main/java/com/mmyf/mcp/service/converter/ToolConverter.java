package com.mmyf.mcp.service.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmyf.commons.util.PinYinUtils;
import com.mmyf.mcp.model.dto.api.*;
import com.mmyf.mcp.model.entity.system.Api;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API到MCP工具的转换器
 *
 * @author Teddy
 * @date 2026-01-26
 */
@Component
@Slf4j
public class ToolConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");


    /**
     * 将API转换为MCP工具（支持自定义MCP名字）
     *
     * @param api API实体
     * @param systemName 系统名称（用于生成工具名称）
     * @param systemMcpName 系统级别的自定义MCP名字（可选）
     * @param apiMcpName 接口级别的自定义MCP名字（可选）
     * @return MCP工具
     */
    public McpSchema.Tool.Builder convertToTool(Api api, String systemName, String systemMcpName, String apiMcpName) {
        if (api == null) {
            return null;
        }

        // 生成工具名称：系统MCP名称-工具MCP名称
        String toolName;
        // 确定系统MCP名称部分
        String systemMcpNamePart;
        if (StringUtils.hasText(systemMcpName)) {
            // 使用自定义的系统MCP名字
            systemMcpNamePart = systemMcpName;
        } else {
            // 使用系统名称自动生成
            systemMcpNamePart = sanitizeName(systemName);
        }
        
        // 确定工具MCP名称部分
        String apiMcpNamePart;
        if (StringUtils.hasText(apiMcpName)) {
            // 使用自定义的工具MCP名字
            apiMcpNamePart = apiMcpName;
        } else {
            // 使用接口名称自动生成
            apiMcpNamePart = sanitizeName(api.getName());
        }
        
        // 组合：系统MCP名称-工具MCP名称
        toolName = systemMcpNamePart + "-" + apiMcpNamePart;
        
        // 确保长度不超过128个字符
        if (toolName.length() > 128) {
            // 如果超过长度，优先保留工具MCP名称，截断系统MCP名称
            int maxSystemNameLength = 128 - apiMcpNamePart.length() - 1; // 减去连字符和工具MCP名称长度
            if (maxSystemNameLength > 0) {
                toolName = systemMcpNamePart.substring(0, maxSystemNameLength) + "-" + apiMcpNamePart;
            } else {
                // 如果工具MCP名称本身就超过128，直接截断
                toolName = apiMcpNamePart.substring(0, 128);
            }
        }
        

        // 设置描述
        String description = StringUtils.hasText(api.getDescription())
                ? api.getDescription()
                : api.getName();

        // 生成inputSchema
        McpSchema.JsonSchema inputSchema = generateInputSchema(api);

        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
                .name(toolName)
                .title(toolName)
                .description(description)
                .inputSchema(inputSchema);
        return toolBuilder;
    }

    /**
     * 生成工具名称
     * 根据MCP协议规范，工具名称应该：
     * - 长度在1-128个字符之间
     * - 只包含ASCII字母、数字、下划线、连字符和点
     * - 不包含中文字符
     * 
     * @param systemName 系统名称
     * @param apiName API名称
     * @return 符合MCP规范的工具名称
     */
    private String generateToolName(String systemName, String apiName) {
        String sanitizedSystemName = sanitizeName(systemName);
        String sanitizedApiName = sanitizeName(apiName);
        
        if (!StringUtils.hasText(sanitizedSystemName)) {
            return sanitizedApiName;
        }
        
        String toolName = sanitizedSystemName + "_" + sanitizedApiName;
        
        // 确保长度不超过128个字符
        if (toolName.length() > 128) {
            // 如果超过长度，优先保留API名称，截断系统名称
            int maxSystemNameLength = 128 - sanitizedApiName.length() - 1; // 减去下划线和API名称长度
            if (maxSystemNameLength > 0) {
                toolName = sanitizedSystemName.substring(0, maxSystemNameLength) + "_" + sanitizedApiName;
            } else {
                // 如果API名称本身就超过128，直接截断
                toolName = sanitizedApiName.substring(0, 128);
            }
        }
        
        return toolName;
    }

    /**
     * 清理名称，将中文转换为拼音，移除特殊字符
     * 确保生成的名称符合MCP协议规范：
     * - 只包含ASCII字母（A-Z, a-z）、数字（0-9）、下划线（_）、连字符（-）和点（.）
     * - 不包含空格、逗号或其他特殊字符
     * 
     * @param name 原始名称
     * @return 清理后的名称
     */
    private String sanitizeName(String name) {
        if (!StringUtils.hasText(name)) {
            return "unnamed";
        }
        
        // 1. 将中文转换为拼音（全拼，小写）
        String pinyinName = convertChineseToPinyin(name);
        
        // 2. 移除所有不允许的字符，只保留ASCII字母、数字、下划线、连字符和点
        // 将其他字符替换为下划线
        String sanitized = pinyinName.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        
        // 3. 将多个连续的下划线合并为一个
        sanitized = sanitized.replaceAll("_{2,}", "_");
        
        // 4. 移除开头和结尾的下划线、连字符和点
        sanitized = sanitized.replaceAll("^[_\\.\\-]+|[_\\.\\-]+$", "");
        
        // 5. 确保名称不为空
        if (!StringUtils.hasText(sanitized)) {
            return "unnamed";
        }
        
        // 6. 确保首字符是字母或数字（不能以下划线、连字符或点开头）
        if (sanitized.matches("^[_\\.\\-].*")) {
            sanitized = "tool_" + sanitized;
        }
        
        return sanitized;
    }

    /**
     * 将中文转换为拼音
     * 如果包含中文字符，转换为全拼；如果已经是英文，保持原样
     * 
     * @param text 原始文本
     * @return 转换后的文本（拼音或原文本）
     */
    private String convertChineseToPinyin(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        
        // 检查是否包含中文字符
        boolean hasChinese = text.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FA5);
        
        if (!hasChinese) {
            // 没有中文字符，直接返回原文本
            return text;
        }
        
        // 包含中文字符，转换为拼音
        try {
            String pinyin = PinYinUtils.getFullSpell(text);
            // 将拼音中的空格替换为下划线，以保持单词分隔
            return pinyin.replaceAll("\\s+", "_");
        } catch (Exception e) {
            log.warn("转换中文到拼音失败: {}", text, e);
            // 转换失败时，移除中文字符，只保留ASCII字符
            return text.replaceAll("[^\\x00-\\x7F]", "");
        }
    }

    /**
     * 生成inputSchema（JSON Schema格式）
     */
    private McpSchema.JsonSchema generateInputSchema(Api api) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        // 处理路径参数
        extractPathParams(api.getPath(), properties, required);

        // 处理Query参数
        extractQueryParams(api.getQueryParams(), properties, required);

        // 处理Body参数
        extractBodyParams(api.getBodyParam(), properties, required);

        // 处理Header参数
        extractHeaderParams(api.getHeaders(), properties, required);

        McpSchema.JsonSchema schema = new McpSchema.JsonSchema(
                "object",
                properties,
                required,
                false,
                null,
                null
        );

        return schema;
    }

    /**
     * 提取路径参数
     */
    private void extractPathParams(String path, Map<String, Object> properties, List<String> required) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Map<String, Object> paramSchema = new LinkedHashMap<>();
            paramSchema.put("type", "string");
            
            // 生成详细描述：路径参数默认必填
            String description = buildPathParamDescription(paramName);
            paramSchema.put("description", description);
            
            properties.put(paramName, paramSchema);
            required.add(paramName);
        }
    }

    /**
     * 提取Query参数
     */
    private void extractQueryParams(List<QueryParam> queryParams, Map<String, Object> properties, List<String> required) {
        if (queryParams == null || queryParams.isEmpty()) {
            return;
        }
        
        for (QueryParam param : queryParams) {
            String key = param.getKey();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            
            Map<String, Object> paramSchema = new LinkedHashMap<>();
            String type = param.getType();
            if (type == null) {
                type = "string";
            }
            paramSchema.put("type", mapType(type));
            
            // 生成详细描述：包含类型、必填、描述和示例
            String description = buildQueryParamDescriptionForSingle(param);
            paramSchema.put("description", description);
            
            // 如果有默认值
            String value = param.getValue();
            if (value != null) {
                paramSchema.put("default", value);
            }
            
            properties.put(key, paramSchema);
            
            // 如果是必填的
            Boolean isRequired = param.getRequired();
            if (Boolean.TRUE.equals(isRequired) && !required.contains(key)) {
                required.add(key);
            }
        }
    }

    /**
     * 提取Body参数
     * 根据bodyParam的类型，只有当类型匹配且有对应数据时才添加参数
     */
    private void extractBodyParams(BodyParam bodyParam, Map<String, Object> properties, List<String> required) {
        if (bodyParam == null) {
            return;
        }
        
        String type = bodyParam.getType();
        if (type == null) {
            type = "json";
        }
        
        // JSON格式的body
        if ("json".equals(type)) {
            // 检查是否有jsonParams字段，只有当有数据时才添加
            List<JsonParamNode> jsonParams = bodyParam.getJsonParams();
            if (jsonParams != null && !jsonParams.isEmpty()) {
                // 统一为一个jsonParam参数
                Map<String, Object> jsonParamSchema = new LinkedHashMap<>();
                jsonParamSchema.put("type", "string");
                
                // 生成描述：将jsonParams转换为指定格式的JSON字符串
                String description = buildJsonParamDescription(jsonParams);
                jsonParamSchema.put("description", description);
                
                properties.put("jsonParam", jsonParamSchema);
                // jsonParam不是必填的，因为可能为空对象
            }
        } else if ("form-data".equals(type)) {
            // Form-data格式，只有当有数据时才添加
            List<FormDataParam> formDataParams = bodyParam.getFormDataParams();
            if (formDataParams != null && !formDataParams.isEmpty()) {
                for (FormDataParam param : formDataParams) {
                    String key = param.getKey();
                    if (!StringUtils.hasText(key)) {
                        continue;
                    }
                    Map<String, Object> paramSchema = new LinkedHashMap<>();
                    paramSchema.put("type", "string");
                    
                    // 生成详细描述：包含类型、必填、描述和示例
                    String description = buildFormDataParamDescriptionForSingle(param);
                    paramSchema.put("description", description);
                    
                    // 如果有默认值
                    String value = param.getValue();
                    if (value != null) {
                        paramSchema.put("default", value);
                    }
                    
                    properties.put(key, paramSchema);
                    
                    // 如果是必填的
                    Boolean isRequired = param.getRequired();
                    if (Boolean.TRUE.equals(isRequired) && !required.contains(key)) {
                        required.add(key);
                    }
                }
            }
        } else if ("x-www-form-urlencoded".equals(type)) {
            // x-www-form-urlencoded格式，只有当有数据时才添加
            List<UrlEncodedParam> urlEncodedParams = bodyParam.getUrlEncodedParams();
            if (urlEncodedParams != null && !urlEncodedParams.isEmpty()) {
                for (UrlEncodedParam param : urlEncodedParams) {
                    String key = param.getKey();
                    if (!StringUtils.hasText(key)) {
                        continue;
                    }
                    Map<String, Object> paramSchema = new LinkedHashMap<>();
                    paramSchema.put("type", "string");
                    
                    // 生成详细描述：包含类型、必填、描述和示例
                    String description = buildUrlEncodedParamDescriptionForSingle(param);
                    paramSchema.put("description", description);
                    
                    // 如果有默认值
                    String value = param.getValue();
                    if (value != null) {
                        paramSchema.put("default", value);
                    }
                    
                    properties.put(key, paramSchema);
                    
                    // 如果是必填的
                    Boolean isRequired = param.getRequired();
                    if (Boolean.TRUE.equals(isRequired) && !required.contains(key)) {
                        required.add(key);
                    }
                }
            }
        } else if ("raw".equals(type)) {
            // Raw格式，只有当有数据时才添加
            String raw = bodyParam.getRaw();
            if (StringUtils.hasText(raw)) {
                Map<String, Object> bodySchema = new LinkedHashMap<>();
                bodySchema.put("type", "string");
                bodySchema.put("description", "请求体（原始格式）");
                bodySchema.put("default", raw);
                properties.put("body", bodySchema);
            }
        }
        // 其他类型不处理，不添加任何参数
    }

    /**
     * 构建JSON参数描述
     * 参考前端的convertParamsToJsonObject方法，将jsonParams转换为JSON结构，然后在描述中展示
     * 
     * @param jsonParams JSON参数列表（树形结构）
     * @return 描述字符串
     */
    private String buildJsonParamDescription(List<JsonParamNode> jsonParams) {
        StringBuilder sb = new StringBuilder();
        sb.append("调用时需要根据每个key中的类型、必填、描述和示例去填充具体参数，参数结构如下：\n");
        
        // 先将jsonParams转换为JSON对象结构
        Object jsonObject = convertParamsToJsonObject(jsonParams);
        
        // 构建参数映射表，方便查找参数信息
        Map<String, JsonParamNode> paramMap = buildParamMap(jsonParams);
        
        // 将JSON对象转换为带描述的格式
        sb.append(convertJsonObjectToDescription(jsonObject, paramMap, 0));
        
        return sb.toString();
    }

    /**
     * 将jsonParams转换为JSON对象（参考前端的convertParamsToJsonObject）
     */
    private Object convertParamsToJsonObject(List<JsonParamNode> params) {
        if (params == null || params.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 如果只有一个根节点且名称为 'root'，则返回其值
        if (params.size() == 1) {
            JsonParamNode firstParam = params.get(0);
            String name = firstParam.getName();
            if ("root".equals(name)) {
                return convertNodeToValue(firstParam);
            }
        }

        // 多个根节点，构建对象
        Map<String, Object> result = new LinkedHashMap<>();
        for (JsonParamNode param : params) {
            String name = param.getName();
            if (StringUtils.hasText(name) && !"root".equals(name)) {
                result.put(name, convertNodeToValue(param));
            }
        }
        return result;
    }

    /**
     * 将单个参数节点转换为对应的值（参考前端的convertNodeToValue）
     */
    private Object convertNodeToValue(JsonParamNode node) {
        String type = node.getType();
        if (type == null) {
            type = "string";
        }
        
        switch (type) {
            case "object": {
                List<JsonParamNode> children = node.getChildren();
                if (children != null && !children.isEmpty()) {
                    Map<String, Object> obj = new LinkedHashMap<>();
                    for (JsonParamNode child : children) {
                        String childName = child.getName();
                        if (StringUtils.hasText(childName) && !"items".equals(childName)) {
                            obj.put(childName, convertNodeToValue(child));
                        }
                    }
                    return obj;
                }
                return new LinkedHashMap<>();
            }
            case "array": {
                List<JsonParamNode> children = node.getChildren();
                if (children != null && !children.isEmpty()) {
                    // 数组类型，查找 items 子节点
                    JsonParamNode itemsNode = null;
                    for (JsonParamNode child : children) {
                        if ("items".equals(child.getName())) {
                            itemsNode = child;
                            break;
                        }
                    }
                    
                    if (itemsNode != null) {
                        // 如果有示例值，使用示例值；否则使用默认值
                        String example = itemsNode.getExample();
                        if (StringUtils.hasText(example)) {
                            Object exampleValue = convertNodeToValue(itemsNode);
                            return Collections.singletonList(exampleValue);
                        }
                        // 根据 items 类型返回默认值
                        String itemType = itemsNode.getType();
                        if (itemType == null) {
                            itemType = "string";
                        }
                        switch (itemType) {
                            case "number":
                                return Collections.singletonList(0);
                            case "boolean":
                                return Collections.singletonList(false);
                            case "object":
                                return Collections.singletonList(new LinkedHashMap<>());
                            case "array":
                                return Collections.singletonList(new ArrayList<>());
                            default:
                                return Collections.singletonList("");
                        }
                    }
                    
                    // 如果没有 items 节点，但有其他子节点，说明是对象数组
                    List<Object> objArray = new ArrayList<>();
                    if (!children.isEmpty()) {
                        Map<String, Object> firstItem = new LinkedHashMap<>();
                        for (JsonParamNode child : children) {
                            String childName = child.getName();
                            if (StringUtils.hasText(childName) && !"items".equals(childName)) {
                                firstItem.put(childName, convertNodeToValue(child));
                            }
                        }
                        if (!firstItem.isEmpty()) {
                            objArray.add(firstItem);
                        }
                    }
                    return objArray.isEmpty() ? new ArrayList<>() : objArray;
                }
                return new ArrayList<>();
            }
            case "number": {
                String example = node.getExample();
                if (StringUtils.hasText(example)) {
                    try {
                        double num = Double.parseDouble(example);
                        return num == (int) num ? (int) num : num;
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
                return 0;
            }
            case "boolean": {
                String example = node.getExample();
                if (StringUtils.hasText(example)) {
                    return "true".equalsIgnoreCase(example);
                }
                return false;
            }
            case "string":
            default: {
                String example = node.getExample();
                return StringUtils.hasText(example) ? example : "";
            }
        }
    }

    /**
     * 构建参数映射表，用于根据key查找参数信息
     */
    private Map<String, JsonParamNode> buildParamMap(List<JsonParamNode> params) {
        Map<String, JsonParamNode> paramMap = new HashMap<>();
        buildParamMapRecursive(params, paramMap, "");
        return paramMap;
    }

    /**
     * 递归构建参数映射表
     */
    private void buildParamMapRecursive(List<JsonParamNode> params, Map<String, JsonParamNode> paramMap, String parentPath) {
        for (JsonParamNode param : params) {
            String name = param.getName();
            if (!StringUtils.hasText(name) || "root".equals(name)) {
                continue;
            }
            
            String type = param.getType();
            if (type == null) {
                type = "string";
            }
            
            // 对于items节点，使用parentPath + ".items"作为key
            if ("items".equals(name)) {
                String itemsKey = StringUtils.hasText(parentPath) ? parentPath + ".items" : "items";
                paramMap.put(itemsKey, param);
                
                // 如果items是object类型，递归处理其子节点
                if ("object".equals(type)) {
                    List<JsonParamNode> children = param.getChildren();
                    if (children != null) {
                        buildParamMapRecursive(children, paramMap, itemsKey);
                    }
                }
                continue;
            }
            
            // 普通节点
            String fullKey = StringUtils.hasText(parentPath) ? parentPath + "." + name : name;
            paramMap.put(fullKey, param);
            
            // 递归处理子节点
            List<JsonParamNode> children = param.getChildren();
            if (children != null) {
                if ("object".equals(type)) {
                    // object类型的子节点，使用当前key作为parentPath
                    buildParamMapRecursive(children, paramMap, fullKey);
                } else if ("array".equals(type)) {
                    // array类型的子节点，递归处理（items节点会在上面处理）
                    buildParamMapRecursive(children, paramMap, fullKey);
                }
            }
        }
    }

    /**
     * 将JSON对象转换为带描述的格式字符串
     * 同时传递参数列表用于查找参数信息
     */
    @SuppressWarnings("unchecked")
    private String convertJsonObjectToDescription(Object jsonObject, Map<String, JsonParamNode> paramMap, int indent) {
        return convertJsonObjectToDescriptionRecursive(jsonObject, paramMap, indent, "");
    }

    /**
     * 递归将JSON对象转换为带描述的格式字符串
     */
    @SuppressWarnings("unchecked")
    private String convertJsonObjectToDescriptionRecursive(Object jsonObject, Map<String, JsonParamNode> paramMap, int indent, String currentPath) {
        if (jsonObject == null) {
            return "null";
        }
        
        String indentStr = "  ".repeat(indent);
        
        if (jsonObject instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) jsonObject;
            if (map.isEmpty()) {
                return "{}";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            
            int index = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // 构建当前key的完整路径
                String fullKey = StringUtils.hasText(currentPath) ? currentPath + "." + key : key;
                
                // 查找参数信息，尝试多种路径匹配方式
                JsonParamNode paramInfo = paramMap.get(fullKey);
                if (paramInfo == null) {
                    // 尝试直接匹配key（用于顶层参数）
                    paramInfo = paramMap.get(key);
                }
                if (paramInfo == null && StringUtils.hasText(currentPath)) {
                    // 尝试匹配相对路径（用于嵌套结构）
                    String[] pathParts = currentPath.split("\\.");
                    if (pathParts.length > 0) {
                        String lastPart = pathParts[pathParts.length - 1];
                        if ("items".equals(lastPart) && pathParts.length > 1) {
                            // 如果是 items，尝试查找父路径下的子节点
                            // 例如：currentPath = "parent.items", key = "childKey"
                            // 尝试查找 "parent.items.childKey"（已经在上面尝试过）
                            // 或者查找 "parent.childKey"（如果 items 是数组元素的占位符）
                            String parentPath = String.join(".", Arrays.copyOf(pathParts, pathParts.length - 1));
                            String alternativeKey = parentPath + "." + key;
                            paramInfo = paramMap.get(alternativeKey);
                        }
                        // 尝试从路径的各个层级查找
                        for (int i = pathParts.length - 1; i >= 0 && paramInfo == null; i--) {
                            String partialPath = String.join(".", Arrays.copyOf(pathParts, i + 1)) + "." + key;
                            paramInfo = paramMap.get(partialPath);
                        }
                    }
                }
                
                sb.append(indentStr).append("  \"").append(key).append("\": ");
                
                if (value instanceof Map || value instanceof List) {
                    // 复杂类型，递归处理
                    String nestedDesc = convertJsonObjectToDescriptionRecursive(value, paramMap, indent + 1, fullKey);
                    sb.append(nestedDesc);
                } else {
                    // 基本类型，添加描述信息
                    sb.append("\"");
                    if (paramInfo != null) {
                        String paramDesc = buildParamDescriptionString(paramInfo);
                        sb.append(paramDesc);
                    } else {
                        sb.append("类型：").append(inferType(value));
                        sb.append("，必填：false");
                    }
                    sb.append("\"");
                }
                
                if (index < map.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
                index++;
            }
            
            sb.append(indentStr).append("}");
            return sb.toString();
        } else if (jsonObject instanceof List) {
            List<Object> list = (List<Object>) jsonObject;
            if (list.isEmpty()) {
                return "[]";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            
            // 数组通常只有一个元素作为示例
            Object firstItem = list.get(0);
            sb.append(indentStr).append("  ");
            
            // 查找数组元素的参数信息（通过items路径）
            String itemsKey = currentPath + ".items";
            JsonParamNode itemsParamInfo = paramMap.get(itemsKey);
            if (itemsParamInfo == null) {
                // 尝试查找父路径下的items
                String parentPath = currentPath.contains(".") ? currentPath.substring(0, currentPath.lastIndexOf(".")) : "";
                if (StringUtils.hasText(parentPath)) {
                    itemsKey = parentPath + ".items";
                    itemsParamInfo = paramMap.get(itemsKey);
                }
            }
            
            if (firstItem instanceof Map || firstItem instanceof List) {
                // 数组元素是复杂类型
                // 对于数组中的对象，使用 itemsKey 作为基础路径
                // 这样在递归处理对象内部的 key 时，路径会是 itemsKey.childKey，能匹配到参数映射表中的 parent.items.childKey
                // 如果 itemsKey 不存在，使用 currentPath + ".items" 作为基础路径
                String basePath = StringUtils.hasText(itemsKey) ? itemsKey : (currentPath + ".items");
                String nestedDesc = convertJsonObjectToDescriptionRecursive(firstItem, paramMap, indent + 1, basePath);
                sb.append(nestedDesc);
            } else {
                // 数组元素是基本类型
                sb.append("\"");
                if (itemsParamInfo != null) {
                    String paramDesc = buildParamDescriptionString(itemsParamInfo);
                    sb.append(paramDesc);
                } else {
                    sb.append("类型：").append(inferType(firstItem));
                    sb.append("，必填：false");
                }
                sb.append("\"");
            }
            
            sb.append("\n").append(indentStr).append("]");
            return sb.toString();
        } else {
            // 基本类型
            return "\"" + jsonObject.toString() + "\"";
        }
    }

    /**
     * 构建参数描述字符串
     */
    @SuppressWarnings("unchecked")
    private String buildParamDescriptionString(JsonParamNode param) {
        StringBuilder desc = new StringBuilder();
        String type = param.getType();
        if (type == null) {
            type = "string";
        }
        Boolean required = param.getRequired();
        if (required == null) {
            required = false;
        }
        String description = param.getDescription();
        if (description == null) {
            description = "";
        }
        String example = param.getExample();
        if (example == null) {
            example = "";
        }
        
        desc.append("类型：").append(type);
        desc.append("，必填：").append(required ? "true" : "false");
        if (StringUtils.hasText(description)) {
            desc.append("，描述：").append(description);
        }
        if (StringUtils.hasText(example)) {
            desc.append("，示例：").append(example);
        }
        return desc.toString();
    }

    /**
     * 构建单个Form-data参数的详细描述
     * 格式类似JSON参数：类型、必填、描述、示例
     * 
     * @param param Form-data参数
     * @return 描述字符串
     */
    private String buildFormDataParamDescriptionForSingle(FormDataParam param) {
        StringBuilder desc = new StringBuilder();
        
        // 类型（text或file）
        String type = param.getType();
        if (type == null) {
            type = "text";
        }
        desc.append("类型：").append(type);
        
        // 必填
        Boolean required = param.getRequired();
        if (required == null) {
            required = false;
        }
        desc.append("，必填：").append(required ? "true" : "false");
        
        // 描述
        String description = param.getDescription();
        if (StringUtils.hasText(description)) {
            desc.append("，描述：").append(description);
        }
        
        // 示例（使用value作为示例）
        String value = param.getValue();
        if (StringUtils.hasText(value)) {
            desc.append("，示例：").append(value);
        }
        
        return desc.toString();
    }

    /**
     * 构建单个URL编码参数的详细描述
     * 格式类似JSON参数：类型、必填、描述、示例
     * 
     * @param param URL编码参数
     * @return 描述字符串
     */
    private String buildUrlEncodedParamDescriptionForSingle(UrlEncodedParam param) {
        StringBuilder desc = new StringBuilder();
        
        // 类型（URL编码参数默认是string类型）
        desc.append("类型：string");
        
        // 必填
        Boolean required = param.getRequired();
        if (required == null) {
            required = false;
        }
        desc.append("，必填：").append(required ? "true" : "false");
        
        // 描述
        String description = param.getDescription();
        if (StringUtils.hasText(description)) {
            desc.append("，描述：").append(description);
        }
        
        // 示例（使用value作为示例）
        String value = param.getValue();
        if (StringUtils.hasText(value)) {
            desc.append("，示例：").append(value);
        }
        
        return desc.toString();
    }

    /**
     * 构建单个Query参数的详细描述
     * 格式类似JSON参数：类型、必填、描述、示例
     * 
     * @param param Query参数
     * @return 描述字符串
     */
    private String buildQueryParamDescriptionForSingle(QueryParam param) {
        StringBuilder desc = new StringBuilder();
        
        // 类型
        String type = param.getType();
        if (type == null) {
            type = "string";
        }
        desc.append("类型：").append(type);
        
        // 必填
        Boolean required = param.getRequired();
        if (required == null) {
            required = false;
        }
        desc.append("，必填：").append(required ? "true" : "false");
        
        // 描述
        String description = param.getDescription();
        if (StringUtils.hasText(description)) {
            desc.append("，描述：").append(description);
        }
        
        // 示例（使用value作为示例）
        String value = param.getValue();
        if (StringUtils.hasText(value)) {
            desc.append("，示例：").append(value);
        }
        
        return desc.toString();
    }

    /**
     * 构建单个Header参数的详细描述
     * 格式类似JSON参数：类型、必填、描述、示例
     * 
     * @param header Header参数
     * @return 描述字符串
     */
    private String buildHeaderParamDescriptionForSingle(Header header) {
        StringBuilder desc = new StringBuilder();
        
        // 类型（Header参数默认是string类型）
        desc.append("类型：string");
        
        // 必填
        Boolean required = header.getRequired();
        if (required == null) {
            required = false;
        }
        desc.append("，必填：").append(required ? "true" : "false");
        
        // 描述
        String description = header.getDescription();
        if (StringUtils.hasText(description)) {
            desc.append("，描述：").append(description);
        }
        
        // 示例（使用value作为示例）
        String value = header.getValue();
        if (StringUtils.hasText(value)) {
            desc.append("，示例：").append(value);
        }
        
        return desc.toString();
    }

    /**
     * 构建路径参数的详细描述
     * 路径参数默认必填，格式：类型、必填
     * 
     * @param paramName 参数名称
     * @return 描述字符串
     */
    private String buildPathParamDescription(String paramName) {
        StringBuilder desc = new StringBuilder();
        
        // 类型（路径参数默认是string类型）
        desc.append("类型：string");
        
        // 路径参数默认必填
        desc.append("，必填：true");
        
        // 添加参数名称说明
        desc.append("，路径参数：").append(paramName);
        
        return desc.toString();
    }

    /**
     * 提取URL编码参数
     */
    private void extractUrlEncodedParams(List<UrlEncodedParam> urlEncodedParams, Map<String, Object> properties, List<String> required) {
        if (urlEncodedParams == null || urlEncodedParams.isEmpty()) {
            return;
        }
        
        for (UrlEncodedParam param : urlEncodedParams) {
            String key = param.getKey();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            
            Map<String, Object> paramSchema = new LinkedHashMap<>();
            paramSchema.put("type", "string");
            String description = param.getDescription();
            if (description == null) {
                description = "URL编码参数: " + key;
            }
            paramSchema.put("description", description);
            properties.put(key, paramSchema);
        }
    }

    /**
     * 提取Header参数
     */
    private void extractHeaderParams(List<Header> headers, Map<String, Object> properties, List<String> required) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        
        for (Header header : headers) {
            String key = header.getKey();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            
            Map<String, Object> headerSchema = new LinkedHashMap<>();
            headerSchema.put("type", "string");
            
            // 生成详细描述：包含类型、必填、描述和示例
            String description = buildHeaderParamDescriptionForSingle(header);
            headerSchema.put("description", description);
            
            // 如果有默认值
            String value = header.getValue();
            if (value != null) {
                headerSchema.put("default", value);
            }
            
            properties.put(key, headerSchema);
            
            // 如果是必填的
            Boolean isRequired = header.getRequired();
            if (Boolean.TRUE.equals(isRequired) && !required.contains(key)) {
                required.add(key);
            }
        }
    }

    /**
     * 映射类型
     */
    private String mapType(String type) {
        if (type == null) {
            return "string";
        }
        switch (type.toLowerCase()) {
            case "integer":
            case "int":
                return "integer";
            case "number":
            case "float":
            case "double":
                return "number";
            case "boolean":
            case "bool":
                return "boolean";
            case "array":
                return "array";
            case "object":
                return "object";
            default:
                return "string";
        }
    }

    /**
     * 推断值的类型
     */
    private String inferType(Object value) {
        if (value == null) {
            return "string";
        }
        if (value instanceof Number) {
            if (value instanceof Integer || value instanceof Long) {
                return "integer";
            }
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof List || value instanceof Object[]) {
            return "array";
        }
        if (value instanceof Map) {
            return "object";
        }
        return "string";
    }
}
