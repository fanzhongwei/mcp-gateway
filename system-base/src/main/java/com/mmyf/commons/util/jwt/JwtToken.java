package com.mmyf.commons.util.jwt;

import com.mmyf.commons.constant.SystemCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.shiro.authc.HostAuthenticationToken;

/**
 * package com.mmyf.commons.util.jwt <br/>
 * description: JwtToken <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
@Data
@AllArgsConstructor
public class JwtToken implements HostAuthenticationToken {
    private String token;
    private String hots;

    private SystemCodeEnum.JwtTokenType type;

    public JwtToken(String token, SystemCodeEnum.JwtTokenType type) {
        this(token, null, type);
    }

    @Override
    public String getHost() {
        return this.hots;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
