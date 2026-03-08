package com.mmyf.commons.service.code;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmyf.commons.mapper.code.CodeTypeMapper;
import com.mmyf.commons.model.entity.code.CodeType;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 代码类型 服务实现类
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Service
public class CodeTypeService extends ServiceImpl<CodeTypeMapper, CodeType> implements IService<CodeType> {

}
