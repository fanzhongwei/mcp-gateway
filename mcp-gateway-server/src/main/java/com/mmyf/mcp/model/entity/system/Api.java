package com.mmyf.mcp.model.entity.system;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.mybatis.PostgreSQLJsonTypeHandler;
import com.mmyf.mcp.model.dto.api.*;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

import jakarta.validation.constraints.*;
import com.mmyf.commons.validator.groups.*;
import com.mmyf.commons.translate.annotation.DeepTranslate;
import com.mmyf.commons.translate.annotation.TranslateTenant;

/**
 * <p>
 * API接口表
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-24 17:29:26
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "db_mcp.t_api", autoResultMap = true)
@Schema(name = "Api对象", description = "API接口表")
@DeepTranslate
public class Api implements IEntity<String> {

    private static final long serialVersionUID = 1L;


    /**
     * 编号
     */
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    @Schema(description = "编号，新增或修改时不能为空", required = true)
    @NotEmpty(message = "编号，新增或修改时不能为空", groups = {Update.class})
    @Size(message = "编号最大长度为32", max = 32)
    private String id;

    /**
     * 租户
     */
    @TableField("c_tenant")
    @Schema(description = "租户")
    @TranslateTenant
    private String tenant;

    /**
     * 所属系统
     */
    @TableField("c_system_id")
    @Schema(description = "所属系统")
    @Size(message = "所属系统最大长度为32", max = 32)
    private String systemId;

    /**
     * HTTP方法
     */
    @TableField("c_method")
    @Schema(description = "HTTP方法")
    @Size(message = "HTTP方法最大长度为100", max = 100)
    private String method;

    /**
     * 接口路径
     */
    @TableField("c_path")
    @Schema(description = "接口路径")
    @Size(message = "接口路径最大长度为500", max = 500)
    private String path;

    /**
     * 接口名称
     */
    @TableField("c_name")
    @Schema(description = "接口名称")
    @Size(message = "接口名称最大长度为900", max = 900)
    private String name;

    /**
     * 接口描述
     */
    @TableField("c_description")
    @Schema(description = "接口描述")
    private String description;

    /**
     * Query参数列表（数组）
     * 
     * @see QueryParam
     */
    @TableField(value = "j_query_params", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    @Schema(description = "Query参数列表（数组）")
    private List<QueryParam> queryParams;

    /**
     * 请求头列表（数组）
     * 
     * @see Header
     */
    @TableField(value = "j_headers", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    @Schema(description = "请求头列表（数组）")
    private List<Header> headers;

    /**
     * Body参数
     * 
     * @see BodyParam
     */
    @TableField(value = "j_body_param", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    @Schema(description = "Body参数")
    private BodyParam bodyParam;

    /**
     * URL编码参数列表（数组）
     * 
     * @see UrlEncodedParam
     */
    @TableField(value = "j_url_encoded_params", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    @Schema(description = "URL编码参数列表（数组）")
    private List<UrlEncodedParam> urlEncodedParams;

    /**
     * Cookies列表（数组）
     * 
     * @see Cookie
     */
    @TableField(value = "j_cookies", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    @Schema(description = "Cookies列表（数组）")
    private List<Cookie> cookies;

    /**
     * 响应配置（对象）
     * 
     * @see ResponseConfig
     */
    @TableField(value = "j_response_config", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    @Schema(description = "响应配置（对象）")
    private ResponseConfig responseConfig;

    /**
     * 创建时间
     */
    @TableField(value = "dt_create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(value = "dt_modify_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "最后修改时间")
    private LocalDateTime modifyTime;

}
