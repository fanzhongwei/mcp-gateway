package com.mmyf.commons.model.entity.code;

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
 * 代码类型
 * </p>
 *
 * @author mmyf
 * @since 2024-03-03
 */
@Getter
@Setter
@TableName("system_base.t_code_type")
@Schema(name = "CodeType对象", description = "代码类型")
@DeepTranslate
public class CodeType implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "代码类型ID")
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    @TranslateCode(codeType = "ID")
    private String id;

    @Schema(description = "名称")
    @TableField("c_name")
    private String name;

    @Schema(description = "是否有效，代码类型100001")
    @CodeValidate(codeType = "100001", message = "是否有效，代码类型：100001，码值不合法", groups = {Default.class, PageParam.ValidateGroup.class})
    @TableField("c_valid")
    private String valid;

    @Schema(description = "是否可编辑，代码类型100001")
    @CodeValidate(message = "是否可编辑，代码类型：100001，码值不合法", groups = {Default.class, PageParam.ValidateGroup.class})
    @TableField("c_editable")
    private String editable;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @TableField(exist = false)
    private LocalDateTime modifyTime;
}
