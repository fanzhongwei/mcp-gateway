package com.mmyf.mcp.model.entity.system;

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
 * 系统环境
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-23 10:49:25
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_mcp.t_system_env")
@Schema(name = "SystemEnv对象", description = "系统环境")
@DeepTranslate
public class SystemEnv implements IEntity<String> {

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
     * 名称
     */
    @TableField("c_name")
    @Schema(description = "名称")
    @Size(message = "名称最大长度为300", max = 300)
    private String name;

    /**
     * 简介
     */
    @TableField("c_desc")
    @Schema(description = "简介")
    @Size(message = "简介最大长度为300", max = 300)
    private String desc;

    /**
     * 系统基础地址
     */
    @TableField("c_base_url")
    @Schema(description = "系统基础地址")
    @Size(message = "系统基础地址最大长度为900", max = 900)
    private String baseUrl;

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
