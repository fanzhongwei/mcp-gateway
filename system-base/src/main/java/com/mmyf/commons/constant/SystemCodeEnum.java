package com.mmyf.commons.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * package com.mmyf.constant
 * description: TODO
 * Copyright 2022 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022-05-15 15:18:38
 */
public interface SystemCodeEnum {

    @AllArgsConstructor
    @Getter
    enum OrganType implements AbstractEnum {
        /**  租户 */
        TENANT("TENANT", "租户--租户"),
        /** 租户--部门 */
        DEPT("DEPT", "租户--部门"),
        /** 租户--用户 */
        USER("USER", "租户--用户");

        private final String code;
        private final String desc;
    }

    @AllArgsConstructor
    @Getter
    enum UserRigtType implements AbstractEnum {
        RIGHT("1", "权限"),

        ROLE("2", "角色");

        @EnumValue
        private final String code;
        private final String desc;
    }


    @Getter
    @AllArgsConstructor
    enum Sfxz implements AbstractEnum {
        /** 是 */
        S("1", "是"),
        /** 否 */
        F("2", "否");

        @EnumValue
        private final String code;
        private final String desc;
    }

    @Getter
    @AllArgsConstructor
    enum JwtTokenType {
        /** web请求token */
        WEB_TOKEN("1", "web请求token"),
        /** 对外接口token */
        DWJK_TOKEN("2", "对外接口token");

        private final String code;
        private final String desc;
    }
}
