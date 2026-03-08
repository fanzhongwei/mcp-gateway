package com.mmyf.commons.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.mmyf.commons.model.entity.IEntity;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * package com.mmyf.commons.mybatis
 * description: 增量字段填充
 * Copyright 2024 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2024-06-18 23:08:47
 */
@Component
public class IncrementFieldMetaHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        Object obj = metaObject.getOriginalObject();
        if (!(obj instanceof IEntity)) {
            return;
        }
        IEntity entity = (IEntity) obj;
        entity.setCreateTime(LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Object obj = metaObject.getOriginalObject();
        if (!(obj instanceof IEntity)) {
            return;
        }
        IEntity entity = (IEntity) obj;
        entity.setModifyTime(LocalDateTime.now());
    }
}
