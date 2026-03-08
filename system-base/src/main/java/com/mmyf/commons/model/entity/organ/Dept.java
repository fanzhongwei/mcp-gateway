package com.mmyf.commons.model.entity.organ;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mmyf.commons.constant.SystemBaseConstant;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.translate.annotation.TranslateCode;
import com.mmyf.commons.translate.annotation.TranslateTenant;
import com.mmyf.commons.translate.annotation.TranslateDept;
import com.mmyf.commons.validator.CodeValidate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.groups.Default;

/**
 * <p>
 * 部门
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Getter
@Setter
@TableName("system_base.t_dept")
@Schema(name = "Dept对象", description = "部门")
public class Dept implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键", required = false)
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "外部租户映射")
    @TableField("n_external_nid")
    private Long externalNid;

    @TableField("c_external_cid")
    @Schema(description = "外部租户映射")
    private String externalCid;

    @Schema(description = "名称，新增或修改时不能为空", required = true)
    @TableField("c_name")
    @NotEmpty(message = "部门名称，新增或修改时不能为空", groups = {Default.class})
    private String name;

    @Schema(description = "父ID")
    @TableField("c_pid")
    @TranslateDept
    private String pid;

    @Schema(description = "所属租户，新增或修改时不能为空", required = true)
    @TableField("c_tenant")
    @TranslateTenant
    @NotEmpty(message = "部门所属租户，新增或修改时不能为空", groups = {Default.class})
    private String tenant;

    @Schema(description = "别名")
    @TableField("c_alias")
    private String alias;

    @Schema(description = "是否有效，代码类型：100001")
    @CodeValidate(codeType = "100001", message = "是否有效，代码类型：100001，码值不合法", groups = {Default.class, PageParam.ValidateGroup.class})
    @TableField("c_valid")
    @TranslateCode(codeType = SystemBaseConstant.CODE_TYPE_SFXZ)
    private String valid;

    @Schema(description = "序号")
    @TableField("n_order")
    private Integer nOrder;

    @Schema(description = "扩展字段")
    @TableField("lc_ext")
    private String ext;

    @Schema(description = "创建时间")
    @TableField("dt_create_time")
    private LocalDateTime createTime;

    @Schema(description = "最后修改时间")
    @TableField("dt_last_modify")
    private LocalDateTime modifyTime;

}
