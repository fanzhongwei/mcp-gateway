package com.mmyf.commons.service.access;

import com.mmyf.commons.model.entity.access.DwjkAccess;
import com.mmyf.commons.mapper.access.DwjkAccessMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 对外接口授权 服务实现类
 * </p>
 *
 * @author mmyf
 * @since 2024-02-28
 */
@Service
@Slf4j
public class DwjkAccessService extends ServiceImpl<DwjkAccessMapper, DwjkAccess> implements IService<DwjkAccess> {

}
