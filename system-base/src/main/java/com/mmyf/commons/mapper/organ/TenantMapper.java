package com.mmyf.commons.mapper.organ;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmyf.commons.model.entity.organ.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 租户 Mapper 接口
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

}
