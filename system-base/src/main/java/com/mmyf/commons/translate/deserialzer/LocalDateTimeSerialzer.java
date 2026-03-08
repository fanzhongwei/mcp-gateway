package com.mmyf.commons.translate.deserialzer;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * package com.mmyf.commons.translate.deserialzer
 * description: TODO
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-24 22:36:58
 */
public class LocalDateTimeSerialzer implements ObjectSerializer {
        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features){
            if(object !=null){
                if(object instanceof LocalDateTime){
                    LocalDateTime localDateTime =(LocalDateTime) object;
                    // 将localDateTime转换为中国区（+8）时间戳。
                    serializer.write(localDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
                    return;
                }
                if(object instanceof Date){
                    serializer.write(((Date) object).getTime());
                    return;
                }
                throw new RuntimeException("date type exception");
            }else{
                serializer.out.writeNull();
            }
        }
}
