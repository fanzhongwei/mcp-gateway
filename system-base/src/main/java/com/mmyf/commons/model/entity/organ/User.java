package com.mmyf.commons.model.entity.organ;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.mmyf.commons.constant.SystemBaseConstant;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.mybatis.PostgreSQLJsonTypeHandler;
import com.mmyf.commons.translate.annotation.TranslateCode;
import com.mmyf.commons.translate.annotation.TranslateTenant;
import com.mmyf.commons.translate.annotation.TranslateDept;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.type.JdbcType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.groups.Default;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Getter
@Setter
@TableName(value = "system_base.t_user", autoResultMap = true)
@Schema(name = "User对象", description = "用户")
public class User implements IEntity<String> {

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

    @Schema(description = "登录名，新增或修改时不能为空", required = true)
    @TableField("c_loginid")
    @NotEmpty(message = "用户登录名，新增或修改时不能为空", groups = {Default.class})
    private String loginid;

    @Schema(description = "姓名，新增或修改时不能为空", required = true)
    @TableField("c_name")
    @NotEmpty(message = "用户姓名，新增或修改时不能为空", groups = {Default.class})
    private String name;

    @Schema(description = "密码")
    @TableField("c_password")
    @JSONField(serialize = false)
    private String password;

    @Schema(description = "邮箱")
    @TableField("c_mail")
    private String mail;

    @Schema(description = "IP地址")
    @TableField("c_ip")
    private String ip;

    @Schema(description = "所属租户，新增或修改时不能为空", required = true)
    @TableField("c_tenant")
    @TranslateTenant
    @NotEmpty(message = "用户所属租户，新增或修改时不能为空", groups = {Default.class})
    private String tenant;

    @Schema(description = "所属部门")
    @TableField("c_dept")
    @TranslateDept
    private String dept;

    @Schema(description = "是否有效")
    @TableField("c_valid")
    @TranslateCode(codeType = SystemBaseConstant.CODE_TYPE_SFXZ)
    private String valid;

    @Schema(description = "序号")
    @TableField("n_order")
    private Integer nOrder;

    @Schema(description = "拥有的角色")
    @TableField(value = "j_role_id", jdbcType = JdbcType.OTHER, typeHandler = PostgreSQLJsonTypeHandler.class)
    private Set<String> roleId;

    @Schema(description = "扩展字段")
    @TableField("lc_ext")
    private String ext;

    @Schema(description = "创建时间")
    @TableField("dt_create_time")
    private LocalDateTime createTime;

    @Schema(description = "最后修改时间")
    @TableField("dt_last_modify")
    private LocalDateTime modifyTime;

    @Schema(description = "用户拥有的权限")
    @TableField(exist = false)
    private Set<String> rightList;

    @Schema(description = "用户类型，代码类型100002")
    @TableField("c_type")
    @TranslateCode(codeType = SystemBaseConstant.CODE_TYPE_YHLX)
    private String userType;
}
