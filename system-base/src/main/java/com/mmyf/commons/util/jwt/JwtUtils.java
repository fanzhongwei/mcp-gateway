package com.mmyf.commons.util.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mmyf.commons.util.lang.EncryptUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;

/**
 * package com.mmyf.commons.util.jwt <br/>
 * description: JWT <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
public class JwtUtils {
    public static final String SECRET = "SECRET_VALUE";
    public static final String AUTH_HEADER = "Authorization";

    public static final String AUTH_INTERFACE_HEADER = "AccessToken";

    public JwtUtils() {
    }

    public static boolean verify(String token, String userid, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("userid", userid).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException var5) {
            return false;
        }
    }

    public static String getClaimFiled(String token, String filed) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim(filed).asString();
        } catch (JWTDecodeException var3) {
            return null;
        }
    }

    public static String getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("userid").asString();
        } catch (JWTDecodeException var2) {
            return null;
        }
    }

    public static String sign(String userid, String secret, Long expireSeconds) {
        try {
            Date date = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create().withClaim("userid", userid).withExpiresAt(date).sign(algorithm);
        } catch (JWTCreationException var4) {
            return null;
        }
    }

    public static Date getIssuedAt(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getIssuedAt();
        } catch (JWTDecodeException var2) {
            return null;
        }
    }

    public static boolean isTokenExpired(String token) {
        Date now = Calendar.getInstance().getTime();
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getExpiresAt().before(now);
    }

    public static String refreshTokenExpired(String token, String secret, Long expireSeconds) {
        DecodedJWT jwt = JWT.decode(token);
        Map claims = jwt.getClaims();

        try {
            Date date = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTCreator.Builder builer = JWT.create();
            Iterator var7 = claims.entrySet().iterator();

            while(var7.hasNext()) {
                Map.Entry<String, Claim> entry = (Map.Entry)var7.next();
                builer.withClaim(entry.getKey(), entry.getValue().asString());
            }

            return builer.withExpiresAt(date).sign(algorithm);
        } catch (JWTCreationException var9) {
            return null;
        }
    }

    public static String generateSalt() {
        SecureRandomNumberGenerator secureRandom = new SecureRandomNumberGenerator();
        String hex = secureRandom.nextBytes(16).toHex();
        return hex;
    }

    public static String generateSalt(String slat) {
        return EncryptUtils.encrypt(slat);
    }
}
