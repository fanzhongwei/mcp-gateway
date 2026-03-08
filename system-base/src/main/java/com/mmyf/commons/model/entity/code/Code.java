package com.mmyf.commons.model.entity.code;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
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
 * 代码值
 * </p>
 *
 * @author mmyf
 * @since 2024-03-03
 */
@Getter
@Setter
@TableName("system_base.t_code")
@Schema(name = "Code对象", description = "代码值")
@DeepTranslate
public class Code implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "代码类型，新增或修改时不能为空")
    @NotEmpty(message = "代码类型，新增或修改时不能为空", groups = {Default.class})
    @TableField(value = "c_pid")
    @TranslateCode(codeType = "")
    private String pid;

    @Schema(description = "代码值，新增或修改时不能为空")
    @NotEmpty(message = "代码值，新增或修改时不能为空", groups = {Default.class})
    @TableField(value = "c_code")
    private String code;

    @Schema(description = "代码名称，新增或修改时不能为空")
    @NotEmpty(message = "代码名称，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_name")
    private String name;

    @Schema(description = "层级")
    @TableField("c_levelinfo")
    private String levelinfo;

    @Schema(description = "是否有效，代码类型：100001")
    @CodeValidate(codeType = "100001", message = "是否有效，代码类型：100001，码值不合法", groups = {Default.class, PageParam.ValidateGroup.class})
    @TableField("c_valid")
    private String valid;

    @Schema(description = "序号")
    @TableField("n_order")
    private Integer nOrder;

    @Schema(description = "代码简拼")
    @TableField("c_dmjp")
    private String dmjp;


}
