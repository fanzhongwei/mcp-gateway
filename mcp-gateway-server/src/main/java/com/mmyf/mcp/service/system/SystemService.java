package com.mmyf.mcp.service.system;

import com.mmyf.mcp.model.entity.system.System;
import com.mmyf.mcp.mapper.system.SystemMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 业务系统 服务实现类
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-23 10:49:25
 */
@Service
@Slf4j
public class SystemService extends ServiceImpl<SystemMapper, System> implements IService<System> {

}
