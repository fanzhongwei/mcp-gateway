package com.mmyf.commons.model.entity.organ;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mmyf.commons.model.entity.IEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.groups.Default;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户权限
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Getter
@Setter
@TableName("system_base.t_user_right")
@Schema(name = "UserRight对象", description = "用户权限")
public class UserRight implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键", required = false)
    @TableId(value = "c_id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "用户ID，新增或修改时不能为空", required = true)
    @TableField("c_userid")
    @NotEmpty(message = "用户ID，新增或修改时不能为空", groups = {Default.class})
    private String userid;

    @Schema(description = "类型")
    @TableField("c_type")
    private String type;

    @Schema(description = "角色ID")
    @TableField("c_roleid")
    private String roleid;

    @Schema(description = "权限key，新增或修改时不能为空", required = true)
    @TableField("c_rightkey")
    @NotEmpty(message = "权限KEY，新增或修改时不能为空", groups = {Default.class})
    private String rightkey;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @TableField(exist = false)
    private LocalDateTime modifyTime;
}
