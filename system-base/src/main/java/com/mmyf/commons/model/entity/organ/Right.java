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
 * 权限定义
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Getter
@Setter
@TableName("system_base.t_right")
@Schema(name = "Right对象", description = "权限定义")
public class Right implements IEntity<String> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "权限KEY，新增或修改时不能为空", required = true)
    @TableId(value = "c_rightkey", type = IdType.ASSIGN_UUID)
    @NotEmpty(message = "权限KEY，新增或修改时不能为空", groups = {Default.class})
    private String rightkey;

    @Schema(description = "名称，新增或修改时不能为空", required = true)
    @TableField("c_name")
    @NotEmpty(message = "权限名称，新增或修改时不能为空", groups = {Default.class})
    private String name;

    @Schema(description = "描述")
    @TableField("c_descript")
    private String descript;

    @Schema(description = "序号")
    @TableField("n_order")
    private Integer nOrder;

    @Schema(description = "创建时间")
    @TableField(exist = false)
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @TableField(exist = false)
    private LocalDateTime modifyTime;

    @Override
    public String getId() {
        return this.getRightkey();
    }

    @Override
    public void setId(String s) {
        this.setRightkey(s);
    }
}
