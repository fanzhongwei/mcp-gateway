package com.mmyf.commons.service.log;

import com.mmyf.commons.model.entity.log.ApiLog;
import com.mmyf.commons.mapper.log.ApiLogMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户权限 服务实现类
 * </p>
 *
 * @author Teddy
 * @since 2022-05-29
 */
@Service
public class ApiLogService extends ServiceImpl<ApiLogMapper, ApiLog> implements IService<ApiLog> {

}
