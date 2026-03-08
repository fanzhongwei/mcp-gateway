package com.mmyf.commons.model.entity.log;

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
 * 用户权限
 * </p>
 *
 * @author mmyf
 * @since 2024-03-03
 */
@Getter
@Setter
@TableName("system_base.t_api_log")
@Schema(name = "ApiLog对象", description = "用户权限")
@DeepTranslate
public class ApiLog implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "访问IP，新增或修改时不能为空", required = true)
    @NotEmpty(message = "访问IP，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_ip")
    private String ip;

    @Schema(description = "访问API的用户ID，新增或修改时不能为空", required = true)
    @NotEmpty(message = "访问API的用户ID，新增或修改时不能为空", groups = {Default.class})
    @TableField("c_user")
    private String user;

    @Schema(description = "用户名")
    @TableField("c_user_name")
    private String userName;

    @Schema(description = "所属租户")
    @TableField("c_tenant")
    private String tenant;

    @Schema(description = "访问模块")
    @TableField("c_module")
    private String module;

    @Schema(description = "访问的api地址")
    @TableField("c_api")
    private String api;

    @Schema(description = "接口名")
    @TableField("c_api_name")
    private String apiName;

    @Schema(description = "请求内容")
    @TableField("c_request")
    private String request;

    @Schema(description = "请求状态")
    @TableField("c_status")
    private String status;

    @Schema(description = "请求结果")
    @TableField("c_response")
    private String response;

    @Schema(description = "请求开始时间")
    @TableField("dt_request_start_time")
    private LocalDateTime requestStartTime;

    @Schema(description = "请求结束时间")
    @TableField("dt_request_end_time")
    private LocalDateTime requestEndTime;

    @Schema(description = "请求耗时")
    @TableField("n_request_times")
    private Long requestTimes;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @TableField(exist = false)
    private LocalDateTime modifyTime;
}
