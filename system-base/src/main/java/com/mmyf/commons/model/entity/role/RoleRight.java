package com.mmyf.commons.model.entity.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.mmyf.commons.model.entity.IEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;

import com.mmyf.commons.translate.annotation.DeepTranslate;
import com.mmyf.commons.translate.annotation.TranslateCode;

/**
 * <p>
 * 角色权限映射
 * </p>
 *
 * @author mmyf
 * @since 2024-03-03
 */
@Getter
@Setter
@TableName("system_base.t_role_right")
@Schema(name = "RoleRight对象", description = "角色权限映射")
@DeepTranslate
public class RoleRight implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "角色ID，新增或修改时不能为空", required = true)
    @NotEmpty(message = "角色ID，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_role_id")
    private String roleId;

    @Schema(description = "权限字，新增或修改时不能为空", required = true)
    @NotEmpty(message = "权限字，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_rightkey")
    private String rightkey;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @TableField(exist = false)
    private LocalDateTime modifyTime;
}
