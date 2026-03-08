package com.mmyf.mcp.model.entity.mcp;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.model.vo.PageParam;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;
import com.mmyf.commons.validator.groups.*;
import com.mmyf.commons.translate.annotation.DeepTranslate;
import com.mmyf.commons.translate.annotation.TranslateCode;
import com.mmyf.commons.translate.annotation.TranslateTenant;
import com.mmyf.commons.validator.CodeValidate;

/**
 * <p>
 * mcp服务
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-26 15:01:45
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_mcp.t_mcp_service")
@Schema(name = "McpService对象", description = "mcp服务")
@DeepTranslate
public class McpService implements IEntity<String> {

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
     * 服务名
     */
    @TableField("c_name")
    @Schema(description = "服务名")
    @Size(message = "服务名最大长度为300", max = 300)
    private String name;

    /**
     * 服务描述
     */
    @TableField("c_desc")
    @Schema(description = "服务描述")
    private String desc;

    /**
     * 服务端点
     */
    @TableField("c_endpoint")
    @Schema(description = "服务端点")
    @Size(message = "服务端点最大长度为300", max = 300)
    private String endpoint;

    /**
     * 接口数量
     */
    @TableField("n_api_count")
    @Schema(description = "接口数量")
    private Integer apiCount;

    /**
     * 服务状态
     */
    @TableField("c_status")
    @Schema(description = "服务状态")
    @Size(message = "服务状态最大长度为300", max = 300)
    private String status;

    /**
     * 访问令牌
     */
    @TableField("c_access_token")
    @Schema(description = "访问令牌")
    @Size(message = "访问令牌最大长度为300", max = 300)
    private String accessToken;

    /**
     * 发布时间
     */
    @TableField("dt_publish_time")
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

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
