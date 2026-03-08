package com.mmyf.commons.util.uuid;

import java.util.UUID;
import org.apache.commons.codec.binary.Hex;

/**
 * package com.mmyf.commons.util.uuid <br/>
 * description: TODO <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/5/30
 */
public class UUIDHelper {
    /**
     * 得到uuid, 默认小写
     *
     * @return
     */
    public static String getUuid() {
        return getUuid(false);
    }

    /**
     * 得到uuid
     * @param upperCase 是否大写
     * @return
     */
    public static String getUuid(boolean upperCase) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return upperCase ? uuid.toUpperCase() : uuid;
    }

    /**
     * MD5摘要
     * @see  com.mmyf.commons.util.uuid.MD5Helper#encrypt
     * @param s 源字符串
     * @return
     */
    @Deprecated
    public static String digest(String s) {
        try {
            return MD5Helper.encrypt(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * byte数组转16进制字符串
     * @see  com.mmyf.commons.util.uuid.MD5Helper#byte2hex
     * @param data data
     * @return
     */
    @Deprecated
    public static String byte2hex(byte[] data) {
        return MD5Helper.byte2hex(data);
    }
    /**
     * byte数组转16进制char数组
     * @see org.apache.commons.codec.binary.Hex#encodeHex
     * @param data data
     * @return
     */
    @Deprecated
    public static char[] encodeHex(byte[] data) {
        return Hex.encodeHex(data);
    }
}
