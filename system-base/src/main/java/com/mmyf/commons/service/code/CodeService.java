package com.mmyf.commons.service.code;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmyf.commons.mapper.code.CodeMapper;
import com.mmyf.commons.model.entity.code.Code;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 代码值 服务实现类
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Service
public class CodeService extends ServiceImpl<CodeMapper, Code> implements IService<Code> {

}
