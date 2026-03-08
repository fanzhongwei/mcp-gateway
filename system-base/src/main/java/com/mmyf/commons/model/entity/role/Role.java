package com.mmyf.commons.model.entity.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.validator.CodeValidate;
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
 * 角色定义
 * </p>
 *
 * @author mmyf
 * @since 2024-03-03
 */
@Getter
@Setter
@TableName("system_base.t_role")
@Schema(name = "Role对象", description = "角色定义")
@DeepTranslate
public class Role implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "角色名称，新增或修改时不能为空", required = true)
    @NotEmpty(message = "角色名称，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_name")
    private String name;

    @Schema(description = "角色描述")
    @TableField("c_descript")
    private String descript;

    @Schema(description = "是否有效，代码类型：100001")
    @CodeValidate(codeType = "100001", message = "是否有效，代码类型：100001，码值不合法", groups = {Default.class, PageParam.ValidateGroup.class})
    @TableField("c_valid")
    private String valid;

    @Schema(description = "显示顺序")
    @TableField("n_order")
    private Integer nOrder;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @TableField(exist = false)
    private LocalDateTime modifyTime;
}
