package com.mmyf.mcp.service.system;

import com.mmyf.mcp.model.entity.system.Api;
import com.mmyf.mcp.mapper.system.ApiMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * API接口表 服务实现类
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-24 17:29:26
 */
@Service
@Slf4j
public class ApiService extends ServiceImpl<ApiMapper, Api> implements IService<Api> {

}
