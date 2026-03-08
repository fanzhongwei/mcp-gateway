package com.mmyf.commons.service.organ;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmyf.commons.mapper.organ.DeptMapper;
import com.mmyf.commons.model.entity.organ.Dept;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 部门 服务实现类
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Service
public class DeptService extends ServiceImpl<DeptMapper, Dept> implements IService<Dept> {

}
