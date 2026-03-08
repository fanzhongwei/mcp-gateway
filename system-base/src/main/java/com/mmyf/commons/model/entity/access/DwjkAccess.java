package com.mmyf.commons.model.entity.access;

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
 * 对外接口授权
 * </p>
 *
 * @author mmyf
 * @since 2024-03-03
 */
@Getter
@Setter
@TableName("system_base.t_dwjk_access")
@Schema(name = "DwjkAccess对象", description = "对外接口授权")
@DeepTranslate
public class DwjkAccess implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "所属租户，新增或修改时不能为空")
    @NotEmpty(message = "所属租户，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_tenant")
    private String tenant;

    @Schema(description = "系统名称，新增或修改时不能为空", required = true)
    @NotEmpty(message = "系统名称，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_system_name")
    private String systemName;

    @Schema(description = "系统授权")
    @TableField("c_access_token")
    private String accessToken;

    @Schema(description = "IP白名单，多个以;隔开，如果设置则仅允许白名单内的IP访问")
    @TableField("c_ip_whitelist")
    private String ipWhitelist;

    @Schema(description = "IP黑名单，多个以;隔开")
    @TableField("c_ip_blacklist")
    private String ipBlacklist;

    @Schema(description = "授权开始时间")
    @TableField("dt_access_time_start")
    private LocalDateTime accessTimeStart;

    @Schema(description = "授权结束时间")
    @TableField("dt_access_time_end")
    private LocalDateTime accessTimeEnd;

    @Schema(description = "系统联系人")
    @TableField("c_contact")
    private String contact;

    @Schema(description = "系统联系人电话")
    @TableField("c_contact_number")
    private String contactNumber;

    @Schema(description = "授权人，新增或修改时不能为空", required = true)
    @NotEmpty(message = "授权人，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_authorizer")
    private String authorizer;

    @Schema(description = "授权时间")
    @TableField("c_authorization_time")
    private LocalDateTime authorizationTime;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @TableField(exist = false)
    private LocalDateTime modifyTime;
}
