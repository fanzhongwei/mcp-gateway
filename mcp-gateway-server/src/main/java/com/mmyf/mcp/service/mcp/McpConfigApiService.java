package com.mmyf.mcp.service.mcp;

import com.mmyf.mcp.model.entity.mcp.McpConfigApi;
import com.mmyf.mcp.mapper.mcp.McpConfigApiMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * mcp服务配置API接口 服务实现类
 * </p>
 *
 * @author 超级管理员
 * @since 2026-01-26 14:56:34
 */
@Service
@Slf4j
public class McpConfigApiService extends ServiceImpl<McpConfigApiMapper, McpConfigApi> implements IService<McpConfigApi> {

}
