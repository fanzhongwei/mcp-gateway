package com.mmyf.mcp.model.entity.mcp;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.*;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.mybatis.PostgreSQLJsonTypeHandler;
import com.mmyf.commons.translate.annotation.DeepTranslate;
import com.mmyf.commons.translate.annotation.TranslateTenant;
import com.mmyf.commons.validator.groups.*;
import com.mmyf.mcp.model.dto.mcp.McpApiConfigItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

/**
 * <p>
 * mcp服务配置API接口
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-26 14:56:34
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "db_mcp.t_mcp_config_api", autoResultMap = true)
@Schema(name = "McpConfigApi对象", description = "mcp服务配置API接口")
@DeepTranslate
public class McpConfigApi implements IEntity<String> {

    private static final long serialVersionUID = 1L;


    /**
     * 主键
     */
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    @Schema(description = "主键，新增或修改时不能为空", required = true)
    @NotEmpty(message = "主键，新增或修改时不能为空", groups = {Update.class})
    @Size(message = "主键最大长度为32", max = 32)
    private String id;

    /**
     * 租户
     */
    @TableField("c_tenant")
    @Schema(description = "租户")
    @TranslateTenant
    private String tenant;

    /**
     * mcp服务ID
     */
    @TableField("c_mcp_service_id")
    @Schema(description = "mcp服务ID")
    @Size(message = "mcp服务ID最大长度为32", max = 32)
    private String mcpServiceId;

    /**
     * 业务系统ID
     */
    @TableField("c_system_id")
    @Schema(description = "业务系统ID")
    @Size(message = "业务系统ID最大长度为32", max = 32)
    private String systemId;

    @TableField("c_system_mcp_name")
    @Schema(description = "业务系统mcp名称, 未设置则由系统自动生成")
    @Size(message = "业务系统mcp名称最大长度为300", max = 300)
    private String systemMcpName;

    /**
     * 关联的 API 接口配置列表
     * <p>
     * JSON 结构：[{"apiId": "xxx", "customMcpName": "yyy"}, ...]
     * </p>
     *
     * @see McpApiConfigItem
     */
    @TableField(value = "j_api_config", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    @Schema(description = "关联的 API 接口配置列表")
    private List<McpApiConfigItem> apiConfig;

    /**
     * 关联的环境ID
     */
    @TableField("c_env_id")
    @Schema(description = "关联的环境ID")
    @Size(message = "关联的环境ID最大长度为32", max = 32)
    private String envId;

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
