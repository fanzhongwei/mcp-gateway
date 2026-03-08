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
 * 业务系统
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-23 10:49:25
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_mcp.t_system")
@Schema(name = "System对象", description = "业务系统")
@DeepTranslate
public class System implements IEntity<String> {

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
     * 管理员
     */
    @TableField("c_admin")
    @Schema(description = "管理员")
    @Size(message = "管理员最大长度为300", max = 300)
    private String admin;

    /**
     * 管理员联系方式
     */
    @TableField("c_admin_contact")
    @Schema(description = "管理员联系方式")
    @Size(message = "管理员联系方式最大长度为300", max = 300)
    private String adminContact;

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
