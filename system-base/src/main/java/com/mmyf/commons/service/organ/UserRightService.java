package com.mmyf.commons.service.organ;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmyf.commons.mapper.organ.UserRightMapper;
import com.mmyf.commons.model.entity.organ.UserRight;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户权限 服务实现类
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Service
public class UserRightService extends ServiceImpl<UserRightMapper, UserRight> implements IService<UserRight> {

}
