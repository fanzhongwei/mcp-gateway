package com.mmyf.commons.util.uuid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;

/**
 * package com.mmyf.commons.util.uuid <br/>
 * description: TODO <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/5/30
 */
public class MD5Helper {

    public static String encrypt(String str) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        md5.update(str.getBytes());
        byte[] abResult = md5.digest();

        return byte2hex(abResult);
    }

    public static String encrypt(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        md5.update(data);
        byte[] abResult = md5.digest();

        return byte2hex(abResult);
    }

    public static String byte2hex(byte[] data) {
        if (data == null) {
            return null;
        }
        return new String(Hex.encodeHex(data));
    }

}
