package com.mmyf.commons.mybatis;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.postgresql.util.PGobject;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL JSON 类型处理器（通用版本）
 * 通过 MyBatis-Plus 的 TableInfo 缓存获取实体类字段的实际类型，准确识别类型（包括泛型类型）
 * 
 * 使用方式：
 * 1. 在实体类中使用 @TableField(typeHandler = PostgreSQLJsonTypeHandler.class)
 * 2. 在 XML 中使用 typeHandler="com.mmyf.commons.mybatis.PostgreSQLJsonTypeHandler"
 * 
 * TypeHandler 会通过以下方式识别类型：
 * 1. 从 ResultSetMetaData 获取表名和列名
 * 2. 通过 MyBatis-Plus 的 TableInfoHelper 获取实体类信息（利用其缓存机制）
 * 3. 通过 TableFieldInfo 获取字段的实际类型（包括泛型类型）
 * 4. 缓存类型信息以提高性能
 * 
 * @author teddy
 * @date 2026-01-29
 */
@MappedJdbcTypes({JdbcType.OTHER})
public class PostgreSQLJsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 类型缓存：表名 + 列名 -> TypeReference
    private static final Map<String, TypeReference<?>> TYPE_CACHE = new ConcurrentHashMap<>();

    private TypeReference<T> typeReference;
    
    // 构造函数传入的类型（用于兜底逻辑）
    private Class<T> fallbackType;

    /**
     * 默认构造函数 - MyBatis 会调用此构造函数
     */
    public PostgreSQLJsonTypeHandler() {
        this.typeReference = null;
    }

    /**
     * 构造函数 - 显式指定类型（用于测试或特殊场景）
     * 当无法从元数据获取类型信息时，会使用此类型作为兜底
     * 
     * <p><b>重要限制：</b>
     * <ul>
     *   <li>由于 Java 类型擦除，Class<T> 类型参数不支持泛型信息</li>
     *   <li>例如：List<String> 会被擦除为 List，泛型参数 String 会丢失</li>
     *   <li>如果需要处理泛型类型（如 List<String>、Map<String, Object>），请使用 
     *       {@link #PostgreSQLJsonTypeHandler(TypeReference)} 构造函数</li>
     * </ul>
     * 
     * <p><b>MyBatis 初始化说明：</b>
     * <ul>
     *   <li>MyBatis 在初始化 TypeHandler 时，通常只会传入 Class 类型（通过反射获取）</li>
     *   <li>由于类型擦除，无法获取泛型信息，因此此构造函数主要用于简单类型（非泛型）</li>
     *   <li>对于泛型类型，建议通过 XML 配置或注解配置，让 TypeHandler 从实体类字段的元数据中获取完整类型信息</li>
     * </ul>
     * 
     * @param type 目标类型（不支持泛型参数）
     */
    public PostgreSQLJsonTypeHandler(Class<T> type) {
        this.typeReference = null;
        this.fallbackType = type;
    }

    /**
     * 构造函数 - 显式指定 TypeReference（用于复杂泛型类型）
     */
    public PostgreSQLJsonTypeHandler(TypeReference<T> typeReference) {
        if (typeReference == null) {
            throw new IllegalArgumentException("TypeReference argument cannot be null");
        }
        this.typeReference = typeReference;
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        // 注意：CallableStatement 无法像 ResultSet 那样通过 getMetaData() 获取列名和表名元数据
        // 因此无法在此处初始化 typeReference
        // 
        // 如果 typeReference 未初始化（为 null），parseJson 方法会使用兜底逻辑：
        // 1. 优先使用构造函数传入的 fallbackType（如果通过 PostgreSQLJsonTypeHandler(Class<T> type) 构造）
        //    - 注意：fallbackType 不支持泛型参数（由于 Java 类型擦除）
        //    - 对于泛型类型（如 List<String>），会被擦除为原始类型（List），泛型参数丢失
        // 2. 如果 fallbackType 也为 null，返回原始 JSON 字符串（String 类型）
        //
        // 建议：
        // 1. 如果可能，优先使用 ResultSet 的 getNullableResult 方法，以便自动识别类型（支持泛型）
        // 2. 或者在调用存储过程前，通过其他方式（如 XML 映射配置）显式指定类型
        // 3. 如果需要处理泛型类型，使用 PostgreSQLJsonTypeHandler(TypeReference<T>) 构造函数
        // 4. 如果类型是简单类型（非泛型），可以使用 PostgreSQLJsonTypeHandler(Class<T>) 构造函数
        
        return parseJson(cs.getObject(columnIndex));
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 尝试从 ResultSetMetaData 获取表名和字段信息
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 找到对应的列索引
            int columnIndex = -1;
            for (int i = 1; i <= columnCount; i++) {
                if (columnName.equalsIgnoreCase(metaData.getColumnName(i)) || 
                    columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                    columnIndex = i;
                    break;
                }
            }
            
            if (columnIndex > 0) {
                initTypeFromResultSet(metaData, columnIndex, columnName);
            } else {
                // 如果找不到列索引，尝试通过列名推断
                initTypeFromColumnName(columnName);
            }
        } catch (Exception e) {
            // 如果获取元数据失败，尝试通过列名推断
            initTypeFromColumnName(columnName);
        }
        
        return parseJson(rs.getObject(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        // 尝试从 ResultSetMetaData 获取表名和字段信息
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            String columnName = metaData.getColumnName(columnIndex);
            initTypeFromResultSet(metaData, columnIndex, columnName);
        } catch (Exception e) {
            // 如果获取元数据失败，尝试通过列名推断
            try {
                String columnName = rs.getMetaData().getColumnName(columnIndex);
                initTypeFromColumnName(columnName);
            } catch (Exception ex) {
                // 忽略异常
            }
        }
        
        return parseJson(rs.getObject(columnIndex));
    }
    
    /**
     * 通过表名和列名获取类型信息（使用 MyBatis-Plus 的 TableInfo 缓存）
     * 根据数据库的 column 名从 MyBatis-Plus 元数据中查找，而不是通过属性名
     */
    @SuppressWarnings("unchecked")
    private void initTypeByTableAndColumn(String tableName, String columnName) {
        if (typeReference != null) {
            return; // 已经初始化
        }
        
        // 构建缓存 key（使用规范化后的表名和列名）
        String normalizedTableName = normalizeTableName(tableName);
        String cacheKey = normalizedTableName + "#" + columnName;
        
        // 先从缓存中获取
        TypeReference<?> cachedRef = TYPE_CACHE.get(cacheKey);
        if (cachedRef != null) {
            this.typeReference = (TypeReference<T>) cachedRef;
            return;
        }
        
        // 通过 MyBatis-Plus 的 TableInfoHelper 获取表信息（利用其缓存机制）
        // 先尝试使用完整表名，如果失败再尝试规范化后的表名
        TableInfo tableInfo = TableInfoHelper.getTableInfo(tableName);
        if (tableInfo == null && !normalizedTableName.equals(tableName)) {
            tableInfo = TableInfoHelper.getTableInfo(normalizedTableName);
        }
        
        // 如果还是找不到，遍历所有 TableInfo 查找匹配的表名
        if (tableInfo == null) {
            for (TableInfo info : TableInfoHelper.getTableInfos()) {
                String infoTableName = info.getTableName();
                // 处理带 schema 的表名匹配
                if (tableName.equals(infoTableName) || 
                    normalizedTableName.equals(normalizeTableName(infoTableName))) {
                    tableInfo = info;
                    break;
                }
            }
        }
        
        if (tableInfo == null) {
            return; // 找不到对应的表信息
        }
        
        // 通过数据库列名（column）查找 TableFieldInfo，而不是通过属性名
        TableFieldInfo fieldInfo = tableInfo.getFieldList().stream()
            .filter(f -> columnName.equalsIgnoreCase(f.getColumn()))
            .findFirst()
            .orElse(null);
        
        if (fieldInfo == null) {
            return; // 找不到字段
        }
        
        // 获取字段的实际类型（包括泛型）
        Field field = fieldInfo.getField();
        Type fieldType = field.getGenericType();
        TypeReference<?> typeRef = createTypeReference(fieldType);
        
        if (typeRef != null) {
            TYPE_CACHE.put(cacheKey, typeRef);
            this.typeReference = (TypeReference<T>) typeRef;
        }
    }
    
    /**
     * 规范化表名（去掉 schema 前缀，只保留表名）
     */
    private String normalizeTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return tableName;
        }
        // 处理带 schema 的表名，如 "db_mcp.t_api" -> "t_api"
        if (tableName.contains(".")) {
            return tableName.substring(tableName.lastIndexOf('.') + 1);
        }
        return tableName;
    }
    
    /**
     * 根据 Type 创建 TypeReference
     */
    private TypeReference<?> createTypeReference(Type type) {
        if (type == null) {
            return null;
        }
        
        // 使用 TypeReference 的匿名内部类来创建 TypeReference
        // 通过 ObjectMapper 的 TypeFactory 来构造 JavaType
        com.fasterxml.jackson.databind.JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        
        return new TypeReference<Object>() {
            @Override
            public com.fasterxml.jackson.databind.JavaType getType() {
                return javaType;
            }
        };
    }

    /**
     * 根据参数类型初始化类型
     */
    private void initTypeByParameter(T parameter) {
        if (typeReference != null) {
            return; // 已经初始化
        }
        
        if (parameter == null) {
            return; // 无法从 null 推断类型
        }
        
        // 直接使用参数的实际类型创建 TypeReference
        Class<?> paramClass = parameter.getClass();
        this.typeReference = new TypeReference<T>() {
            @Override
            public com.fasterxml.jackson.databind.JavaType getType() {
                return objectMapper.getTypeFactory().constructType(paramClass);
            }
        };
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        // 在设置参数时初始化类型信息
        if (typeReference == null) {
            initTypeByParameter(parameter);
        }
        
        try {
            String jsonString = objectMapper.writeValueAsString(parameter);
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(jsonString);
            ps.setObject(i, jsonObject);
        } catch (Exception e) {
            throw new SQLException("Error converting object to JSON string", e);
        }
    }

    /**
     * 从 ResultSetMetaData 初始化类型
     */
    private void initTypeFromResultSet(ResultSetMetaData metaData, int columnIndex, String columnName) throws SQLException {
        String tableName = getTableName(metaData, columnIndex);
        
        if (tableName != null && columnName != null) {
            initTypeByTableAndColumn(tableName, columnName);
        } else if (columnName != null) {
            // 如果无法获取表名，尝试在所有已注册的实体类中查找列
            initTypeByColumnNameOnly(columnName);
        }
    }
    
    /**
     * 仅通过列名初始化类型（在所有已注册的实体类中查找，使用 MyBatis-Plus 缓存）
     * 根据数据库的 column 名查找，而不是通过属性名
     */
    @SuppressWarnings("unchecked")
    private void initTypeByColumnNameOnly(String columnName) {
        if (typeReference != null) {
            return; // 已经初始化
        }
        
        // 遍历 MyBatis-Plus 缓存的所有 TableInfo
        for (TableInfo tableInfo : TableInfoHelper.getTableInfos()) {
            // 通过数据库列名（column）查找 TableFieldInfo，而不是通过属性名
            TableFieldInfo fieldInfo = tableInfo.getFieldList().stream()
                .filter(f -> columnName.equalsIgnoreCase(f.getColumn()))
                .findFirst()
                .orElse(null);
            
            if (fieldInfo != null) {
                // 找到字段，获取类型
                Field field = fieldInfo.getField();
                Type fieldType = field.getGenericType();
                TypeReference<?> typeRef = createTypeReference(fieldType);
                
                if (typeRef != null) {
                    String cacheKey = normalizeTableName(tableInfo.getTableName()) + "#" + columnName;
                    TYPE_CACHE.put(cacheKey, typeRef);
                    this.typeReference = (TypeReference<T>) typeRef;
                    return;
                }
            }
        }
    }
    
    /**
     * 通过列名初始化类型（备用方案）
     */
    private void initTypeFromColumnName(String columnName) {
        if (columnName != null) {
            initTypeByColumnNameOnly(columnName);
        }
    }
    
    /**
     * 从 ResultSetMetaData 获取表名
     */
    private String getTableName(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        try {
            // 尝试获取表名（可能包含 schema，如 db_mcp.t_api）
            String tableName = metaData.getTableName(columnIndex);
            if (tableName != null && !tableName.isEmpty()) {
                // 处理带 schema 的表名，提取表名部分
                if (tableName.contains(".")) {
                    tableName = tableName.substring(tableName.lastIndexOf('.') + 1);
                }
                return tableName;
            }
        } catch (Exception e) {
            // 某些数据库驱动可能不支持 getTableName
        }
        return null;
    }
    

    /**
     * 解析 JSON 字符串为对象
     */
    @SuppressWarnings("unchecked")
    private T parseJson(Object value) throws SQLException {
        if (value == null) {
            return null;
        }

        String jsonString;
        if (value instanceof PGobject) {
            jsonString = ((PGobject) value).getValue();
        } else {
            jsonString = value.toString();
        }

        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        try {
            if (typeReference != null) {
                // 优先使用：支持完整泛型信息
                return objectMapper.readValue(jsonString, typeReference);
            } else if (fallbackType != null) {
                // 兜底：使用构造函数传入的类型（不支持泛型参数）
                // 注意：对于泛型类型（如 List<UserInfo>），会被擦除为原始类型（List）
                // 反序列化后的元素类型可能不正确（如 List<UserInfo> 可能变成 List<LinkedHashMap>）
                return objectMapper.readValue(jsonString, fallbackType);
            } else {
                // 最后兜底：返回原始 JSON 字符串
                // 注意：这会导致类型不匹配，但不会抛出异常
                return (T) jsonString;
            }
        } catch (Exception e) {
            throw new SQLException("Error parsing JSON string to object: " + jsonString, e);
        }
    }
}
