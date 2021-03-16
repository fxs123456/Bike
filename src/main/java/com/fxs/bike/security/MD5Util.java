package com.fxs.bike.security;


import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {

    public static String getMD5(String source) {
        return DigestUtils.md5Hex(source);
    }

    public static void main(String[] args) {
        String s = "b405c640d6f444f3294a5afa92cb43ec" + "51cbec44d59bab2ce87ad6dfe69b5b2e" + "1610105000";
        System.out.println(getMD5(s));
    }
}
