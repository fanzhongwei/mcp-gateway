package com.mmyf.commons.service.organ;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmyf.commons.mapper.organ.TenantMapper;
import com.mmyf.commons.model.entity.organ.Tenant;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户 服务实现类
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Service
public class TenantService extends ServiceImpl<TenantMapper, Tenant> implements IService<Tenant> {

}
