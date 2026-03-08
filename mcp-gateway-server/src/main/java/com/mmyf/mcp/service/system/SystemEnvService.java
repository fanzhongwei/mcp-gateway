package com.mmyf.mcp.service.system;

import com.mmyf.mcp.model.entity.system.SystemEnv;
import com.mmyf.mcp.mapper.system.SystemEnvMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 系统环境 服务实现类
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-23 10:49:25
 */
@Service
@Slf4j
public class SystemEnvService extends ServiceImpl<SystemEnvMapper, SystemEnv> implements IService<SystemEnv> {

}
